package org.ewsk.searchingsafebox.manager

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.ewsk.searchingsafebox.api.SafeBoxMessages
import org.ewsk.searchingsafebox.api.unlock.UnlockContext
import org.ewsk.searchingsafebox.bridge.SearchingOpenBridge
import org.ewsk.searchingsafebox.config.SafeBoxConfig
import org.ewsk.searchingsafebox.config.SafeBoxRule
import org.ewsk.searchingsafebox.event.SafeBoxAutoUnlockCompleteEvent
import org.ewsk.searchingsafebox.event.SafeBoxOpenOriginalEvent
import org.ewsk.searchingsafebox.event.SafeBoxUnlockCancelEvent
import org.ewsk.searchingsafebox.event.SafeBoxUnlockFailEvent
import org.ewsk.searchingsafebox.event.SafeBoxUnlockStartEvent
import org.ewsk.searchingsafebox.event.SafeBoxUnlockSuccessEvent
import org.ewsk.searchingsafebox.registry.UnlockUIRegistry
import org.ewsk.searchingsafebox.session.UnlockSession
import org.ewsk.searchingsafebox.session.UnlockSessionStore
import org.ewsk.searchingsafebox.session.UnlockState
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SafeBoxManager(
    private val config: SafeBoxConfig,
    private val sessions: UnlockSessionStore,
    private val uiRegistry: UnlockUIRegistry,
    private var bridge: SearchingOpenBridge,
    private val autoUnlockService: AutoUnlockService
) {
    private val cooldowns = ConcurrentHashMap<String, Long>()
    private val timeoutTasks = ConcurrentHashMap<Int, PlatformExecutor.PlatformTask>()
    private val openingOriginal = ConcurrentHashMap.newKeySet<String>()
    private val nextTaskId = AtomicInteger(1)

    fun isSafeBox(nodeTypeId: String): Boolean {
        return config.getRule(nodeTypeId)?.enabled == true
    }

    fun requiresUnlock(node: Any): Boolean {
        return bridge.requiresUnlock(node)
    }

    fun setBridge(bridge: SearchingOpenBridge) {
        this.bridge = bridge
    }

    fun isOpeningOriginal(playerId: UUID, node: Any): Boolean {
        return openingOriginalKey(playerId, node) in openingOriginal
    }

    fun handlePreOpen(player: Player, node: Any, rule: SafeBoxRule) {
        if (rule.permission != null && !player.hasPermission(rule.permission)) {
            SafeBoxMessages.send(player, "&c你没有权限打开这个保险箱。")
            return
        }
        val existing = sessions.findByPlayer(player.uniqueId)
        if (existing != null) {
            SafeBoxMessages.send(player, "&c你已经在破译一个保险箱。")
            return
        }
        val now = System.currentTimeMillis()
        val cooldownKey = cooldownKey(player.uniqueId, nodeKey(node))
        val cooldownUntil = cooldowns[cooldownKey] ?: 0L
        if (cooldownUntil > now) {
            val seconds = ((cooldownUntil - now) + 999L) / 1000L
            SafeBoxMessages.send(player, "&c请等待 ${seconds}s 后再尝试。")
            return
        }
        if (rule.cooldownSeconds > 0) {
            cooldowns[cooldownKey] = now + rule.cooldownSeconds * 1000L
        }
        SafeBoxMessages.sendRule(player, rule, "locked", "&c这个保险箱被加密保护，需要先破译。")

        val session = UnlockSession(
            sessionId = UUID.randomUUID(),
            player = player.uniqueId,
            nodeTypeId = rule.nodeTypeId,
            nodeRef = node,
            rule = rule,
            startTime = now,
            expiresAt = now + rule.unlockTimeoutSeconds * 1000L,
            state = UnlockState.CREATED
        )
        sessions.add(session)
        if (!SafeBoxUnlockStartEvent(player, session, rule).call()) {
            cancel(session, "start-event-cancelled")
            return
        }

        val context = DefaultUnlockContext(player, session)
        session.state = UnlockState.OPENING_UI
        val opened = uiRegistry.open(rule.unlockUi, context) || uiRegistry.open("default", context)
        if (!opened) {
            fail(session, "missing-ui:${rule.unlockUi}")
            return
        }
        session.state = UnlockState.UNLOCKING
        scheduleTimeout(session)
        autoUnlockService.start(context) {
            autoUnlockComplete(session)
        }
    }

    fun success(session: UnlockSession) {
        val player = onlinePlayer(session) ?: return cleanup(session)
        if (!session.isActive()) {
            return
        }
        session.state = UnlockState.SUCCESS
        cancelTimeout(session)
        SafeBoxUnlockSuccessEvent(player, session, session.rule).call()
        SafeBoxMessages.sendRule(player, session.rule, "unlocked", "&a破译成功，正在打开保险箱。")
        autoUnlockService.cancel(session)
        openOriginal(session)
    }

    fun fail(session: UnlockSession, reason: String? = null) {
        val player = onlinePlayer(session)
        session.state = UnlockState.FAILED
        autoUnlockService.cancel(session)
        cancelTimeout(session)
        if (player != null) {
            SafeBoxUnlockFailEvent(player, session, session.rule, reason).call()
            SafeBoxMessages.sendRule(player, session.rule, "failed", "&c破译失败。")
        }
        cleanup(session)
    }

    fun cancel(session: UnlockSession, reason: String? = null) {
        val player = onlinePlayer(session)
        session.state = UnlockState.CANCELLED
        autoUnlockService.cancel(session)
        cancelTimeout(session)
        if (player != null) {
            SafeBoxUnlockCancelEvent(player, session, session.rule, reason).call()
        }
        cleanup(session)
    }

    fun cancelByPlayer(playerId: UUID, reason: String? = null): Boolean {
        val session = sessions.findByPlayer(playerId) ?: return false
        cancel(session, reason)
        return true
    }

    fun clearSessions(reason: String? = null) {
        sessions.all().forEach { cancel(it, reason) }
        autoUnlockService.cancelAll()
        timeoutTasks.values.forEach { it.cancel() }
        timeoutTasks.clear()
    }

    fun activeSessions(): List<UnlockSession> {
        return sessions.all()
    }

    private fun autoUnlockComplete(session: UnlockSession) {
        if (!session.isActive() && session.state != UnlockState.SUCCESS) {
            return
        }
        val player = onlinePlayer(session) ?: return cleanup(session)
        session.state = UnlockState.AUTO_UNLOCKED
        SafeBoxAutoUnlockCompleteEvent(player, session, session.rule).call()
        SafeBoxMessages.sendRule(player, session.rule, "auto-unlocked", "&a自动破译完成。")
        openOriginal(session)
    }

    private fun openOriginal(session: UnlockSession) {
        val player = onlinePlayer(session) ?: return cleanup(session)
        cancelTimeout(session)
        val openEvent = SafeBoxOpenOriginalEvent(player, session, session.rule, session.nodeRef)
        if (!openEvent.call()) {
            cancel(session, "open-original-cancelled")
            return
        }
        val key = openingOriginalKey(player.uniqueId, session.nodeRef)
        openingOriginal += key
        try {
            bridge.openOriginal(player, session.nodeRef)
        } finally {
            openingOriginal -= key
        }
        session.state = UnlockState.OPENED
        cleanup(session)
    }

    private fun scheduleTimeout(session: UnlockSession) {
        val taskId = nextTaskId.getAndIncrement()
        val delayTicks = session.rule.unlockTimeoutSeconds.coerceAtLeast(1) * 20L
        val task = submit(delay = delayTicks) {
            if (sessions.get(session.sessionId) != null && session.isActive()) {
                session.state = UnlockState.EXPIRED
                cancel(session, "timeout")
            }
        }
        timeoutTasks[taskId] = task
        session.timeoutTaskId = taskId
    }

    private fun cancelTimeout(session: UnlockSession) {
        val taskId = session.timeoutTaskId ?: return
        timeoutTasks.remove(taskId)?.cancel()
        session.timeoutTaskId = null
    }

    private fun cleanup(session: UnlockSession) {
        cancelTimeout(session)
        autoUnlockService.cancel(session)
        sessions.remove(session.sessionId)
    }

    private fun onlinePlayer(session: UnlockSession): Player? {
        return Bukkit.getPlayer(session.player)
    }

    private fun cooldownKey(player: UUID, nodeKey: String): String {
        return "$player:$nodeKey"
    }

    private fun openingOriginalKey(player: UUID, node: Any): String {
        return cooldownKey(player, nodeKey(node))
    }

    private fun nodeKey(node: Any): String {
        return runCatching {
            val id = node.javaClass.methods.firstOrNull { it.name == "getId" && it.parameterCount == 0 }?.invoke(node)
            id?.toString()
        }.getOrNull() ?: node.toString()
    }

    private inner class DefaultUnlockContext(
        override val player: Player,
        override val session: UnlockSession
    ) : UnlockContext {
        override val rule: SafeBoxRule
            get() = session.rule

        override fun success() {
            this@SafeBoxManager.success(session)
        }

        override fun fail(reason: String?) {
            this@SafeBoxManager.fail(session, reason)
        }

        override fun cancel(reason: String?) {
            this@SafeBoxManager.cancel(session, reason)
        }
    }
}

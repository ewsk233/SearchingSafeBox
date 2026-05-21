package org.ewsk.searchingsafebox.command

import org.bukkit.Bukkit
import org.ewsk.searchingsafebox.SearchingSafeBox
import org.ewsk.searchingsafebox.api.SafeBoxMessages
import org.ewsk.searchingsafebox.config.SafeBoxConfigSource
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import java.util.UUID

@CommandHeader(
    name = "searchingsafebox",
    aliases = ["ssafebox", "safebox"],
    description = "SearchingSafeBox 命令。",
    permission = "searchingsafebox.command",
    permissionDefault = PermissionDefault.OP
)
object SearchingSafeBoxCommand {

    @CommandBody
    val main = mainCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            SafeBoxMessages.send(sender.cast(), "&7/searchingsafebox reload")
            SafeBoxMessages.send(sender.cast(), "&7/searchingsafebox debug <nodeTypeId>")
            SafeBoxMessages.send(sender.cast(), "&7/searchingsafebox ui list")
            SafeBoxMessages.send(sender.cast(), "&7/searchingsafebox session list")
            SafeBoxMessages.send(sender.cast(), "&7/searchingsafebox session clear <player>")
        }
    }

    @CommandBody
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            if (!sender.hasPermission("searchingsafebox.command.reload")) {
                SafeBoxMessages.send(sender.cast(), "&c你没有权限执行这个命令。")
                return@execute
            }
            SafeBoxConfigSource.config.reload()
            SearchingSafeBox.config.reload()
            SearchingSafeBox.manager.clearSessions("command-reload")
            SafeBoxMessages.send(sender.cast(), "&a已重载 ${SearchingSafeBox.config.allRules().size} 条保险箱规则。")
        }
    }

    @CommandBody
    val debug = subCommand {
        dynamic("nodeTypeId") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                SearchingSafeBox.config.allRules().map { it.nodeTypeId }
            }
            execute<ProxyCommandSender> { sender, context, _ ->
                if (!sender.hasPermission("searchingsafebox.command.debug")) {
                    SafeBoxMessages.send(sender.cast(), "&c你没有权限执行这个命令。")
                    return@execute
                }
                val nodeTypeId = context["nodeTypeId"]
                val rule = SearchingSafeBox.config.getRule(nodeTypeId)
                if (rule == null) {
                    SafeBoxMessages.send(sender.cast(), "&c没有找到 $nodeTypeId 对应的保险箱规则。")
                    return@execute
                }
                SafeBoxMessages.send(sender.cast(), "&f节点类型: &b${rule.nodeTypeId}")
                SafeBoxMessages.send(sender.cast(), "&f是否启用: &b${rule.enabled}")
                SafeBoxMessages.send(sender.cast(), "&f破译界面: &b${rule.unlockUi}")
                SafeBoxMessages.send(sender.cast(), "&f自动破译: &b${rule.autoUnlock} (${rule.autoUnlockSeconds}s)")
                SafeBoxMessages.send(sender.cast(), "&f超时时间: &b${rule.unlockTimeoutSeconds}s")
                SafeBoxMessages.send(sender.cast(), "&f冷却时间: &b${rule.cooldownSeconds}s")
                SafeBoxMessages.send(sender.cast(), "&f权限: &b${rule.permission ?: "-"}")
            }
        }
    }

    @CommandBody
    val ui = subCommand {
        literal("list") {
            execute<ProxyCommandSender> { sender, _, _ ->
                if (!sender.hasPermission("searchingsafebox.command.ui")) {
                    SafeBoxMessages.send(sender.cast(), "&c你没有权限执行这个命令。")
                    return@execute
                }
                val ids = SearchingSafeBox.uiRegistry.ids().joinToString(", ").ifBlank { "-" }
                SafeBoxMessages.send(sender.cast(), "&f已注册破译界面: &b$ids")
            }
        }
    }

    @CommandBody
    val session = subCommand {
        literal("list") {
            execute<ProxyCommandSender> { sender, _, _ ->
                if (!sender.hasPermission("searchingsafebox.command.session")) {
                    SafeBoxMessages.send(sender.cast(), "&c你没有权限执行这个命令。")
                    return@execute
                }
                val sessions = SearchingSafeBox.manager.activeSessions()
                SafeBoxMessages.send(sender.cast(), "&f活跃破译会话: &b${sessions.size}")
                sessions.forEach { session ->
                    SafeBoxMessages.send(
                        sender.cast(),
                        "&7${session.sessionId} &f玩家=&b${session.player} &f类型=&b${session.nodeTypeId} &f状态=&b${session.state}"
                    )
                }
            }
        }
        literal("clear") {
            dynamic("player") {
                execute<ProxyCommandSender> { sender, context, _ ->
                    if (!sender.hasPermission("searchingsafebox.command.session")) {
                        SafeBoxMessages.send(sender.cast(), "&c你没有权限执行这个命令。")
                        return@execute
                    }
                    val playerId = parsePlayer(context["player"])
                    if (playerId == null) {
                        SafeBoxMessages.send(sender.cast(), "&c未知玩家: ${context["player"]}")
                        return@execute
                    }
                    val cleared = SearchingSafeBox.manager.cancelByPlayer(playerId, "command-clear")
                    SafeBoxMessages.send(sender.cast(), if (cleared) "&a已清理破译会话。" else "&e没有活跃破译会话。")
                }
            }
        }
    }

    private fun parsePlayer(value: String): UUID? {
        return runCatching { UUID.fromString(value) }.getOrNull()
            ?: Bukkit.getPlayerExact(value)?.uniqueId
    }

    private fun ProxyCommandSender.cast(): org.bukkit.command.CommandSender {
        return origin as org.bukkit.command.CommandSender
    }
}

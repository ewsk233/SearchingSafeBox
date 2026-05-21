package org.ewsk.searchingsafebox

import org.ewsk.searchingsafebox.bridge.DefaultSearchingOpenBridge
import org.ewsk.searchingsafebox.bridge.SearchingOpenBridge
import org.ewsk.searchingsafebox.config.SafeBoxConfig
import org.ewsk.searchingsafebox.config.SafeBoxConfigSource
import org.ewsk.searchingsafebox.manager.AutoUnlockService
import org.ewsk.searchingsafebox.manager.SafeBoxManager
import org.ewsk.searchingsafebox.registry.DefaultUnlockUIRegistry
import org.ewsk.searchingsafebox.registry.UnlockUIRegistry
import org.ewsk.searchingsafebox.session.UnlockSessionStore
import org.ewsk.searchingsafebox.ui.DefaultAutoUnlockProgressRenderer
import org.ewsk.searchingsafebox.ui.DefaultUnlockUIProvider
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info

object SearchingSafeBox : Plugin() {

    lateinit var config: SafeBoxConfig
        private set
    lateinit var uiRegistry: UnlockUIRegistry
        private set
    lateinit var sessionStore: UnlockSessionStore
        private set
    lateinit var manager: SafeBoxManager
        private set
    lateinit var bridge: SearchingOpenBridge
        private set

    fun isReady(): Boolean {
        return ::config.isInitialized && ::uiRegistry.isInitialized && ::sessionStore.isInitialized && ::manager.isInitialized
    }

    fun registerBridge(bridge: SearchingOpenBridge) {
        this.bridge = bridge
        if (::manager.isInitialized) {
            manager.setBridge(bridge)
        }
    }

    override fun onEnable() {
        config = SafeBoxConfig { SafeBoxConfigSource.config }.also { it.reload() }
        SafeBoxConfigSource.config.onReload {
            config.reload()
            if (::manager.isInitialized) {
                manager.clearSessions("config-reload")
            }
        }

        uiRegistry = DefaultUnlockUIRegistry()
        uiRegistry.register(DefaultUnlockUIProvider())
        sessionStore = UnlockSessionStore()
        registerBridge(DefaultSearchingOpenBridge())
        manager = SafeBoxManager(
            config = config,
            sessions = sessionStore,
            uiRegistry = uiRegistry,
            bridge = bridge,
            autoUnlockService = AutoUnlockService(DefaultAutoUnlockProgressRenderer())
        )

        info("[SearchingSafeBox] 已启用，加载 ${config.allRules().size} 条保险箱规则。")
        info("[SearchingSafeBox] 作者 QQ: 2962271068")
    }

    override fun onDisable() {
        if (::manager.isInitialized) {
            manager.clearSessions("plugin-disable")
        }
        info("[SearchingSafeBox] 已卸载。")
    }
}

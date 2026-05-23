package org.ewsk.searchingsafebox.bridge

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.ewsk.searching.api.ApiResult
import org.ewsk.searching.api.SearchingApi
import org.ewsk.searching.api.SearchingNodeView
import org.ewsk.searching.core.common.ids.PlayerId
import org.ewsk.searching.platform.bukkit.event.SearchingPreOpenEvent
import org.ewsk.searchingsafebox.api.SafeBoxMessages
import taboolib.common.platform.function.info

class DefaultSearchingOpenBridge : SearchingOpenBridge {
    override fun requiresUnlock(node: Any): Boolean {
        val view = node as? SearchingNodeView ?: return true
        return view.state == UnsearchedState
    }

    override fun openOriginal(player: Player, node: Any) {
        val view = node as? SearchingNodeView
        if (view == null) {
            info("[SearchingSafeBox] Cannot open original Searching node for ${player.name}: unsupported node ref $node")
            SafeBoxMessages.send(player, "&c无法打开原搜刮箱：节点引用类型不受支持。")
            return
        }

        val api = Bukkit.getServicesManager().load(SearchingApi::class.java)
        if (api == null) {
            info("[SearchingSafeBox] Cannot open original Searching node for ${player.name}: SearchingApi service is unavailable.")
            SafeBoxMessages.send(player, "&c无法打开原搜刮箱：Searching API 不可用。")
            return
        }

        if (!SearchingPreOpenEvent(player, view).call()) {
            info("[SearchingSafeBox] Opening original Searching node ${view.id.value} for ${player.name} was cancelled by another plugin.")
            SafeBoxMessages.send(player, "&cCannot open original Searching node: access denied.")
            return
        }

        when (val result = api.nodes.openNode(PlayerId(player.uniqueId.toString()), view.id)) {
            is ApiResult.Success -> Unit
            is ApiResult.Failure -> {
                info("[SearchingSafeBox] Failed to open original Searching node ${view.id.value} for ${player.name}: ${result.code} ${result.message}")
                SafeBoxMessages.send(player, "&c无法打开原搜刮箱：${result.message}")
            }
        }
    }

    private companion object {
        const val UnsearchedState = "Unsearched"
    }
}

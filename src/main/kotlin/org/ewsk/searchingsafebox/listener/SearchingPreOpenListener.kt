package org.ewsk.searchingsafebox.listener

import org.ewsk.searching.platform.bukkit.event.SearchingPreOpenEvent
import org.ewsk.searchingsafebox.SearchingSafeBox
import taboolib.common.platform.event.SubscribeEvent

object SearchingPreOpenListener {
    @SubscribeEvent
    fun onSearchingPreOpen(event: SearchingPreOpenEvent) {
        if (!SearchingSafeBox.isReady()) {
            return
        }
        if (event.isCancelled) {
            return
        }
        if (SearchingSafeBox.manager.isOpeningOriginal(event.player.uniqueId, event.node)) {
            return
        }
        val nodeTypeId = event.node.typeId
        val rule = SearchingSafeBox.config.getRule(nodeTypeId) ?: return
        if (!rule.enabled) {
            return
        }
        if (!SearchingSafeBox.manager.requiresUnlock(event.node)) {
            return
        }
        event.isCancelled = true
        SearchingSafeBox.manager.handlePreOpen(event.player, event.node, rule)
    }
}

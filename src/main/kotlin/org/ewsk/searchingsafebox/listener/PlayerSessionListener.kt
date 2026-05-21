package org.ewsk.searchingsafebox.listener

import org.bukkit.event.player.PlayerQuitEvent
import org.ewsk.searchingsafebox.SearchingSafeBox
import taboolib.common.platform.event.SubscribeEvent

object PlayerSessionListener {
    @SubscribeEvent
    fun onQuit(event: PlayerQuitEvent) {
        if (!SearchingSafeBox.isReady()) {
            return
        }
        SearchingSafeBox.manager.cancelByPlayer(event.player.uniqueId, "player-quit")
    }
}

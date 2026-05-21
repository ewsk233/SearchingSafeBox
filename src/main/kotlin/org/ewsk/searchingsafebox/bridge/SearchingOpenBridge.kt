package org.ewsk.searchingsafebox.bridge

import org.bukkit.entity.Player

interface SearchingOpenBridge {
    fun requiresUnlock(node: Any): Boolean {
        return true
    }

    fun openOriginal(player: Player, node: Any)
}

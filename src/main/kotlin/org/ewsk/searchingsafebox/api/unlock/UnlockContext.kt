package org.ewsk.searchingsafebox.api.unlock

import org.bukkit.entity.Player
import org.ewsk.searchingsafebox.config.SafeBoxRule
import org.ewsk.searchingsafebox.session.UnlockSession

interface UnlockContext {
    val player: Player
    val session: UnlockSession
    val rule: SafeBoxRule

    fun success()

    fun fail(reason: String? = null)

    fun cancel(reason: String? = null)
}

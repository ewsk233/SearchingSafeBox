package org.ewsk.searchingsafebox.session

import org.ewsk.searchingsafebox.config.SafeBoxRule
import java.util.UUID

data class UnlockSession(
    val sessionId: UUID,
    val player: UUID,
    val nodeTypeId: String,
    val nodeRef: Any,
    val rule: SafeBoxRule,
    val startTime: Long,
    val expiresAt: Long,
    var state: UnlockState,
    var autoUnlockTaskId: Int? = null,
    var autoUnlockProgress: Int = 0,
    var timeoutTaskId: Int? = null
) {
    fun isActive(): Boolean {
        return state == UnlockState.CREATED ||
            state == UnlockState.OPENING_UI ||
            state == UnlockState.UNLOCKING ||
            state == UnlockState.SUCCESS
    }
}

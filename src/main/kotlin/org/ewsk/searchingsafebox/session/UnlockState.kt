package org.ewsk.searchingsafebox.session

enum class UnlockState {
    CREATED,
    OPENING_UI,
    UNLOCKING,
    SUCCESS,
    FAILED,
    CANCELLED,
    EXPIRED,
    AUTO_UNLOCKED,
    OPENED
}

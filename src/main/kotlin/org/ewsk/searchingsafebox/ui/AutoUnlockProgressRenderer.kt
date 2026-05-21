package org.ewsk.searchingsafebox.ui

import org.ewsk.searchingsafebox.api.unlock.UnlockContext

interface AutoUnlockProgressRenderer {
    fun renderInitial(context: UnlockContext)

    fun renderProgress(context: UnlockContext, progressSlots: Int, totalSlots: Int)

    fun clear(context: UnlockContext)
}

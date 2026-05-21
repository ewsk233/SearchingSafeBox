package org.ewsk.searchingsafebox.api.unlock

interface UnlockUIProvider {
    val id: String

    fun open(context: UnlockContext)
}

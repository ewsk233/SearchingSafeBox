package org.ewsk.searchingsafebox.registry

import org.ewsk.searchingsafebox.api.unlock.UnlockContext
import org.ewsk.searchingsafebox.api.unlock.UnlockUIProvider

interface UnlockUIRegistry {
    fun register(provider: UnlockUIProvider)

    fun unregister(id: String)

    fun get(id: String): UnlockUIProvider?

    fun open(uiId: String, context: UnlockContext): Boolean

    fun ids(): List<String>
}

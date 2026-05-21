package org.ewsk.searchingsafebox.registry

import org.ewsk.searchingsafebox.api.unlock.UnlockContext
import org.ewsk.searchingsafebox.api.unlock.UnlockUIProvider
import java.util.concurrent.ConcurrentHashMap

class DefaultUnlockUIRegistry : UnlockUIRegistry {
    private val providers = ConcurrentHashMap<String, UnlockUIProvider>()

    override fun register(provider: UnlockUIProvider) {
        providers[provider.id.lowercase()] = provider
    }

    override fun unregister(id: String) {
        providers.remove(id.lowercase())
    }

    override fun get(id: String): UnlockUIProvider? {
        return providers[id.lowercase()]
    }

    override fun open(uiId: String, context: UnlockContext): Boolean {
        val provider = get(uiId) ?: return false
        provider.open(context)
        return true
    }

    override fun ids(): List<String> {
        return providers.keys.sorted()
    }
}

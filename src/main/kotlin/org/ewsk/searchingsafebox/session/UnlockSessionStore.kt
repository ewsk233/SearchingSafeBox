package org.ewsk.searchingsafebox.session

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class UnlockSessionStore {
    private val byId = ConcurrentHashMap<UUID, UnlockSession>()
    private val byPlayer = ConcurrentHashMap<UUID, UUID>()

    fun add(session: UnlockSession): Boolean {
        val previous = byPlayer.putIfAbsent(session.player, session.sessionId)
        if (previous != null) {
            return false
        }
        byId[session.sessionId] = session
        return true
    }

    fun get(sessionId: UUID): UnlockSession? {
        return byId[sessionId]
    }

    fun findByPlayer(player: UUID): UnlockSession? {
        val sessionId = byPlayer[player] ?: return null
        return byId[sessionId]
    }

    fun remove(sessionId: UUID): UnlockSession? {
        val session = byId.remove(sessionId) ?: return null
        byPlayer.remove(session.player, sessionId)
        return session
    }

    fun all(): List<UnlockSession> {
        return byId.values.sortedBy { it.startTime }
    }

    fun clear(): List<UnlockSession> {
        val sessions = all()
        byId.clear()
        byPlayer.clear()
        return sessions
    }
}

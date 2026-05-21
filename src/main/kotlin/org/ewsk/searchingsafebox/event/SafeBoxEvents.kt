package org.ewsk.searchingsafebox.event

import org.bukkit.entity.Player
import org.ewsk.searchingsafebox.config.SafeBoxRule
import org.ewsk.searchingsafebox.session.UnlockSession
import taboolib.platform.type.BukkitProxyEvent

class SafeBoxUnlockStartEvent(
    val player: Player,
    val session: UnlockSession,
    val rule: SafeBoxRule
) : BukkitProxyEvent()

class SafeBoxUnlockSuccessEvent(
    val player: Player,
    val session: UnlockSession,
    val rule: SafeBoxRule
) : BukkitProxyEvent()

class SafeBoxUnlockFailEvent(
    val player: Player,
    val session: UnlockSession,
    val rule: SafeBoxRule,
    val reason: String?
) : BukkitProxyEvent()

class SafeBoxUnlockCancelEvent(
    val player: Player,
    val session: UnlockSession,
    val rule: SafeBoxRule,
    val reason: String?
) : BukkitProxyEvent()

class SafeBoxAutoUnlockCompleteEvent(
    val player: Player,
    val session: UnlockSession,
    val rule: SafeBoxRule
) : BukkitProxyEvent()

class SafeBoxOpenOriginalEvent(
    val player: Player,
    val session: UnlockSession,
    val rule: SafeBoxRule,
    val node: Any
) : BukkitProxyEvent()

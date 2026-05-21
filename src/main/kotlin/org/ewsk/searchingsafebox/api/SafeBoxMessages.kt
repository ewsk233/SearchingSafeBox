package org.ewsk.searchingsafebox.api

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.ewsk.searchingsafebox.config.SafeBoxRule

object SafeBoxMessages {
    const val Prefix = "&8[&bSearchingSafeBox&8] &r"

    fun send(sender: CommandSender, message: String) {
        sender.sendMessage(color(Prefix + message))
    }

    fun sendRaw(sender: CommandSender, message: String) {
        sender.sendMessage(color(message))
    }

    fun sendRule(player: Player, rule: SafeBoxRule, key: String, fallback: String) {
        sendRaw(player, rule.messages[key] ?: fallback)
    }

    fun color(message: String): String {
        return message.replace('&', '\u00A7')
    }
}

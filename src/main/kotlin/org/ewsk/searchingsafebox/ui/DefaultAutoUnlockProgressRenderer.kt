package org.ewsk.searchingsafebox.ui

import org.bukkit.inventory.ItemStack
import org.ewsk.searchingsafebox.api.SafeBoxMessages
import org.ewsk.searchingsafebox.api.unlock.UnlockContext
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem

class DefaultAutoUnlockProgressRenderer : AutoUnlockProgressRenderer {
    override fun renderInitial(context: UnlockContext) {
        renderProgress(context, 0, ProgressSlotCount)
    }

    override fun renderProgress(context: UnlockContext, progressSlots: Int, totalSlots: Int) {
        val inventory = context.player.openInventory.topInventory
        if (inventory.size < ProgressSlots.last() + 1) {
            return
        }
        val filled = progressSlots.coerceIn(0, totalSlots.coerceAtMost(ProgressSlotCount))
        ProgressSlots.forEachIndexed { index, slot ->
            inventory.setItem(
                slot,
                if (index < filled) ProgressOnItem else ProgressOffItem
            )
        }
    }

    override fun clear(context: UnlockContext) {
        val inventory = context.player.openInventory.topInventory
        if (inventory.size < ProgressSlots.last() + 1) {
            return
        }
        ProgressSlots.forEach { slot ->
            inventory.setItem(slot, ProgressOffItem)
        }
    }

    private companion object {
        val ProgressSlots = 45..53
        val ProgressSlotCount = ProgressSlots.count()
        val ProgressOffItem = item(XMaterial.GRAY_STAINED_GLASS_PANE, "&7自动破译：等待中")
        val ProgressOnItem = item(XMaterial.LIME_STAINED_GLASS_PANE, "&a自动破译：进行中")

        fun item(material: XMaterial, name: String): ItemStack {
            return buildItem(material) {
                this.name = SafeBoxMessages.color(name)
            }
        }
    }
}

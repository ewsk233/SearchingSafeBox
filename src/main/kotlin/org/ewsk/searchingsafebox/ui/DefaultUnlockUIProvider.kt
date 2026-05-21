package org.ewsk.searchingsafebox.ui

import org.bukkit.inventory.ItemStack
import org.ewsk.searchingsafebox.api.SafeBoxMessages
import org.ewsk.searchingsafebox.api.unlock.UnlockContext
import org.ewsk.searchingsafebox.api.unlock.UnlockUIProvider
import taboolib.common.platform.function.submit
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import taboolib.platform.util.buildItem
import java.util.concurrent.ThreadLocalRandom

class DefaultUnlockUIProvider : UnlockUIProvider {
    override val id: String = "default"

    override fun open(context: UnlockContext) {
        val puzzle = PasswordPuzzle.generate()
        val input = PasswordInput()

        context.player.openMenu<Chest>(SafeBoxMessages.color(context.rule.title)) {
            rows(6)
            onClick { event -> event.isCancelled = true }
            onClose(true, true) {
                if (input.completed || !context.session.isActive()) {
                    return@onClose
                }
                input.completed = true
                submit(delay = 1L) {
                    if (context.session.isActive()) {
                        context.cancel("inventory-close")
                    }
                }
            }

            renderFrame()
            renderCodeSlots()
            renderKeypad(context, puzzle, input)
        }
    }

    private fun handleDigit(
        context: UnlockContext,
        puzzle: PasswordPuzzle,
        input: PasswordInput,
        digit: Int
    ) {
        if (!context.session.isActive() || input.locked || input.isFull()) {
            return
        }

        val index = input.append(digit)
        renderDigit(context, index, digit, puzzle.isCorrect(index, digit))

        if (!input.isFull()) {
            return
        }

        input.locked = true
        if (puzzle.matches(input.digits)) {
            input.completed = true
            context.player.closeInventory()
            submit(delay = 1L) {
                if (context.session.isActive()) {
                    context.success()
                }
            }
            return
        }

        if (context.rule.consumeOnFail) {
            submit(delay = 8L) {
                if (context.session.isActive()) {
                    input.completed = true
                    context.player.closeInventory()
                    context.fail("wrong-code")
                }
            }
            return
        }

        submit(delay = 12L) {
            if (!context.session.isActive()) {
                return@submit
            }
            input.reset()
            clearCodeSlots(context)
        }
    }

    private fun handleBackspace(context: UnlockContext, input: PasswordInput) {
        if (!context.session.isActive() || input.locked || input.digits.isEmpty()) {
            return
        }
        val index = input.removeLast()
        context.setOpenMenuItem(CodeSlots[index], codePlaceholder())
    }

    private fun Chest.renderFrame() {
        FrameSlots.forEach { slot ->
            set(slot, item(XMaterial.GRAY_STAINED_GLASS_PANE, "&7自动破译"))
        }
    }

    private fun Chest.renderCodeSlots() {
        CodeSlots.forEach { slot ->
            set(slot, codePlaceholder())
        }
    }

    private fun Chest.renderKeypad(context: UnlockContext, puzzle: PasswordPuzzle, input: PasswordInput) {
        DigitSlots.forEach { (slot, digit) ->
            set(
                slot,
                item(
                    material = XMaterial.BLUE_STAINED_GLASS_PANE,
                    name = "&b数字 $digit",
                    amount = digit.itemAmount(),
                    glowing = digit in puzzle.hintDigits
                )
            ) {
                isCancelled = true
                handleDigit(context, puzzle, input, digit)
            }
        }
        set(BackspaceSlot, item(XMaterial.RED_STAINED_GLASS_PANE, "&c退格")) {
            isCancelled = true
            handleBackspace(context, input)
        }
    }

    private fun renderDigit(context: UnlockContext, index: Int, digit: Int, correct: Boolean) {
        val material = if (correct) XMaterial.GREEN_STAINED_GLASS_PANE else XMaterial.RED_STAINED_GLASS_PANE
        val name = if (correct) "&a正确：$digit" else "&c错误：$digit"
        context.setOpenMenuItem(CodeSlots[index], item(material, name, amount = digit.itemAmount()))
    }

    private fun clearCodeSlots(context: UnlockContext) {
        CodeSlots.forEach { slot ->
            context.setOpenMenuItem(slot, codePlaceholder())
        }
    }

    private fun UnlockContext.setOpenMenuItem(slot: Int, item: ItemStack) {
        val inventory = player.openInventory.topInventory
        if (slot in 0 until inventory.size) {
            inventory.setItem(slot, item)
        }
    }

    private fun item(
        material: XMaterial,
        name: String,
        vararg lore: String,
        amount: Int = 1,
        glowing: Boolean = false
    ): ItemStack {
        return buildItem(material) {
            this.name = SafeBoxMessages.color(name)
            this.lore += lore.map(SafeBoxMessages::color)
            this.amount = amount.coerceIn(1, 64)
            if (glowing) {
                shiny()
            }
        }
    }

    private fun codePlaceholder(): ItemStack {
        return item(XMaterial.GRAY_STAINED_GLASS_PANE, "&7待输入")
    }

    private fun Int.itemAmount(): Int {
        return if (this == 0) 1 else this
    }

    private data class PasswordPuzzle(
        val code: List<Int>,
        val hintDigits: Set<Int>
    ) {
        fun isCorrect(index: Int, digit: Int): Boolean {
            return code.getOrNull(index) == digit
        }

        fun matches(input: List<Int>): Boolean {
            return code == input
        }

        companion object {
            fun generate(): PasswordPuzzle {
                val repeated = randomDigit()
                val single = randomDistinctDigit(repeated)
                val code = mutableListOf(repeated, repeated, single).also { it.shuffle() }
                return PasswordPuzzle(code = code, hintDigits = setOf(repeated, single))
            }

            private fun randomDigit(): Int {
                return ThreadLocalRandom.current().nextInt(0, 10)
            }

            private fun randomDistinctDigit(excluded: Int): Int {
                var value: Int
                do {
                    value = randomDigit()
                } while (value == excluded)
                return value
            }
        }
    }

    private class PasswordInput {
        val digits = mutableListOf<Int>()
        var locked: Boolean = false
        var completed: Boolean = false

        fun append(digit: Int): Int {
            digits += digit
            return digits.lastIndex
        }

        fun removeLast(): Int {
            val index = digits.lastIndex
            digits.removeAt(index)
            return index
        }

        fun isFull(): Boolean {
            return digits.size >= CodeSlots.size
        }

        fun reset() {
            digits.clear()
            locked = false
        }
    }

    private companion object {
        val CodeSlots = listOf(3, 4, 5)
        val FrameSlots = 45 until 54
        const val BackspaceSlot = 43

        val DigitSlots = linkedMapOf(
            12 to 1,
            13 to 2,
            14 to 3,
            21 to 4,
            22 to 5,
            23 to 6,
            30 to 7,
            31 to 8,
            32 to 9,
            40 to 0
        )
    }
}

package org.ewsk.searchingsafebox.manager

import org.ewsk.searchingsafebox.api.unlock.UnlockContext
import org.ewsk.searchingsafebox.session.UnlockSession
import org.ewsk.searchingsafebox.ui.AutoUnlockProgressRenderer
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

class AutoUnlockService(
    private val renderer: AutoUnlockProgressRenderer
) {
    private val nextTaskId = AtomicInteger(1)
    private val tasks = ConcurrentHashMap<Int, PlatformExecutor.PlatformTask>()

    fun start(context: UnlockContext, onComplete: () -> Unit): Int? {
        val seconds = context.rule.autoUnlockSeconds
        if (!context.rule.autoUnlock || seconds <= 0) {
            return null
        }
        val session = context.session
        val totalTicks = seconds * 20
        val slotIntervalTicks = max(1, totalTicks / TotalSlots)
        val taskId = nextTaskId.getAndIncrement()
        renderer.renderInitial(context)
        var elapsedTicks = 0
        var task: PlatformExecutor.PlatformTask? = null
        task = submit(period = 1L) {
            if (session.autoUnlockTaskId != taskId || !session.isActive()) {
                task?.cancel()
                tasks.remove(taskId)
                return@submit
            }
            elapsedTicks += 1
            val progressSlots = if (elapsedTicks >= totalTicks) {
                TotalSlots
            } else {
                min(TotalSlots, elapsedTicks / slotIntervalTicks)
            }
            if (progressSlots != session.autoUnlockProgress) {
                session.autoUnlockProgress = progressSlots
                renderer.renderProgress(context, progressSlots, TotalSlots)
            }
            if (elapsedTicks >= totalTicks) {
                task?.cancel()
                tasks.remove(taskId)
                session.autoUnlockTaskId = null
                session.autoUnlockProgress = TotalSlots
                renderer.renderProgress(context, TotalSlots, TotalSlots)
                renderer.clear(context)
                onComplete()
            }
        }
        tasks[taskId] = task
        session.autoUnlockTaskId = taskId
        return taskId
    }

    fun cancel(session: UnlockSession) {
        val taskId = session.autoUnlockTaskId ?: return
        tasks.remove(taskId)?.cancel()
        session.autoUnlockTaskId = null
    }

    fun cancelAll() {
        tasks.values.forEach { it.cancel() }
        tasks.clear()
    }

    private companion object {
        const val TotalSlots = 9
    }
}

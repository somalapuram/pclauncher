package com.somalapuram.pclauncher.feature.shell.interaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.somalapuram.pclauncher.core.apps.AppEntry

/** Where a drag started, which decides what dropping it means. */
enum class DragOrigin { Desktop, Dock }

/**
 * A drag in flight.
 *
 * Held in one place rather than per-surface: the desktop starts drags the bar has to answer, and
 * vice versa, so a shared value is the only version that cannot disagree with itself.
 */
class DragState {
    var entry: AppEntry? by mutableStateOf(null)
        private set
    var origin: DragOrigin = DragOrigin.Desktop
        private set
    var position: Offset by mutableStateOf(Offset.Zero)
        private set
    var target: DropTarget by mutableStateOf(DropTarget.None)
        private set

    val isActive: Boolean get() = entry != null

    fun start(entry: AppEntry, origin: DragOrigin, at: Offset) {
        this.entry = entry
        this.origin = origin
        position = at
        target = DropTarget.None
    }

    fun moveTo(at: Offset, barTopY: Float, barBottomY: Float) {
        position = at
        target = dropTargetFor(at.y, barTopY, barBottomY)
    }

    /**
     * End the drag and report what should happen.
     *
     * Returns null when nothing should change — dropped on nothing, dropped where it already is, or
     * dropped back where it came from. Deciding here keeps the caller from having to re-derive it.
     */
    fun end(isPinned: (AppEntry) -> Boolean): DragResult? {
        val dragged = entry ?: return null
        val landedOn = target
        entry = null
        target = DropTarget.None

        if (!dropChangesAnything(landedOn, isPinned(dragged))) return null

        return when (landedOn) {
            DropTarget.Dock -> DragResult(dragged, pin = true)
            DropTarget.Desktop -> DragResult(dragged, pin = false)
            DropTarget.None -> null
        }
    }

    fun cancel() {
        entry = null
        target = DropTarget.None
    }
}

data class DragResult(val entry: AppEntry, val pin: Boolean)

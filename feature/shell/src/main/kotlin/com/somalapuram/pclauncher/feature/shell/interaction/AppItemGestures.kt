package com.somalapuram.pclauncher.feature.shell.interaction

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

/**
 * What happened while waiting for movement.
 *
 * [TakenByChild] is the one that matters: the desktop container watches the same events its icons
 * do, and without noticing that a child has claimed the gesture its long-press timer fires
 * mid-drag, opens the desktop menu, and the menu steals the pointer — killing the drag a few
 * events in.
 */
private sealed interface SlopOutcome {
    data class Moved(val travelled: Offset) : SlopOutcome
    data object Lifted : SlopOutcome
    data object TakenByChild : SlopOutcome
}

/**
 * The gesture contract every surface that lists an app shares (direct-manipulation.md).
 *
 * One modifier rather than three, so the desktop grid, the Start rows and the dock icons cannot
 * drift apart — the same press means the same thing wherever it lands.
 *
 * Long-press and drag compete for the same gesture. Hold *and release* opens the menu; hold *and
 * move* becomes a drag. A mouse never waits: with a pointer, movement past slop starts a drag
 * straight away, because sitting out a long-press timer on a desktop feels broken.
 *
 * **Everything happens inside one `awaitPointerEventScope`.** Leaving the scope and re-entering it
 * between reads drops the events that arrive in between, which is why the timer uses the scope's
 * own [AwaitPointerEventScope.withTimeoutOrNull] rather than the coroutines one.
 */
fun Modifier.appItemGestures(
    key: Any?,
    enabled: Boolean = true,
    /**
     * Whether a touch drag has to wait for the long-press.
     *
     * True inside a scrollable — the grid, the Start list — where early movement belongs to the
     * scroll and stealing it would strand the user on the first screen. **False on the desktop**,
     * which scrolls nothing: there the rule buys nothing and costs everything, because an
     * abandoned gesture is abandoned into empty space and a quick drag simply does nothing.
     */
    dragRequiresLongPress: Boolean = true,
    onClick: () -> Unit,
    onContextMenu: (Offset) -> Unit,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
): Modifier = if (!enabled) this else this.pointerInput(key) {
    val slop = viewConfiguration.touchSlop
    val longPressMillis = viewConfiguration.longPressTimeoutMillis

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        // Right-click resolves on press: a menu that waits for the button to come up feels laggy,
        // and there is nothing else a secondary press could mean on an app item.
        if (currentEvent.buttons.isSecondaryPressed) {
            onContextMenu(down.position)
            drainUntilUp(down.id)
            return@awaitEachGesture
        }

        if (down.type == PointerType.Mouse || !dragRequiresLongPress) {
            val dragged = trackImmediateDrag(down.id, slop, down.position, onDragStart, onDrag)
            if (dragged) onDragEnd() else onClick()
            return@awaitEachGesture
        }

        // Touch. Movement *before* the long-press timer is a scroll, not a drag — the grid and
        // the Start list are scrollable, and stealing that would leave the user unable to reach
        // anything past the first screen. So this phase deliberately does not consume: the
        // scrollable ancestor is watching the same events and should win.
        // null from withTimeoutOrNull means the timer won.
        val beforeTimeout = withTimeoutOrNull(longPressMillis) {
            awaitSlopOrUp(down.id, slop, claimOnSlop = false)
        }

        when (beforeTimeout) {
            // Scrolling, or a child has taken the gesture. Either way, hands off.
            is SlopOutcome.Moved, SlopOutcome.TakenByChild -> return@awaitEachGesture

            SlopOutcome.Lifted -> onClick()

            // The finger has been held long enough to mean business. Now which is it — hold and
            // move is a drag, hold and release is the menu.
            null -> when (val after = awaitSlopOrUp(down.id, slop, claimOnSlop = true)) {
                is SlopOutcome.Moved -> {
                    onDragStart(down.position)
                    // Report the travel that *triggered* the drag too. Without this the first few
                    // hundred pixels are swallowed by slop detection and the ghost starts behind
                    // the finger.
                    onDrag(after.travelled)
                    followUntilUp(down.id, onDrag)
                    onDragEnd()
                }

                SlopOutcome.TakenByChild -> Unit
                SlopOutcome.Lifted -> onContextMenu(down.position)
            }
        }
    }
}

/** Swallow the rest of the gesture so its release is not read as a click. */
private suspend fun AwaitPointerEventScope.drainUntilUp(id: PointerId) {
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.id == id && it.pressed })
}

/**
 * The accumulated travel once movement passes [slop], or null if the pointer lifts first.
 *
 * Returns the travel rather than a flag so the caller can replay it: the movement that crosses the
 * threshold is real movement, and dropping it makes a drag start behind the finger.
 *
 * [claimOnSlop] decides whether crossing the threshold *takes* the gesture. Before the long-press
 * timer it must not — the scrollable ancestor is entitled to that movement. After it, it must, or
 * the scroll and the drag both run.
 */
private suspend fun AwaitPointerEventScope.awaitSlopOrUp(
    id: PointerId,
    slop: Float,
    claimOnSlop: Boolean,
): SlopOutcome {
    var travelled = Offset.Zero
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == id }
            ?: return SlopOutcome.Lifted
        if (!change.pressed) return SlopOutcome.Lifted

        // Someone nested inside has claimed this gesture — almost always an icon starting a drag
        // while its container is still timing a long-press. Standing down here is what stops the
        // container's menu opening on top of an in-flight drag.
        if (change.isConsumed) return SlopOutcome.TakenByChild

        travelled += change.positionChange()
        if (travelled.getDistance() > slop) {
            if (claimOnSlop) change.consume()
            return SlopOutcome.Moved(travelled)
        }
    }
}

/** Start a drag the moment movement passes slop, with no long-press gate. True if one happened. */
private suspend fun AwaitPointerEventScope.trackImmediateDrag(
    id: PointerId,
    slop: Float,
    startPosition: Offset,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
): Boolean {
    var travelled = Offset.Zero
    var dragging = false
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == id } ?: return dragging
        if (!change.pressed) return dragging

        // Read the delta *before* consuming: positionChange() reports zero once a change is
        // consumed, so consuming first silently turns every drag into a stationary one.
        val delta = change.positionChange()
        travelled += delta

        if (!dragging && travelled.getDistance() > slop) {
            dragging = true
            onDragStart(startPosition)
            change.consume()
            // Replay everything travelled so far, not just this frame's delta.
            onDrag(travelled)
            continue
        }
        if (dragging) {
            change.consume()
            onDrag(delta)
        }
    }
}

/** Report movement until the pointer lifts. The drag has already started. */
private suspend fun AwaitPointerEventScope.followUntilUp(id: PointerId, onDrag: (Offset) -> Unit) {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == id } ?: return
        if (!change.pressed) return
        // Delta first, then consume — see the note in trackMouseDrag.
        val delta = change.positionChange()
        change.consume()
        onDrag(delta)
    }
}

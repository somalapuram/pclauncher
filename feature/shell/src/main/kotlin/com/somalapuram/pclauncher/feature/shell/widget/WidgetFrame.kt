package com.somalapuram.pclauncher.feature.shell.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.core.data.layout.ResizeEdge
import com.somalapuram.pclauncher.core.data.layout.ResizePermission
import com.somalapuram.pclauncher.core.data.layout.canDrag
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners

/** Handle size — big enough to grab on touch without covering the widget it belongs to. */
private val HandleSize = 22.dp

/**
 * The resize frame shown around a widget in resize mode.
 *
 * Handles appear only on axes the provider permits, so a widget that cannot grow sideways does not
 * offer a grip that would do nothing. Dragging reports pixels; turning those into cells is the
 * caller's job, and pure.
 */
@Composable
fun WidgetResizeFrame(
    permission: ResizePermission,
    onDragEdge: (ResizeEdge, Float) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * A handle drag has begun.
     *
     * Reported because [onDragEdge] sends *cumulative* pixels: without a start marker the caller
     * has no base to measure against, applies each report as a fresh increment, and a one-cell
     * drag compounds into five.
     */
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    val colors = LocalPcColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .border(2.dp, colors.accent, RoundedCornerShape(PcCorners.Surface)),
    ) {
        if (canDrag(ResizeEdge.Left, permission)) {
            Handle(Modifier.align(Alignment.CenterStart), onDragStart, onDragEnd) {
                onDragEdge(ResizeEdge.Left, -it.first)
            }
        }
        if (canDrag(ResizeEdge.Right, permission)) {
            Handle(Modifier.align(Alignment.CenterEnd), onDragStart, onDragEnd) {
                onDragEdge(ResizeEdge.Right, it.first)
            }
        }
        if (canDrag(ResizeEdge.Top, permission)) {
            Handle(Modifier.align(Alignment.TopCenter), onDragStart, onDragEnd) {
                onDragEdge(ResizeEdge.Top, -it.second)
            }
        }
        if (canDrag(ResizeEdge.Bottom, permission)) {
            Handle(Modifier.align(Alignment.BottomCenter), onDragStart, onDragEnd) {
                onDragEdge(ResizeEdge.Bottom, it.second)
            }
        }
    }
}

/** One grip. Reports cumulative drag as (dx, dy) so the caller can decide what a cell is. */
@Composable
private fun Handle(
    modifier: Modifier = Modifier,
    onStart: () -> Unit = {},
    onEnd: () -> Unit = {},
    onDrag: (Pair<Float, Float>) -> Unit,
) {
    val colors = LocalPcColors.current
    var totalX = 0f
    var totalY = 0f

    Box(
        modifier = modifier
            .size(HandleSize)
            .background(colors.accent, CircleShape)
            .border(2.dp, colors.onAccent, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { totalX = 0f; totalY = 0f; onStart() },
                    onDragEnd = { onEnd() },
                    onDragCancel = { onEnd() },
                    // Cumulative, not per-event: a resize is judged against where the drag began,
                    // and summing deltas at the call site would spread that state across surfaces.
                    onDrag = { change, delta ->
                        change.consume()
                        totalX += delta.x
                        totalY += delta.y
                        onDrag(totalX to totalY)
                    },
                )
            },
    )
}

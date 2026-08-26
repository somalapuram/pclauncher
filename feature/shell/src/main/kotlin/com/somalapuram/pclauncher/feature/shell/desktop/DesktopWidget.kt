package com.somalapuram.pclauncher.feature.shell.desktop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.somalapuram.pclauncher.core.data.layout.DesktopCell
import com.somalapuram.pclauncher.core.data.layout.DesktopPlacement
import com.somalapuram.pclauncher.core.data.layout.ResizeEdge
import com.somalapuram.pclauncher.core.data.layout.ResizePermission
import com.somalapuram.pclauncher.core.data.layout.cellAfterDrag
import com.somalapuram.pclauncher.core.data.layout.widgetSizeDp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcHover
import com.somalapuram.pclauncher.core.design.PcMotion
import com.somalapuram.pclauncher.feature.shell.widget.HostedWidget
import com.somalapuram.pclauncher.feature.shell.widget.WidgetResizeFrame
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/**
 * One widget on the desktop: where it sits, how it is moved, resized, and removed.
 *
 * Lives apart from the icon grid because a widget answers a press in three different ways — its
 * own tap, a menu, a drag — and none of those are what an icon does.
 */
@Composable
fun DesktopWidget(
    placement: DesktopPlacement,
    widgetId: Int,
    view: android.appwidget.AppWidgetHostView?,
    permission: ResizePermission,
    isResizing: Boolean,
    cellWidth: Dp,
    cellHeight: Dp,
    cellWidthPx: Float,
    cellHeightPx: Float,
    columnsAvailable: Int,
    rowsAvailable: Int,
    onOpenResize: () -> Unit,
    onRemove: () -> Unit,
    onMove: (DesktopCell) -> Unit,
    onResizeDrag: (ResizeEdge, Float) -> Unit,
    onResizeStart: () -> Unit,
    onResizeEnd: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Tell the provider how much room it has, in dp.
     *
     * Driven by the placement rather than by a resize event: a widget that is merely *placed* — or
     * re-created after a restart, which is every widget on every boot — is drawn at a size nobody
     * ever told it, and lays out for its default inside a box that is not that size
     * (widget-sizing.md).
     */
    onReportSize: (widthDp: Int, heightDp: Int) -> Unit = { _, _ -> },
) {
    var menuOpen by remember { mutableStateOf(false) }
    // How far this widget has been dragged from its cell. Reset on drop, so a refused move simply
    // leaves the widget back where it started (widget-drag.md requirement 6).
    var dragged by remember { mutableStateOf(Offset.Zero) }
    val isDragging = dragged != Offset.Zero

    // Watched, never consumed: `hoverable` does not take the pointer, so the widget's own buttons
    // keep working while the container is hovered.
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val outline by animateFloatAsState(
        targetValue = PcHover.outlineFor(hovered),
        animationSpec = PcMotion.DockMagnify,
        label = "widget-hover-outline",
    )
    val colors = LocalPcColors.current

    val size = widgetSizeDp(
        span = placement.span,
        cellWidthDp = cellWidth.value.toInt(),
        cellHeightDp = cellHeight.value.toInt(),
    )
    LaunchedEffect(widgetId, size) { onReportSize(size.width, size.height) }

    Box(
        modifier = modifier
            // The cell only. The drag is drawn by the child's layer rather than moved here,
            // because this node is where the gesture is measured: translating it would slide the
            // coordinate space out from under the finger and lose part of every drag.
            .offset {
                IntOffset(
                    (placement.cell.column * cellWidthPx).roundToInt(),
                    (placement.cell.row * cellHeightPx).roundToInt(),
                )
            }
            .size(
                width = cellWidth * placement.span.columns,
                height = cellHeight * placement.span.rows,
            )
            // A widget being dragged passes over the ones it is dragged across.
            .zIndex(if (isDragging) 1f else 0f)
            .hoverable(interactions)
            // An outline rather than a scale: a hosted widget is someone else's rendered UI, and
            // scaling it resamples their pixels into a blur at the size widgets occupy.
            .border(
                width = 2.dp,
                color = colors.accent.copy(alpha = outline),
                shape = RoundedCornerShape(PcCorners.Surface),
            )
            .widgetGestures(
                // Keyed on everything the drop is computed from. `pointerInput` keeps the lambdas
                // it was created with until a key changes, so leaving them out means a second drag
                // is measured from the cell the widget occupied before the first one.
                keys = arrayOf(placement, cellWidthPx, cellHeightPx, columnsAvailable, rowsAvailable),
                // In resize mode the handles own the gesture; moving and resizing at once would
                // make it ambiguous which one a drag meant (widget-drag.md requirement 9).
                enabled = !isResizing,
                onLongPress = { menuOpen = true },
                onDrag = { delta -> dragged += delta },
                onDragEnd = {
                    val landed = cellAfterDrag(
                        from = placement.cell,
                        span = placement.span,
                        deltaX = dragged.x,
                        deltaY = dragged.y,
                        cellWidth = cellWidthPx,
                        cellHeight = cellHeightPx,
                        columnsAvailable = columnsAvailable,
                        rowsAvailable = rowsAvailable,
                    )
                    dragged = Offset.Zero
                    if (landed != placement.cell) onMove(landed)
                },
            ),
    ) {
        // Drawn offset, not laid out offset: a layer translation costs no relayout and, unlike an
        // `offset` here, leaves the gesture node above it exactly where the grid put it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = dragged.x; translationY = dragged.y },
        ) {
            HostedWidget(view = view, modifier = Modifier.fillMaxSize())
        }

        if (isResizing) {
            WidgetResizeFrame(
                permission = permission,
                onDragEdge = onResizeDrag,
                onDragStart = onResizeStart,
                onDragEnd = onResizeEnd,
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            // Offered only where it would do something: a RESIZE_NONE widget still needs a menu,
            // because it still needs removing (widget-removal.md requirement 3).
            if (permission.isResizable) {
                DropdownMenuItem(
                    text = { Text("Resize") },
                    onClick = { onOpenResize(); menuOpen = false },
                )
            }
            DropdownMenuItem(
                text = { Text("Remove widget") },
                onClick = { onRemove(); menuOpen = false },
            )
        }
    }
}

/** What the press turned out to be, once it was over. */
private enum class PressOutcome { Lifted, Moved }

/**
 * Tell a tap, a hold and a drag apart on a widget, without stealing the first of them.
 *
 * An `AppWidgetHostView` handles its own clicks — that is the point of a widget — so the container
 * watches the **Initial** pass and consumes nothing while it is still deciding. A press that lifts
 * without moving was never ours and the widget has already had it. A press held still past the
 * long-press timeout is the menu. Movement past the touch slop is a drag, and only from that
 * moment are the events consumed.
 */
private fun Modifier.widgetGestures(
    enabled: Boolean,
    keys: Array<Any?>,
    onLongPress: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
): Modifier = if (!enabled) this else this.pointerInput(enabled, *keys) {
    val slop = viewConfiguration.touchSlop
    val longPressMillis = viewConfiguration.longPressTimeoutMillis

    awaitEachGesture {
        val down = awaitPointerEvent(PointerEventPass.Initial)
            .changes.firstOrNull { it.pressed } ?: return@awaitEachGesture

        var travelled = Offset.Zero
        // Nothing is consumed in here. `withTimeoutOrNull` returning null is the hold.
        val outcome = withTimeoutOrNull(longPressMillis) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                    ?: return@withTimeoutOrNull PressOutcome.Lifted
                if (!change.pressed) return@withTimeoutOrNull PressOutcome.Lifted
                travelled += change.positionChange()
                if (travelled.getDistance() > slop) return@withTimeoutOrNull PressOutcome.Moved
            }
            @Suppress("UNREACHABLE_CODE") PressOutcome.Lifted
        }

        when (outcome) {
            null -> onLongPress()
            PressOutcome.Lifted -> Unit
            PressOutcome.Moved -> {
                // Carry the pre-slop travel across so the widget sits under the finger rather than
                // lagging it by the slop distance for the rest of the drag.
                onDrag(travelled)
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    // Read the delta before consuming: a consumed change reports no movement.
                    val delta = change.positionChange()
                    change.consume()
                    onDrag(delta)
                }
                onDragEnd()
            }
        }
    }
}

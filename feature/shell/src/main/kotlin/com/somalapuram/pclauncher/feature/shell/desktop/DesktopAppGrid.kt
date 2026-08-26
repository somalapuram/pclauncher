package com.somalapuram.pclauncher.feature.shell.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.data.layout.DesktopCell
import com.somalapuram.pclauncher.core.data.layout.DesktopLayout
import com.somalapuram.pclauncher.core.data.layout.cellAt
import com.somalapuram.pclauncher.core.data.layout.widgetIdOf
import com.somalapuram.pclauncher.core.data.layout.ResizeEdge
import com.somalapuram.pclauncher.core.data.layout.ResizePermission
import com.somalapuram.pclauncher.feature.shell.widget.HostedWidget
import com.somalapuram.pclauncher.feature.shell.widget.WidgetResizeFrame
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcHover
import com.somalapuram.pclauncher.core.design.PcMotion
import com.somalapuram.pclauncher.core.design.PcSize
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.feature.shell.bar.bitmapPainterFor
import com.somalapuram.pclauncher.feature.shell.interaction.appItemGestures
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/**
 * Apps on the desktop, at cells the user chooses.
 *
 * **Positioned, not laid out.** A desktop's defining property is that the user decides where things
 * go — position is data, not a consequence of alphabetical order. That rules out a lazy grid, which
 * can only ever *flow*. A desktop holds tens of icons, not thousands, so nothing is lost.
 *
 * It also fixes something a flowing grid made impossible: the desktop's own context menu. A
 * scrollable grid claims every press across its whole area, so the empty desktop could never
 * receive one. With positioned children, empty space genuinely has no child and the press falls
 * through to the container.
 */
@Composable
fun DesktopAppGrid(
    entries: List<AppEntry>,
    layout: DesktopLayout,
    isPinned: (AppEntry) -> Boolean,
    onLaunch: (AppEntry) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    iconFor: (AppEntry) -> android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier,
    onDragStart: (AppEntry, Offset) -> Unit = { _, _ -> },
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onChangeWallpaper: () -> Unit = {},
    onAddWidget: () -> Unit = {},
    /**
     * Reports what a drop needs to become a cell: cell size, row count, and the grid's origin in
     * root coordinates. Origin is part of it because the drag position is in root space and the
     * grid is inset by its own padding.
     */
    onGridMetrics: (cellWidthPx: Float, cellHeightPx: Float, rows: Int, originInRoot: Offset, widthPx: Float) -> Unit =
        { _, _, _, _, _ -> },
    /** Resolves a placed widget id to its hosted view. Null means the provider failed to inflate. */
    widgetViewFor: (Int) -> android.appwidget.AppWidgetHostView? = { null },
    /** What the provider allows. `None` means the widget cannot enter resize mode at all. */
    resizePermissionFor: (Int) -> ResizePermission = { ResizePermission.None },
    /** Reports a handle drag in pixels; the caller turns it into cells. */
    onResizeDrag: (widgetId: Int, edge: ResizeEdge, pixels: Float) -> Unit = { _, _, _ -> },
    onResizeStart: (widgetId: Int) -> Unit = {},
    onResizeEnd: () -> Unit = {},
    /** Drop a widget on a new cell. Refused by the store when it would overlap. */
    onMoveWidget: (widgetId: Int, cell: DesktopCell) -> Unit = { _, _ -> },
    onRemoveWidget: (widgetId: Int) -> Unit = {},
    /** Report the dp a widget occupies, so its provider can lay out for it. */
    onReportWidgetSize: (widgetId: Int, widthDp: Int, heightDp: Int) -> Unit = { _, _, _ -> },
) {
    val density = LocalDensity.current
    var desktopMenuOpen by remember { mutableStateOf(false) }
    // Where the menu was asked for. A context menu that opens far from the pointer makes the user
    // hunt for the thing they just summoned.
    var menuAt by remember { mutableStateOf(Offset.Zero) }
    // Which widget is being resized, if any. One at a time: two frames on screen would make it
    // ambiguous which one a handle belongs to.
    var resizingWidget by remember { mutableStateOf<Int?>(null) }
    var heightPx by remember { mutableStateOf(0) }
    var widthPx by remember { mutableStateOf(0) }
    var originInRoot by remember { mutableStateOf(Offset.Zero) }

    val cellW = PcSize.DesktopGridCell
    val cellH = DesktopCellHeight
    val cellWpx = with(density) { cellW.toPx() }
    val cellHpx = with(density) { cellH.toPx() }
    val rows = if (cellHpx > 0f) (heightPx / cellHpx).toInt().coerceAtLeast(1) else 1
    val columns = if (cellWpx > 0f) (widthPx / cellWpx).toInt().coerceAtLeast(1) else 1

    // [layout] arrives already auto-placed. Doing it here instead would keep the arrangement
    // private to the UI, and anything else choosing a free cell — adding a widget — would think
    // every cell was empty and drop straight on top of an icon.
    val placed = layout

    // `appItemGestures` creates its pointer handler once and keeps the lambdas it was given, so
    // anything the guard below reads has to be read through state that updates rather than
    // captured by value. Captured, it decides against the empty layout and the zero-height grid of
    // the very first frame — and then the desktop's menu opens on top of every widget, forever.
    val currentPlaced by rememberUpdatedState(placed)
    val currentRows by rememberUpdatedState(rows)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(PcSpacing.Large)
            .onGloballyPositioned {
                heightPx = it.size.height
                widthPx = it.size.width
                originInRoot = it.positionInRoot()
                onGridMetrics(cellWpx, cellHpx, rows, originInRoot, it.size.width.toFloat())
            }
            // The empty desktop's own gesture. Children are hit-tested first, so an icon handles
            // its own press and anything landing on bare desktop arrives here.
            .appItemGestures(
                key = "desktop-surface",
                // A tap on bare desktop also leaves resize mode — the same "click away to finish"
                // every other editing surface has.
                onClick = { resizingWidget = null },
                onContextMenu = { at ->
                    // A press that landed on a widget belongs to the widget — it means "resize
                    // me", and the desktop's own menu must not open on top of it. Both gestures
                    // fire at the same instant otherwise, and which one wins is a race.
                    val over = cellAt(at.x, at.y, cellWpx, cellHpx, currentRows)
                        ?.let { cell -> currentPlaced.placements.firstOrNull { it.covers(cell) } }
                    val onWidget = over != null && widgetIdOf(over.id) != null

                    if (!onWidget) {
                        menuAt = at
                        desktopMenuOpen = true
                    }
                },
            ),
    ) {
        DropdownMenu(
            expanded = desktopMenuOpen,
            onDismissRequest = { desktopMenuOpen = false },
            offset = with(density) { DpOffset(menuAt.x.toDp(), menuAt.y.toDp()) },
        ) {
            DropdownMenuItem(
                text = { Text("Change wallpaper") },
                onClick = { onChangeWallpaper(); desktopMenuOpen = false },
            )
            DropdownMenuItem(
                text = { Text("Add widget") },
                onClick = { onAddWidget(); desktopMenuOpen = false },
            )
        }

        // Widgets share the icons' cell space, so they are drawn from the same placements.
        placed.placements.forEach { placement ->
            val widgetId = widgetIdOf(placement.id) ?: return@forEach

            DesktopWidget(
                placement = placement,
                widgetId = widgetId,
                view = widgetViewFor(widgetId),
                permission = resizePermissionFor(widgetId),
                isResizing = resizingWidget == widgetId,
                cellWidth = cellW,
                cellHeight = cellH,
                cellWidthPx = cellWpx,
                cellHeightPx = cellHpx,
                columnsAvailable = columns,
                rowsAvailable = rows,
                onOpenResize = { resizingWidget = widgetId },
                onRemove = {
                    // The frame belongs to a widget that is about to stop existing.
                    if (resizingWidget == widgetId) resizingWidget = null
                    onRemoveWidget(widgetId)
                },
                onMove = { cell -> onMoveWidget(widgetId, cell) },
                onResizeDrag = { edge, pixels -> onResizeDrag(widgetId, edge, pixels) },
                onResizeStart = { onResizeStart(widgetId) },
                onResizeEnd = onResizeEnd,
                onReportSize = { w, h -> onReportWidgetSize(widgetId, w, h) },
            )
        }

        entries.forEach { entry ->
            val id = entry.key.component.flattenToShortString()
            val cell = placed.cellFor(id) ?: DesktopCell(0, 0)

            DesktopIcon(
                entry = entry,
                isPinned = isPinned(entry),
                painter = iconFor(entry),
                onLaunch = { onLaunch(entry) },
                onTogglePin = { onTogglePin(entry) },
                onDragStart = { local -> onDragStart(entry, originInRoot + local) },
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                modifier = Modifier
                    .offset { IntOffset((cell.column * cellWpx).roundToInt(), (cell.row * cellHpx).roundToInt()) }
                    .width(cellW),
            )
        }
    }
}

/** Tall enough for a 52 dp icon plus a two-line label without the rows touching. */
val DesktopCellHeight = 104.dp

@Composable
private fun DesktopIcon(
    entry: AppEntry,
    isPinned: Boolean,
    painter: android.graphics.drawable.Drawable?,
    onLaunch: () -> Unit,
    onTogglePin: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPcColors.current
    var menuOpen by remember { mutableStateOf(false) }
    var originInRoot by remember { mutableStateOf(Offset.Zero) }

    // Hover only; the gesture modifier below owns presses and drags, and taking the press here too
    // would mean two things watching one pointer.
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = PcHover.scaleFor(hovered),
        animationSpec = PcMotion.DockMagnify,
        label = "desktop-icon-scale",
    )
    val wash by animateFloatAsState(
        targetValue = if (menuOpen) PcHover.PressedWash else PcHover.washFor(hovered),
        animationSpec = PcMotion.DockMagnify,
        label = "desktop-icon-wash",
    )

    Box(modifier = modifier.onGloballyPositioned { originInRoot = it.positionInRoot() }) {
        Column(
            modifier = Modifier
                .width(PcSize.DesktopGridCell)
                .hoverable(interactions)
                .appItemGestures(
                    key = entry.key,
                    enabled = entry.isLaunchable,
                    // Nothing scrolls here, so a drag need not wait for the long-press.
                    dragRequiresLongPress = false,
                    onClick = onLaunch,
                    onContextMenu = { _ -> menuOpen = true },
                    onDragStart = { local -> onDragStart(local) },
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                )
                // *After* the gesture, deliberately: a transform applies to everything inside it,
                // so scaling above the gesture node would scale the drag's own coordinates — by 8%
                // exactly while hovering, which is when a mouse drag begins. The hit area stays
                // put and only the picture grows.
                .graphicsLayer { scaleX = scale; scaleY = scale }
                // Icon and label together, so the pair reads as one target rather than two.
                .background(
                    colors.onSurface.copy(alpha = wash),
                    RoundedCornerShape(PcCorners.Popover),
                )
                .padding(vertical = PcSpacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
        ) {
            Box(modifier = Modifier.alpha(if (entry.isLaunchable) 1f else 0.4f)) {
                bitmapPainterFor(painter)?.let {
                    Image(
                        painter = it,
                        contentDescription = null,
                        modifier = Modifier.size(PcSize.DesktopIcon),
                    )
                } ?: Box(Modifier.size(PcSize.DesktopIcon))
            }

            Text(
                text = entry.label,
                color = colors.onSurface,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    // A readable label over an arbitrary wallpaper needs its own ground; a text
                    // shadow would cost a layer per icon on a CPU renderer.
                    .background(colors.scrim.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(if (isPinned) "Unpin from taskbar" else "Pin to taskbar") },
                onClick = { onTogglePin(); menuOpen = false },
            )
        }
    }
}

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
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
import com.somalapuram.pclauncher.core.data.layout.withAutoPlacement
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcSize
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.feature.shell.bar.bitmapPainterFor
import com.somalapuram.pclauncher.feature.shell.interaction.appItemGestures
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
    onGridMetrics: (cellWidthPx: Float, cellHeightPx: Float, rows: Int, originInRoot: Offset) -> Unit =
        { _, _, _, _ -> },
) {
    val density = LocalDensity.current
    var desktopMenuOpen by remember { mutableStateOf(false) }
    // Where the menu was asked for. A context menu that opens far from the pointer makes the user
    // hunt for the thing they just summoned.
    var menuAt by remember { mutableStateOf(Offset.Zero) }
    var heightPx by remember { mutableStateOf(0) }
    var originInRoot by remember { mutableStateOf(Offset.Zero) }

    val cellW = PcSize.DesktopGridCell
    val cellH = DesktopCellHeight
    val cellWpx = with(density) { cellW.toPx() }
    val cellHpx = with(density) { cellH.toPx() }
    val rows = if (cellHpx > 0f) (heightPx / cellHpx).toInt().coerceAtLeast(1) else 1

    // Everything without a placement gets one, in the same column-major order the flowing grid
    // used — so this change does not scramble an arrangement anyone is already used to.
    val placed = remember(entries, layout, rows) {
        withAutoPlacement(layout, entries.map { it.key.component.flattenToShortString() }, rows)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(PcSpacing.Large)
            .onGloballyPositioned {
                heightPx = it.size.height
                originInRoot = it.positionInRoot()
                onGridMetrics(cellWpx, cellHpx, rows, originInRoot)
            }
            // The empty desktop's own gesture. Children are hit-tested first, so an icon handles
            // its own press and anything landing on bare desktop arrives here.
            .appItemGestures(
                key = "desktop-surface",
                onClick = {},
                onContextMenu = { at -> menuAt = at; desktopMenuOpen = true },
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

    Box(modifier = modifier.onGloballyPositioned { originInRoot = it.positionInRoot() }) {
        Column(
            modifier = Modifier
                .width(PcSize.DesktopGridCell)
                .background(
                    if (menuOpen) colors.onSurface.copy(alpha = 0.10f)
                    else androidx.compose.ui.graphics.Color.Transparent,
                    RoundedCornerShape(PcCorners.Popover),
                )
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
                .padding(vertical = PcSpacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
        ) {
            Box(modifier = Modifier.alpha(if (entry.isLaunchable) 1f else 0.4f)) {
                bitmapPainterFor(painter)?.let {
                    Image(painter = it, contentDescription = null, modifier = Modifier.size(52.dp))
                } ?: Box(Modifier.size(52.dp))
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

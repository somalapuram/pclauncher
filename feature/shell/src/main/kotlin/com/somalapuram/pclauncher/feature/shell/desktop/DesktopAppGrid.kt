package com.somalapuram.pclauncher.feature.shell.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcSize
import com.somalapuram.pclauncher.core.design.PcSpacing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.somalapuram.pclauncher.feature.shell.bar.bitmapPainterFor
import com.somalapuram.pclauncher.feature.shell.interaction.appItemGestures

/**
 * Apps on the desktop.
 *
 * A first cut of `desktop/icon-grid.md`: a fixed grid that lists what is installed, so apps are
 * reachable and pinnable from the desktop as well as from Start. Free placement, drag, folders and
 * marquee selection belong to that doc and are not here.
 */
@Composable
fun DesktopAppGrid(
    entries: List<AppEntry>,
    isPinned: (AppEntry) -> Boolean,
    onLaunch: (AppEntry) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    iconFor: (AppEntry) -> android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier,
    onDragStart: (AppEntry, Offset) -> Unit = { _, _ -> },
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = PcSize.DesktopGridCell),
        modifier = modifier.fillMaxSize().padding(PcSpacing.Large),
        horizontalArrangement = Arrangement.spacedBy(PcSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(PcSpacing.Small),
    ) {
        items(entries, key = { it.key.component.flattenToShortString() + it.key.user }) { entry ->
            DesktopIcon(
                entry = entry,
                isPinned = isPinned(entry),
                painter = iconFor(entry),
                onLaunch = { onLaunch(entry) },
                onTogglePin = { onTogglePin(entry) },
                onDragStart = { local -> onDragStart(entry, local) },
                onDrag = onDrag,
                onDragEnd = onDragEnd,
            )
        }
    }
}

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
) {
    val colors = LocalPcColors.current
    var menuOpen by remember { mutableStateOf(false) }
    // Drag positions have to be in root coordinates: the drop test compares them against the bar,
    // which lives in a different part of the tree.
    var originInRoot by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.onGloballyPositioned { originInRoot = it.positionInRoot() }) {
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
                    onClick = onLaunch,
                    onContextMenu = { menuOpen = true },
                    onDragStart = { local -> onDragStart(originInRoot + local) },
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

        // Right-click has no Compose primitive on Android; the affordance is a long-press-style
        // secondary action exposed here, and the same menu the Start list uses.
        Text(
            text = "⋯",
            color = colors.onSurfaceMuted,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable { menuOpen = true }
                .padding(horizontal = PcSpacing.ExtraSmall),
        )

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(if (isPinned) "Unpin from taskbar" else "Pin to taskbar") },
                onClick = { onTogglePin(); menuOpen = false },
            )
        }
    }
}

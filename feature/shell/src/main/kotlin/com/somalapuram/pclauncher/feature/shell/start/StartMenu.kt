package com.somalapuram.pclauncher.feature.shell.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.feature.shell.bar.bitmapPainterFor
import com.somalapuram.pclauncher.feature.shell.interaction.appItemGestures

/**
 * The Start menu: every launchable app, with a filter (SRS §6.4).
 *
 * Keyboard-first, because a PC user's hands are already there — the field takes focus on open, ↑/↓
 * move, Enter launches, Esc closes. A Start menu that needs the mouse for every launch is slower
 * than the app drawer it replaces.
 */
@Composable
fun StartMenu(
    entries: List<AppEntry>,
    isPinned: (AppEntry) -> Boolean,
    onLaunch: (AppEntry) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
    iconFor: (AppEntry) -> android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPcColors.current
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()

    val filtered = remember(entries, query) { AppSearch.filter(entries, query) }

    // The selection must survive filtering — clamping rather than resetting keeps the highlight
    // where the user is looking when results shrink under them.
    LaunchedEffect(filtered.size) { selected = selected.coerceIn(0, maxOf(0, filtered.size - 1)) }
    LaunchedEffect(selected) { if (filtered.isNotEmpty()) gridState.animateScrollToItem(selected) }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(
        modifier = modifier
            .width(420.dp)
            .heightIn(max = 560.dp)
            // Swallow clicks that land on the menu's own inert areas — its padding, the gap beside
            // a row. A surface that only *looks* solid lets those fall through to the dismiss
            // layer beneath, so clicking inside the menu would close it. Rows and the search field
            // are hit-tested first and are unaffected.
            .clickable(interactionSource = null, indication = null) { }
            // Opaque, unlike the bar. The bar is thin chrome over wallpaper, where translucency
            // reads as depth; the Start menu is a dense panel sitting over a grid of bright icons,
            // and at 96% those icons still bleed through and look like rendering ghosts. A menu
            // you can read the desktop through is the wrong trade (SRS §6.1 principle 1: the
            // user's content is what matters, and here the menu *is* the content).
            .background(colors.surface, RoundedCornerShape(PcCorners.Surface))
            .border(1.dp, colors.hairline, RoundedCornerShape(PcCorners.Surface))
            .padding(PcSpacing.Medium)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                // Arrow arithmetic lives in moveInGrid: in a grid, up/down move by a whole row,
                // and every seam (row edges, a partial last row) is an off-by-one waiting to happen.
                when (event.key) {
                    Key.DirectionDown -> {
                        selected = moveInGrid(selected, GridMove.Down, filtered.size, StartColumns); true
                    }
                    Key.DirectionUp -> {
                        selected = moveInGrid(selected, GridMove.Up, filtered.size, StartColumns); true
                    }
                    Key.DirectionLeft -> {
                        selected = moveInGrid(selected, GridMove.Left, filtered.size, StartColumns); true
                    }
                    Key.DirectionRight -> {
                        selected = moveInGrid(selected, GridMove.Right, filtered.size, StartColumns); true
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        filtered.getOrNull(selected)?.takeIf { it.isLaunchable }?.let(onLaunch); true
                    }
                    Key.Escape -> { onDismiss(); true }
                    else -> false
                }
            },
    ) {
        SearchField(
            query = query,
            onQueryChange = { query = it; selected = 0 },
            focusRequester = focusRequester,
        )

        Text(
            text = if (query.isBlank()) "All apps" else "${filtered.size} of ${entries.size}",
            color = colors.onSurfaceMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = PcSpacing.Small),
        )

        when {
            entries.isEmpty() -> EmptyNote("Loading apps…")
            filtered.isEmpty() -> EmptyNote("No apps match \"$query\"")
            else -> LazyVerticalGrid(
                // Fixed, not adaptive: the keyboard step size has to be exact, and an adaptive
                // count would make it depend on the measured width.
                columns = GridCells.Fixed(StartColumns),
                state = gridState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
                verticalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
            ) {
                items(filtered, key = { it.key.component.flattenToShortString() + it.key.user }) { entry ->
                    AppCell(
                        entry = entry,
                        isSelected = filtered.getOrNull(selected) === entry,
                        isPinned = isPinned(entry),
                        painter = iconFor(entry),
                        onLaunch = { onLaunch(entry) },
                        onTogglePin = { onTogglePin(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val colors = LocalPcColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(colors.onSurface.copy(alpha = 0.07f), RoundedCornerShape(PcCorners.Popover))
            .padding(horizontal = PcSpacing.Medium),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (query.isEmpty()) {
            Text("Search apps", color = colors.onSurfaceMuted, fontSize = 14.sp)
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        )
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        color = LocalPcColors.current.onSurfaceMuted,
        fontSize = 13.sp,
        modifier = Modifier.padding(PcSpacing.Large),
    )
}

@Composable
private fun AppCell(
    entry: AppEntry,
    isSelected: Boolean,
    isPinned: Boolean,
    painter: android.graphics.drawable.Drawable?,
    onLaunch: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val colors = LocalPcColors.current
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    if (isSelected) colors.onSurface.copy(alpha = 0.14f)
                    else androidx.compose.ui.graphics.Color.Transparent,
                    RoundedCornerShape(PcCorners.Popover),
                )
                .appItemGestures(
                    key = entry.key,
                    enabled = entry.isLaunchable,
                    onClick = onLaunch,
                    onContextMenu = { menuOpen = true },
                )
                .padding(vertical = PcSpacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
        ) {
            // Unavailable and suspended entries are greyed rather than hidden — the inventory's
            // contract, carried through to every surface that lists an app.
            Box(modifier = Modifier.alpha(if (entry.isLaunchable) 1f else 0.4f)) {
                bitmapPainterFor(painter)?.let {
                    Image(painter = it, contentDescription = null, modifier = Modifier.size(44.dp))
                } ?: Box(Modifier.size(44.dp))
            }

            Text(
                text = entry.label,
                color = if (entry.isLaunchable) colors.onSurface else colors.onSurfaceMuted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }

        if (isPinned) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(6.dp)
                    .background(colors.accent, RoundedCornerShape(50)),
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

/**
 * Start's column count.
 *
 * Fixed so keyboard movement has exact arithmetic. Five columns of 44 dp icons fit the 420 dp
 * popover with room for two-line labels, and show thirty-five apps where the old list showed
 * eleven.
 */
const val StartColumns = 5

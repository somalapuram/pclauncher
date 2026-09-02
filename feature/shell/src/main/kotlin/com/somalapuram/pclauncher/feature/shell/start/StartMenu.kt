package com.somalapuram.pclauncher.feature.shell.start

import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
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
import com.somalapuram.pclauncher.core.design.PcMenu
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcHover
import com.somalapuram.pclauncher.core.design.PcMotion
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.feature.shell.bar.bitmapPainterFor
import com.somalapuram.pclauncher.feature.shell.interaction.appItemGestures
import com.somalapuram.pclauncher.feature.shell.input.handleShellKey

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
    /** Most recently launched first. Empty until the user has opened something (recent-apps.md). */
    recent: List<AppEntry> = emptyList(),
    deviceName: String? = null,
    /** What the shell is allowed to do to the device, read at runtime (start-power.md). */
    powerPrivileges: PowerPrivileges = PowerPrivileges(),
    onPowerAction: (PowerAction) -> Unit = {},
    /** Ctrl+Esc while the menu has focus. Closing it is the host's business, not the menu's. */
    onToggleStart: () -> Unit = onDismiss,
) {
    val colors = LocalPcColors.current
    var query by remember { mutableStateOf("") }
    // Null until the user engages the keyboard. An Int starting at zero cannot express "nothing
    // selected", which is why the menu used to open with its first app already highlighted
    // (start-selection.md).
    var selected by remember { mutableStateOf<Selection>(null) }
    val focusRequester = remember { FocusRequester() }
    // The menu itself, not the search field. A key event only reaches `onPreviewKeyEvent` if
    // something in this subtree holds focus, and nothing did — the field deliberately does not take
    // it (see `shouldFocusSearchOnOpen`), so the arrow keys had nowhere to land. Focusing the panel
    // gives them a target without putting a caret in a text field, which is what raises the IME
    // over the menu (recent-apps.md requirement 7).
    val panelFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()

    val filtered = remember(entries, query) { AppSearch.filter(entries, query) }

    // Hidden while searching: the user is looking at results, and a row of unrelated apps above
    // them is noise (recent-apps.md requirement 3).
    val sections = remember(recent, filtered, query) {
        StartSections(
            recent = recentFor(recent, query),
            all = filtered,
            columns = StartColumns,
        )
    }

    // The selection must survive filtering — clamping rather than resetting keeps the highlight
    // where the user is looking when results shrink under them.
    LaunchedEffect(sections.navigable.size) {
        selected = selectionAfterFilter(selected, sections.navigable.size)
    }
    LaunchedEffect(selected) {
        // Only when the caret is in the grid. The recent row is always in view, and scrolling to it
        // would yank the grid about under a selection that is not in it.
        sections.gridIndexFor(selected)?.let { gridState.animateScrollToItem(it) }
    }
    // SRS §6.4 wants typing to land in search the moment the menu opens — which on the target
    // machine means a hardware keyboard, and there focusing costs nothing. Where the only keyboard
    // is the on-screen one, focusing raises it over half the shell, including the menu it belongs
    // to, and the user has to dismiss it before they can see what they opened.
    LaunchedEffect(Unit) {
        if (shouldFocusSearchOnOpen()) runCatching { focusRequester.requestFocus() }
        else runCatching { panelFocusRequester.requestFocus() }
    }

    Column(
        modifier = modifier
            .width(420.dp)
            // Taller when the Recent row is up, by roughly what that row costs. Holding the
            // panel at one height instead squeezes the grid below it, and the grid then clips
            // through the middle of a row of labels — which reads as a rendering fault rather
            // than as "scroll for more".
            .heightIn(max = if (sections.recent.isEmpty()) PanelHeight else PanelHeightWithRecent)
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
            // Above the desktop, not cut into it: a hard 1 px edge left the eye to work the
            // boundary out from colour alone over a busy wallpaper (visual-pass.md).
            .shadow(PanelElevation, RoundedCornerShape(PcCorners.Surface), clip = false)
            .background(colors.surface, RoundedCornerShape(PcCorners.Surface))
            .border(1.dp, colors.hairline, RoundedCornerShape(PcCorners.Surface))
            .padding(PcSpacing.Medium)
            .focusRequester(panelFocusRequester)
            // Focusable, but with no indication of its own: this exists to receive keys, and a
            // focus ring around the whole panel would say something untrue about where Enter acts.
            .focusable()
            // Preview, so the arrows are read on the way *down* — they steer the grid even when
            // the search field below has taken focus from a click.
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                // Arrow arithmetic lives in moveInGrid: in a grid, up/down move by a whole row,
                // and every seam (row edges, a partial last row) is an off-by-one waiting to happen.
                when (event.key) {
                    Key.DirectionDown -> {
                        selected = selectionAfterMoveInSections(selected, GridMove.Down, sections); true
                    }
                    Key.DirectionUp -> {
                        selected = selectionAfterMoveInSections(selected, GridMove.Up, sections); true
                    }
                    Key.DirectionLeft -> {
                        selected = selectionAfterMoveInSections(selected, GridMove.Left, sections); true
                    }
                    Key.DirectionRight -> {
                        selected = selectionAfterMoveInSections(selected, GridMove.Right, sections); true
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        // Nothing selected launches nothing: Enter must not fire an app the user
                        // never pointed the keyboard at.
                        sections.entryAt(selected)?.takeIf { it.isLaunchable }?.let(onLaunch)
                        true
                    }
                    // Typing selects the top hit, so Enter launches what the user is looking at —
                    // the same thing a click in the search field does.
                    else -> handleShellKey(event, query, onToggleStart, onPowerAction) {
                        query = it
                        selected = selectionAfterQuery(it, filtered.size)
                    }
                }
            },
    ) {
        SearchField(
            query = query,
            onQueryChange = { query = it; selected = selectionAfterQuery(it, filtered.size) },
            focusRequester = focusRequester,
        )

        RecentRow(
            entries = sections.recent,
            columns = StartColumns,
            selectedIndex = selected?.takeIf { it < sections.recentSlots },
            isPinned = isPinned,
            iconFor = iconFor,
            onLaunch = onLaunch,
            onTogglePin = onTogglePin,
        )

        Text(
            text = if (query.isBlank()) "All apps" else "${filtered.size} of ${entries.size}",
            color = colors.onSurfaceMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            // Room above it so it reads as a heading over the grid rather than a caption stuck to
            // the search field.
            modifier = Modifier.padding(top = PcSpacing.Medium, bottom = PcSpacing.Small),
        )

        when {
            entries.isEmpty() -> EmptyNote("Loading apps…")
            filtered.isEmpty() -> EmptyNote("No apps match \"$query\"")
            else -> LazyVerticalGrid(
                // Fixed, not adaptive: the keyboard step size has to be exact, and an adaptive
                // count would make it depend on the measured width.
                columns = GridCells.Fixed(StartColumns),
                state = gridState,
                // The flexible one, so the footer keeps its own height instead of being clipped
                // by a grid that took everything.
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
                verticalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
            ) {
                items(filtered, key = { it.key.component.flattenToShortString() + it.key.user }) { entry ->
                    AppCell(
                        entry = entry,
                        isSelected = sections.entryAt(selected) === entry &&
                            sections.gridIndexFor(selected) != null,
                        isPinned = isPinned(entry),
                        painter = iconFor(entry),
                        onLaunch = { onLaunch(entry) },
                        onTogglePin = { onTogglePin(entry) },
                    )
                }
            }
        }

        PowerFooter(
            deviceName = deviceName,
            privileges = powerPrivileges,
            onAction = onPowerAction,
        )
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

/** Four full rows of apps under the search field, with the footer below them. */
private val PanelHeight = 560.dp

/**
 * The Recent row and its heading, plus three whole rows of apps rather than four.
 *
 * Keeping all four would need 684 dp, and on an 800 dp-tall display with the bar below and the
 * status bar above that leaves the panel touching the status bar. One row of All apps is the right
 * thing to give up: it is a scrolling list, and the row above it is not.
 */
private val PanelHeightWithRecent = 600.dp

/** Enough to lift the panel off the wallpaper; one shadow, well inside the budget (SRS §4.3). */
private val PanelElevation = 16.dp

package com.somalapuram.pclauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.apps.AppInventory
import com.somalapuram.pclauncher.core.design.PcGlyphs
import com.somalapuram.pclauncher.desktop.BarStateFactory
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.somalapuram.pclauncher.core.data.layout.DesktopCell
import com.somalapuram.pclauncher.core.data.layout.DesktopLayout
import com.somalapuram.pclauncher.core.data.layout.firstFreeCell
import com.somalapuram.pclauncher.core.data.layout.withAutoPlacement
import com.somalapuram.pclauncher.core.data.pins.Pins
import com.somalapuram.pclauncher.feature.shell.bar.ShellBar
import com.somalapuram.pclauncher.feature.shell.desktop.DesktopAppGrid
import com.somalapuram.pclauncher.feature.shell.start.PinResolution
import com.somalapuram.pclauncher.feature.shell.interaction.DragGhost
import com.somalapuram.pclauncher.feature.shell.interaction.DragOrigin
import com.somalapuram.pclauncher.feature.shell.interaction.DragState
import com.somalapuram.pclauncher.feature.shell.interaction.DropTarget
import com.somalapuram.pclauncher.feature.shell.start.StartMenu
import com.somalapuram.pclauncher.feature.shell.tray.TrayState
import com.somalapuram.pclauncher.feature.shell.widget.WidgetChoice
import com.somalapuram.pclauncher.feature.shell.widget.WidgetPicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.LocalSurfaceTreatment
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.core.design.PcTheme
import com.somalapuram.pclauncher.core.design.SurfaceTreatment

/**
 * The desktop surface.
 *
 * The window is transparent and shows the wallpaper (`android:windowShowWallpaper`), which is also
 * how the home app renders *behind* desktop windows on the target device (SRS §4.1).
 */
@Composable
fun HomeScreen(
    outcome: StartupOutcome,
    inventory: StateFlow<AppInventory> = MutableStateFlow(AppInventory()),
    pins: StateFlow<Pins> = MutableStateFlow(Pins()),
    desktopLayout: StateFlow<DesktopLayout> = MutableStateFlow(DesktopLayout()),
    iconFor: (AppEntry) -> android.graphics.drawable.Drawable? = { null },
    onLaunchApp: (AppEntry) -> Unit = {},
    onTogglePin: (AppEntry) -> Unit = {},
    onPlace: (AppEntry, DesktopCell) -> Unit = { _, _ -> },
    tray: TrayState = TrayState(),
    onChangeWallpaper: () -> Unit = {},
    widgetChoices: () -> List<WidgetChoice> = { emptyList() },
    onPickWidget: (WidgetChoice, DesktopCell) -> Unit = { _, _ -> },
    widgetViewFor: (Int) -> android.appwidget.AppWidgetHostView? = { null },
    isDefaultHome: Boolean,
    onSetDefaultHome: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    safeModeApps: List<AppEntry> = emptyList(),
) {
    val apps by inventory.collectAsState()
    val currentPins by pins.collectAsState()
    val layout by desktopLayout.collectAsState()
    // Reported by the desktop once it has measured, so a drop can be turned into a cell.
    var cellW by remember { mutableStateOf(0f) }
    var cellH by remember { mutableStateOf(0f) }
    var gridRows by remember { mutableStateOf(1) }
    // Auto-placement happens once, here, so the desktop and anything looking for a free cell agree
    // about what is occupied. Computed in the same column-major order the flowing grid used.
    val effectiveLayout = remember(apps, layout, gridRows) {
        withAutoPlacement(layout, apps.entries.map { it.key.component.flattenToShortString() }, gridRows)
    }

    var desktopOrigin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var startOpen by remember { mutableStateOf(false) }
    var widgetPickerOpen by remember { mutableStateOf(false) }

    val pinnedIds = currentPins.items.map { it.component }
    val isPinned = { entry: AppEntry -> PinResolution.isPinned(pinnedIds, entry) }

    // One drag, shared. The desktop starts drags the bar has to answer and vice versa, so a single
    // value is the only version that cannot disagree with itself (direct-manipulation.md).
    val drag = remember { DragState() }
    var barTopY by remember { mutableStateOf(Float.MAX_VALUE) }
    var barBottomY by remember { mutableStateOf(Float.MAX_VALUE) }

    fun finishDrag() {
        // A desktop icon dropped back on the desktop is a *move*, not an unpin — unpinning is what
        // dropping a dock icon there means. Same gesture, different meaning by origin.
        val movingOnDesktop = drag.origin == DragOrigin.Desktop && drag.target == DropTarget.Desktop
        val entry = drag.entry
        val at = drag.position

        if (movingOnDesktop && entry != null) {
            val cell = com.somalapuram.pclauncher.core.data.layout.cellAt(
                x = at.x - desktopOrigin.x,
                y = at.y - desktopOrigin.y,
                cellWidth = cellW,
                cellHeight = cellH,
                rowsPerColumn = gridRows,
            )
            drag.cancel()
            if (cell != null) onPlace(entry, cell)
            return
        }

        // The store operation is the same one the context menu calls — dragging is a second route
        // to it, never a second implementation.
        drag.end(isPinned)?.let { onTogglePin(it.entry) }
    }

    // An outer Box so the Start menu can float *over* the desktop. Putting it in the column made
    // it a sibling that squeezed the desktop upward as it opened.
    Box(modifier = modifier.fillMaxSize().safeDrawingPadding()) {

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (outcome) {
                is StartupOutcome.Ready -> Desktop(
                    environment = outcome.environment,
                    isDefaultHome = isDefaultHome,
                    onSetDefaultHome = onSetDefaultHome,
                    apps = apps.entries,
                    isPinned = isPinned,
                    onLaunch = onLaunchApp,
                    onTogglePin = onTogglePin,
                    iconFor = iconFor,
                    onDragStart = { entry, at -> drag.start(entry, DragOrigin.Desktop, at) },
                    onDrag = { delta -> drag.moveTo(drag.position + delta, barTopY, barBottomY) },
                    onDragEnd = { finishDrag() },
                    onChangeWallpaper = onChangeWallpaper,
                    onAddWidget = { widgetPickerOpen = true },
                    widgetViewFor = widgetViewFor,
                    layout = effectiveLayout,
                    onGridMetrics = { w, h, rows, origin ->
                        cellW = w; cellH = h; gridRows = rows; desktopOrigin = origin
                    },
                )

                is StartupOutcome.Fallback -> FallbackDesktop(
                    reason = outcome.reason,
                    onRetry = onRetry,
                    onSetDefaultHome = onSetDefaultHome,
                    apps = safeModeApps,
                )
            }
        }

        // The bar renders from the inventory Flow, so it appears with the desktop and fills in —
        // an empty dock is a valid first frame, never a spinner (dock-taskbar.md requirement 8).
        // Safe mode gets no bar: it must not depend on the inventory or the icon cache.
        if (outcome is StartupOutcome.Ready) {
            val docked = PinResolution.resolve(apps.entries, pinnedIds)
            ShellBar(
                state = BarStateFactory.from(apps.copy(entries = docked), iconFor = iconFor),
                startGlyph = PcGlyphs.Start,
                isStartOpen = startOpen,
                onStartClick = { startOpen = !startOpen },
                onDockItemClick = { item ->
                    docked.firstOrNull { it.key.component.flattenToShortString() == item.id }
                        ?.let(onLaunchApp)
                },
                onWindowFocus = {},
                onWindowClose = {},
                onShowDesktop = {},
                onBoundsChanged = { top, bottom -> barTopY = top; barBottomY = bottom },
                isDropTarget = drag.isActive && drag.target == DropTarget.Dock,
                onItemContextMenu = { item ->
                    docked.firstOrNull { it.key.component.flattenToShortString() == item.id }
                        ?.let(onTogglePin)
                },
                onItemDragStart = { item, at ->
                    docked.firstOrNull { it.key.component.flattenToShortString() == item.id }
                        ?.let { drag.start(it, DragOrigin.Dock, at) }
                },
                onItemDrag = { delta -> drag.moveTo(drag.position + delta, barTopY, barBottomY) },
                onItemDragEnd = { finishDrag() },
                tray = tray,
                modifier = Modifier.padding(
                    horizontal = PcSpacing.Large,
                    vertical = PcSpacing.Small,
                ),
            )
        }
    }

    if (drag.isActive) {
        DragGhost(drag = drag, iconFor = iconFor)
    }

    if (widgetPickerOpen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = null, indication = null) { widgetPickerOpen = false },
        )
        Box(modifier = Modifier.align(Alignment.Center)) {
            WidgetPicker(
                choices = widgetChoices(),
                onPick = { choice ->
                    // Against the *effective* layout, so a widget never lands on an icon.
                    onPickWidget(choice, firstFreeCell(effectiveLayout, gridRows))
                    widgetPickerOpen = false
                },
                onDismiss = { widgetPickerOpen = false },
            )
        }
    }

    if (outcome is StartupOutcome.Ready && startOpen) {
        // A full-screen catcher under the menu. It covers the bar too, so a click on the Start
        // button while the menu is open lands here and closes it once, rather than reaching the
        // button and toggling twice back to open.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(StartScrimTag)
                .clickable(
                    interactionSource = null,
                    indication = null,
                ) { startOpen = false },
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = PcSpacing.Large, bottom = StartMenuBottomInset),
        ) {
            StartMenu(
                entries = apps.entries,
                isPinned = isPinned,
                onLaunch = { onLaunchApp(it); startOpen = false },
                onTogglePin = onTogglePin,
                onDismiss = { startOpen = false },
                iconFor = iconFor,
            )
        }
    }
    }
}

/**
 * The dismiss layer.
 *
 * Tagged so a test can assert it is reachable: the failure mode here is z-order, where the scrim
 * ends up *under* the desktop and clicks silently never reach it. That is invisible in a diff and
 * only shows up by trying to click.
 */
const val StartScrimTag = "start-scrim"

/** Clears the bar, so the menu sits above it rather than over it. */
private val StartMenuBottomInset = 72.dp

@Composable
private fun Desktop(
    environment: DesktopEnvironment,
    isDefaultHome: Boolean,
    onSetDefaultHome: () -> Unit,
    apps: List<AppEntry>,
    isPinned: (AppEntry) -> Boolean,
    onLaunch: (AppEntry) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    iconFor: (AppEntry) -> android.graphics.drawable.Drawable?,
    onDragStart: (AppEntry, androidx.compose.ui.geometry.Offset) -> Unit,
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: () -> Unit,
    onChangeWallpaper: () -> Unit,
    onAddWidget: () -> Unit,
    widgetViewFor: (Int) -> android.appwidget.AppWidgetHostView?,
    layout: DesktopLayout,
    onGridMetrics: (Float, Float, Int, androidx.compose.ui.geometry.Offset) -> Unit,
) {
    if (apps.isNotEmpty()) {
        DesktopAppGrid(
            entries = apps,
            layout = layout,
            onGridMetrics = onGridMetrics,
            isPinned = isPinned,
            onLaunch = onLaunch,
            onTogglePin = onTogglePin,
            iconFor = iconFor,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onChangeWallpaper = onChangeWallpaper,
            onAddWidget = onAddWidget,
        )
        if (!isDefaultHome) {
            SetHomePrompt(onSetDefaultHome)
        }
        return
    }

    // Placeholder only. The icon grid, folders, and widgets arrive with
    // docs/requirements/desktop/icon-grid.md; the dock and taskbar with shell/overlay-service.md.
    ShellCard {
        Text(
            text = "pclauncher",
            color = LocalPcColors.current.onSurface,
            fontSize = 22.sp,
        )
        Text(
            text = "Windowing tier: ${environment.tier}",
            color = LocalPcColors.current.onSurfaceMuted,
            fontSize = 13.sp,
        )
        if (!isDefaultHome) {
            TextButton(onClick = onSetDefaultHome) {
                Text("Set as default home", color = LocalPcColors.current.accent)
            }
        }
    }
}

/**
 * The guarded desktop (GATE 4). Reached when startup failed — it must not depend on anything that
 * could have been what failed, so it takes no injected state at all.
 */
@Composable
private fun FallbackDesktop(
    reason: FallbackReason,
    onRetry: () -> Unit,
    onSetDefaultHome: () -> Unit,
    apps: List<AppEntry> = emptyList(),
) {
    ShellCard {
        Text(
            text = "pclauncher started in safe mode",
            color = LocalPcColors.current.onSurface,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = when (reason) {
                FallbackReason.StartupFailed ->
                    "The shell could not load its settings, so the desktop is running with defaults. " +
                        "Your apps are unaffected."
            },
            color = LocalPcColors.current.onSurfaceMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text("Try again", color = LocalPcColors.current.accent)
        }
        TextButton(onClick = onSetDefaultHome) {
            Text("Home settings", color = LocalPcColors.current.onSurfaceMuted)
        }

        // Labels only, no icons: safe mode must not touch the icon cache, which is one of the
        // things whose failure lands the user here (GATE 4).
        if (apps.isNotEmpty()) {
            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                items(apps, key = { it.key.component.flattenToShortString() }) { app ->
                    Text(
                        text = app.label,
                        color = LocalPcColors.current.onSurface,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = PcSpacing.ExtraSmall),
                    )
                }
            }
        }
    }
}

/** Shown over the grid until pclauncher owns the home role. */
@Composable
private fun SetHomePrompt(onSetDefaultHome: () -> Unit) {
    ShellCard {
        Text("pclauncher is not your home screen yet", color = LocalPcColors.current.onSurface, fontSize = 15.sp)
        TextButton(onClick = onSetDefaultHome) {
            Text("Set as default home", color = LocalPcColors.current.accent)
        }
    }
}

/** A shell surface, drawn with whatever treatment the renderer can afford (SRS §4.3). */
@Composable
private fun ShellCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalPcColors.current
    val alpha = when (val treatment = LocalSurfaceTreatment.current) {
        is SurfaceTreatment.Scrim -> treatment.alpha
        is SurfaceTreatment.Blur -> treatment.scrimAlpha
    }

    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .background(colors.scrim.copy(alpha = alpha), RoundedCornerShape(PcCorners.Surface))
            .border(PcSpacing.Hairline, colors.hairline, RoundedCornerShape(PcCorners.Surface))
            .padding(PcSpacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PcSpacing.Small),
        content = { content() },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF202430, widthDp = 1280, heightDp = 800)
@Composable
private fun DesktopPreview() {
    PcTheme(darkTheme = true) {
        Box(Modifier.fillMaxSize().background(Color(0xFF202430))) {
            HomeScreen(
                outcome = StartupOutcome.Ready(
                    DesktopEnvironment(com.somalapuram.pclauncher.platform.privileged.Tier.Basic),
                ),
                isDefaultHome = false,
                onSetDefaultHome = {},
                onRetry = {},
            )
        }
    }
}

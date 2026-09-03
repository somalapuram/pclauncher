package com.somalapuram.pclauncher.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.somalapuram.pclauncher.feature.shell.input.ShellAction
import com.somalapuram.pclauncher.feature.shell.input.modifiers
import com.somalapuram.pclauncher.feature.shell.input.shortcutFor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.design.PcGlyphs
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.desktop.AppLauncher
import com.somalapuram.pclauncher.desktop.BarStateFactory
import com.somalapuram.pclauncher.desktop.ShellController
import com.somalapuram.pclauncher.feature.shell.bar.ShellBar
import com.somalapuram.pclauncher.feature.shell.start.PinResolution
import com.somalapuram.pclauncher.feature.shell.start.PowerAction
import com.somalapuram.pclauncher.feature.shell.start.StartMenu
import com.somalapuram.pclauncher.feature.shell.start.displayableDeviceName
import com.somalapuram.pclauncher.feature.shell.tray.TrayAction
import com.somalapuram.pclauncher.feature.shell.tray.TrayState
import com.somalapuram.pclauncher.feature.shell.tray.QuickSettingsPanel
import com.somalapuram.pclauncher.feature.shell.tray.dismissesPanel
import com.somalapuram.pclauncher.feature.shell.menu.ShellMenu
import com.somalapuram.pclauncher.feature.shell.menu.ShellMenuBarClearance
import com.somalapuram.pclauncher.feature.shell.menu.ShellMenuEdgeInset
import com.somalapuram.pclauncher.power.powerPrivilegesOf

/**
 * The chrome as it appears in the overlay: a bar window, and a menu window above it.
 *
 * Split across two windows rather than one, because a single window would have to grow when the
 * Start menu opens and shrink when it closes — and each of those resizes blinks the bar
 * (overlay-service.md). Here the bar window never changes size, so it never blinks; the menu simply
 * arrives in a window of its own.
 *
 * Deliberately thinner than the home activity's copy. Dragging between the dock and the desktop
 * cannot work here — they are separate windows and a Compose drag does not cross one — so the drag
 * hooks are absent rather than present and broken. Pinning by context menu, which is the same store
 * operation, works exactly as it does on the desktop.
 */
@Composable
fun OverlayBar(
    shell: ShellController?,
    iconFor: (AppEntry) -> android.graphics.drawable.Drawable?,
    tray: TrayState,
    onTrayAction: (TrayAction) -> Unit,
    openMenu: ShellMenu,
    onToggleMenu: (ShellMenu) -> Unit,
) {
    val context = LocalContext.current
    val inventory by (shell?.inventory ?: EmptyInventory).collectAsState()
    val pins by (shell?.pins ?: EmptyPins).collectAsState()
    val launcher = remember(context, shell) {
        AppLauncher(context.applicationContext) { shell?.recordLaunch(it) }
    }
    val docked = PinResolution.resolve(inventory.entries, pins.items.map { it.component })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Headroom for magnification. A dock icon at peak scale stands taller than the bar it
            // sits in, and this window is exactly as tall as its content — without the gap the
            // window edge would slice the top off the icon under the pointer.
            .padding(top = MagnificationHeadroom),
    ) {
        ShellBar(
            state = BarStateFactory.from(inventory.copy(entries = docked), iconFor = iconFor),
            startGlyph = PcGlyphs.Start,
            isStartOpen = openMenu == ShellMenu.Start,
            onStartClick = { onToggleMenu(ShellMenu.Start) },
            isTrayOpen = openMenu == ShellMenu.QuickSettings,
            onTrayToggle = { onToggleMenu(ShellMenu.QuickSettings) },
            onDockItemClick = { item ->
                docked.firstOrNull { it.key.component.flattenToShortString() == item.id }
                    ?.let(launcher::launch)
            },
            onWindowFocus = {},
            onWindowClose = {},
            onShowDesktop = {},
            onItemContextMenu = { item ->
                docked.firstOrNull { it.key.component.flattenToShortString() == item.id }
                    ?.let { shell?.togglePin(it) }
            },
            tray = tray,
            onTrayAction = onTrayAction,
            modifier = Modifier.padding(
                horizontal = PcSpacing.Large,
                vertical = PcSpacing.Small,
            ),
        )
    }
}

/**
 * The Start menu, in its own full-screen window above the bar.
 *
 * Full-screen because the click-catcher behind the menu has to cover the screen for a click
 * anywhere else to close it, the same as on the desktop.
 */
@Composable
fun OverlayMenuLayer(
    menu: ShellMenu,
    shell: ShellController?,
    iconFor: (AppEntry) -> android.graphics.drawable.Drawable?,
    tray: TrayState,
    onTrayAction: (TrayAction) -> Unit,
    onPowerAction: (PowerAction) -> Unit,
    onToggleMenu: (ShellMenu) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val inventory by (shell?.inventory ?: EmptyInventory).collectAsState()
    val recent by (shell?.recent ?: EmptyRecent).collectAsState()
    val launcher = remember(context, shell) {
        AppLauncher(context.applicationContext) { shell?.recordLaunch(it) }
    }

    // Keys are handled here rather than inside a menu, because the window draws more than one and
    // only the Start menu ever had a handler — with quick settings up, Ctrl+Esc and Esc did nothing
    // at all (tray-popover-host.md requirement 5, shell-shortcuts.md requirements 1 and 3).
    val layerFocus = remember { FocusRequester() }
    LaunchedEffect(menu) { runCatching { layerFocus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(layerFocus)
            .focusable()
            // Preview, so these are read on the way down: a menu below keeps its own arrows and
            // typing, and only the keys named here are taken from it.
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (shortcutFor(event.key, event.modifiers(), menuOpen = true)) {
                    ShellAction.ToggleStart -> { onToggleMenu(ShellMenu.Start); true }
                    ShellAction.CloseMenu -> { onDismiss(); true }
                    ShellAction.OpenSettings -> { onPowerAction(PowerAction.OpenSettings); true }
                    null -> false
                }
            }
            .clickable(interactionSource = null, indication = null) { onDismiss() },
        // Start at one corner, quick settings at the other — both bottom, both clear of the bar,
        // both inset by the bar's own margin. One expression, so they cannot disagree
        // (tray-popover-host.md requirement 2).
        contentAlignment = if (menu == ShellMenu.QuickSettings) {
            Alignment.BottomEnd
        } else {
            Alignment.BottomStart
        },
    ) {
        Box(
            modifier = Modifier.padding(
                start = ShellMenuEdgeInset,
                end = ShellMenuEdgeInset,
                // Clear of the bar, which is a separate window below this one.
                bottom = ShellMenuBarClearance,
            ),
        ) {
            if (menu == ShellMenu.QuickSettings) {
                QuickSettingsPanel(
                    state = tray,
                    onAction = { action ->
                        onTrayAction(action)
                        if (dismissesPanel(action)) onDismiss()
                    },
                )
                return@Box
            }
            StartMenu(
                entries = inventory.entries,
                recent = recent,
                isPinned = { shell?.isPinned(it) == true },
                onLaunch = { launcher.launch(it); onDismiss() },
                onTogglePin = { shell?.togglePin(it) },
                onDismiss = onDismiss,
                onToggleStart = onDismiss,
                iconFor = iconFor,
                deviceName = displayableDeviceName(
                    deviceName = runCatching {
                        android.provider.Settings.Global.getString(
                            context.contentResolver,
                            android.provider.Settings.Global.DEVICE_NAME,
                        )
                    }.getOrNull(),
                    model = android.os.Build.MODEL,
                ),
                powerPrivileges = powerPrivilegesOf(context),
                onPowerAction = { onDismiss(); onPowerAction(it) },
            )
        }
    }
}

/** How far a dock icon at peak magnification stands above the bar's own height. */
private val MagnificationHeadroom = 12.dp

/** Stand-ins for a shell that could not be built, so the chrome still renders (GATE 4). */
private val EmptyInventory =
    kotlinx.coroutines.flow.MutableStateFlow(com.somalapuram.pclauncher.core.apps.AppInventory())
private val EmptyPins =
    kotlinx.coroutines.flow.MutableStateFlow(com.somalapuram.pclauncher.core.data.pins.Pins())
private val EmptyRecent = kotlinx.coroutines.flow.MutableStateFlow(emptyList<AppEntry>())

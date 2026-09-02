package com.somalapuram.pclauncher.feature.shell.input

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.utf16CodePoint
import com.somalapuram.pclauncher.feature.shell.start.PowerAction

/** The modifiers a Compose key event carries. */
fun KeyEvent.modifiers(): Modifiers = Modifiers(
    ctrl = isCtrlPressed,
    meta = isMetaPressed,
    alt = isAltPressed,
    shift = isShiftPressed,
)

/**
 * Apply a key press to an open menu. Returns whether it was consumed.
 *
 * The decisions are all in [shortcutFor] and [queryAfterKey]; this only turns their answers into
 * calls. Keeping it separate from the menu's own arrow handling is what stops the two growing into
 * one `when` block that nobody can check for overlaps (shell-shortcuts.md requirement 6).
 */
fun handleShellKey(
    event: KeyEvent,
    query: String,
    onToggleStart: () -> Unit,
    onPowerAction: (PowerAction) -> Unit,
    onQueryChange: (String) -> Unit,
): Boolean {
    val modifiers = event.modifiers()

    shortcutFor(event.key, modifiers, menuOpen = true)?.let { action ->
        when (action) {
            ShellAction.ToggleStart, ShellAction.CloseMenu -> onToggleStart()
            ShellAction.OpenSettings -> onPowerAction(PowerAction.OpenSettings)
        }
        return true
    }

    // SRS §6.4: typing anywhere in the menu filters it, without a caret ever being placed in the
    // field — which is what would raise the on-screen keyboard over the menu (SearchFocus.kt).
    queryAfterKey(query, event.key, event.utf16CodePoint, modifiers, menuOpen = true)?.let {
        onQueryChange(it)
        return true
    }

    return false
}

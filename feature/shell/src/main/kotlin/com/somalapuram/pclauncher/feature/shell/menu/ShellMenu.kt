package com.somalapuram.pclauncher.feature.shell.menu

import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.core.design.PcSpacing

/**
 * Which of the shell's menus is open.
 *
 * One value rather than a flag per menu, so "two panels open at once" is not a state that exists.
 * Both are drawn in the same surface and reach the same corner of the screen, so a pair of booleans
 * could only ever describe something the user must not see (tray-popover-host.md requirement 5).
 */
enum class ShellMenu {
    None,
    Start,
    QuickSettings,
    ;

    val isOpen: Boolean get() = this != None
}

/** Toggle a menu: the same one closes, a different one replaces it. */
fun ShellMenu.toggled(menu: ShellMenu): ShellMenu = if (this == menu) ShellMenu.None else menu

/**
 * How far a menu sits from the screen edge.
 *
 * The bar's own horizontal margin, so a menu's edge and the bar's edge line up. Named once because
 * the Start menu and the quick-settings panel used to be placed by entirely different code, and
 * that is how they came to disagree.
 */
val ShellMenuEdgeInset = PcSpacing.Large

/**
 * How far a menu clears the bar.
 *
 * The bar's height plus its margin and a little air, so a panel sits above the bar rather than on
 * it.
 */
val ShellMenuBarClearance = 84.dp

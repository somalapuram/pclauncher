package com.somalapuram.pclauncher.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * A context menu in the shell's own material.
 *
 * Defined once because there are five of these — the desktop's, an icon's, a widget's, the Start
 * menu's pin menu and the power menu — and styling them at their call sites is how they drift
 * apart. Everything else the shell floats over the wallpaper shares one treatment: the surface
 * colour, a hairline, the popover radius and a shadow. The menus were the only surfaces that did
 * not (context-menu.md).
 */
@Composable
fun PcMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalPcColors.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = RoundedCornerShape(PcCorners.Popover),
        containerColor = colors.surface,
        border = BorderStroke(1.dp, colors.hairline),
        shadowElevation = MenuElevation,
        content = content,
    )
}

/** The same lift the Start menu and the quick-settings popover carry, so the set reads as one. */
private val MenuElevation = 12.dp

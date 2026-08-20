package com.somalapuram.pclauncher.feature.shell.bar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcSpacing

/** Preferred and minimum chip widths (SRS §6.3). */
val ChipPreferredWidth: Dp = 168.dp
val ChipMinWidth: Dp = 84.dp
val ChipHeight: Dp = 34.dp

/**
 * The taskbar strip: one chip per open window.
 *
 * Chips shrink toward [ChipMinWidth] before the strip scrolls — the same bargain Windows strikes,
 * and it keeps titles readable for longer than a fixed width would.
 */
@Composable
fun WindowChipRow(
    windows: List<WindowChip>,
    onFocus: (WindowChip) -> Unit,
    onClose: (WindowChip) -> Unit,
    modifier: Modifier = Modifier,
    chipWidth: Dp = ChipPreferredWidth,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        windows.forEach { window ->
            WindowChipView(
                window = window,
                width = chipWidth,
                onFocus = { onFocus(window) },
                onClose = { onClose(window) },
            )
        }
    }
}

@Composable
private fun WindowChipView(
    window: WindowChip,
    width: Dp,
    onFocus: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalPcColors.current

    // Focused is filled, everything else outlined — a shape difference, so focus survives being
    // read without colour (SRS §6.1 principle 5).
    val background = if (window.isFocused) colors.onSurface.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent
    val borderColor = if (window.isFocused) colors.accent else colors.hairline

    Row(
        modifier = Modifier
            .width(width)
            .height(ChipHeight)
            .background(background, RoundedCornerShape(PcCorners.Popover))
            .border(1.dp, borderColor, RoundedCornerShape(PcCorners.Popover))
            .padding(horizontal = PcSpacing.Small)
            .semantics {
                contentDescription = "${window.appLabel}: ${window.title}" +
                    if (window.isFocused) ", focused" else ""
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
    ) {
        val painter = bitmapPainterFor(window.icon)
        if (painter != null) {
            Image(painter = painter, contentDescription = null, modifier = Modifier.size(18.dp))
        } else {
            Box(Modifier.size(18.dp))
        }

        Text(
            text = window.title,
            color = if (window.isFocused) colors.onSurface else colors.onSurfaceMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

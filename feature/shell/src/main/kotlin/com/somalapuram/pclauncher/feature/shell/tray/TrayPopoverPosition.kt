package com.somalapuram.pclauncher.feature.shell.tray

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Where the quick-settings popover goes.
 *
 * Anchored to the **window**, not to the control that opens it. The tray is not the last thing in
 * the bar — the Show Desktop handle sits to its right — so aligning to the tray put the panel's
 * edge some 44 px inside the bar's, which reads as a mistake rather than a margin. Every desktop
 * aligns a flyout to the screen edge at the bar's own inset, and that is what makes the two read as
 * one piece of furniture.
 *
 * [edgeMarginPx] must come from the same token the bar is padded with, or the two drift apart the
 * next time that padding changes.
 */
class TrayPopoverPosition(
    private val edgeMarginPx: Int,
    private val gapPx: Int,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = horizontalPosition(windowSize.width, popupContentSize.width, edgeMarginPx),
        y = verticalPosition(anchorBounds.top, popupContentSize.height, gapPx),
    )
}

/**
 * Right-aligned to the window, inset by the bar's own margin.
 *
 * Clamped at zero: a popover wider than the window would otherwise be placed at a negative x and
 * lose its left half off-screen, which is a worse failure than a margin that cannot be honoured.
 */
fun horizontalPosition(windowWidth: Int, popupWidth: Int, edgeMargin: Int): Int =
    (windowWidth - popupWidth - edgeMargin).coerceAtLeast(0)

/**
 * Sitting one gap above the anchor.
 *
 * Measured up from the anchor's top rather than down from anything, so the panel grows upward as it
 * gains rows instead of sliding over the bar. Clamped at zero for the same reason as above.
 */
fun verticalPosition(anchorTop: Int, popupHeight: Int, gap: Int): Int =
    (anchorTop - popupHeight - gap).coerceAtLeast(0)

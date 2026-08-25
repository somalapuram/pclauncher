package com.somalapuram.pclauncher.feature.shell.tray

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Where the quick-settings popover lands.
 *
 * It used to be anchored to the tray control, which is not the last thing in the bar — so the
 * panel's right edge sat 44 px inside the bar's on a 2560 px panel. Near enough to read as a
 * mistake rather than a margin, and the kind of thing that is obvious on a device and invisible
 * everywhere else, which is why the arithmetic is pulled out here.
 */
class TrayPopoverPositionTest {

    private val margin = 32
    private val gap = 64

    @Test
    fun `the popover's right edge sits at the bar's margin from the window`() {
        val x = horizontalPosition(windowWidth = 2560, popupWidth = 720, edgeMargin = margin)

        assertEquals(2560 - 720 - 32, x)
        // Which is to say: its right edge and the bar's right edge agree.
        assertEquals(2560 - margin, x + 720)
    }

    @Test
    fun `a popover wider than the window is clamped rather than placed off-screen`() {
        // Losing the left half off the edge is a worse failure than an unhonoured margin.
        assertEquals(0, horizontalPosition(windowWidth = 600, popupWidth = 720, edgeMargin = margin))
    }

    @Test
    fun `a popover exactly filling the window sits flush`() {
        assertEquals(0, horizontalPosition(windowWidth = 720, popupWidth = 720, edgeMargin = 0))
    }

    @Test
    fun `the popover sits one gap above the anchor`() {
        val y = verticalPosition(anchorTop = 1400, popupHeight = 300, gap = gap)

        assertEquals(1400 - 300 - 64, y)
        // Its bottom is exactly one gap clear of the anchor's top.
        assertEquals(1400 - gap, y + 300)
    }

    @Test
    fun `a popover taller than the space above it is clamped to the top`() {
        assertEquals(0, verticalPosition(anchorTop = 200, popupHeight = 900, gap = gap))
    }

    @Test
    fun `the provider combines both axes`() {
        val position = TrayPopoverPosition(edgeMarginPx = margin, gapPx = gap).calculatePosition(
            anchorBounds = IntRect(left = 2200, top = 1400, right = 2500, bottom = 1460),
            windowSize = IntSize(2560, 1600),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(720, 300),
        )

        assertEquals(2560 - 720 - margin, position.x)
        assertEquals(1400 - 300 - gap, position.y)
    }

    @Test
    fun `the anchor's own horizontal position does not move the popover`() {
        // The whole point: the tray is not flush right, so aligning to it is what put the panel
        // out of line with the bar.
        val provider = TrayPopoverPosition(edgeMarginPx = margin, gapPx = gap)
        val window = IntSize(2560, 1600)
        val size = IntSize(720, 300)

        val nearRight = provider.calculatePosition(
            IntRect(2400, 1400, 2500, 1460), window, LayoutDirection.Ltr, size,
        )
        val farLeft = provider.calculatePosition(
            IntRect(100, 1400, 200, 1460), window, LayoutDirection.Ltr, size,
        )

        assertEquals(nearRight.x, farLeft.x)
    }
}

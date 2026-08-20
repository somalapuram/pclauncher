package com.somalapuram.pclauncher.feature.shell.bar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarLayoutTest {

    @Test
    fun `the dock is centred on the screen`() {
        val x = BarLayout.dockStartX(
            barWidth = 1000f, dockWidth = 200f,
            leadingZoneWidth = 60f, trailingZoneWidth = 60f, gap = 8f,
        )
        assertEquals(400f, x, 0.0001f)
    }

    @Test
    fun `adding window chips does not move the dock`() {
        // The reason centring is on the screen and not on the leftover space: a dock that drifts
        // left as windows open is a moving target for muscle memory.
        val withoutChips = BarLayout.dockStartX(1600f, 240f, 60f, 60f, 8f)
        val withChips = BarLayout.dockStartX(1600f, 240f, 60f, 500f, 8f)
        assertEquals(withoutChips, withChips, 0.0001f)
    }

    @Test
    fun `the dock gives way rather than overlapping a neighbour`() {
        val x = BarLayout.dockStartX(
            barWidth = 400f, dockWidth = 300f,
            leadingZoneWidth = 60f, trailingZoneWidth = 200f, gap = 8f,
        )
        assertTrue("must not start before the leading zone", x >= 68f)
    }

    @Test
    fun `an empty dock has zero width`() {
        assertEquals(0f, BarLayout.dockWidth(0, 48f, 8f), 0.0001f)
    }

    @Test
    fun `dock width counts items and padding`() {
        assertEquals(4 * 48f + 16f, BarLayout.dockWidth(4, 48f, 8f), 0.0001f)
    }

    @Test
    fun `chips fill the available width`() {
        assertEquals(4, BarLayout.visibleChipCount(availableWidth = 424f, chipWidth = 100f, gap = 8f))
    }

    @Test
    fun `no room means no chips`() {
        assertEquals(0, BarLayout.visibleChipCount(0f, 100f, 8f))
    }

    @Test
    fun `one chip is shown rather than none plus a scrollbar`() {
        assertEquals(1, BarLayout.visibleChipCount(availableWidth = 100f, chipWidth = 100f, gap = 8f))
    }

    @Test
    fun `chips shrink toward the minimum before scrolling`() {
        val width = BarLayout.chipWidthFor(
            availableWidth = 400f, chipCount = 6,
            preferredWidth = 160f, minWidth = 72f, gap = 8f,
        )
        assertTrue("expected shrink below preferred, was $width", width < 160f)
        assertTrue("must not shrink past the minimum, was $width", width >= 72f)
    }

    @Test
    fun `a single chip gets its preferred width`() {
        val width = BarLayout.chipWidthFor(1000f, 1, 160f, 72f, 8f)
        assertEquals(160f, width, 0.0001f)
    }

    @Test
    fun `no chips is not a division by zero`() {
        assertEquals(160f, BarLayout.chipWidthFor(1000f, 0, 160f, 72f, 8f), 0.0001f)
    }
}

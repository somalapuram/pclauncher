package com.somalapuram.pclauncher.core.data.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The offset that centres whole rows in the desktop's usable height (grid-bounds.md).
 *
 * The interesting cases are all remainders: the height almost never divides by the cell height, and
 * the leftover is exactly what the user sees as "pushed against the top edge".
 */
class RowCentringOffsetTest {

    private val cell = 208f

    @Test
    fun `an exact fit needs no offset`() {
        // Six whole rows and nothing over — centring must not invent a gap.
        assertEquals(0f, rowCentringOffset(availableHeight = 1248f, cellHeight = cell), 0f)
    }

    @Test
    fun `the remainder is split evenly above and below`() {
        // 1320 holds 6 rows (1248) with 72 left; half of it goes above the first row, which is
        // what makes the gap below the last row match it.
        assertEquals(36f, rowCentringOffset(availableHeight = 1320f, cellHeight = cell), 0f)
    }

    @Test
    fun `the measured emulator case centres by 36 pixels`() {
        // 2560x1600 at 320 dpi: 1600 - 48 status bar - 168 bar - 64 padding = 1320.
        val usable = 1600f - 48f - 168f - 64f
        assertEquals(36f, rowCentringOffset(usable, cell), 0f)
    }

    @Test
    fun `an odd remainder is not rounded away`() {
        // Half of 71 is 35.5. Rounding happens once, at the pixel offset, not here — truncating in
        // the arithmetic would bias every grid upward by half a pixel.
        assertEquals(35.5f, rowCentringOffset(availableHeight = 1319f, cellHeight = cell), 0f)
    }

    @Test
    fun `a height too small for one row gets no offset`() {
        // Zero rows means there is nothing to centre. Half the height would push an empty grid off
        // its own origin, and the first row would then appear in the wrong place the moment the
        // measurement grew.
        assertEquals(0f, rowCentringOffset(availableHeight = 100f, cellHeight = cell), 0f)
    }

    @Test
    fun `an unmeasured grid gets no offset`() {
        // Zero is "not measured yet", the same truth the row count relies on.
        assertEquals(0f, rowCentringOffset(availableHeight = 0f, cellHeight = cell), 0f)
    }

    @Test
    fun `a degenerate cell height cannot divide and gets no offset`() {
        assertEquals(0f, rowCentringOffset(availableHeight = 1320f, cellHeight = 0f), 0f)
        assertEquals(0f, rowCentringOffset(availableHeight = 1320f, cellHeight = -8f), 0f)
    }

    @Test
    fun `a negative height gets no offset`() {
        assertEquals(0f, rowCentringOffset(availableHeight = -20f, cellHeight = cell), 0f)
    }

    @Test
    fun `the offset never reaches a whole cell`() {
        // A remainder is strictly less than one cell, so half of it is less than half a cell. If
        // this ever failed, a row's worth of space would be hidden above the grid.
        for (h in 1..2000) {
            val offset = rowCentringOffset(h.toFloat(), cell)
            assertEquals(true, offset < cell / 2f)
            assertEquals(true, offset >= 0f)
        }
    }
}

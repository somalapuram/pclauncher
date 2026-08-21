package com.somalapuram.pclauncher.feature.shell.start

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Grid arrow-key arithmetic.
 *
 * Every case here is a seam — a row edge, a partial final row, a degenerate grid. These are the
 * places a hand-inlined key handler skips a cell or teleports the selection.
 */
class GridNavigationTest {

    // A 5-column grid holding 13 items: two full rows and a partial third of three.
    private val columns = 5
    private val count = 13

    private fun move(from: Int, m: GridMove) = moveInGrid(from, m, count, columns)

    @Test
    fun `right moves one cell`() {
        assertEquals(6, move(5, GridMove.Right))
    }

    @Test
    fun `right stops at the end of a row rather than wrapping`() {
        // Wrapping here would send the selection to the far left of the next row, which reads as
        // teleporting.
        assertEquals(4, move(4, GridMove.Right))
    }

    @Test
    fun `left moves one cell`() {
        assertEquals(6, move(7, GridMove.Left))
    }

    @Test
    fun `left stops at the start of a row`() {
        assertEquals(5, move(5, GridMove.Left))
    }

    @Test
    fun `down moves a whole row`() {
        assertEquals(7, move(2, GridMove.Down))
    }

    @Test
    fun `up moves a whole row`() {
        assertEquals(2, move(7, GridMove.Up))
    }

    @Test
    fun `up from the first row stays put`() {
        assertEquals(3, move(3, GridMove.Up))
    }

    @Test
    fun `down from the last full row lands on the partial row below`() {
        // Item 9 is in row 1; row 2 holds only 10, 11, 12. Straight down from 9 would be 14, which
        // does not exist — but the last item does, and it must stay reachable by keyboard.
        assertEquals(12, move(9, GridMove.Down))
    }

    @Test
    fun `down from the last row stays put`() {
        assertEquals(12, move(12, GridMove.Down))
    }

    @Test
    fun `down within the partial row is a no-op, not an overrun`() {
        assertEquals(10, move(10, GridMove.Down))
    }

    @Test
    fun `a single column behaves like a list`() {
        assertEquals(3, moveInGrid(2, GridMove.Down, itemCount = 5, columns = 1))
        assertEquals(1, moveInGrid(2, GridMove.Up, itemCount = 5, columns = 1))
        assertEquals(2, moveInGrid(2, GridMove.Left, itemCount = 5, columns = 1))
        assertEquals(2, moveInGrid(2, GridMove.Right, itemCount = 5, columns = 1))
    }

    @Test
    fun `a single item never moves`() {
        for (m in GridMove.entries) {
            assertEquals(0, moveInGrid(0, m, itemCount = 1, columns = 5))
        }
    }

    @Test
    fun `an empty grid stays at zero`() {
        for (m in GridMove.entries) {
            assertEquals(0, moveInGrid(0, m, itemCount = 0, columns = 5))
        }
    }

    @Test
    fun `an out-of-range selection is clamped before moving`() {
        // Filtering can shrink the list under the selection; moving from a stale index must land
        // somewhere valid rather than compounding the error.
        assertEquals(11, moveInGrid(99, GridMove.Left, count, columns))
    }

    @Test
    fun `zero columns is not a division by zero`() {
        assertEquals(0, moveInGrid(3, GridMove.Right, itemCount = 5, columns = 0))
    }
}

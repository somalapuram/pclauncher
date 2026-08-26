package com.somalapuram.pclauncher.feature.shell.start

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * When the Start menu has a keyboard caret at all.
 *
 * It used to have one always: the selection was an `Int` starting at zero, so opening the menu
 * highlighted the first app and Enter would have launched it, before any key was pressed.
 */
class StartSelectionTest {

    private val columns = 5

    @Test
    fun `an empty query selects nothing`() {
        assertNull(selectionAfterQuery("", count = 20))
        assertNull(selectionAfterQuery("   ", count = 20))
    }

    @Test
    fun `typing selects the top hit so Enter has something to launch`() {
        assertEquals(0, selectionAfterQuery("ch", count = 3))
    }

    @Test
    fun `a query matching nothing selects nothing`() {
        assertNull(selectionAfterQuery("zzz", count = 0))
    }

    @Test
    fun `the first navigation key selects rather than moves`() {
        // From nothing, "down" means "start here" — not "start at the first row and go down".
        assertEquals(0, selectionAfterMove(null, GridMove.Down, count = 20, columns = columns))
        assertEquals(0, selectionAfterMove(null, GridMove.Up, count = 20, columns = columns))
    }

    @Test
    fun `subsequent keys move from where the caret is`() {
        assertEquals(
            moveInGrid(0, GridMove.Down, 20, columns),
            selectionAfterMove(0, GridMove.Down, 20, columns),
        )
    }

    @Test
    fun `navigating an empty list selects nothing`() {
        assertNull(selectionAfterMove(null, GridMove.Down, count = 0, columns = columns))
        assertNull(selectionAfterMove(3, GridMove.Down, count = 0, columns = columns))
    }

    @Test
    fun `filtering keeps the caret on a row that still exists`() {
        assertEquals(2, selectionAfterFilter(current = 9, count = 3))
        assertEquals(1, selectionAfterFilter(current = 1, count = 3))
    }

    @Test
    fun `filtering does not invent a caret where there was none`() {
        // The bug in miniature: a clamp that returns 0 for null puts the highlight back.
        assertNull(selectionAfterFilter(current = null, count = 20))
    }

    @Test
    fun `filtering to nothing clears the caret`() {
        assertNull(selectionAfterFilter(current = 4, count = 0))
    }
}

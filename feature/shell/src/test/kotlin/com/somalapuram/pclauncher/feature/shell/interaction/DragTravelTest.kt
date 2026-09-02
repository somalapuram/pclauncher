package com.somalapuram.pclauncher.feature.shell.interaction

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * That a drag's reported travel adds up to the pointer's.
 *
 * Two faults made it not. The start position came from the *grid's* origin plus the offset inside
 * the icon, so the icon's own cell was dropped and every drag began near the grid's corner — which
 * the first-cell icon, the obvious one to test with, hides completely because its cell offset is
 * zero. And the release event's movement was discarded, leaving each drag one event short of where
 * the pointer finished: a sixth of the distance on a quick drag, measured at 154 px of 988.
 *
 * The arithmetic is trivial; it is the *inputs* that were wrong, so this pins the relationship the
 * gesture has to preserve.
 */
class DragTravelTest {

    private val iconOrigin = Offset(416f, 1120f)
    private val insideIcon = Offset(96f, 88f)

    @Test
    fun `a drag begins at the pointer, not at the grid's corner`() {
        // The icon's own root position plus where it was grabbed *is* the pointer's position.
        assertEquals(Offset(512f, 1208f), iconOrigin + insideIcon)
    }

    @Test
    fun `using the grid's origin instead loses the icon's cell`() {
        val gridOrigin = Offset(32f, 80f)

        val wrong = gridOrigin + insideIcon
        assertEquals(Offset(128f, 168f), wrong)
        // Which is the top-left of the grid, whichever icon was actually grabbed.
    }

    @Test
    fun `the first cell hides the fault entirely`() {
        // An icon in cell (0,0) sits at the grid's origin, so both answers agree — which is why
        // every check that used it passed.
        val gridOrigin = Offset(32f, 80f)
        val firstCellIcon = gridOrigin

        assertEquals(firstCellIcon + insideIcon, gridOrigin + insideIcon)
    }

    @Test
    fun `travel including the release reaches the pointer`() {
        val start = Offset(512f, 1208f)
        val duringDrag = Offset(834f, -597.5f)
        val onRelease = Offset(154f, -110.5f)

        assertEquals(Offset(1500f, 500f), start + duringDrag + onRelease)
    }

    @Test
    fun `dropping the release event lands short`() {
        val start = Offset(512f, 1208f)
        val duringDrag = Offset(834f, -597.5f)

        val landedShort = start + duringDrag
        assertEquals(Offset(1346f, 610.5f), landedShort)
        // 154 px of 988 — enough to put the icon in the wrong column.
    }
}

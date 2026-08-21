package com.somalapuram.pclauncher.core.data.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopLayoutTest {

    private val rows = 4

    @Test
    fun `an empty desktop places the first item at the origin`() {
        assertEquals(DesktopCell(0, 0), firstFreeCell(DesktopLayout(), rows))
    }

    @Test
    fun `auto-placement fills column-major`() {
        // The same order the old flowing grid used, so adding free placement does not scramble an
        // arrangement the user has already got used to.
        val layout = withAutoPlacement(DesktopLayout(), listOf("a", "b", "c", "d", "e"), rows)
        assertEquals(DesktopCell(0, 0), layout.cellFor("a"))
        assertEquals(DesktopCell(0, 1), layout.cellFor("b"))
        assertEquals(DesktopCell(0, 3), layout.cellFor("d"))
        assertEquals(DesktopCell(1, 0), layout.cellFor("e"))
    }

    @Test
    fun `auto-placement fills gaps left by a moved icon`() {
        val arranged = DesktopLayout(listOf(DesktopPlacement("a", DesktopCell(2, 2))))
        assertEquals(DesktopCell(0, 0), firstFreeCell(arranged, rows))
    }

    @Test
    fun `auto-placement never disturbs an existing placement`() {
        // Arranging one icon must not move the others.
        val arranged = DesktopLayout(listOf(DesktopPlacement("b", DesktopCell(3, 3))))
        val result = withAutoPlacement(arranged, listOf("a", "b"), rows)
        assertEquals(DesktopCell(3, 3), result.cellFor("b"))
        assertEquals(DesktopCell(0, 0), result.cellFor("a"))
    }

    @Test
    fun `moving to a free cell works`() {
        val layout = withAutoPlacement(DesktopLayout(), listOf("a"), rows)
        val moved = layout.moved("a", DesktopCell(2, 1))
        assertEquals(DesktopCell(2, 1), moved?.cellFor("a"))
    }

    @Test
    fun `moving onto an occupied cell is refused, not an overwrite`() {
        // Silently destroying a placement the user made is worse than declining the drop.
        val layout = withAutoPlacement(DesktopLayout(), listOf("a", "b"), rows)
        assertNull(layout.moved("b", DesktopCell(0, 0)))
    }

    @Test
    fun `moving an icon onto itself is a no-op, not a refusal`() {
        val layout = withAutoPlacement(DesktopLayout(), listOf("a"), rows)
        assertEquals(layout, layout.moved("a", DesktopCell(0, 0)))
    }

    @Test
    fun `occupancy tracks placements`() {
        val layout = withAutoPlacement(DesktopLayout(), listOf("a"), rows)
        assertTrue(layout.isOccupied(DesktopCell(0, 0)))
        assertFalse(layout.isOccupied(DesktopCell(1, 0)))
    }

    @Test
    fun `removing frees the cell`() {
        val layout = withAutoPlacement(DesktopLayout(), listOf("a"), rows).without("a")
        assertFalse(layout.isOccupied(DesktopCell(0, 0)))
    }

    @Test
    fun `a pointer maps to the cell under it`() {
        assertEquals(DesktopCell(0, 0), cellAt(10f, 10f, 100f, 100f, rows))
        assertEquals(DesktopCell(2, 1), cellAt(250f, 150f, 100f, 100f, rows))
    }

    @Test
    fun `a pointer on a boundary belongs to the cell it enters`() {
        assertEquals(DesktopCell(1, 0), cellAt(100f, 0f, 100f, 100f, rows))
    }

    @Test
    fun `below the last row is off the grid, not the last row`() {
        // Otherwise a drop near the taskbar lands in the bottom cell by surprise.
        assertNull(cellAt(10f, 450f, 100f, 100f, rowsPerColumn = 4))
    }

    @Test
    fun `negative coordinates are off the grid`() {
        assertNull(cellAt(-1f, 10f, 100f, 100f, rows))
        assertNull(cellAt(10f, -1f, 100f, 100f, rows))
    }

    @Test
    fun `degenerate cell sizes do not divide by zero`() {
        assertNull(cellAt(10f, 10f, 0f, 100f, rows))
        assertNull(cellAt(10f, 10f, 100f, 0f, rows))
        assertNull(cellAt(10f, 10f, 100f, 100f, rowsPerColumn = 0))
    }

    @Test
    fun `a layout round-trips`() {
        val layout = withAutoPlacement(DesktopLayout(), listOf("a", "b", "c"), rows)
        assertEquals(layout.placements.toSet(), DesktopLayoutCodec.decode(DesktopLayoutCodec.encode(layout)).placements.toSet())
    }

    @Test
    fun `a corrupt line loses one placement, not the desktop`() {
        val raw = "a|0|0\ngarbage\nb|1|1\nc|x|2"
        assertEquals(2, DesktopLayoutCodec.decode(raw).placements.size)
    }

    @Test
    fun `negative cells in storage are rejected`() {
        assertEquals(0, DesktopLayoutCodec.decode("a|-1|0").placements.size)
    }

    @Test
    fun `duplicate cells in storage resolve to one`() {
        // Two entries claiming one cell would make occupancy ambiguous.
        assertEquals(1, DesktopLayoutCodec.decode("a|0|0\nb|0|0").placements.size)
    }

    @Test
    fun `null and blank decode to an empty layout`() {
        assertTrue(DesktopLayoutCodec.decode(null).placements.isEmpty())
        assertTrue(DesktopLayoutCodec.decode("  ").placements.isEmpty())
    }
}

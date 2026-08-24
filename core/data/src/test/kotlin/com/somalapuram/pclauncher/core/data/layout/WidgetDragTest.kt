package com.somalapuram.pclauncher.core.data.layout

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Where a dragged widget lands, and what removing one leaves behind. */
class WidgetDragTest {

    private val cell = DesktopCell(4, 4)
    private val span = DesktopSpan(3, 2)

    private fun drop(dx: Float, dy: Float, columns: Int = 20, rows: Int = 10) = cellAfterDrag(
        from = cell,
        span = span,
        deltaX = dx,
        deltaY = dy,
        cellWidth = 96f,
        cellHeight = 104f,
        columnsAvailable = columns,
        rowsAvailable = rows,
    )

    @Test
    fun `a drag of one whole cell moves it one cell`() {
        assertEquals(DesktopCell(5, 4), drop(dx = 96f, dy = 0f))
        assertEquals(DesktopCell(4, 5), drop(dx = 0f, dy = 104f))
    }

    @Test
    fun `most of a cell rounds up to the next one`() {
        assertEquals(DesktopCell(5, 4), drop(dx = 60f, dy = 0f))
    }

    @Test
    fun `less than half a cell leaves it where it was`() {
        // Requirement 4: a nudge that never crosses a boundary must not move the widget.
        assertEquals(cell, drop(dx = 40f, dy = 40f))
    }

    @Test
    fun `dragging backwards rounds the same way`() {
        assertEquals(DesktopCell(3, 3), drop(dx = -60f, dy = -70f))
        assertEquals(cell, drop(dx = -40f, dy = -40f))
    }

    @Test
    fun `a drag past the left or top edge clamps to the origin`() {
        assertEquals(DesktopCell(0, 0), drop(dx = -10_000f, dy = -10_000f))
    }

    @Test
    fun `a drag past the right edge keeps the whole widget on the grid`() {
        // 20 columns, 3 wide: the furthest left edge that still fits is column 17.
        assertEquals(17, drop(dx = 10_000f, dy = 0f).column)
    }

    @Test
    fun `a drag past the bottom edge keeps the whole widget on the grid`() {
        assertEquals(8, drop(dx = 0f, dy = 10_000f).row)
    }

    @Test
    fun `a grid too small for the widget pins it to the origin rather than a negative cell`() {
        assertEquals(DesktopCell(0, 0), drop(dx = 10_000f, dy = 10_000f, columns = 2, rows = 1))
    }

    @Test
    fun `a degenerate cell size moves nothing`() {
        val landed = cellAfterDrag(cell, span, 500f, 500f, 0f, 104f, 20, 10)
        assertEquals(cell, landed)
    }

    @Test
    fun `a move keeps the span`() {
        val layout = DesktopLayout(listOf(DesktopPlacement("widget:1", cell, span)))
        val moved = layout.moved("widget:1", DesktopCell(1, 1))
        assertEquals(span, moved?.spanFor("widget:1"))
    }

    @Test
    fun `a move onto another placement is refused`() {
        val layout = DesktopLayout(
            listOf(
                DesktopPlacement("widget:1", cell, span),
                DesktopPlacement("icon", DesktopCell(1, 1)),
            ),
        )
        // The 3x2 widget landing at (0,0) would cover (1,1).
        assertNull(layout.moved("widget:1", DesktopCell(0, 0)))
    }

    @Test
    fun `removing a placement frees its whole rectangle`() {
        val layout = DesktopLayout(listOf(DesktopPlacement("widget:1", cell, span)))

        val after = layout.without("widget:1")

        assertNull(after.placementFor("widget:1"))
        assertEquals(false, after.isOccupied(DesktopCell(5, 5)))
    }

    @Test
    fun `removing something absent changes nothing`() {
        val layout = DesktopLayout(listOf(DesktopPlacement("widget:1", cell, span)))
        assertEquals(layout.placements, layout.without("widget:2").placements)
    }

    @Test
    fun `the store forgets a removed placement`() = runTest {
        val store = InMemoryDesktopLayoutStore(
            DesktopLayout(listOf(DesktopPlacement("widget:1", cell, span))),
        )

        store.remove("widget:1")

        assertNull(store.currentLayout().placementFor("widget:1"))
    }

    @Test
    fun `the store tolerates removing an id it never had`() = runTest {
        val store = InMemoryDesktopLayoutStore(
            DesktopLayout(listOf(DesktopPlacement("widget:1", cell, span))),
        )

        store.remove("widget:9")

        assertEquals(1, store.currentLayout().placements.size)
    }
}

private suspend fun DesktopLayoutStore.currentLayout(): DesktopLayout =
    layout.first()

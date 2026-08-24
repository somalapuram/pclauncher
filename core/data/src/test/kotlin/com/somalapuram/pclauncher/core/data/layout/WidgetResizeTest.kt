package com.somalapuram.pclauncher.core.data.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetResizeTest {

    private val both = ResizePermission(horizontal = true, vertical = true)
    private val cols = 10
    private val rows = 8

    private fun layoutWith(vararg placements: DesktopPlacement) = DesktopLayout(placements.toList())

    private fun widget(col: Int = 2, row: Int = 2, c: Int = 2, r: Int = 2) =
        DesktopPlacement("widget:1", DesktopCell(col, row), DesktopSpan(c, r))

    private fun resize(layout: DesktopLayout, edge: ResizeEdge, delta: Int, p: ResizePermission = both) =
        resizedBy(layout, "widget:1", edge, delta, p, cols, rows)

    @Test
    fun `dragging the right edge widens it`() {
        val result = resize(layoutWith(widget()), ResizeEdge.Right, 1)
        assertEquals(DesktopSpan(3, 2), result?.spanFor("widget:1"))
        assertEquals(DesktopCell(2, 2), result?.cellFor("widget:1"))
    }

    @Test
    fun `dragging the bottom edge makes it taller`() {
        val result = resize(layoutWith(widget()), ResizeEdge.Bottom, 2)
        assertEquals(DesktopSpan(2, 4), result?.spanFor("widget:1"))
    }

    @Test
    fun `dragging the left edge moves the origin as well as the span`() {
        // Growing from the near edge has to move the top-left corner, or the widget grows the
        // wrong way and appears to jump.
        val result = resize(layoutWith(widget()), ResizeEdge.Left, 1)
        assertEquals(DesktopCell(1, 2), result?.cellFor("widget:1"))
        assertEquals(DesktopSpan(3, 2), result?.spanFor("widget:1"))
    }

    @Test
    fun `dragging the top edge moves the origin upward`() {
        val result = resize(layoutWith(widget()), ResizeEdge.Top, 1)
        assertEquals(DesktopCell(2, 1), result?.cellFor("widget:1"))
        assertEquals(DesktopSpan(2, 3), result?.spanFor("widget:1"))
    }

    @Test
    fun `shrinking works and keeps the far edge`() {
        val result = resize(layoutWith(widget()), ResizeEdge.Right, -1)
        assertEquals(DesktopSpan(1, 2), result?.spanFor("widget:1"))
    }

    @Test
    fun `a forbidden axis offers no resize`() {
        val horizontalOnly = ResizePermission(horizontal = true, vertical = false)
        assertNull(resize(layoutWith(widget()), ResizeEdge.Bottom, 1, horizontalOnly))
        assertTrue(resize(layoutWith(widget()), ResizeEdge.Right, 1, horizontalOnly) != null)
    }

    @Test
    fun `a provider that forbids resizing cannot be resized at all`() {
        for (edge in ResizeEdge.entries) {
            assertNull(resize(layoutWith(widget()), edge, 1, ResizePermission.None))
        }
    }

    @Test
    fun `shrinking below the provider minimum is refused`() {
        // Squeezing a widget into a shape its layout cannot render looks like our bug, not theirs.
        val min = ResizePermission(horizontal = true, vertical = true, minColumns = 2, minRows = 2)
        assertNull(resize(layoutWith(widget()), ResizeEdge.Right, -1, min))
    }

    @Test
    fun `growing past the right edge of the grid is refused`() {
        val atEdge = widget(col = 8, c = 2)
        assertNull(resize(layoutWith(atEdge), ResizeEdge.Right, 1))
    }

    @Test
    fun `growing past the bottom of the grid is refused`() {
        val atBottom = widget(row = 6, r = 2)
        assertNull(resize(layoutWith(atBottom), ResizeEdge.Bottom, 1))
    }

    @Test
    fun `growing past the left edge is refused`() {
        assertNull(resize(layoutWith(widget(col = 0)), ResizeEdge.Left, 1))
    }

    @Test
    fun `growing onto another placement is refused`() {
        // Same answer as dropping an icon on an occupied cell: decline, do not overwrite.
        val neighbour = DesktopPlacement("com.a/.Main", DesktopCell(4, 2))
        assertNull(resize(layoutWith(widget(), neighbour), ResizeEdge.Right, 1))
    }

    @Test
    fun `growing away from a neighbour is allowed`() {
        val neighbour = DesktopPlacement("com.a/.Main", DesktopCell(4, 2))
        assertTrue(resize(layoutWith(widget(), neighbour), ResizeEdge.Left, 1) != null)
    }

    @Test
    fun `a zero drag changes nothing`() {
        val layout = layoutWith(widget())
        assertEquals(layout, resize(layout, ResizeEdge.Right, 0))
    }

    @Test
    fun `resizing something absent is refused`() {
        assertNull(resizedBy(DesktopLayout(), "widget:9", ResizeEdge.Right, 1, both, cols, rows))
    }

    @Test
    fun `handles are offered only on permitted axes`() {
        val horizontalOnly = ResizePermission(horizontal = true, vertical = false)
        assertTrue(canDrag(ResizeEdge.Left, horizontalOnly))
        assertTrue(canDrag(ResizeEdge.Right, horizontalOnly))
        assertTrue(!canDrag(ResizeEdge.Top, horizontalOnly))
        assertTrue(!canDrag(ResizeEdge.Bottom, horizontalOnly))
    }

    @Test
    fun `drag distance rounds to the nearest cell`() {
        // Truncating makes the handle feel like it lags behind the finger.
        assertEquals(1, cellsDragged(pixels = 60f, cellSize = 96f))
        assertEquals(0, cellsDragged(pixels = 40f, cellSize = 96f))
        assertEquals(2, cellsDragged(pixels = 190f, cellSize = 96f))
        assertEquals(-1, cellsDragged(pixels = -60f, cellSize = 96f))
    }

    @Test
    fun `a degenerate cell size drags nothing`() {
        assertEquals(0, cellsDragged(pixels = 100f, cellSize = 0f))
    }
}

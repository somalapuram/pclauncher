package com.somalapuram.pclauncher.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the pointer does to a surface — shared by the desktop, the widgets and the Start menu. */
class HoverFeedbackTest {

    @Test
    fun `at rest a surface is its own size and draws no wash`() {
        assertEquals(1f, PcHover.scaleFor(hovered = false), 0.0001f)
        assertEquals(0f, PcHover.washFor(hovered = false), 0.0001f)
        assertEquals(0f, PcHover.outlineFor(hovered = false), 0.0001f)
    }

    @Test
    fun `hovering grows the surface and washes it`() {
        assertTrue(PcHover.scaleFor(hovered = true) > 1f)
        assertTrue(PcHover.washFor(hovered = true) > 0f)
        assertTrue(PcHover.outlineFor(hovered = true) > 0f)
    }

    @Test
    fun `pressing reads as pushing in, not out`() {
        // A press that grew would fight the hover it always arrives with.
        assertTrue(PcHover.scaleFor(hovered = true, pressed = true) < 1f)
    }

    @Test
    fun `a press outranks the hover it arrives with`() {
        assertEquals(
            PcHover.scaleFor(hovered = false, pressed = true),
            PcHover.scaleFor(hovered = true, pressed = true),
            0.0001f,
        )
        assertEquals(
            PcHover.washFor(hovered = false, pressed = true),
            PcHover.washFor(hovered = true, pressed = true),
            0.0001f,
        )
    }

    @Test
    fun `a press is more strongly washed than a hover`() {
        assertTrue(PcHover.washFor(hovered = true, pressed = true) > PcHover.washFor(hovered = true))
    }

    @Test
    fun `the hover growth stays inside a desktop cell`() {
        // 72 dp of icon in a 96 dp cell: growth must not reach a neighbour's artwork.
        val grown = PcSize.DesktopIcon.value * PcHover.scaleFor(hovered = true)
        assertTrue("a hovered icon overflows its cell at $grown", grown <= PcSize.DesktopGridCell.value)
    }
}

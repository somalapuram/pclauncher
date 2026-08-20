package com.somalapuram.pclauncher.feature.shell.interaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The drop decision, in isolation.
 *
 * This is the seam the whole drag rests on: when the bar becomes an overlay window the *transport*
 * changes, but "is the pointer over the bar" stays exactly this. Worth testing to the edges.
 */
class DropTargetTest {

    private val barTop = 900f
    private val barBottom = 980f

    @Test
    fun `over the bar is the dock`() {
        assertEquals(DropTarget.Dock, dropTargetFor(940f, barTop, barBottom))
    }

    @Test
    fun `above the bar is the desktop`() {
        assertEquals(DropTarget.Desktop, dropTargetFor(400f, barTop, barBottom))
    }

    @Test
    fun `below the bar is nothing`() {
        // Under the bar is off the shell entirely; releasing there must not pin by accident.
        assertEquals(DropTarget.None, dropTargetFor(1050f, barTop, barBottom))
    }

    @Test
    fun `above the desktop is nothing`() {
        assertEquals(DropTarget.None, dropTargetFor(-20f, barTop, barBottom, desktopTopY = 0f))
    }

    @Test
    fun `the bar wins on its top edge`() {
        // The two surfaces meet here. The bar is the smaller, more deliberate target and the
        // desktop forgives a near-miss in a way the bar does not, so the tie goes to the bar.
        assertEquals(DropTarget.Dock, dropTargetFor(barTop, barTop, barBottom))
    }

    @Test
    fun `the bottom edge is still the bar`() {
        assertEquals(DropTarget.Dock, dropTargetFor(barBottom, barTop, barBottom))
    }

    @Test
    fun `a zero-height bar can never be a target`() {
        // Before the bar has been laid out its bounds are degenerate. Without this guard an
        // unmeasured bar would swallow every drop at that coordinate.
        assertEquals(DropTarget.Desktop, dropTargetFor(500f, 900f, 900f))
    }

    @Test
    fun `an inverted bar rect is treated as no bar`() {
        assertEquals(DropTarget.Desktop, dropTargetFor(500f, 980f, 900f))
    }

    @Test
    fun `dropping an unpinned app on the dock pins it`() {
        assertTrue(dropChangesAnything(DropTarget.Dock, isPinned = false))
    }

    @Test
    fun `dropping a pinned app back on the dock does nothing`() {
        // A no-op target is not highlighted, so the user is not promised a change that will not come.
        assertFalse(dropChangesAnything(DropTarget.Dock, isPinned = true))
    }

    @Test
    fun `dropping a pinned app on the desktop unpins it`() {
        assertTrue(dropChangesAnything(DropTarget.Desktop, isPinned = true))
    }

    @Test
    fun `dropping an unpinned app on the desktop does nothing`() {
        assertFalse(dropChangesAnything(DropTarget.Desktop, isPinned = false))
    }

    @Test
    fun `no target never changes anything`() {
        assertFalse(dropChangesAnything(DropTarget.None, isPinned = true))
        assertFalse(dropChangesAnything(DropTarget.None, isPinned = false))
    }
}

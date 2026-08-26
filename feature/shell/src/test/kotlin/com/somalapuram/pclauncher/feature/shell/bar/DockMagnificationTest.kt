package com.somalapuram.pclauncher.feature.shell.bar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DockMagnificationTest {

    @Test
    fun `no pointer means no magnification anywhere`() {
        repeat(6) { assertEquals(1f, DockMagnification.scaleAt(it, null), 0.0001f) }
    }

    @Test
    fun `the item under the pointer is the largest`() {
        val scales = (0..5).map { DockMagnification.scaleAt(it, pointerIndex = 3f) }
        assertEquals(3, scales.indexOf(scales.max()))
        assertEquals(DockMagnification.MAX_SCALE, scales[3], 0.0001f)
    }

    @Test
    fun `scale falls off monotonically with distance`() {
        val at3 = DockMagnification.scaleAt(3, 3f)
        val at4 = DockMagnification.scaleAt(4, 3f)
        val at5 = DockMagnification.scaleAt(5, 3f)
        assertTrue(at3 > at4)
        assertTrue(at4 > at5)
    }

    @Test
    fun `items beyond the falloff are untouched`() {
        assertEquals(1f, DockMagnification.scaleAt(0, pointerIndex = 8f), 0.0001f)
    }

    @Test
    fun `the curve meets its neighbours smoothly`() {
        // A raised cosine has zero gradient at the falloff edge; a linear ramp leaves a crease
        // there, and the eye reads the crease as a bug.
        val falloff = DockMagnification.FALLOFF_ITEMS
        val justInside = DockMagnification.scaleAt(0, pointerIndex = falloff - 0.05f)
        assertTrue("should be within a hair of 1.0, was $justInside", justInside < 1.01f)
    }

    @Test
    fun `magnification is symmetric about the pointer`() {
        assertEquals(
            DockMagnification.scaleAt(2, 4f),
            DockMagnification.scaleAt(6, 4f),
            0.0001f,
        )
    }

    @Test
    fun `a fractional pointer glides the peak between items`() {
        // Between items 3 and 4 the two should magnify equally, so the dock does not lurch as the
        // pointer crosses an item boundary.
        assertEquals(
            DockMagnification.scaleAt(3, 3.5f),
            DockMagnification.scaleAt(4, 3.5f),
            0.0001f,
        )
    }

    @Test
    fun `pointer index is fractional across the dock`() {
        val index = DockMagnification.pointerIndexFor(
            pointerX = 100f, dockStartX = 0f, itemPitch = 40f, itemCount = 5,
        )
        assertEquals(2.0f, index!!, 0.0001f)
    }

    @Test
    fun `a pointer well outside the dock magnifies nothing`() {
        assertNull(
            DockMagnification.pointerIndexFor(
                pointerX = 500f, dockStartX = 0f, itemPitch = 40f, itemCount = 5,
            ),
        )
    }

    @Test
    fun `an empty dock has no pointer index`() {
        assertNull(
            DockMagnification.pointerIndexFor(
                pointerX = 10f, dockStartX = 0f, itemPitch = 40f, itemCount = 0,
            ),
        )
    }

    @Test
    fun `the outermost icon still responds as the pointer approaches from outside`() {
        val index = DockMagnification.pointerIndexFor(
            pointerX = -10f, dockStartX = 0f, itemPitch = 40f, itemCount = 5,
        )
        assertTrue("expected a usable index just outside the dock", index != null)
    }

    @Test
    fun `the bar grows enough to contain a magnified icon`() {
        val base = 56f
        val grown = DockMagnification.barHeightFor(baseHeight = base, iconSize = 40f)
        assertTrue("bar must grow, was $grown", grown > base)
    }

    // --- the origin actually being supplied ---------------------------------------------------

    @Test
    fun `a dock centred in the bar magnifies the icon under the pointer`() {
        // The regression: `dockOriginX` was declared, read, and never assigned, so the pointer was
        // mapped against a dock starting at zero while the real one sits near the middle of a
        // 2560 px bar. Everything still compiled and the wrong icon grew.
        val pitch = 96f
        val dockStart = 1000f
        val items = 8

        // Pointer over the middle of the third item.
        val pointer = dockStart + pitch * 2 + pitch / 2
        val index = DockMagnification.pointerIndexFor(pointer, dockStart, pitch, items)

        assertEquals(2f, index!!, 0.01f)
        assertTrue(
            "the icon under the pointer is not the largest",
            DockMagnification.scaleAt(2, index) > DockMagnification.scaleAt(3, index),
        )
    }

    @Test
    fun `assuming the dock starts at zero picks a different icon entirely`() {
        // What the bug did: same pointer, origin left at its default.
        val pitch = 96f
        val pointer = 1000f + pitch * 2 + pitch / 2

        val correct = DockMagnification.pointerIndexFor(pointer, 1000f, pitch, 8)
        val withDefaultOrigin = DockMagnification.pointerIndexFor(pointer, 0f, pitch, 8)

        assertNotEquals(correct, withDefaultOrigin)
    }

    @Test
    fun `the pitch decides which item a position falls in`() {
        // The second half of the offset bug: the index was computed with the touch minimum while
        // items lay out at the icon size, so every item past the first drifted a little more.
        val atTouchPitch = DockMagnification.pointerIndexFor(1000f + 44f * 5, 1000f, 44f, 8)
        val atRealPitch = DockMagnification.pointerIndexFor(1000f + 44f * 5, 1000f, 48f, 8)

        assertNotEquals(atTouchPitch, atRealPitch)
    }
}
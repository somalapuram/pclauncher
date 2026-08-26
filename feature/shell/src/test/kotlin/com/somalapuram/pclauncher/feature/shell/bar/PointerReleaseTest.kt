package com.somalapuram.pclauncher.feature.shell.bar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * That the dock lets go when the pointer does.
 *
 * The bar's loop assigned the latest event's position unconditionally. Every pointer event carries
 * one — including the event that says the pointer has *left* — so leaving the bar recorded one last
 * coordinate and then went quiet, and the icon under it stayed enlarged indefinitely. Clicking an
 * icon did the same thing by a different route: the launched app took the pointer and no further
 * events arrived.
 */
class PointerReleaseTest {

    @Test
    fun `an exit clears the pointer however plausible its position`() {
        // The position on an Exit is real and usable — which is exactly why taking it was wrong.
        assertNull(pointerXFor(isExit = true, position = 1234f))
    }

    @Test
    fun `an ordinary event keeps its position`() {
        assertEquals(1234f, pointerXFor(isExit = false, position = 1234f))
    }

    @Test
    fun `an event with no position clears the pointer too`() {
        assertNull(pointerXFor(isExit = false, position = null))
    }

    @Test
    fun `a cleared pointer magnifies nothing`() {
        val index = DockMagnification.pointerIndexFor(
            pointerX = pointerXFor(isExit = true, position = 1234f),
            dockStartX = 1000f,
            itemPitch = 48f,
            itemCount = 8,
        )

        assertNull(index)
        assertEquals(1f, DockMagnification.scaleAt(0, index), 0.0001f)
    }
}

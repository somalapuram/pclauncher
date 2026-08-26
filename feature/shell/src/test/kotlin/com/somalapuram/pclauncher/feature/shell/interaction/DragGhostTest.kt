package com.somalapuram.pclauncher.feature.shell.interaction

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * That the ghost sits under the pointer rather than near it.
 *
 * Drag positions are recorded in root coordinates — every producer builds them as
 * `positionInRoot() + local`, which is why drops land on the right cell. The ghost was drawn inside
 * the shell's inset box while still being placed at a root coordinate, so it rendered low and right
 * by exactly the window's safe-drawing inset. Drops were correct and the picture was not, which is
 * the combination that makes a bug hard to name.
 */
class DragGhostTest {

    private val half = 28f

    @Test
    fun `in an uninset space the ghost is simply centred on the pointer`() {
        val at = ghostTopLeft(Offset(300f, 400f), Offset.Zero, half)

        assertEquals(300f - half, at.x, 0.001f)
        assertEquals(400f - half, at.y, 0.001f)
    }

    @Test
    fun `an inset space is backed out of before centring`() {
        // A status bar of 48 px is exactly what put the ghost below the pointer.
        val at = ghostTopLeft(Offset(300f, 400f), Offset(0f, 48f), half)

        assertEquals(300f - half, at.x, 0.001f)
        assertEquals(400f - 48f - half, at.y, 0.001f)
    }

    @Test
    fun `both axes are corrected`() {
        val at = ghostTopLeft(Offset(300f, 400f), Offset(24f, 48f), half)

        assertEquals(300f - 24f - half, at.x, 0.001f)
        assertEquals(400f - 48f - half, at.y, 0.001f)
    }

    @Test
    fun `the ghost's centre lands on the pointer, which is the whole point`() {
        val origin = Offset(24f, 48f)
        val pointer = Offset(900f, 700f)

        val at = ghostTopLeft(pointer, origin, half)
        // Converting the ghost's centre back into root space returns the pointer.
        assertEquals(pointer.x, at.x + half + origin.x, 0.001f)
        assertEquals(pointer.y, at.y + half + origin.y, 0.001f)
    }
}

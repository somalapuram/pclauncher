package com.somalapuram.pclauncher.core.design

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.sqrt

/**
 * Where a rounded corner starts and stops.
 *
 * The only part of drawing a rounded triangle that can be got wrong silently: a bad path still
 * renders as *something*, and it takes measuring to notice it is not the shape that was asked for.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoundedTriangleTest {

    private val left = Offset(0f, 0f)
    private val right = Offset(100f, 0f)
    private val apex = Offset(50f, 80f)

    private fun distance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    @Test
    fun `the corner is walked back by the radius along both edges`() {
        val (start, end) = cornerTangents(apex, left, right, radius = 10f)

        assertEquals(10f, distance(left, start), 0.01f)
        assertEquals(10f, distance(left, end), 0.01f)
    }

    @Test
    fun `the tangent points lie on their own edges`() {
        val (_, end) = cornerTangents(apex, left, right, radius = 10f)

        // The left-to-right edge runs along y = 0.
        assertEquals(0f, end.y, 0.01f)
        assertTrue(end.x in 0f..100f)
    }

    @Test
    fun `a radius larger than the edge is clamped rather than folding the shape`() {
        // Walking further than half an edge would eat the neighbouring corner's own rounding.
        val (start, end) = cornerTangents(apex, left, right, radius = 10_000f)

        assertTrue(distance(left, start) <= distance(left, apex) / 2f + 0.01f)
        assertTrue(distance(left, end) <= distance(left, right) / 2f + 0.01f)
    }

    @Test
    fun `a zero radius leaves the corner where it was`() {
        val (start, end) = cornerTangents(apex, left, right, radius = 0f)

        assertEquals(left, start)
        assertEquals(left, end)
    }

    @Test
    fun `a degenerate edge does not divide by zero`() {
        val (start, end) = cornerTangents(left, left, right, radius = 10f)

        assertEquals(left, start)
        assertEquals(left, end)
    }

    @Test
    fun `every corner of the triangle rounds by the same walk`() {
        listOf(
            Triple(apex, left, right),
            Triple(left, right, apex),
            Triple(right, apex, left),
        ).forEach { (previous, vertex, next) ->
            val (start, end) = cornerTangents(previous, vertex, next, radius = 8f)
            assertEquals(8f, distance(vertex, start), 0.01f)
            assertEquals(8f, distance(vertex, end), 0.01f)
        }
    }

    @Test
    fun `the path spans the triangle it was given`() {
        val bounds = roundedTrianglePath(left, right, apex, radius = 8f).getBounds()

        // Rounding pulls the outline inside the vertices, so the bounds sit within the triangle
        // and close to it — not at some other size entirely, which is what a mis-built path gives.
        assertTrue("left edge at ${'$'}{bounds.left}", bounds.left in 0f..12f)
        assertTrue("right edge at ${'$'}{bounds.right}", bounds.right in 88f..100f)
        assertTrue("bottom at ${'$'}{bounds.bottom}", bounds.bottom in 68f..80f)
    }
}

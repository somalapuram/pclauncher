package com.somalapuram.pclauncher.core.design.icon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class SquircleTest {

    @Test
    fun `the curve passes through its four extremes`() {
        val (rx, ry) = squirclePoint(0f)
        assertEquals(1f, rx, 0.001f)
        assertEquals(0f, ry, 0.001f)

        val (bx, by) = squirclePoint(0.25f)
        assertEquals(0f, bx, 0.001f)
        assertEquals(1f, by, 0.001f)
    }

    @Test
    fun `every point satisfies the superellipse equation`() {
        // The definition, checked directly: |x|^n + |y|^n == 1.
        for (i in 0..40) {
            val (x, y) = squirclePoint(i / 40f)
            val sum = abs(x).pow(SQUIRCLE_EXPONENT) + abs(y).pow(SQUIRCLE_EXPONENT)
            assertEquals("at t=${i / 40f}", 1f, sum, 0.01f)
        }
    }

    @Test
    fun `the corner is fuller than a circle's`() {
        // What distinguishes a squircle from a rounded rectangle: at 45 degrees it reaches further
        // out than a circle would, which is why the silhouette reads as continuous.
        val (x, y) = squirclePoint(0.125f)
        val circleDiagonal = 0.7071f
        assertTrue("expected > $circleDiagonal, got $x", x > circleDiagonal)
        assertTrue(y > circleDiagonal)
    }

    @Test
    fun `a higher exponent flattens toward a rectangle`() {
        val squircle = squirclePoint(0.125f, exponent = 4.6f).first
        val boxy = squirclePoint(0.125f, exponent = 12f).first
        assertTrue("higher exponent should push the corner out", boxy > squircle)
    }

    @Test
    fun `a lower exponent rounds toward an ellipse`() {
        val squircle = squirclePoint(0.125f, exponent = 4.6f).first
        val round = squirclePoint(0.125f, exponent = 2f).first
        assertTrue("lower exponent should pull the corner in", round < squircle)
    }

    @Test
    fun `glyph bounds are centred and square`() {
        val bounds = glyphBounds(tileSize = 192, inset = 0.25f)
        val left = bounds[0]
        val right = bounds[2]
        assertEquals(48, left)
        assertEquals(144, right)
        assertEquals("must be square", right - left, bounds[3] - bounds[1])
        assertEquals("must be centred", left, 192 - right)
    }

    @Test
    fun `a zero inset fills the tile`() {
        val bounds = glyphBounds(tileSize = 100, inset = 0f)
        assertEquals(0, bounds[0])
        assertEquals(100, bounds[2])
    }
}

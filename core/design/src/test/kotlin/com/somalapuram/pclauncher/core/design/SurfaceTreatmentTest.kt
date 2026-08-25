package com.somalapuram.pclauncher.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceTreatmentTest {

    @Test
    fun `defaults to a scrim`() {
        val treatment = surfaceTreatmentFor(hardwareAccelerated = true, blurEnabledByUser = false)
        assertEquals(SurfaceTreatment.Scrim(DefaultScrimAlpha), treatment)
    }

    @Test
    fun `blur needs the user to ask for it`() {
        val treatment = surfaceTreatmentFor(hardwareAccelerated = true, blurEnabledByUser = false)
        assertTrue(treatment is SurfaceTreatment.Scrim)
    }

    @Test
    fun `blur needs a renderer that can afford it`() {
        // The pc_x86_64 case: the user asked for blur, but SwiftShader cannot pay for it.
        val treatment = surfaceTreatmentFor(hardwareAccelerated = false, blurEnabledByUser = true)
        assertEquals(SurfaceTreatment.Scrim(DefaultScrimAlpha), treatment)
    }

    @Test
    fun `blur applies only when both hold`() {
        val treatment = surfaceTreatmentFor(hardwareAccelerated = true, blurEnabledByUser = true)
        assertEquals(SurfaceTreatment.Blur(DefaultBlurRadius, BlurScrimAlpha), treatment)
    }

    @Test
    fun `blur is still scrimmed so contrast does not depend on the wallpaper`() {
        val treatment = surfaceTreatmentFor(hardwareAccelerated = true, blurEnabledByUser = true)
        val scrimAlpha = (treatment as SurfaceTreatment.Blur).scrimAlpha
        assertTrue("blur must carry a scrim", scrimAlpha > 0f)
    }

    @Test
    fun `the sheen lifts the top and deepens the bottom around the base alpha`() {
        val stops = surfaceSheen(baseAlpha = 0.72f)

        assertEquals(3, stops.size)
        assertTrue("the top should be lighter than the middle", stops[0] < stops[1])
        assertTrue("the bottom should be deeper than the middle", stops[2] > stops[1])
        assertEquals(0.72f, stops[1], 0.0001f)
    }

    @Test
    fun `a sheen never drives the scrim outside a valid alpha`() {
        // An opaque or fully transparent surface must not produce alphas Compose will reject.
        listOf(0f, 0.02f, 0.5f, 0.98f, 1f).forEach { base ->
            surfaceSheen(base).forEach { stop ->
                assertTrue("alpha $stop out of range for base $base", stop in 0f..1f)
            }
        }
    }

    @Test
    fun `a flat surface is still available by asking for no lift`() {
        assertEquals(listOf(0.6f, 0.6f, 0.6f), surfaceSheen(0.6f, lift = 0f))
    }
}
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
}

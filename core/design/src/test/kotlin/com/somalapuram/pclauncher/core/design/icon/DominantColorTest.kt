package com.somalapuram.pclauncher.core.design.icon

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DominantColorTest {

    private val fallback = Color(0xFF6E7480)

    @Test
    fun `a solid red icon reports red`() {
        val pixels = IntArray(64) { AndroidColor.argb(255, 255, 0, 0) }
        val result = dominantColor(pixels, fallback)
        assertEquals(1f, result.red, 0.02f)
        assertEquals(0f, result.green, 0.02f)
    }

    @Test
    fun `a fully transparent icon falls back`() {
        val pixels = IntArray(64) { AndroidColor.argb(0, 255, 0, 0) }
        assertEquals(fallback, dominantColor(pixels, fallback))
    }

    @Test
    fun `an empty icon falls back`() {
        assertEquals(fallback, dominantColor(IntArray(0), fallback))
    }

    @Test
    fun `transparent pixels contribute no colour`() {
        // Most icons are mostly transparent; counting those pixels is what produces mud.
        val opaqueBlue = IntArray(8) { AndroidColor.argb(255, 0, 0, 255) }
        val clearRed = IntArray(56) { AndroidColor.argb(0, 255, 0, 0) }
        val result = dominantColor(opaqueBlue + clearRed, fallback)
        assertTrue("blue must dominate, got $result", result.blue > 0.8f)
        assertTrue(result.red < 0.2f)
    }

    @Test
    fun `a saturated minority beats a desaturated majority`() {
        // The colour a person would name is the vivid one, not the arithmetic mean — an icon that
        // is mostly white with a red mark reads as red.
        val white = IntArray(48) { AndroidColor.argb(255, 255, 255, 255) }
        val red = IntArray(16) { AndroidColor.argb(255, 255, 0, 0) }
        val result = dominantColor(white + red, fallback)
        assertTrue("red should pull ahead of its area share, got $result", result.red > result.blue)
    }

    @Test
    fun `a greyscale icon yields its own grey rather than the fallback`() {
        val grey = IntArray(64) { AndroidColor.argb(255, 128, 128, 128) }
        val result = dominantColor(grey, fallback)
        assertEquals(result.red, result.green, 0.02f)
        assertEquals(0.5f, result.red, 0.05f)
    }

    @Test
    fun `nearly transparent pixels are ignored`() {
        val faint = IntArray(64) { AndroidColor.argb(10, 255, 0, 0) }
        assertEquals(fallback, dominantColor(faint, fallback))
    }

    @Test
    fun `a bitmap with drawn content is detected`() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(AndroidColor.RED)
        assertTrue(hasVisibleContent(bitmap))
    }

    @Test
    fun `a blank bitmap is detected as empty`() {
        // This is the check that stops an adaptive icon with an empty foreground layer from
        // producing a blank tile.
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(AndroidColor.TRANSPARENT)
        assertFalse(hasVisibleContent(bitmap))
    }

    @Test
    fun `barely-there content does not count as visible`() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(AndroidColor.argb(5, 255, 255, 255))
        assertFalse(hasVisibleContent(bitmap))
    }
}

package com.somalapuram.pclauncher.core.design.icon

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * That a tile reads as a curved lit surface rather than a flat fill.
 *
 * "Looks glossy" is not checkable, and the absence of a check is how the light style drifted to
 * matte: it was tuned by eye against a reference and shipped producing a **4 of 255** luminance
 * falloff on a real device. The layers were all present — two of them were simply painted under the
 * artwork, so raising their alpha would have brightened the margin and nothing else.
 *
 * So the assertion is on the composited pixels, with an opaque tile-filling glyph in the way.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
// Without this Robolectric's canvas is a no-op and every pixel comes back transparent, so the
// assertions below would pass or fail on nothing at all.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconGlossTest {

    private val size = 192

    /** An opaque fill: the hardest case, because it hides anything painted beneath it. */
    private fun composite(style: IconStyle, glyph: Int = AndroidColor.rgb(90, 90, 90)): Bitmap =
        IconCompositor(style).composite(ColorDrawable(glyph), size)

    /** Mean luminance of a horizontal band, sampled inside the tile away from its edges. */
    private fun bandLuminance(bitmap: Bitmap, fromY: Float, toY: Float): Double {
        val x0 = (size * 0.30f).toInt()
        val x1 = (size * 0.70f).toInt()
        val y0 = (size * fromY).toInt()
        val y1 = (size * toY).toInt()
        var total = 0.0
        var count = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val p = bitmap.getPixel(x, y)
                total += 0.299 * AndroidColor.red(p) + 0.587 * AndroidColor.green(p) +
                    0.114 * AndroidColor.blue(p)
                count++
            }
        }
        return total / count
    }

    private fun shadingSpread(style: IconStyle): Double {
        val bitmap = composite(style)
        return bandLuminance(bitmap, 0.24f, 0.34f) - bandLuminance(bitmap, 0.66f, 0.76f)
    }

    @Test
    fun `the light tile is lit at the top and shaded at the bottom`() {
        // A flat fill scores 0. The device measured 4/255 before this was fixed.
        assertTrue(
            "light tiles are flat: the sheen is not reaching the surface",
            shadingSpread(IconStyle.SoftClay) > 35.0,
        )
    }

    @Test
    fun `the dark tile is lit at the top and shaded at the bottom`() {
        assertTrue(
            "dark tiles are flat",
            shadingSpread(IconStyle.DarkGlass) > 20.0,
        )
    }

    @Test
    fun `the shading survives an opaque glyph covering the whole tile`() {
        // The point of the glaze. With the highlight painted under the artwork this is ~0.
        val covered = composite(IconStyle.SoftClay, glyph = AndroidColor.BLACK)
        val spread = bandLuminance(covered, 0.24f, 0.34f) - bandLuminance(covered, 0.66f, 0.76f)

        assertTrue("the glaze is not above the glyph: an opaque icon hides it", spread > 35.0)
    }

    @Test
    fun `removing the glaze removes the shading`() {
        // Proves the assertions above are held up by the glaze rather than by something else.
        val flat = IconStyle.SoftClay.copy(glaze = Color.Transparent, innerShade = Color.Transparent)
        val covered = IconCompositor(flat).composite(ColorDrawable(AndroidColor.BLACK), size)
        val spread = bandLuminance(covered, 0.24f, 0.34f) - bandLuminance(covered, 0.66f, 0.76f)

        assertTrue("with no glaze an opaque glyph should read flat, but spread was $spread", spread < 8.0)
    }

    @Test
    fun `light tiles take their colour from the app rather than all landing on white`() {
        val red = composite(IconStyle.SoftClay, glyph = AndroidColor.rgb(200, 40, 40))
        val blue = composite(IconStyle.SoftClay, glyph = AndroidColor.rgb(40, 40, 200))

        // Sampled just inside the tile's left edge: far enough in to be tile rather than the
        // shadow and glow outside it, far enough out to be clear of the glyph.
        //
        // This used to sample at 0.12, which is outside the tile altogether — the compositor
        // reserves 24% of the box on every side for the shadow and glow. What it actually measured
        // there was the *glow*, which is drawn in the app's colour, so the test passed for the
        // wrong reason and only said so when the glow's radius changed.
        val redCorner = red.getPixel((size * 0.30f).toInt(), (size * 0.5f).toInt())
        val blueCorner = blue.getPixel((size * 0.30f).toInt(), (size * 0.5f).toInt())

        assertNotEquals(
            "every tile lands on the same near-white and the set reads as identical squares",
            AndroidColor.red(redCorner) - AndroidColor.blue(redCorner),
            AndroidColor.red(blueCorner) - AndroidColor.blue(blueCorner),
        )
    }

    @Test
    fun `the dark style stays dark`() {
        val bitmap = composite(IconStyle.DarkGlass)
        assertTrue(
            "dark glass has been washed out",
            bandLuminance(bitmap, 0.40f, 0.60f) < 150.0,
        )
    }
}

package com.somalapuram.pclauncher.core.design.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The ceiling that stops a white app icon landing on a white tile (white-icon-tiles.md). */
class TileCeilingTest {

    private val white = Color.White
    private val ceiling = IconStyle.SoftClay.maxTileLuminance

    @Test
    fun `a white app does not get a white tile`() {
        val tile = tileColorFor(IconStyle.SoftClay, white)
        assertTrue(
            "a white app's tile came out at ${tile.luminance()}, over the $ceiling ceiling",
            tile.luminance() <= ceiling,
        )
    }

    /** The whole point: the glyph needs a ground, so the tile has to be darker than the glyph. */
    @Test
    fun `a white glyph has something to sit on`() {
        val tile = tileColorFor(IconStyle.SoftClay, white)
        assertTrue(white.luminance() - tile.luminance() > 0.2f)
    }

    @Test
    fun `a tile already below the ceiling is untouched`() {
        val chromeish = Color(0xFF2E7D32)
        val plain = IconStyle.SoftClay.let { style ->
            val t = style.tileTint
            Color(
                red = style.tileBase.red + (chromeish.red - style.tileBase.red) * t,
                green = style.tileBase.green + (chromeish.green - style.tileBase.green) * t,
                blue = style.tileBase.blue + (chromeish.blue - style.tileBase.blue) * t,
            )
        }
        assertTrue("this fixture is meant to be under the ceiling", plain.luminance() < ceiling)
        assertEquals(plain, tileColorFor(IconStyle.SoftClay, chromeish))
    }

    /**
     * A pale yellow that is too light must come down as yellow, not as grey — otherwise the
     * correction would erase exactly the colour the tile exists to carry.
     */
    @Test
    fun `hue survives the correction`() {
        val paleYellow = Color(0xFFFFFBD6)
        val tile = tileColorFor(IconStyle.SoftClay, paleYellow)

        assertTrue("came out over the ceiling", tile.luminance() <= ceiling + 0.001f)
        assertTrue("the yellow was flattened to grey", tile.red - tile.blue > 0.04f)
    }

    /** A colour with no hue has none to keep, and a neutral grey is the honest result. */
    @Test
    fun `a colourless app gets a neutral tile`() {
        val tile = tileColorFor(IconStyle.SoftClay, white)
        assertTrue(kotlin.math.abs(tile.red - tile.blue) < 0.03f)
        assertTrue(kotlin.math.abs(tile.red - tile.green) < 0.03f)
    }

    @Test
    fun `the dark style has no ceiling and is unaffected`() {
        assertEquals(1f, IconStyle.DarkGlass.maxTileLuminance)
        val tile = tileColorFor(IconStyle.DarkGlass, white)
        // The blend at 13% of white over near-black, exactly as before.
        assertTrue(tile.luminance() < 0.2f)
    }

    /**
     * At the ceiling, and never above it. An sRGB colour holds 8 bits a channel, so the exact
     * target falls between two representable colours; the one below is the one to take.
     */
    @Test
    fun `the ceiling is a bound, and the result sits just under it`() {
        val tile = tileColorFor(IconStyle.SoftClay, white)

        assertTrue("above the ceiling at ${tile.luminance()}", tile.luminance() <= ceiling)
        // Within one 8-bit step of it, so the clamp is not quietly overshooting downward either.
        assertTrue("further under the ceiling than one step", tile.luminance() > ceiling - 0.02f)
    }
}

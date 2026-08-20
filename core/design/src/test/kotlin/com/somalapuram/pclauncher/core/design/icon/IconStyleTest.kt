package com.somalapuram.pclauncher.core.design.icon

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconStyleTest {

    @Test
    fun `dark theme gets glass and light gets clay`() {
        assertEquals(IconStyle.DarkGlass, iconStyleFor(darkTheme = true))
        assertEquals(IconStyle.SoftClay, iconStyleFor(darkTheme = false))
    }

    @Test
    fun `the two styles are distinguishable by id`() {
        // The id goes into the cache key, so a collision would serve dark tiles on a light desktop.
        assertNotEquals(IconStyle.DarkGlass.id, IconStyle.SoftClay.id)
    }

    @Test
    fun `glass keeps tiles near-black`() {
        // Blend too much of the app's colour and every tile becomes a different mid-grey; the set
        // then stops reading as one design.
        assertTrue(
            "dark tint should stay low, was ${IconStyle.DarkGlass.tileTint}",
            IconStyle.DarkGlass.tileTint < 0.2f,
        )
        assertTrue(IconStyle.DarkGlass.tileBase.red < 0.2f)
    }

    @Test
    fun `clay carries more of the app colour than glass does`() {
        assertTrue(IconStyle.SoftClay.tileTint > IconStyle.DarkGlass.tileTint)
    }

    @Test
    fun `both styles overscale an adaptive foreground`() {
        // Adaptive icons confine artwork to the central 72 of 108dp; drawn at face value the glyph
        // occupies a third of the tile and looks lost.
        assertTrue(IconStyle.DarkGlass.adaptiveForegroundScale > 1f)
        assertTrue(IconStyle.SoftClay.adaptiveForegroundScale > 1f)
    }

    @Test
    fun `legacy icons sit smaller than adaptive ones`() {
        // A legacy icon carries its own padding and often its own corners, so it needs more room.
        assertTrue(IconStyle.DarkGlass.legacyInset > IconStyle.DarkGlass.adaptiveInset)
    }

    @Test
    fun `no tint yields the base colour`() {
        val style = IconStyle.DarkGlass.copy(tileTint = 0f)
        assertEquals(style.tileBase, tileColorFor(style, Color.Red))
    }

    @Test
    fun `full tint yields the app colour`() {
        val style = IconStyle.DarkGlass.copy(tileTint = 1f)
        val tinted = tileColorFor(style, Color.Red)
        assertEquals(1f, tinted.red, 0.001f)
        assertEquals(0f, tinted.green, 0.001f)
    }

    @Test
    fun `tinting always yields an opaque tile`() {
        // A translucent tile would let the wallpaper through and break the silhouette.
        assertEquals(1f, tileColorFor(IconStyle.SoftClay, Color(0x40FF0000)).alpha, 0.001f)
    }
}

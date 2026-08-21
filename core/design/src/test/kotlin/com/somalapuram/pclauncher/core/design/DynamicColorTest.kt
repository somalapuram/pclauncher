package com.somalapuram.pclauncher.core.design

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The system-scheme → shell-token mapping.
 *
 * The value of testing this is the *pairing*: each foreground has to come from the role Material
 * guarantees is legible on the background it sits on. Mixing unrelated roles is how dynamic colour
 * ends up grey-on-grey on somebody's wallpaper, and it only shows up on that one wallpaper.
 */
class DynamicColorTest {

    private val scheme = lightColorScheme(
        primary = Color(0xFF7B4EA8),
        onPrimary = Color(0xFFFFFFFF),
        surface = Color(0xFFFDF7FF),
        onSurface = Color(0xFF1D1B20),
        onSurfaceVariant = Color(0xFF49454F),
        outlineVariant = Color(0xFFCAC4D0),
    )

    @Test
    fun `the accent comes from primary`() {
        assertEquals(scheme.primary, pcColorsFrom(scheme).accent)
        assertEquals(scheme.onPrimary, pcColorsFrom(scheme).onAccent)
    }

    @Test
    fun `surface and its foreground are a matched pair`() {
        val colors = pcColorsFrom(scheme)
        assertEquals(scheme.surface, colors.surface)
        assertEquals(scheme.onSurface, colors.onSurface)
    }

    @Test
    fun `the muted foreground uses the variant role, not a faded onSurface`() {
        // Fading onSurface loses contrast unpredictably once the surface itself is translucent,
        // which every shell surface is.
        val colors = pcColorsFrom(scheme)
        assertEquals(scheme.onSurfaceVariant, colors.onSurfaceMuted)
        assertNotEquals(colors.onSurface, colors.onSurfaceMuted)
    }

    @Test
    fun `hairlines use the outline variant`() {
        assertEquals(scheme.outlineVariant, pcColorsFrom(scheme).hairline)
    }

    @Test
    fun `the scrim is the same ground onSurface is guaranteed against`() {
        // Shell surfaces tint from the scrim; if it were a different role, the contrast guarantee
        // that makes onSurface readable would no longer apply.
        val colors = pcColorsFrom(scheme)
        assertEquals(colors.surface, colors.scrim)
    }

    @Test
    fun `a dark scheme maps to a dark palette`() {
        val dark = darkColorScheme(
            primary = Color(0xFFD0BCFF),
            surface = Color(0xFF141218),
            onSurface = Color(0xFFE6E0E9),
        )
        val colors = pcColorsFrom(dark)
        assertEquals(dark.surface, colors.surface)
        assertEquals(dark.primary, colors.accent)
    }

    @Test
    fun `every token is populated`() {
        // A missed field would silently default to black and only show on one surface.
        val colors = pcColorsFrom(scheme)
        for (c in listOf(
            colors.surface, colors.onSurface, colors.onSurfaceMuted,
            colors.hairline, colors.accent, colors.onAccent, colors.scrim,
        )) {
            assertNotEquals(Color.Unspecified, c)
        }
    }

    @Test
    fun `the static palettes are still available as the override`() {
        // dynamic-color.md requirement 3: these must remain, unchanged, for the manual override
        // and as the fallback when a device cannot produce a scheme.
        assertNotEquals(PcLightColors, PcDarkColors)
        assertEquals(Color(0xFF2F6FED), PcLightColors.accent)
    }
}

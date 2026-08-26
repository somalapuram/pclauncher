package com.somalapuram.pclauncher.core.design

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which way round the shell's chrome goes.
 *
 * It used to follow `isSystemInDarkTheme()`, which answers what the user's *apps* should look like —
 * not what is behind a shell that sits directly on the wallpaper. A light system theme over a dark
 * wallpaper is an ordinary configuration and was exactly where the shell looked worst.
 */
class WallpaperChromeTest {

    @Test
    fun `a wallpaper that supports dark text gets light chrome`() {
        val tone = WallpaperTone(supportsDarkText = true)

        assertFalse(chromeIsDark(tone, systemDark = false))
        // And the system setting does not get a vote once the wallpaper has spoken.
        assertFalse(chromeIsDark(tone, systemDark = true))
    }

    @Test
    fun `a wallpaper that does not support dark text gets dark chrome`() {
        val tone = WallpaperTone(supportsDarkText = false)

        assertTrue(chromeIsDark(tone, systemDark = false))
        assertTrue(chromeIsDark(tone, systemDark = true))
    }

    @Test
    fun `without a hint the dominant colour decides`() {
        assertTrue(chromeIsDark(WallpaperTone(dominant = Color(0xFF101014)), systemDark = false))
        assertFalse(chromeIsDark(WallpaperTone(dominant = Color(0xFFF2F1EC)), systemDark = true))
    }

    @Test
    fun `the hint outranks the dominant colour`() {
        // A mostly-light wallpaper can still be dark where the bar goes; the platform's hint sees
        // that and a single colour cannot.
        val lightColourDarkHint = WallpaperTone(
            supportsDarkText = false,
            dominant = Color(0xFFF2F1EC),
        )

        assertTrue(chromeIsDark(lightColourDarkHint, systemDark = false))
    }

    @Test
    fun `a wallpaper that says nothing leaves the shell as it is`() {
        // Live wallpapers, one still loading, or a device that returns nothing at all.
        assertTrue(chromeIsDark(WallpaperTone(), systemDark = true))
        assertFalse(chromeIsDark(WallpaperTone(), systemDark = false))
    }

    @Test
    fun `mid grey takes light text`() {
        // Being wrong here costs contrast, not legibility, and light text carries better on mid.
        assertTrue(chromeIsDark(WallpaperTone(dominant = Color(0xFF6E6E72)), systemDark = false))
    }
}

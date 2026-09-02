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
    fun `a wallpaper that supports dark text and reports no colours gets light chrome`() {
        val tone = WallpaperTone(supportsDarkText = true)

        assertFalse(chromeIsDark(tone, systemDark = false))
        // And the system setting does not get a vote once the wallpaper has spoken.
        assertFalse(chromeIsDark(tone, systemDark = true))
    }

    @Test
    fun `a wallpaper without dark-text support and no colours gets dark chrome`() {
        val tone = WallpaperTone(supportsDarkText = false)

        assertTrue(chromeIsDark(tone, systemDark = false))
        assertTrue(chromeIsDark(tone, systemDark = true))
    }

    @Test
    fun `the dominant colour decides when it is all there is`() {
        assertTrue(chromeIsDark(WallpaperTone(dominant = Color(0xFF101014)), systemDark = false))
        assertFalse(chromeIsDark(WallpaperTone(dominant = Color(0xFFF2F1EC)), systemDark = true))
    }

    /**
     * The reverse of what this file used to assert, and the reversal is the point.
     *
     * The hint used to outrank the colours, on the argument that a mostly-light wallpaper can still
     * be dark where the bar goes. The measured counter-example is this wallpaper: `hints=4`, no
     * dark-text support, reported colours `#b88545` `#cdb68b` `#f0f1f2` — all light — and 180–208
     * of 255 measured behind the desktop labels. Following the hint put white text on a pale ground
     * (wallpaper-theme-alignment.md).
     */
    @Test
    fun `the colours outrank the hint`() {
        val paleWallpaperWithNoDarkTextHint = WallpaperTone(
            supportsDarkText = false,
            dominant = Color(0xFFB88545),
            palette = listOf(Color(0xFFCDB68B), Color(0xFFF0F1F2)),
        )

        assertFalse(chromeIsDark(paleWallpaperWithNoDarkTextHint, systemDark = true))
    }

    @Test
    fun `a dark wallpaper stays dark whatever the hint says`() {
        val dark = WallpaperTone(
            supportsDarkText = true,
            dominant = Color(0xFF101318),
            palette = listOf(Color(0xFF1A2230), Color(0xFF20303F)),
        )

        assertTrue(chromeIsDark(dark, systemDark = false))
    }

    /** The mean, so one large mid-tone feature cannot speak for a pale picture. */
    @Test
    fun `pale surrounds outvote a mid-tone dominant`() {
        val tone = WallpaperTone(
            dominant = Color(0xFF8A8A8A),
            palette = listOf(Color(0xFFF2F2F2), Color(0xFFEDEDED)),
        )

        assertFalse(chromeIsDark(tone, systemDark = true))
    }

    /** The hint is still the answer when it is the only thing the wallpaper reported. */
    @Test
    fun `a hint without colours still decides`() {
        assertTrue(chromeIsDark(WallpaperTone(supportsDarkText = false), systemDark = false))
        assertFalse(chromeIsDark(WallpaperTone(supportsDarkText = true), systemDark = true))
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

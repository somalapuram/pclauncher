package com.somalapuram.pclauncher.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * What the wallpaper says about itself.
 *
 * [supportsDarkText] is the platform's own judgement, computed from the wallpaper's actual pixels —
 * `WallpaperColors.HINT_SUPPORTS_DARK_TEXT`. Null means it did not say.
 */
data class WallpaperTone(
    val supportsDarkText: Boolean? = null,
    val dominant: Color? = null,
    /**
     * The other colours the platform reported alongside [dominant].
     *
     * Kept because one colour is a poor summary of a picture: the dominant is the *most common*
     * colour, which on a wallpaper with a large mid-tone feature is not the colour the shell sits
     * on (wallpaper-theme-alignment.md).
     */
    val palette: List<Color> = emptyList(),
)

/**
 * Whether the shell's chrome should be dark.
 *
 * `isSystemInDarkTheme()` answers a different question: what the user's *apps* should look like. The
 * shell does not sit on a page of its own — it sits on the wallpaper — so what it needs to know is
 * what is behind it. The two coincide often enough that the mistake is easy to make, and a light
 * theme over a dark wallpaper is exactly where it shows (wallpaper-chrome.md).
 *
 * One answer for the whole shell — bar, menus and desktop labels alike — so they cannot disagree
 * with each other. Everything downstream reads the scheme this produces.
 *
 * **The colours decide, not the dark-text hint.** The hint used to come first, on the argument that
 * it sees the whole wallpaper including the regions text sits on, which one colour cannot. The
 * measured counter-example: a wallpaper reporting `hints=4` — no dark-text support — whose reported
 * colours were `#b88545`, `#cdb68b` and `#f0f1f2`, every one light, and whose luminance behind the
 * desktop labels was 180–208 of 255. The hint was speaking for a dark feature through the middle
 * that neither the labels nor the bar sit on; following it put white text on a pale ground at about
 * 1.3:1, against the 4.5:1 SRS §12 requires (wallpaper-theme-alignment.md).
 *
 * The mean across every reported colour, so one large feature cannot speak for the whole picture.
 * The hint is still the fallback when a wallpaper reports one but no colours, and the system setting
 * the last resort — so a wallpaper that says nothing changes nothing.
 */
fun chromeIsDark(tone: WallpaperTone, systemDark: Boolean): Boolean {
    val colours = listOfNotNull(tone.dominant) + tone.palette
    if (colours.isNotEmpty()) {
        return colours.map { it.luminance() }.average() < DarkLuminanceThreshold
    }
    tone.supportsDarkText?.let { return !it }
    return systemDark
}

/**
 * Below this, a colour is treated as dark enough to need light text.
 *
 * Slightly under the midpoint: mid-grey chrome reads better carrying light text than dark, and the
 * cost of being wrong is a contrast loss rather than an illegible surface.
 */
const val DarkLuminanceThreshold = 0.45f

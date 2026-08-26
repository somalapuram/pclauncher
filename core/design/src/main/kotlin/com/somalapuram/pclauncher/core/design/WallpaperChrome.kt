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
)

/**
 * Whether the shell's chrome should be dark.
 *
 * `isSystemInDarkTheme()` answers a different question: what the user's *apps* should look like. The
 * shell does not sit on a page of its own — it sits on the wallpaper — so what it needs to know is
 * what is behind it. The two coincide often enough that the mistake is easy to make, and a light
 * theme over a dark wallpaper is exactly where it shows (wallpaper-chrome.md).
 *
 * Preference order is deliberate: the platform's hint sees the whole wallpaper including the
 * regions text will sit on, which a single dominant colour cannot. Falling all the way through to
 * the system setting means a wallpaper that says nothing leaves the shell exactly as it is today.
 */
fun chromeIsDark(tone: WallpaperTone, systemDark: Boolean): Boolean {
    tone.supportsDarkText?.let { return !it }
    tone.dominant?.let { return it.luminance() < DarkLuminanceThreshold }
    return systemDark
}

/**
 * Below this, a colour is treated as dark enough to need light text.
 *
 * Slightly under the midpoint: mid-grey chrome reads better carrying light text than dark, and the
 * cost of being wrong is a contrast loss rather than an illegible surface.
 */
const val DarkLuminanceThreshold = 0.45f

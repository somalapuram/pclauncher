package com.somalapuram.pclauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.somalapuram.pclauncher.core.design.WallpaperTone

/**
 * The wallpaper's tone, kept current.
 *
 * `getWallpaperColors` needs no permission — unlike `getDrawable`, which is why this reads the
 * colours rather than the pixels. Everything is guarded: this runs on the way to drawing the home
 * screen, and a wallpaper that cannot be read must cost the tint and nothing else (GATE 4).
 *
 * Read **during** composition, not from the effect that follows it. An effect runs after the first
 * frame is composed, so the chrome drew one frame knowing nothing — which `chromeIsDark` resolves
 * through to the system setting, and on a light-mode device over a dark wallpaper that is a white
 * flash before the dark theme arrives. Harmless once at launch; visible on every press once the
 * Start menu became a window of its own (chrome-tone-flash.md).
 */
@Composable
fun rememberWallpaperTone(): WallpaperTone {
    val context = LocalContext.current
    val manager = remember(context) {
        runCatching { WallpaperManager.getInstance(context) }.getOrNull()
    }
    // The cache, so the second window and every one after it starts from the answer rather than
    // paying for the platform call again on every Start press.
    var tone by remember(context) {
        mutableStateOf(lastKnownTone ?: manager.readTone().also { lastKnownTone = it })
    }

    DisposableEffect(context) {
        // Re-read on attach as well: the wallpaper may have changed while no window of ours was
        // composed, and the cache would then be stale.
        manager.readTone().let { tone = it; lastKnownTone = it }

        // A wallpaper change has to flip the chrome without a restart; the shell is the one app
        // that is always already running when it happens.
        val listener = WallpaperManager.OnColorsChangedListener { _, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                manager.readTone().let { tone = it; lastKnownTone = it }
            }
        }
        val registered = runCatching {
            manager?.addOnColorsChangedListener(listener, android.os.Handler(context.mainLooper))
        }.isSuccess

        onDispose {
            if (registered) runCatching { manager?.removeOnColorsChangedListener(listener) }
        }
    }

    return tone
}

/**
 * The last tone any window resolved.
 *
 * Main-thread only — composition and the colours listener both run there — so a plain var is the
 * whole of the synchronisation it needs. A cache of a platform value, not shell state: the listener
 * keeps it current, and being wrong for one frame after a wallpaper change costs a tint.
 */
private var lastKnownTone: WallpaperTone? = null

private fun WallpaperManager?.readTone(): WallpaperTone = runCatching {
    val colors = this?.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        ?: return@runCatching WallpaperTone()
    WallpaperTone(
        supportsDarkText = colors.colorHints and HINT_SUPPORTS_DARK_TEXT != 0,
        dominant = Color(colors.primaryColor.toArgb()),
    )
}.getOrDefault(WallpaperTone())

/**
 * `WallpaperColors.HINT_SUPPORTS_DARK_TEXT`.
 *
 * Named here because the constant is only exposed from API 31 upward on some surfaces and the value
 * is stable; reading it reflectively would be worse than stating it.
 */
private const val HINT_SUPPORTS_DARK_TEXT = 1 shl 0

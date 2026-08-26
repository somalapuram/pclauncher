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
 */
@Composable
fun rememberWallpaperTone(): WallpaperTone {
    val context = LocalContext.current
    var tone by remember { mutableStateOf(WallpaperTone()) }

    DisposableEffect(context) {
        val manager = runCatching { WallpaperManager.getInstance(context) }.getOrNull()
        tone = manager.readTone()

        // A wallpaper change has to flip the chrome without a restart; the shell is the one app
        // that is always already running when it happens.
        val listener = WallpaperManager.OnColorsChangedListener { _, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) tone = manager.readTone()
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

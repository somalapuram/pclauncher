package com.somalapuram.pclauncher.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

val LocalPcColors = staticCompositionLocalOf { PcDarkColors }

val LocalSurfaceTreatment = staticCompositionLocalOf<SurfaceTreatment> {
    SurfaceTreatment.Scrim(DefaultScrimAlpha)
}

/**
 * The shell theme. Provides the palette and the surface treatment every chrome surface reads.
 *
 * [dynamicColor] takes the system's Material You colours, so the shell looks like it belongs on the
 * user's device rather than shipping its own opinion (dynamic-color.md). `minSdk 31` means this
 * needs no version gate — dynamic colour arrived in Android 12, and the minimum was already set
 * there for freeform windowing.
 *
 * [blurEnabled] is the user's preference; whether it is honoured also depends on the renderer
 * (SRS §4.3) — see [surfaceTreatmentFor].
 */
@Composable
fun PcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    blurEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // Kept as a scheme, not just as our own tokens, because Material's own components — dropdown
    // menus, the volume slider — read `MaterialTheme` and nothing else. Without wrapping them, a
    // dark shell served light menus, which is what a shell that never wrapped MaterialTheme looks
    // like once its polarity stops matching the system's.
    val scheme = when {
        // A device that refuses to produce a scheme falls back rather than failing: this runs on
        // the way to drawing the home screen (GATE 4).
        dynamicColor -> runCatching {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }.getOrElse { fallbackScheme(darkTheme) }

        else -> fallbackScheme(darkTheme)
    }
    val colors = runCatching { pcColorsFrom(scheme) }
        .getOrElse { if (darkTheme) PcDarkColors else PcLightColors }
    val hardwareAccelerated = LocalView.current.isHardwareAccelerated

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalPcColors provides colors,
            LocalSurfaceTreatment provides surfaceTreatmentFor(
                hardwareAccelerated = hardwareAccelerated,
                blurEnabledByUser = blurEnabled,
            ),
            content = content,
        )
    }
}

/** The palette when the platform will not produce a dynamic one. */
private fun fallbackScheme(darkTheme: Boolean) =
    if (darkTheme) darkColorScheme() else lightColorScheme()

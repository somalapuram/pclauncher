package com.somalapuram.pclauncher.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

val LocalPcColors = staticCompositionLocalOf { PcDarkColors }

val LocalSurfaceTreatment = staticCompositionLocalOf<SurfaceTreatment> {
    SurfaceTreatment.Scrim(DefaultScrimAlpha)
}

/**
 * The shell theme. Provides the palette and the surface treatment every chrome surface reads.
 *
 * [blurEnabled] is the user's preference; whether it is honoured also depends on the renderer
 * (SRS §4.3) — see [surfaceTreatmentFor].
 */
@Composable
fun PcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    blurEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) PcDarkColors else PcLightColors
    val hardwareAccelerated = LocalView.current.isHardwareAccelerated

    CompositionLocalProvider(
        LocalPcColors provides colors,
        LocalSurfaceTreatment provides surfaceTreatmentFor(
            hardwareAccelerated = hardwareAccelerated,
            blurEnabledByUser = blurEnabled,
        ),
        content = content,
    )
}

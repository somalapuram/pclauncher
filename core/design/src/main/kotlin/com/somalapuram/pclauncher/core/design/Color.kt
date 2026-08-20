package com.somalapuram.pclauncher.core.design

import androidx.compose.ui.graphics.Color

/**
 * The shell palette. Light and dark are both first-class (SRS §6.1 principle 6), and a single
 * accent carries emphasis — running, focused, and needs-attention are told apart by shape, not by
 * colour alone (principle 5).
 */
data class PcColors(
    val surface: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val hairline: Color,
    val accent: Color,
    val onAccent: Color,
    val scrim: Color,
)

val PcLightColors = PcColors(
    surface = Color(0xFFF7F7F8),
    onSurface = Color(0xFF16181D),
    onSurfaceMuted = Color(0xFF5B6069),
    hairline = Color(0x1F000000),
    accent = Color(0xFF2F6FED),
    onAccent = Color(0xFFFFFFFF),
    scrim = Color(0xFFFFFFFF),
)

val PcDarkColors = PcColors(
    surface = Color(0xFF15171C),
    onSurface = Color(0xFFECEEF2),
    onSurfaceMuted = Color(0xFF9BA1AC),
    hairline = Color(0x24FFFFFF),
    accent = Color(0xFF5B93FF),
    onAccent = Color(0xFF0B1220),
    scrim = Color(0xFF0B0D11),
)

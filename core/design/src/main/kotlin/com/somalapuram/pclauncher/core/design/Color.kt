package com.somalapuram.pclauncher.core.design

import androidx.compose.material3.ColorScheme
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

/**
 * Map the system's dynamic scheme onto the shell's tokens.
 *
 * One function, in one place, so nothing outside this module learns about Material colour roles —
 * every surface keeps reading [LocalPcColors] and none of them care where the values came from.
 *
 * Roles are paired deliberately: each foreground comes from the role that Material guarantees is
 * legible on the background it is placed on. Assembling a palette from unrelated roles is how
 * dynamic colour ends up with grey-on-grey on somebody's wallpaper.
 */
fun pcColorsFrom(scheme: ColorScheme): PcColors = PcColors(
    surface = scheme.surface,
    onSurface = scheme.onSurface,
    // The muted role that Material pairs with surface — not onSurface at reduced alpha, which
    // loses contrast unpredictably once the surface itself is translucent.
    onSurfaceMuted = scheme.onSurfaceVariant,
    hairline = scheme.outlineVariant,
    accent = scheme.primary,
    onAccent = scheme.onPrimary,
    // The shell's translucent surfaces tint from this, so it must be the same ground onSurface is
    // guaranteed against.
    scrim = scheme.surface,
)

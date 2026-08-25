package com.somalapuram.pclauncher.core.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How a shell surface — menu bar, dock, Start menu, palette — separates itself from the wallpaper.
 *
 * SRS §4.3: the target device renders on the CPU through SwiftShader until Mesa lands, which makes
 * a full-width backdrop blur a per-frame full-screen CPU cost. So blur is **opt-in and detected,
 * never assumed**, and a translucent scrim with a hairline border is the default. At these surface
 * sizes the two read almost identically.
 */
sealed interface SurfaceTreatment {

    /** The cheap default: flat translucency over the wallpaper. */
    data class Scrim(val alpha: Float) : SurfaceTreatment

    /** The expensive option: a real backdrop blur, still scrimmed for contrast. */
    data class Blur(val radius: Dp, val scrimAlpha: Float) : SurfaceTreatment
}

/** Opaque enough for 4.5:1 text contrast over an arbitrary wallpaper (SRS §12). */
const val DefaultScrimAlpha: Float = 0.72f

/** Kept modest — a large radius costs more than it looks like it should. */
val DefaultBlurRadius: Dp = 24.dp

/** Blur still needs a scrim underneath it, or text contrast depends on the wallpaper. */
const val BlurScrimAlpha: Float = 0.45f

/**
 * The vertical sheen on a shell surface.
 *
 * A bar of one flat colour has no dimension, which is conspicuous once the icons sitting on it are
 * glossy (icon-gloss.md). One linear gradient is affordable where a blur is not (SRS §4.3): it is a
 * single shader on a surface that already draws a background, with no extra layer and no per-frame
 * cost beyond the fill that was happening anyway.
 *
 * Returned as stops rather than a Brush so `core:design` states the intent and the surface applies
 * it — and so the values are testable without a composition.
 */
fun surfaceSheen(baseAlpha: Float, lift: Float = DefaultSheenLift): List<Float> =
    listOf(
        (baseAlpha - lift).coerceIn(0f, 1f),
        baseAlpha.coerceIn(0f, 1f),
        (baseAlpha + lift * 0.5f).coerceIn(0f, 1f),
    )

/** Kept small: the bar is furniture (SRS §6.1) and a strong gradient would compete with the icons. */
const val DefaultSheenLift: Float = 0.10f

/**
 * Decide a surface's treatment.
 *
 * Blur requires **both** a renderer that can afford it and the user asking for it. Either one
 * missing falls back to the scrim — a user who enables blur on a software renderer still gets the
 * cheap path, because the setting expresses a preference, not an override of physics.
 */
fun surfaceTreatmentFor(
    hardwareAccelerated: Boolean,
    blurEnabledByUser: Boolean,
    scrimAlpha: Float = DefaultScrimAlpha,
    blurRadius: Dp = DefaultBlurRadius,
): SurfaceTreatment = when {
    hardwareAccelerated && blurEnabledByUser -> SurfaceTreatment.Blur(blurRadius, BlurScrimAlpha)
    else -> SurfaceTreatment.Scrim(scrimAlpha)
}

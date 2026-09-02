package com.somalapuram.pclauncher.core.design.icon

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance

/**
 * The colour a legacy icon should sit on.
 *
 * A legacy icon has no separable background layer, so the tile has to be derived from the artwork
 * itself. Averaging every pixel gives mud — most icons are mostly transparent or mostly white — so
 * this weights by saturation: the colour a person would name if asked "what colour is this icon?"
 * is the most saturated one that covers meaningful area, not the arithmetic mean.
 */
fun dominantColor(
    pixels: IntArray,
    fallback: Color = Color(0xFF6E7480),
    minAlpha: Int = 40,
): Color {
    var weightedR = 0.0
    var weightedG = 0.0
    var weightedB = 0.0
    var totalWeight = 0.0

    for (pixel in pixels) {
        val alpha = AndroidColor.alpha(pixel)
        // Fully and nearly transparent pixels carry no colour information; an icon that is 80%
        // transparent would otherwise report whatever the compositor left behind.
        if (alpha < minAlpha) continue

        val r = AndroidColor.red(pixel)
        val g = AndroidColor.green(pixel)
        val b = AndroidColor.blue(pixel)

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        // Saturation, plus a floor so a genuinely greyscale icon still yields its own grey rather
        // than falling through to the fallback.
        val saturation = if (max == 0) 0.0 else (max - min).toDouble() / max
        val weight = (alpha / 255.0) * (0.15 + saturation)

        weightedR += r * weight
        weightedG += g * weight
        weightedB += b * weight
        totalWeight += weight
    }

    if (totalWeight <= 0.0) return fallback

    return Color(
        red = (weightedR / totalWeight / 255.0).toFloat().coerceIn(0f, 1f),
        green = (weightedG / totalWeight / 255.0).toFloat().coerceIn(0f, 1f),
        blue = (weightedB / totalWeight / 255.0).toFloat().coerceIn(0f, 1f),
        alpha = 1f,
    )
}

/** Sample a bitmap for [dominantColor]. Downsampled — colour does not need every pixel. */
fun dominantColorOf(bitmap: Bitmap, sampleEdge: Int = 32): Color {
    val scaled = runCatching {
        Bitmap.createScaledBitmap(bitmap, sampleEdge, sampleEdge, true)
    }.getOrNull() ?: return dominantColor(IntArray(0))

    val pixels = IntArray(scaled.width * scaled.height)
    scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
    return dominantColor(pixels)
}

/**
 * Mix the app's colour into the style's base by [amount].
 *
 * Dark glass keeps most of its own near-black so the set stays uniform; soft clay lets more of the
 * app through so the tiles read as pastel rather than as identical white squares.
 */
fun tileColorFor(style: IconStyle, appColor: Color): Color {
    val t = style.tileTint.coerceIn(0f, 1f)
    val blended = Color(
        red = style.tileBase.red + (appColor.red - style.tileBase.red) * t,
        green = style.tileBase.green + (appColor.green - style.tileBase.green) * t,
        blue = style.tileBase.blue + (appColor.blue - style.tileBase.blue) * t,
        alpha = 1f,
    )
    return blended.darkenedTo(style.maxTileLuminance)
}

/**
 * Bring a colour down to a maximum luminance, keeping its hue.
 *
 * The tile is built by blending the app's own colour into a near-white base, which works until the
 * app's colour is itself near-white: the blend has nothing to move toward, the tile stays white, and
 * a pale glyph disappears into it. Android's stock Clock is exactly that — a white background layer
 * behind a pale face (white-icon-tiles.md).
 *
 * Scaled in **linear** light rather than on the sRGB components, because luminance is linear there:
 * one multiply lands on the value asked for instead of near it. Scaling every channel by the same
 * factor is what keeps the hue — a pale yellow becomes a deeper yellow, and only a colour with no
 * hue at all becomes grey.
 *
 * One multiply gets there, but not exactly: an sRGB `Color` holds 8 bits a channel, so the target
 * usually falls between two representable colours and the conversion rounds to the nearer — which
 * may be the lighter one. For white against a 0.75 ceiling the neighbours are 224 (0.738) and 225
 * (0.753), and it rounds up. Repeating the scale does not help; 225 is a fixed point of it.
 *
 * So the rounding is corrected rather than tolerated: one step down, once, which makes the ceiling
 * a bound the result is never above rather than a value it is merely near.
 */
private fun Color.darkenedTo(max: Float): Color {
    if (max >= 1f) return this
    val current = luminance()
    if (current <= max || current <= 0f) return this

    val factor = max / current
    val linear = convert(ColorSpaces.LinearSrgb)
    val scaled = Color(
        red = (linear.red * factor).coerceIn(0f, 1f),
        green = (linear.green * factor).coerceIn(0f, 1f),
        blue = (linear.blue * factor).coerceIn(0f, 1f),
        alpha = alpha,
        colorSpace = ColorSpaces.LinearSrgb,
    ).convert(ColorSpaces.Srgb)

    return if (scaled.luminance() <= max) scaled else scaled.oneStepDarker()
}

/** One 8-bit step down each channel — the smallest change an sRGB [Color] can represent. */
private fun Color.oneStepDarker(): Color = Color(
    red = (red - EightBitStep).coerceAtLeast(0f),
    green = (green - EightBitStep).coerceAtLeast(0f),
    blue = (blue - EightBitStep).coerceAtLeast(0f),
    alpha = alpha,
)

private const val EightBitStep = 1f / 255f

/**
 * Does this bitmap actually draw anything?
 *
 * Some adaptive icons put their artwork somewhere the foreground layer alone does not render —
 * a foreground that is an empty `InsetDrawable`, or one that only paints once the parent adaptive
 * drawable has bounds. Drawing that layer yields a blank tile, which is worse than not applying
 * the treatment at all. Checked once at bake time, so the cost is paid on first sight of an app.
 */
fun hasVisibleContent(bitmap: Bitmap, minAlpha: Int = 24): Boolean {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return pixels.any { AndroidColor.alpha(it) >= minAlpha }
}

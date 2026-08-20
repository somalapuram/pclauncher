package com.somalapuram.pclauncher.core.design.icon

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color

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
    return Color(
        red = style.tileBase.red + (appColor.red - style.tileBase.red) * t,
        green = style.tileBase.green + (appColor.green - style.tileBase.green) * t,
        blue = style.tileBase.blue + (appColor.blue - style.tileBase.blue) * t,
        alpha = 1f,
    )
}

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

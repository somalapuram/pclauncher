package com.somalapuram.pclauncher.core.design.icon

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb

/**
 * Bakes one app icon into a finished tile.
 *
 * **Runs once per icon, never per frame.** SRS §4.3: `pc_x86_64` composites on the CPU, so a
 * gradient, a rim and a shadow recomputed during dock magnification is not affordable. Everything
 * here is paid at load time and the result lands in the icon cache, leaving drawing as one blit.
 *
 * Light is top-left and identical on every icon — inconsistent lighting is what makes a mixed set
 * look wrong even when every tile is masked the same.
 */
class IconCompositor(private val style: IconStyle) {

    fun composite(source: Drawable, sizePx: Int): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val size = Size(sizePx.toFloat(), sizePx.toFloat())

        val rawAdaptive = source as? AdaptiveIconDrawable
        // An adaptive icon whose foreground renders nothing is treated as legacy: better a
        // correctly-drawn whole icon on a tile than a blank tile.
        val adaptive = rawAdaptive?.takeIf { it.foregroundHasContent(sizePx) }
        val appColor = appColorFor(adaptive, source, sizePx)
        val tile = tileColorFor(style, appColor)

        // The shadow has to be drawn outside the tile, so the tile is inset by enough to leave
        // room for it. Without this the shadow is clipped by the bitmap edge and reads as a band.
        val shadowRoom = sizePx * (style.shadowRadiusFraction + style.shadowOffsetFraction)
        val tilePath = squirclePath(size, inset = shadowRoom).asAndroidPath()

        drawShadow(canvas, size, shadowRoom, sizePx)
        drawTile(canvas, tilePath, tile)
        drawGloss(canvas, tilePath, sizePx)
        drawRim(canvas, tilePath, sizePx)
        drawGlyph(canvas, source, adaptive, sizePx, tilePath)

        return output
    }

    /**
     * The app's own colour — sampled from the **foreground**, not the background plate.
     *
     * A background layer is usually one flat fill chosen to sit behind a mask, and averaging it
     * gives the muddy mid-greys that make a tiled set look cheap. The colour a person associates
     * with an app is in its logo, so that is what tints the tile.
     */
    private fun appColorFor(
        adaptive: AdaptiveIconDrawable?,
        source: Drawable,
        sizePx: Int,
    ): Color {
        val layer = adaptive?.foreground ?: source
        val sample = runCatching { layer.toSampleBitmap(sizePx) }.getOrNull()
            ?: return Color(0xFF6E7480)
        return dominantColorOf(sample)
    }

    private fun drawShadow(canvas: Canvas, size: Size, shadowRoom: Float, sizePx: Int) {
        // Values are read before the Paint builder: inside `Paint.apply`, `style` would resolve to
        // Paint.style, not to this compositor's IconStyle.
        val shadowColor = style.shadow
        val blurRadius = (sizePx * style.shadowRadiusFraction).coerceAtLeast(1f)
        val offsetY = sizePx * style.shadowOffsetFraction
        if (shadowColor.alpha <= 0f) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shadowColor.toArgb()
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.save()
        canvas.translate(0f, offsetY)
        canvas.drawPath(squirclePath(size, inset = shadowRoom).asAndroidPath(), paint)
        canvas.restore()
    }

    private fun drawTile(canvas: Canvas, path: android.graphics.Path, tile: Color) {
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tile.toArgb() })
    }

    /** Top-left specular, fading out by [IconStyle.glossStop]. */
    private fun drawGloss(canvas: Canvas, path: android.graphics.Path, sizePx: Int) {
        val glossColor = style.gloss
        val stopY = sizePx * style.glossStop
        if (glossColor.alpha <= 0f) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // Diagonal, from the top-left corner: a vertical ramp reads as a gradient fill,
            // while a diagonal one reads as light falling on glass. Same cost.
            shader = LinearGradient(
                0f, 0f, stopY * 0.85f, stopY,
                intArrayOf(glossColor.toArgb(), Color.Transparent.toArgb()),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawPaint(paint)
        canvas.restore()
    }

    private fun drawRim(canvas: Canvas, path: android.graphics.Path, sizePx: Int) {
        val rimColor = style.rim
        val rimWidth = (sizePx * style.rimWidthFraction).coerceAtLeast(1f)
        if (rimColor.alpha <= 0f) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = rimWidth
            color = rimColor.toArgb()
        }
        canvas.drawPath(path, paint)
    }

    /**
     * The glyph. An adaptive icon contributes its foreground layer only — its background was
     * already consumed to colour the tile, and drawing it too would defeat the whole treatment.
     */
    private fun drawGlyph(
        canvas: Canvas,
        source: Drawable,
        adaptive: AdaptiveIconDrawable?,
        sizePx: Int,
        tilePath: android.graphics.Path,
    ) {
        val glyph = adaptive?.foreground ?: source
        val inset = if (adaptive != null) style.adaptiveInset else style.legacyInset
        val bounds = glyphBounds(sizePx, inset)

        glyph.setBounds(bounds[0], bounds[1], bounds[2], bounds[3])

        canvas.save()
        // Clip to the tile so an overscaled foreground cannot bleed past the silhouette — the
        // uniform outline is the whole point of the treatment.
        canvas.clipPath(tilePath)
        if (adaptive != null) {
            // Recover the optical size the designer intended, scaling about the tile centre so the
            // glyph stays centred as it grows.
            val scale = style.adaptiveForegroundScale
            canvas.scale(scale, scale, sizePx / 2f, sizePx / 2f)
        }
        runCatching { glyph.draw(canvas) }
        canvas.restore()
    }
}

private fun AdaptiveIconDrawable.foregroundHasContent(sizePx: Int): Boolean = runCatching {
    hasVisibleContent(foreground.toSampleBitmap(sizePx))
}.getOrDefault(false)

private fun Drawable.toSampleBitmap(sizePx: Int): Bitmap {
    val edge = sizePx.coerceAtMost(48).coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888)
    setBounds(0, 0, edge, edge)
    draw(Canvas(bitmap))
    return bitmap
}

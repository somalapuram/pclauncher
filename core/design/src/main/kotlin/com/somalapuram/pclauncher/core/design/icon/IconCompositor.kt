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
        // Room for the shadow *and* the glow: both are drawn outside the tile, and a halo clipped
        // by the bitmap edge reads as a band rather than as light.
        val shadowRoom = sizePx * (
            style.shadowRadiusFraction + style.shadowOffsetFraction + style.glowRadiusFraction
        )
        val tilePath = squirclePath(size, inset = shadowRoom).asAndroidPath()

        drawShadow(canvas, size, shadowRoom, sizePx)
        drawGlow(canvas, tilePath, appColor, sizePx)
        drawTile(canvas, tilePath, tile)
        drawGloss(canvas, tilePath, sizePx)
        drawSpecular(canvas, tilePath, sizePx)
        drawRim(canvas, tilePath, appColor, sizePx)
        drawGlyph(canvas, source, adaptive, sizePx, tilePath)
        // After the glyph, deliberately. Glass covers what is under it — painted before, these two
        // survive only in the margin around the artwork, which is why tiles with two highlight
        // layers still measured flat (icon-gloss.md).
        drawGlaze(canvas, tilePath, sizePx)
        drawInnerShade(canvas, tilePath, sizePx)

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

    /**
     * The app's colour bleeding out from under the tile.
     *
     * Drawn *before* the tile so only the halo escapes — this is what makes a near-black tile feel
     * lit from within rather than merely dark, and it is the single biggest difference between the
     * reference set and a flat masked icon.
     */
    private fun drawGlow(
        canvas: Canvas,
        path: android.graphics.Path,
        appColor: Color,
        sizePx: Int,
    ) {
        val alpha = style.glowAlpha
        val radius = (sizePx * style.glowRadiusFraction).coerceAtLeast(1f)
        if (alpha <= 0f) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = appColor.copy(alpha = alpha).toArgb()
            maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, paint)
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

    /**
     * A tight bright band just inside the top edge.
     *
     * The soft gloss alone reads as a tinted fill. Adding a narrow, much brighter band is what the
     * eye interprets as a curved glossy surface catching a light — it is the "wet" look in the
     * reference tiles, and it costs one more gradient at bake time.
     */
    private fun drawSpecular(canvas: Canvas, path: android.graphics.Path, sizePx: Int) {
        val specularColor = style.specular
        val stopY = sizePx * style.specularStop
        if (specularColor.alpha <= 0f) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, stopY,
                intArrayOf(specularColor.toArgb(), Color.Transparent.toArgb()),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawPaint(paint)
        canvas.restore()
    }

    /**
     * The highlight that sits *above* the artwork.
     *
     * Same shape as the gloss and drawn from the same top-left direction, so the tile reads as one
     * lit surface rather than two competing gradients.
     */
    private fun drawGlaze(canvas: Canvas, path: android.graphics.Path, sizePx: Int) {
        val glazeColor = style.glaze
        val stop = sizePx * style.glazeStop
        if (glazeColor.alpha <= 0f) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, stop * 0.85f, stop,
                intArrayOf(glazeColor.toArgb(), Color.Transparent.toArgb()),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawPaint(paint)
        canvas.restore()
    }

    /**
     * The underside, inside the shape.
     *
     * Runs upward from the bottom edge, so the gradient's opaque end is where the tile turns away
     * from the light. This is the difference between a lit edge and a body.
     */
    private fun drawInnerShade(canvas: Canvas, path: android.graphics.Path, sizePx: Int) {
        val shadeColor = style.innerShade
        val stop = sizePx * style.innerShadeStop
        if (shadeColor.alpha <= 0f) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, sizePx.toFloat(), 0f, sizePx - stop,
                intArrayOf(shadeColor.toArgb(), Color.Transparent.toArgb()),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawPaint(paint)
        canvas.restore()
    }

    /**
     * The lit edge — white along the top, the app's own colour along the bottom.
     *
     * A uniform white rim looks like a border. Letting the lower half pick up the app's colour is
     * bounce light: it ties the rim to the glow underneath and is why the reference tiles read as
     * objects rather than as stickers.
     */
    private fun drawRim(
        canvas: Canvas,
        path: android.graphics.Path,
        appColor: Color,
        sizePx: Int,
    ) {
        val rimColor = style.rim
        val rimWidth = (sizePx * style.rimWidthFraction).coerceAtLeast(1f)
        if (rimColor.alpha <= 0f) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = rimWidth
            shader = LinearGradient(
                0f, 0f, 0f, sizePx.toFloat(),
                intArrayOf(
                    rimColor.toArgb(),
                    Color.Transparent.toArgb(),
                    appColor.copy(alpha = rimColor.alpha * 0.9f).toArgb(),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
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

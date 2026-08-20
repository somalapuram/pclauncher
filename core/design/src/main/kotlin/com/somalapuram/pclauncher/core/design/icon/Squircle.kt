package com.somalapuram.pclauncher.core.design.icon

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/**
 * Superellipse geometry — |x/a|^n + |y/a|^n = 1.
 *
 * A rounded rectangle joins a straight edge to a circular arc, and the curvature jumps at the
 * seam. A superellipse has continuous curvature the whole way round, which is the difference
 * between the reference tiles and an Android rounded square. n ≈ 4–5 is the range that reads as a
 * squircle; below 3 it goes oval, above 8 it flattens back to a rectangle.
 */
const val SQUIRCLE_EXPONENT: Float = 4.6f

/** Enough segments to be smooth at 192 px and cheap enough to build per icon. */
const val SQUIRCLE_SEGMENTS: Int = 96

/**
 * A point on the unit superellipse at parameter [t] in 0..1.
 *
 * Pure and separately testable — the whole shape is this function sampled, so getting it right is
 * checkable without a `Canvas`.
 */
fun squirclePoint(t: Float, exponent: Float = SQUIRCLE_EXPONENT): Pair<Float, Float> {
    val angle = t * 2f * Math.PI.toFloat()
    val cos = kotlin.math.cos(angle)
    val sin = kotlin.math.sin(angle)
    val exp = 2f / exponent
    val x = sign(cos) * abs(cos).pow(exp)
    val y = sign(sin) * abs(sin).pow(exp)
    return x to y
}

/** The squircle inscribed in [size], optionally inset by [inset] on every edge. */
fun squirclePath(
    size: Size,
    inset: Float = 0f,
    exponent: Float = SQUIRCLE_EXPONENT,
    segments: Int = SQUIRCLE_SEGMENTS,
): Path {
    val halfW = size.width / 2f - inset
    val halfH = size.height / 2f - inset
    val cx = size.width / 2f
    val cy = size.height / 2f

    return Path().apply {
        for (i in 0..segments) {
            val (ux, uy) = squirclePoint(i.toFloat() / segments, exponent)
            val x = cx + ux * halfW
            val y = cy + uy * halfH
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/**
 * The square a glyph occupies inside a tile of [tileSize], given a fractional [inset].
 *
 * Kept as arithmetic rather than folded into the drawing code so the inset tokens can be checked
 * directly — an icon that is 2 px off centre is obvious in a grid and invisible in a diff.
 */
fun glyphBounds(tileSize: Int, inset: Float): IntArray {
    val padding = (tileSize * inset).toInt()
    val edge = tileSize - padding * 2
    return intArrayOf(padding, padding, padding + edge, padding + edge)
}

package com.somalapuram.pclauncher.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Shell glyphs, drawn in code.
 *
 * Vector assets would mean an XML round-trip and a resource id per glyph for shapes this small;
 * building them here keeps them next to the tokens they are drawn with, and lets them be tinted
 * per state without a second asset.
 */
object PcGlyphs {

    /**
     * The Start mark: a rounded triangle, drawn pointing **down**.
     *
     * Was a three-by-three grid of dots, which said "apps" but said nothing about what pressing the
     * button does or which state it is in. A triangle says both: down at rest, and turned up while
     * the menu is open it becomes that menu's own indicator (start-button-mark.md).
     *
     * Rounded by stroking the same path it fills, with round joins — the shell has no other
     * hard-cornered mark, and a bare triangle would be conspicuous among squircles.
     */
    val Start: ImageVector by lazy {
        ImageVector.Builder(
            name = "pc_start",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                // The stroke is what rounds the corners, so the path is drawn inside the intended
                // silhouette and the stroke grows it back out.
                strokeLineWidth = 3.2f,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(6.9f, 8.6f)
                lineTo(17.1f, 8.6f)
                lineTo(12f, 15.8f)
                close()
            }
        }.build()
    }

    /**
     * A gear.
     *
     * Teeth are placed by trigonometry rather than by a rotated path: the vector builder has no
     * transform, and eight hand-written quads would drift where the eye notices it most — on the
     * one glyph the user already knows the shape of.
     */
    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "pc_settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            val cx = 12f
            val cy = 12f
            val teeth = 8
            val toothHalfWidth = 1.5f
            val inner = 6.2f
            val outer = 9.4f

            repeat(teeth) { index ->
                val angle = index * (2.0 * Math.PI / teeth)
                val nx = kotlin.math.cos(angle).toFloat()
                val ny = kotlin.math.sin(angle).toFloat()
                // Perpendicular, to give the tooth its width.
                val px = -ny * toothHalfWidth
                val py = nx * toothHalfWidth
                path(fill = SolidColor(Color.White)) {
                    moveTo(cx + nx * inner + px, cy + ny * inner + py)
                    lineTo(cx + nx * outer + px, cy + ny * outer + py)
                    lineTo(cx + nx * outer - px, cy + ny * outer - py)
                    lineTo(cx + nx * inner - px, cy + ny * inner - py)
                    close()
                }
            }

            // The body, with the hub punched out of it.
            path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
                moveTo(cx - 7.2f, cy)
                arcToRelative(7.2f, 7.2f, 0f, true, true, 14.4f, 0f)
                arcToRelative(7.2f, 7.2f, 0f, true, true, -14.4f, 0f)
                close()
                moveTo(cx - 3.1f, cy)
                arcToRelative(3.1f, 3.1f, 0f, true, false, 6.2f, 0f)
                arcToRelative(3.1f, 3.1f, 0f, true, false, -6.2f, 0f)
                close()
            }
        }.build()
    }

    /** The power mark: a broken ring with a stem through the gap. */
    val Power: ImageVector by lazy {
        ImageVector.Builder(
            name = "pc_power",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
            ) {
                // Open at the top, which is what distinguishes it from a plain circle.
                moveTo(6.6f, 7.2f)
                arcToRelative(7.6f, 7.6f, 0f, true, false, 10.8f, 0f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 3.4f)
                lineTo(12f, 11.4f)
            }
        }.build()
    }
}
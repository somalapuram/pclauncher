package com.somalapuram.pclauncher.feature.shell.tray

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * The tray's glyphs, drawn rather than imported.
 *
 * Four icons do not justify a vector library, and these four are *stateful* — Wi-Fi has a signal,
 * battery has a fill and a bolt, volume has a loudness. Drawing them puts the state in the geometry
 * instead of in a table of near-identical assets, and flat paths stay crisp under a software
 * renderer (SRS §4.3) where a scaled bitmap would not.
 */
private val GlyphSize = 16.dp

@Composable
fun WifiGlyph(bars: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(GlyphSize)) { drawWifi(bars, color) }
}

@Composable
fun BluetoothGlyph(on: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(GlyphSize)) { drawBluetooth(on, color) }
}

@Composable
fun BatteryGlyph(fill: Float, charging: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(GlyphSize)) { drawBattery(fill, charging, color) }
}

@Composable
fun VolumeGlyphIcon(glyph: VolumeGlyph, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(GlyphSize)) { drawVolume(glyph, color) }
}

/** Three arcs from a common origin. Unfilled arcs stay drawn, faintly, so the icon keeps its shape. */
private fun DrawScope.drawWifi(bars: Int, color: Color) {
    val origin = Offset(size.width / 2f, size.height * 0.82f)
    val stroke = Stroke(width = size.minDimension * 0.10f)

    repeat(3) { index ->
        val radius = size.minDimension * (0.22f + 0.19f * index)
        val arc = Rect(center = origin, radius = radius)
        drawArc(
            color = if (index < bars) color else color.copy(alpha = 0.25f),
            startAngle = 215f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = arc.topLeft,
            size = arc.size,
            style = stroke,
        )
    }
    drawCircle(color = if (bars > 0) color else color.copy(alpha = 0.25f), radius = size.minDimension * 0.06f, center = origin)
}

/** The Bluetooth rune: two triangles meeting a vertical spine. Off draws it muted with a slash. */
private fun DrawScope.drawBluetooth(on: Boolean, color: Color) {
    val ink = if (on) color else color.copy(alpha = 0.30f)
    val stroke = Stroke(width = size.minDimension * 0.10f)
    val w = size.width
    val h = size.height

    val path = Path().apply {
        moveTo(w * 0.32f, h * 0.32f)
        lineTo(w * 0.68f, h * 0.68f)
        lineTo(w * 0.50f, h * 0.86f)
        lineTo(w * 0.50f, h * 0.14f)
        lineTo(w * 0.68f, h * 0.32f)
        lineTo(w * 0.32f, h * 0.68f)
    }
    drawPath(path, ink, style = stroke)
}

/** A rounded cell with a nub, filled left to right, and a bolt cut across it while charging. */
private fun DrawScope.drawBattery(fill: Float, charging: Boolean, color: Color) {
    val w = size.width
    val h = size.height
    val bodyWidth = w * 0.78f
    val bodyHeight = h * 0.46f
    val top = (h - bodyHeight) / 2f
    val radius = CornerRadius(h * 0.10f)
    val stroke = Stroke(width = size.minDimension * 0.08f)

    drawRoundRect(
        color = color,
        topLeft = Offset(0f, top),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = radius,
        style = stroke,
    )
    // The terminal nub, so the shape reads as a battery at 16 dp rather than as a rounded box.
    drawRoundRect(
        color = color,
        topLeft = Offset(bodyWidth + w * 0.04f, top + bodyHeight * 0.28f),
        size = Size(w * 0.08f, bodyHeight * 0.44f),
        cornerRadius = CornerRadius(h * 0.04f),
    )

    val inset = size.minDimension * 0.10f
    val trackWidth = bodyWidth - inset * 2f
    if (fill > 0f && trackWidth > 0f) {
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, top + inset),
            size = Size(trackWidth * fill.coerceIn(0f, 1f), bodyHeight - inset * 2f),
            cornerRadius = CornerRadius(h * 0.05f),
        )
    }

    if (charging) {
        val bolt = Path().apply {
            moveTo(w * 0.44f, top - h * 0.04f)
            lineTo(w * 0.26f, h * 0.56f)
            lineTo(w * 0.39f, h * 0.56f)
            lineTo(w * 0.33f, top + bodyHeight + h * 0.06f)
            lineTo(w * 0.52f, h * 0.44f)
            lineTo(w * 0.39f, h * 0.44f)
            close()
        }
        // Punched out of the fill in the background colour would need a layer; a contrasting
        // stroke reads the same at this size and costs nothing on a CPU renderer.
        drawPath(bolt, color, style = Stroke(width = size.minDimension * 0.07f))
    }
}

/** A speaker cone, plus as many waves as the level earns. Muted gets a slash instead. */
private fun DrawScope.drawVolume(glyph: VolumeGlyph, color: Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = size.minDimension * 0.09f)

    val cone = Path().apply {
        moveTo(w * 0.06f, h * 0.38f)
        lineTo(w * 0.20f, h * 0.38f)
        lineTo(w * 0.38f, h * 0.20f)
        lineTo(w * 0.38f, h * 0.80f)
        lineTo(w * 0.20f, h * 0.62f)
        lineTo(w * 0.06f, h * 0.62f)
        close()
    }
    drawPath(cone, color)

    val waves = when (glyph) {
        VolumeGlyph.Muted -> 0
        VolumeGlyph.Low -> 1
        VolumeGlyph.Medium -> 2
        VolumeGlyph.High -> 3
    }
    repeat(waves) { index ->
        val radius = w * (0.16f + 0.13f * index)
        val arc = Rect(center = Offset(w * 0.40f, h / 2f), radius = radius)
        drawArc(
            color = color,
            startAngle = -50f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = arc.topLeft,
            size = arc.size,
            style = stroke,
        )
    }

    if (glyph == VolumeGlyph.Muted) {
        drawLine(
            color = color,
            start = Offset(w * 0.52f, h * 0.34f),
            end = Offset(w * 0.86f, h * 0.66f),
            strokeWidth = size.minDimension * 0.09f,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.86f, h * 0.34f),
            end = Offset(w * 0.52f, h * 0.66f),
            strokeWidth = size.minDimension * 0.09f,
        )
    }
}

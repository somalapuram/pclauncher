package com.somalapuram.pclauncher.core.design

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Where a rounded corner begins and ends on the two edges meeting at [vertex].
 *
 * Rounding a polygon means walking back from each corner along both of its edges by the same
 * distance and curving between those points. The distance is clamped to half of the shorter edge,
 * because a radius larger than that would consume the neighbouring corner's own rounding and the
 * shape would fold through itself.
 *
 * Pulled out because it is the only part of drawing a rounded triangle that can be got wrong
 * silently — a bad path renders as *something*, and it takes measuring to see it is not the shape
 * that was asked for.
 */
fun cornerTangents(
    previous: Offset,
    vertex: Offset,
    next: Offset,
    radius: Float,
): Pair<Offset, Offset> {
    val toPrevious = previous - vertex
    val toNext = next - vertex
    val previousLength = toPrevious.length()
    val nextLength = toNext.length()
    if (previousLength == 0f || nextLength == 0f) return vertex to vertex

    val limit = min(previousLength, nextLength) / 2f
    val walk = min(radius, limit)

    return Offset(
        vertex.x + toPrevious.x / previousLength * walk,
        vertex.y + toPrevious.y / previousLength * walk,
    ) to Offset(
        vertex.x + toNext.x / nextLength * walk,
        vertex.y + toNext.y / nextLength * walk,
    )
}

private fun Offset.length() = sqrt(x * x + y * y)

/**
 * A triangle with rounded corners, pointing down, filling the space it is given.
 *
 * A `Shape` rather than a glyph, so the *button* is the triangle — its background, its border and
 * its press states all take this outline, instead of a triangular picture sitting inside a rounded
 * square (start-button-mark.md).
 */
class RoundedTriangleShape(private val corner: Dp) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = with(density) { corner.toPx() }
        val apex = Offset(size.width / 2f, size.height)
        val left = Offset(0f, 0f)
        val right = Offset(size.width, 0f)

        return Outline.Generic(roundedTrianglePath(left, right, apex, radius))
    }
}

/** The path itself: walk the corners in order, curving through each vertex. */
fun roundedTrianglePath(a: Offset, b: Offset, c: Offset, radius: Float): Path {
    val path = Path()
    val corners = listOf(Triple(c, a, b), Triple(a, b, c), Triple(b, c, a))

    corners.forEachIndexed { index, (previous, vertex, next) ->
        val (start, end) = cornerTangents(previous, vertex, next, radius)
        if (index == 0) path.moveTo(start.x, start.y) else path.lineTo(start.x, start.y)
        // The vertex is the control point, which is what makes the curve hug the corner it
        // replaces rather than cutting a chord across it.
        path.quadraticTo(vertex.x, vertex.y, end.x, end.y)
    }
    path.close()
    return path
}

package com.somalapuram.pclauncher.core.data.layout

/** Which edge is being dragged. */
enum class ResizeEdge { Left, Right, Top, Bottom }

/**
 * What a provider allows, mirroring `AppWidgetProviderInfo.resizeMode`.
 *
 * Kept as our own type so `core:data` stays free of framework classes and the rules can be tested
 * without a device — the mapping from the framework flags happens where the provider is read.
 */
data class ResizePermission(
    val horizontal: Boolean,
    val vertical: Boolean,
    val minColumns: Int = 1,
    val minRows: Int = 1,
) {
    val isResizable: Boolean get() = horizontal || vertical

    companion object {
        val None = ResizePermission(horizontal = false, vertical = false)
    }
}

/** Whether an edge can be dragged at all. A widget that forbids an axis offers no handle on it. */
fun canDrag(edge: ResizeEdge, permission: ResizePermission): Boolean = when (edge) {
    ResizeEdge.Left, ResizeEdge.Right -> permission.horizontal
    ResizeEdge.Top, ResizeEdge.Bottom -> permission.vertical
}

/**
 * The placement after dragging [edge] by [deltaCells].
 *
 * Returns null when the resize is refused — a forbidden axis, a span below the provider's minimum
 * once clamped, a rectangle off the grid, or one overlapping something else. Refusing leaves the
 * widget exactly as it was, which is the same answer dropping an icon on an occupied cell gives.
 *
 * Dragging a left or top edge moves the origin as well as the span, which is why this returns a
 * whole placement rather than just a size.
 */
fun resizedBy(
    layout: DesktopLayout,
    id: String,
    edge: ResizeEdge,
    deltaCells: Int,
    permission: ResizePermission,
    columnsAvailable: Int,
    rowsAvailable: Int,
): DesktopLayout? {
    if (!canDrag(edge, permission)) return null

    val current = layout.placementFor(id) ?: return null
    if (deltaCells == 0) return layout

    var column = current.cell.column
    var row = current.cell.row
    var columns = current.span.columns
    var rows = current.span.rows

    when (edge) {
        ResizeEdge.Right -> columns += deltaCells
        ResizeEdge.Bottom -> rows += deltaCells
        // Dragging the near edge grows away from the origin, so the origin moves with it.
        ResizeEdge.Left -> { column -= deltaCells; columns += deltaCells }
        ResizeEdge.Top -> { row -= deltaCells; rows += deltaCells }
    }

    if (columns < permission.minColumns || rows < permission.minRows) return null
    if (column < 0 || row < 0) return null
    if (columnsAvailable in 1..(column + columns - 1)) return null
    if (rowsAvailable in 1..(row + rows - 1)) return null

    val candidate = DesktopPlacement(id, DesktopCell(column, row), DesktopSpan(columns, rows))
    val clash = layout.placements.any { it.id != id && it.overlaps(candidate) }
    if (clash) return null

    return DesktopLayout(layout.placements.filterNot { it.id == id } + candidate)
}

/**
 * How many whole cells a drag of [pixels] represents.
 *
 * Rounded rather than truncated so a handle dragged most of the way to the next cell snaps to it —
 * truncating makes a resize feel like it is lagging behind the finger.
 */
fun cellsDragged(pixels: Float, cellSize: Float): Int {
    if (cellSize <= 0f) return 0
    return Math.round(pixels / cellSize)
}

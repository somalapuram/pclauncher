package com.somalapuram.pclauncher.core.data.layout

import kotlin.math.roundToInt

/**
 * Where a dragged placement lands.
 *
 * The corner snaps, not the cell under the pointer: with a 3×1 widget the finger may be anywhere
 * along it, and snapping what is under the pointer would move the widget by an amount that depends
 * on where the user happened to grab it (widget-drag.md requirement 4).
 *
 * Clamped so no part of the rectangle leaves the grid — a widget half off the edge is unreachable,
 * and refusing the whole drop for a drag that went a little too far would be worse than landing it
 * against the edge.
 */
fun cellAfterDrag(
    from: DesktopCell,
    span: DesktopSpan,
    deltaX: Float,
    deltaY: Float,
    cellWidth: Float,
    cellHeight: Float,
    columnsAvailable: Int,
    rowsAvailable: Int,
): DesktopCell {
    if (cellWidth <= 0f || cellHeight <= 0f) return from

    val column = from.column + (deltaX / cellWidth).roundToInt()
    val row = from.row + (deltaY / cellHeight).roundToInt()

    // A grid narrower than the widget would give an empty range; the widget stays where the
    // arithmetic put it rather than being coerced into a nonsense cell.
    val lastColumn = (columnsAvailable - span.columns).coerceAtLeast(0)
    val lastRow = (rowsAvailable - span.rows).coerceAtLeast(0)

    return DesktopCell(
        column = column.coerceIn(0, lastColumn),
        row = row.coerceIn(0, lastRow),
    )
}

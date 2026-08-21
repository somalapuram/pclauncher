package com.somalapuram.pclauncher.feature.shell.start

/** Arrow-key movement within a grid. */
enum class GridMove { Left, Right, Up, Down }

/**
 * Where the selection lands after an arrow key.
 *
 * Pure, because the arithmetic is the whole feature and every interesting case is an edge: the
 * first cell, the last, a partial final row. Inlining this into a key handler is how grids end up
 * skipping cells at the seams.
 *
 * Movement **clamps** rather than wraps. Wrapping from the end of one row to the start of the next
 * reads as the selection teleporting across the screen, and loses the user's place in a way that
 * is hard to recover from without looking.
 */
fun moveInGrid(
    current: Int,
    move: GridMove,
    itemCount: Int,
    columns: Int,
): Int {
    if (itemCount <= 0 || columns <= 0) return 0

    val index = current.coerceIn(0, itemCount - 1)
    val column = index % columns

    return when (move) {
        // Stop at the row's edges: left from the first column stays put rather than jumping to the
        // end of the row above.
        GridMove.Left -> if (column == 0) index else index - 1
        GridMove.Right -> if (column == columns - 1 || index == itemCount - 1) index else index + 1

        GridMove.Up -> if (index < columns) index else index - columns

        // Down from the last full row lands on the final item when the row below is partial —
        // otherwise the last few apps would be unreachable by keyboard.
        GridMove.Down -> when {
            index + columns < itemCount -> index + columns
            index == itemCount - 1 -> index
            // Below the current cell is past the end, but there *is* a partial row: land on it.
            index / columns < (itemCount - 1) / columns -> itemCount - 1
            else -> index
        }
    }
}

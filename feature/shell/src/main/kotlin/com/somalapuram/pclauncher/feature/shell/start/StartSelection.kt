package com.somalapuram.pclauncher.feature.shell.start

/**
 * Where the keyboard caret is, or null for "the user has not used the keyboard".
 *
 * The distinction is the point. An `Int` starting at zero cannot express "no selection", so the
 * menu opened with its first app highlighted — a claim about where the keyboard would act, made
 * before any key had been pressed, on a shell where the pointer is the likelier input.
 */
typealias Selection = Int?

/**
 * The selection after a navigation key.
 *
 * The first press *selects* rather than moves: coming from nothing, "down" means "start here", not
 * "start at the first row and then go down from it".
 */
fun selectionAfterMove(
    current: Selection,
    move: GridMove,
    count: Int,
    columns: Int,
): Selection {
    if (count <= 0) return null
    if (current == null) return 0
    return moveInGrid(current, move, count, columns)
}

/**
 * The selection after the query changes.
 *
 * Typing is the user engaging the keyboard, and Enter has to launch something — so the top hit is
 * selected. Clearing the query returns the menu to the state it opened in.
 */
fun selectionAfterQuery(query: String, count: Int): Selection =
    if (query.isBlank() || count <= 0) null else 0

/** Keep a selection on a row that still exists after filtering, without inventing one. */
fun selectionAfterFilter(current: Selection, count: Int): Selection {
    if (current == null || count <= 0) return null
    return current.coerceIn(0, count - 1)
}

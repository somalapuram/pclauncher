package com.somalapuram.pclauncher.feature.shell.start

import com.somalapuram.pclauncher.core.apps.AppEntry

/**
 * The Start menu's two sections as one keyboard-navigable list.
 *
 * The menu draws a Recent row above the All-apps grid, and SRS §12 requires the whole shell to be
 * operable from the keyboard — so the caret has to cross between them. Rather than give the row its
 * own selection state and a second set of edge cases, both sections are flattened into one list
 * that `moveInGrid` already knows how to walk.
 *
 * The flattening is only sound because the Recent row is padded to a full row of [columns]. Without
 * the padding, three recent apps would put All-apps' first entry at index 3 — the same grid row as
 * the recent ones, though it is drawn on the row below — and Right from the last recent app would
 * jump the caret across the menu (recent-apps.md requirement 7).
 */
data class StartSections(
    val recent: List<AppEntry>,
    val all: List<AppEntry>,
    val columns: Int,
) {
    /** Recent, padded to a whole row, then everything else. Nulls are the empty slots. */
    val navigable: List<AppEntry?> =
        if (recent.isEmpty()) all else recent + List(columns - recent.size.coerceAtMost(columns)) { null } + all

    /** How many slots the recent row occupies, padding included. Zero when the row is not drawn. */
    val recentSlots: Int = if (recent.isEmpty()) 0 else columns

    fun entryAt(index: Int?): AppEntry? = index?.let { navigable.getOrNull(it) }

    /**
     * Where the All-apps grid should scroll for a selection, or null when the caret is on the
     * Recent row — which is always in view and must not drag the grid around under it.
     */
    fun gridIndexFor(index: Int?): Int? {
        if (index == null) return null
        val inGrid = index - recentSlots
        return inGrid.takeIf { it >= 0 && it < all.size }
    }
}

/**
 * The Recent row for a given query.
 *
 * Empty while the user is searching: they are looking at results, and a row of apps unrelated to
 * what they typed sitting above those results is noise (recent-apps.md requirement 3). A named
 * function rather than an inline conditional so the rule is testable without a device.
 */
fun recentFor(recent: List<AppEntry>, query: String): List<AppEntry> =
    if (query.isBlank()) recent else emptyList()

/**
 * A move that refuses to land on an empty padding slot.
 *
 * Left and Right stop, because an empty slot is the end of the drawn row and `moveInGrid` already
 * treats a row edge as somewhere to stay. Up and Down keep going, because the slot is a hole in the
 * middle of the path rather than its end — stopping there would strand the caret on nothing.
 */
fun selectionAfterMoveInSections(
    current: Selection,
    move: GridMove,
    sections: StartSections,
): Selection {
    val entries = sections.navigable
    val next = selectionAfterMove(current, move, entries.size, sections.columns) ?: return null
    if (entries.getOrNull(next) != null) return next

    return when (move) {
        GridMove.Left, GridMove.Right -> current
        GridMove.Up, GridMove.Down -> {
            val step = if (move == GridMove.Down) sections.columns else -sections.columns
            var probe = next + step
            while (probe in entries.indices) {
                if (entries[probe] != null) return probe
                probe += step
            }
            current
        }
    }
}

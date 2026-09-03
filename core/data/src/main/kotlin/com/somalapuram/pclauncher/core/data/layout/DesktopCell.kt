package com.somalapuram.pclauncher.core.data.layout

/** A cell on the desktop grid. Column first, because the desktop fills column-major. */
data class DesktopCell(val column: Int, val row: Int)

/** How many cells wide and tall something is. Icons are always 1×1; widgets are not. */
data class DesktopSpan(val columns: Int = 1, val rows: Int = 1) {
    companion object {
        val Single = DesktopSpan(1, 1)
    }
}

/** Where one item sits, and how much room it takes. */
data class DesktopPlacement(
    val id: String,
    val cell: DesktopCell,
    val span: DesktopSpan = DesktopSpan.Single,
) {
    /**
     * Does this overlap [other]?
     *
     * With spans, "is this cell taken" becomes "do these rectangles intersect". Icons are 1×1, so
     * their behaviour is unchanged — but the test has to live in one place or icons and widgets
     * will eventually disagree about what is free.
     */
    fun overlaps(other: DesktopPlacement): Boolean =
        cell.column < other.cell.column + other.span.columns &&
            other.cell.column < cell.column + span.columns &&
            cell.row < other.cell.row + other.span.rows &&
            other.cell.row < cell.row + span.rows

    fun covers(target: DesktopCell): Boolean =
        target.column >= cell.column && target.column < cell.column + span.columns &&
            target.row >= cell.row && target.row < cell.row + span.rows
}

/**
 * The arrangement.
 *
 * Position is *data* here, not a consequence of sort order — that is the whole difference between
 * a desktop and a list. An item with no placement is not an error; it simply has not been arranged
 * yet and gets auto-placed.
 */
data class DesktopLayout(val placements: List<DesktopPlacement> = emptyList()) {

    private val byId = placements.associateBy { it.id }

    fun cellFor(id: String): DesktopCell? = byId[id]?.cell

    fun spanFor(id: String): DesktopSpan? = byId[id]?.span

    fun placementFor(id: String): DesktopPlacement? = byId[id]

    fun isOccupied(cell: DesktopCell): Boolean = placements.any { it.covers(cell) }

    /** Who covers this cell, if anyone. */
    fun idAt(cell: DesktopCell): String? = placements.firstOrNull { it.covers(cell) }?.id

    /**
     * Move [id] to [cell], keeping its span.
     *
     * Refuses rather than overwrites when the target overlaps someone else: silently destroying a
     * placement the user made is worse than declining the drop (icon-grid.md requirement 4).
     * Returns null when refused, so the caller can put the item back.
     */
    fun moved(id: String, cell: DesktopCell): DesktopLayout? {
        val existing = byId[id]
        val span = existing?.span ?: DesktopSpan.Single
        return placedAt(id, cell, span)
    }

    /** Resize [id] in place. Refused, like a move, when the new rectangle would overlap. */
    fun resized(id: String, span: DesktopSpan): DesktopLayout? {
        val existing = byId[id] ?: return null
        if (span.columns < 1 || span.rows < 1) return null
        if (existing.span == span) return this
        return placedAt(id, existing.cell, span)
    }

    private fun placedAt(id: String, cell: DesktopCell, span: DesktopSpan): DesktopLayout? {
        val candidate = DesktopPlacement(id, cell, span)
        val clash = placements.any { it.id != id && it.overlaps(candidate) }
        if (clash) return null
        if (byId[id] == candidate) return this
        return DesktopLayout(placements.filterNot { it.id == id } + candidate)
    }

    fun without(id: String): DesktopLayout =
        DesktopLayout(placements.filterNot { it.id == id })
}

/**
 * The first free cell, scanning column-major.
 *
 * Deliberately the same order the old flowing grid used, so introducing free placement does not
 * scramble an arrangement the user has already got used to — and so a newly installed app lands
 * somewhere that looks deliberate rather than at the end of everything.
 */
fun firstFreeCell(layout: DesktopLayout, rowsPerColumn: Int): DesktopCell {
    if (rowsPerColumn <= 0) return DesktopCell(0, 0)
    var column = 0
    while (true) {
        for (row in 0 until rowsPerColumn) {
            val candidate = DesktopCell(column, row)
            if (!layout.isOccupied(candidate)) return candidate
        }
        column++
    }
}

/**
 * Auto-place everything without a placement, in the order given.
 *
 * Existing placements are never disturbed — arranging one icon must not move the others.
 */
fun withAutoPlacement(
    layout: DesktopLayout,
    ids: List<String>,
    rowsPerColumn: Int,
): DesktopLayout {
    // An unmeasured grid has no row count, and "one row" is not a safe stand-in — it is a
    // horizontal arrangement the user can see before the real one replaces it
    // (placement-timing.md).
    if (rowsPerColumn <= 0) return layout

    var result = layout
    for (id in ids) {
        if (result.cellFor(id) != null) continue
        result = DesktopLayout(
            result.placements + DesktopPlacement(id, firstFreeCell(result, rowsPerColumn)),
        )
    }
    return result
}

/** Pointer position → the cell it is over. Pure so every boundary case is checkable. */
fun cellAt(
    x: Float,
    y: Float,
    cellWidth: Float,
    cellHeight: Float,
    rowsPerColumn: Int,
): DesktopCell? {
    if (cellWidth <= 0f || cellHeight <= 0f || rowsPerColumn <= 0) return null
    if (x < 0f || y < 0f) return null

    val column = (x / cellWidth).toInt()
    val row = (y / cellHeight).toInt()
    // Below the last row is not "the last row" — it is off the grid, and pretending otherwise
    // makes a drop near the taskbar land in the bottom cell by surprise.
    if (row >= rowsPerColumn) return null
    return DesktopCell(column, row)
}

/**
 * How far to push whole rows down so they sit centred in [availableHeight].
 *
 * Rows are whole and the height almost never divides exactly. Left alone the remainder falls
 * entirely below the last row, which reads as a grid pushed against the top edge rather than one
 * placed in its area (grid-bounds.md).
 *
 * Returns zero for a degenerate size, and for a height that cannot hold even one row — there is
 * nothing to centre, and inventing an offset there would push the empty grid off its own origin.
 */
fun rowCentringOffset(availableHeight: Float, cellHeight: Float): Float {
    if (availableHeight <= 0f || cellHeight <= 0f) return 0f
    val rows = (availableHeight / cellHeight).toInt()
    if (rows <= 0) return 0f
    return (availableHeight - rows * cellHeight) / 2f
}

/**
 * Encodes a layout for storage; skips malformed entries on read rather than losing the desktop.
 *
 * Written as `id|col|row|spanCols|spanRows`. Three-field lines predate spans and decode as 1×1 —
 * dropping them would silently clear a desktop that someone had arranged.
 */
object DesktopLayoutCodec {

    fun encode(layout: DesktopLayout): String = layout.placements.joinToString("\n") {
        "${it.id}|${it.cell.column}|${it.cell.row}|${it.span.columns}|${it.span.rows}"
    }

    fun decode(raw: String?): DesktopLayout {
        if (raw.isNullOrBlank()) return DesktopLayout()
        val placements = raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size != 3 && parts.size != 5) return@mapNotNull null

            val id = parts[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val column = parts[1].toIntOrNull() ?: return@mapNotNull null
            val row = parts[2].toIntOrNull() ?: return@mapNotNull null
            if (column < 0 || row < 0) return@mapNotNull null

            val span = if (parts.size == 5) {
                val cols = parts[3].toIntOrNull() ?: return@mapNotNull null
                val rows = parts[4].toIntOrNull() ?: return@mapNotNull null
                if (cols < 1 || rows < 1) return@mapNotNull null
                DesktopSpan(cols, rows)
            } else {
                DesktopSpan.Single
            }

            DesktopPlacement(id, DesktopCell(column, row), span)
        }

        // Two entries claiming the same space would make occupancy ambiguous; the first wins.
        val kept = mutableListOf<DesktopPlacement>()
        for (placement in placements) {
            if (kept.none { it.overlaps(placement) }) kept += placement
        }
        return DesktopLayout(kept)
    }
}

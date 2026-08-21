package com.somalapuram.pclauncher.core.data.layout

/** A cell on the desktop grid. Column first, because the desktop fills column-major. */
data class DesktopCell(val column: Int, val row: Int)

/** Where one item sits. The id is a flattened component, as everywhere else. */
data class DesktopPlacement(val id: String, val cell: DesktopCell)

/**
 * The arrangement.
 *
 * Position is *data* here, not a consequence of sort order — that is the whole difference between
 * a desktop and a list. An item with no placement is not an error; it simply has not been arranged
 * yet and gets auto-placed.
 */
data class DesktopLayout(val placements: List<DesktopPlacement> = emptyList()) {

    private val byId = placements.associateBy { it.id }
    private val occupied = placements.map { it.cell }.toSet()

    fun cellFor(id: String): DesktopCell? = byId[id]?.cell

    fun isOccupied(cell: DesktopCell): Boolean = cell in occupied

    /** Who is in this cell, if anyone. */
    fun idAt(cell: DesktopCell): String? = placements.firstOrNull { it.cell == cell }?.id

    /**
     * Move [id] to [cell].
     *
     * Refuses rather than overwrites when the target is taken by someone else: silently destroying
     * a placement the user made is worse than declining the drop (icon-grid.md requirement 4).
     * Returns null when the move is refused, so the caller can put the icon back.
     */
    fun moved(id: String, cell: DesktopCell): DesktopLayout? {
        val current = byId[id]
        if (current?.cell == cell) return this
        val sittingThere = idAt(cell)
        if (sittingThere != null && sittingThere != id) return null

        return DesktopLayout(placements.filterNot { it.id == id } + DesktopPlacement(id, cell))
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
    var result = layout
    for (id in ids) {
        if (result.cellFor(id) != null) continue
        result = DesktopLayout(result.placements + DesktopPlacement(id, firstFreeCell(result, rowsPerColumn)))
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

/** Encodes a layout for storage; skips malformed entries on read rather than losing the desktop. */
object DesktopLayoutCodec {

    fun encode(layout: DesktopLayout): String =
        layout.placements.joinToString("\n") { "${it.id}|${it.cell.column}|${it.cell.row}" }

    fun decode(raw: String?): DesktopLayout {
        if (raw.isNullOrBlank()) return DesktopLayout()
        val placements = raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size != 3) return@mapNotNull null
            val id = parts[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val column = parts[1].toIntOrNull() ?: return@mapNotNull null
            val row = parts[2].toIntOrNull() ?: return@mapNotNull null
            if (column < 0 || row < 0) return@mapNotNull null
            DesktopPlacement(id, DesktopCell(column, row))
        }
        // Two entries claiming one cell would make occupancy ambiguous; the first wins.
        val seen = mutableSetOf<DesktopCell>()
        return DesktopLayout(placements.filter { seen.add(it.cell) })
    }
}

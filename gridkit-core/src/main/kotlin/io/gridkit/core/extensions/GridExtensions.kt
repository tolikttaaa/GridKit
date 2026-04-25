package io.gridkit.core.extensions

import io.gridkit.core.core.*

/**
 * Flood-fills from [start] via BFS, returning all coordinates reachable through
 * cells that satisfy [predicate].
 */
fun <C : GridCoordinate, Dir : GridDirection, D> Grid<C, Dir, D>.flood(
    start: C,
    predicate: (Cell<C, D>) -> Boolean = { it.state != CellState.Blocked }
): Set<C> {
    val startCell = getCell(start) ?: return emptySet()
    if (!predicate(startCell)) return emptySet()

    val visited = mutableSetOf(start)
    val queue = ArrayDeque<C>()
    queue.add(start)

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        for (neighbor in getNeighbors(current)) {
            if (neighbor.coordinate !in visited && predicate(neighbor)) {
                visited.add(neighbor.coordinate)
                queue.add(neighbor.coordinate)
            }
        }
    }
    return visited
}

/**
 * Returns true when every non-[CellState.Blocked] cell is reachable from
 * every other non-[CellState.Blocked] cell (the traversable sub-graph is
 * fully connected).
 */
fun <C : GridCoordinate, Dir : GridDirection, D> Grid<C, Dir, D>.isConnected(): Boolean {
    val passable = cells.values.filter { it.state != CellState.Blocked }
    if (passable.isEmpty()) return true
    return flood(passable.first().coordinate).size == passable.size
}

/**
 * Returns a human-readable ASCII map of the grid.
 * Square and hex grids render as 2-D maps; other topologies list coordinate→state pairs.
 */
fun <C : GridCoordinate, Dir : GridDirection, D> Grid<C, Dir, D>.toAsciiMap(): String {
    if (cells.isEmpty()) return "(empty grid)"

    fun CellState?.glyph() = when (this) {
        CellState.Empty    -> "."
        CellState.Occupied -> "O"
        CellState.Blocked  -> "#"
        null               -> " "
    }

    val sample = cells.keys.first()
    return when (sample) {
        is SquareCoordinate -> {
            @Suppress("UNCHECKED_CAST")
            val sq = cells as Map<SquareCoordinate, Cell<SquareCoordinate, D>>
            val cols = sq.keys.map { it.col }
            val rows = sq.keys.map { it.row }
            buildString {
                for (row in rows.min()..rows.max()) {
                    for (col in cols.min()..cols.max()) {
                        append(sq[SquareCoordinate(col, row)]?.state.glyph())
                    }
                    append('\n')
                }
            }.trimEnd()
        }

        is HexCoordinate -> {
            @Suppress("UNCHECKED_CAST")
            val hex = cells as Map<HexCoordinate, Cell<HexCoordinate, D>>
            val rows = hex.keys.map { it.row }
            val cols = hex.keys.map { it.col }
            buildString {
                for (row in rows.min()..rows.max()) {
                    if (row % 2 != 0) append(" ")
                    for (col in cols.min()..cols.max()) {
                        append(hex[HexCoordinate(row, col)]?.state.glyph())
                        append(' ')
                    }
                    append('\n')
                }
            }.trimEnd()
        }

        else -> buildString {
            cells.entries
                .sortedWith(compareBy { it.key.toString() })
                .forEach { (coord, cell) -> appendLine("$coord → ${cell.state}") }
        }.trimEnd()
    }
}

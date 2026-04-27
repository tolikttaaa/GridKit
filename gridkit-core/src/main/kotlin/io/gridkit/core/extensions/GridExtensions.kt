package io.gridkit.core.extensions

import io.gridkit.core.core.*

/**
 * Flood-fills from [start] via BFS, returning all coordinates reachable through
 * cells that satisfy [predicate].
 */
fun <C : GridCoordinate, Dir : GridDirection, D> Grid<C, Dir, D>.flood(
    start: C,
    predicate: (Cell<C, D>) -> Boolean = { true }
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
 * Returns true when every cell is reachable from every other cell.
 */
fun <C : GridCoordinate, Dir : GridDirection, D> Grid<C, Dir, D>.isConnected(): Boolean {
    if (cells.isEmpty()) return true
    return flood(cells.values.first().coordinate).size == cells.size
}

/**
 * Returns a human-readable ASCII map of the grid.
 *
 * - **Square** grids render as a compact 2-D character grid.
 * - **Hex** grids render with the odd-row half-step offset.
 * - **Triangle** grids render each row as a sequence of `/\` and `\/` glyphs.
 * - Other topologies list `coordinate → data` pairs.
 */
fun <C : GridCoordinate, Dir : GridDirection, D> Grid<C, Dir, D>.toAsciiMap(): String {
    if (cells.isEmpty()) return "(empty grid)"

    fun Cell<*, *>?.glyph() = this?.data?.toString()?.firstOrNull()?.toString() ?: "."

    return when (cells.keys.first()) {
        is SquareCoordinate -> {
            @Suppress("UNCHECKED_CAST")
            val sq = cells as Map<SquareCoordinate, Cell<SquareCoordinate, D>>
            val cols = sq.keys.map { it.col }
            val rows = sq.keys.map { it.row }
            buildString {
                for (row in rows.min()..rows.max()) {
                    for (col in cols.min()..cols.max())
                        append(sq[SquareCoordinate(col, row)].glyph())
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
                        append(hex[HexCoordinate(row, col)].glyph())
                        append(' ')
                    }
                    append('\n')
                }
            }.trimEnd()
        }

        is TriangleCoordinate -> {
            @Suppress("UNCHECKED_CAST")
            val tri = cells as Map<TriangleCoordinate, Cell<TriangleCoordinate, D>>
            val cols = tri.keys.map { it.col }
            val rows = tri.keys.map { it.row }
            buildString {
                for (row in rows.min()..rows.max()) {
                    for (col in cols.min()..cols.max()) {
                        val cell = tri[TriangleCoordinate(col, row)]
                        val isUp = col % 2 == 0
                        append(cell?.data?.toString()?.firstOrNull() ?: if (isUp) '/' else '\\')
                    }
                    append('\n')
                }
            }.trimEnd()
        }

        else -> buildString {
            cells.entries.sortedWith(compareBy { it.key.toString() })
                .forEach { (coord, cell) -> appendLine("$coord -> ${cell.data}") }
        }.trimEnd()
    }
}

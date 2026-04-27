package io.gridkit.core.dsl

import io.gridkit.core.core.*
import io.gridkit.core.grid.HexGrid
import io.gridkit.core.grid.SquareGrid
import io.gridkit.core.grid.TriangleGrid

// ── Square DSL ────────────────────────────────────────────────────────────────

/** DSL scope for configuring a [SquareGrid]. */
class SquareGridBuilder<D>(private val grid: SquareGrid<D>) {
    /** Places a cell at [coordinate] with the given [data] payload. */
    fun place(coordinate: SquareCoordinate, data: D? = null) {
        grid.setCell(coordinate, Cell(coordinate, data))
    }
}

/**
 * Creates a [SquareGrid] and applies the [init] DSL.
 *
 * Use the type parameter [D] to attach per-cell data; omit it (or use [Nothing])
 * when you don't need cell payloads.
 *
 * ```kotlin
 * val board = squareGrid<String>(width = 10, height = 10) {
 *     place(SquareCoordinate(3, 3), data = "treasure")
 * }
 * ```
 */
fun <D> squareGrid(
    width: Int,
    height: Int,
    diagonal: Boolean = false,
    init: SquareGridBuilder<D>.() -> Unit = {}
): SquareGrid<D> {
    val grid = SquareGrid<D>(width, height, diagonal)
    SquareGridBuilder(grid).init()
    return grid
}

// ── Hex DSL ───────────────────────────────────────────────────────────────────

/** DSL scope for configuring a [HexGrid]. */
class HexGridBuilder<D>(private val grid: HexGrid<D>) {
    /** Places a cell at [coordinate] with the given [data] payload. */
    fun place(coordinate: HexCoordinate, data: D? = null) {
        grid.setCell(coordinate, Cell(coordinate, data))
    }
}

/**
 * Creates a [HexGrid] and applies the [init] DSL.
 *
 * ```kotlin
 * val board = hexGrid<String>(rows = 5, cols = 6) {
 *     place(HexCoordinate(row = 1, col = 2), data = "forest")
 * }
 * ```
 */
fun <D> hexGrid(
    rows: Int,
    cols: Int,
    init: HexGridBuilder<D>.() -> Unit = {}
): HexGrid<D> {
    val grid = HexGrid<D>(rows, cols)
    HexGridBuilder(grid).init()
    return grid
}

// ── Triangle DSL ──────────────────────────────────────────────────────────────

/** DSL scope for configuring a [TriangleGrid]. */
class TriangleGridBuilder<D>(private val grid: TriangleGrid<D>) {
    /** Places a cell at [coordinate] with the given [data] payload. */
    fun place(coordinate: TriangleCoordinate, data: D? = null) {
        grid.setCell(coordinate, Cell(coordinate, data))
    }
}

/**
 * Creates a [TriangleGrid] and applies the [init] DSL.
 *
 * ```kotlin
 * val board = triangleGrid<Nothing>(width = 8, height = 4) {}
 * ```
 */
fun <D> triangleGrid(
    cols: Int,
    rows: Int,
    init: TriangleGridBuilder<D>.() -> Unit = {}
): TriangleGrid<D> {
    val grid = TriangleGrid<D>(cols, rows)
    TriangleGridBuilder(grid).init()
    return grid
}

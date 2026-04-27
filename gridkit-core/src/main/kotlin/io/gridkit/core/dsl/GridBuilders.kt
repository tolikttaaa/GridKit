package io.gridkit.core.dsl

import io.gridkit.core.core.*
import io.gridkit.core.grid.HexGrid
import io.gridkit.core.grid.SquareGrid
import io.gridkit.core.grid.TriangleGrid

// ── Square DSL ────────────────────────────────────────────────────────────────

/** DSL scope for configuring a [SquareGrid]. */
class SquareGridBuilder<D>(private val grid: SquareGrid<D>) {
    /** Marks [coordinate] as [CellState.Blocked]. */
    fun block(coordinate: SquareCoordinate) {
        grid.setCell(coordinate, Cell(coordinate, CellState.Blocked))
    }

    /** Marks [coordinate] as [CellState.Occupied]. */
    fun occupy(coordinate: SquareCoordinate) {
        grid.setCell(coordinate, Cell(coordinate, CellState.Occupied))
    }

    /** Places a cell at [coordinate] with the given [data] payload. */
    fun place(coordinate: SquareCoordinate, state: CellState = CellState.Empty, data: D? = null) {
        grid.setCell(coordinate, Cell(coordinate, state, data))
    }
}

/**
 * Creates a [SquareGrid] and applies the [init] DSL.
 *
 * Use the type parameter [D] to attach per-cell data; omit it (or use [Nothing])
 * when you don't need cell payloads.
 *
 * ```kotlin
 * val board = squareGrid<Nothing>(width = 10, height = 10) {
 *     block(SquareCoordinate(3, 3))
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
    /** Marks [coordinate] as [CellState.Blocked]. */
    fun block(coordinate: HexCoordinate) {
        grid.setCell(coordinate, Cell(coordinate, CellState.Blocked))
    }

    /** Marks [coordinate] as [CellState.Occupied]. */
    fun occupy(coordinate: HexCoordinate) {
        grid.setCell(coordinate, Cell(coordinate, CellState.Occupied))
    }

    /** Places a cell at [coordinate] with the given [data] payload. */
    fun place(coordinate: HexCoordinate, state: CellState = CellState.Empty, data: D? = null) {
        grid.setCell(coordinate, Cell(coordinate, state, data))
    }
}

/**
 * Creates a [HexGrid] and applies the [init] DSL.
 *
 * ```kotlin
 * val board = hexGrid<Nothing>(rows = 5, cols = 6) {
 *     block(HexCoordinate(row = 1, col = 2))
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
    /** Marks [coordinate] as [CellState.Blocked]. */
    fun block(coordinate: TriangleCoordinate) {
        grid.setCell(coordinate, Cell(coordinate, CellState.Blocked))
    }

    /** Marks [coordinate] as [CellState.Occupied]. */
    fun occupy(coordinate: TriangleCoordinate) {
        grid.setCell(coordinate, Cell(coordinate, CellState.Occupied))
    }

    /** Places a cell at [coordinate] with the given [data] payload. */
    fun place(coordinate: TriangleCoordinate, state: CellState = CellState.Empty, data: D? = null) {
        grid.setCell(coordinate, Cell(coordinate, state, data))
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

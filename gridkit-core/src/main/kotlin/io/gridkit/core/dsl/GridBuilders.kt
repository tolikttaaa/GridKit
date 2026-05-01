package io.gridkit.core.dsl

import io.gridkit.core.core.*
import io.gridkit.core.direction.*
import io.gridkit.core.grid.*

// ── Square DSL ────────────────────────────────────────────────────────────────

/** DSL scope for configuring a [SquareGrid]. */
class SquareGridBuilder<D>(private val grid: SquareGrid<D>) {
    /** Places (or updates) a cell at [coordinate] with the given [data] payload. */
    fun place(coordinate: SquareCoordinate, data: D? = null) = grid.setCell(coordinate, data)

    /**
     * Places the next cell adjacent to [from] in [direction].
     * Idempotent — returns the existing cell if the target already exists.
     */
    fun placeNext(from: SquareCoordinate, direction: SquareDirection) = grid.placeNext(from, direction)
}

/**
 * Creates a filled rectangular [SquareGrid] and applies the [init] DSL.
 *
 * ```kotlin
 * val board = squareGrid<String>(width = 5, height = 5) {
 *     place(SquareCoordinate(2, 2), data = "center")
 * }
 * ```
 */
fun <D> squareGrid(
    width: Int,
    height: Int,
    diagonal: Boolean = false,
    init: SquareGridBuilder<D>.() -> Unit = {}
): SquareGrid<D> = SquareGrid<D>(width, height, diagonal).also { SquareGridBuilder(it).init() }

/**
 * Creates a sparse [SquareGrid] (no initial cells) and applies the [init] DSL.
 *
 * ```kotlin
 * val board = squareGrid<String> {
 *     place(SquareCoordinate(0, 0), data = "start")
 *     placeNext(SquareCoordinate(0, 0), SquareEdgeDirection.RIGHT)
 * }
 * ```
 */
fun <D> squareGrid(
    diagonal: Boolean = false,
    init: SquareGridBuilder<D>.() -> Unit
): SquareGrid<D> = SquareGrid<D>(0, 0, diagonal).also { SquareGridBuilder(it).init() }

// ── Hex DSL ───────────────────────────────────────────────────────────────────

/** DSL scope for configuring a [HexGrid]. */
class HexGridBuilder<D>(private val grid: HexGrid<D>) {
    /** Places (or updates) a cell at [coordinate] with the given [data] payload. */
    fun place(coordinate: HexCoordinate, data: D? = null) = grid.setCell(coordinate, data)

    /** Places the next cell adjacent to [from] in [direction]. Idempotent. */
    fun placeNext(from: HexCoordinate, direction: HexEdgeDirection) = grid.placeNext(from, direction)
}

/**
 * Creates a filled rectangular [HexGrid] and applies the [init] DSL.
 *
 * ```kotlin
 * val board = hexGrid<String>(rows = 4, cols = 5) {
 *     place(HexCoordinate(row = 1, col = 2), data = "forest")
 * }
 * ```
 */
fun <D> hexGrid(
    rows: Int,
    cols: Int,
    init: HexGridBuilder<D>.() -> Unit = {}
): HexGrid<D> = HexGrid<D>(rows, cols).also { HexGridBuilder(it).init() }

/**
 * Creates a sparse [HexGrid] (no initial cells) and applies the [init] DSL.
 */
fun <D> hexGrid(init: HexGridBuilder<D>.() -> Unit): HexGrid<D> =
    HexGrid<D>(0, 0).also { HexGridBuilder(it).init() }

// ── Triangle DSL ──────────────────────────────────────────────────────────────

/** DSL scope for configuring a [TriangleGrid]. */
class TriangleGridBuilder<D>(private val grid: TriangleGrid<D>) {
    /** Places (or updates) a cell at [coordinate] with the given [data] payload. */
    fun place(coordinate: TriangleCoordinate, data: D? = null) = grid.setCell(coordinate, data)

    /** Places the next cell adjacent to [from] in [direction]. Idempotent. */
    fun placeNext(from: TriangleCoordinate, direction: TriangleEdgeDirection) = grid.placeNext(from, direction)
}

/**
 * Creates a filled [TriangleGrid] and applies the [init] DSL.
 *
 * ```kotlin
 * val board = triangleGrid<Nothing>(cols = 8, rows = 4)
 * ```
 */
fun <D> triangleGrid(
    cols: Int,
    rows: Int,
    init: TriangleGridBuilder<D>.() -> Unit = {}
): TriangleGrid<D> = TriangleGrid<D>(cols, rows).also { TriangleGridBuilder(it).init() }

/**
 * Creates a sparse [TriangleGrid] (no initial cells) and applies the [init] DSL.
 */
fun <D> triangleGrid(init: TriangleGridBuilder<D>.() -> Unit): TriangleGrid<D> =
    TriangleGrid<D>(0, 0).also { TriangleGridBuilder(it).init() }

// ── Diamond DSL ─────────────────────────────────────────────────────────────────

/** DSL scope for configuring a [DiamondGrid]. */
class DiamondGridBuilder<D>(private val grid: DiamondGrid<D>) {
    /** Places (or updates) a cell at [coordinate] with the given [data] payload. */
    fun place(coordinate: DiamondCoordinate, data: D? = null) = grid.setCell(coordinate, data)

    /** Places the next cell adjacent to [from] in [direction]. Idempotent. */
    fun placeNext(from: DiamondCoordinate, direction: DiamondEdgeDirection) = grid.placeNext(from, direction)
}

/**
 * Creates a filled [DiamondGrid] and applies the [init] DSL.
 *
 * ```kotlin
 * val board = diamondGrid<Nothing>(rows = 4, cols = 4)
 * ```
 */
fun <D> diamondGrid(
    rows: Int,
    cols: Int,
    init: DiamondGridBuilder<D>.() -> Unit = {}
): DiamondGrid<D> = DiamondGrid<D>(rows, cols).also { DiamondGridBuilder(it).init() }

/**
 * Creates a sparse [DiamondGrid] (no initial cells) and applies the [init] DSL.
 */
fun <D> diamondGrid(init: DiamondGridBuilder<D>.() -> Unit): DiamondGrid<D> =
    DiamondGrid<D>(0, 0).also { DiamondGridBuilder(it).init() }

package io.gridkit.core.core

/** Marker interface for all grid coordinate types. */
interface GridCoordinate

/** Coordinate on a square grid. */
data class SquareCoordinate(val col: Int, val row: Int) : GridCoordinate

/**
 * Coordinate on a hexagonal grid using offset (even-r / odd-r) coordinates.
 * Row increases downward, col increases rightward.
 * Even rows are shifted left; odd rows are shifted right by half a hex width.
 */
data class HexCoordinate(val row: Int, val col: Int) : GridCoordinate

/** Direction of a triangle cell within its row/col slot. */
enum class TriangleDirection { UP, DOWN }

/** Coordinate on a triangular grid. */
data class TriangleCoordinate(
    val col: Int,
    val row: Int,
    val pointing: TriangleDirection
) : GridCoordinate

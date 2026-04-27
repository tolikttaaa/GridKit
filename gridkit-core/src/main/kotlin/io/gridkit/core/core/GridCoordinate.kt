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

/**
 * Coordinate on a triangular grid.
 *
 * The pointing direction is encoded in [col] parity:
 * - **even col** → UP-pointing triangle (apex at top)
 * - **odd col**  → DOWN-pointing triangle (apex at bottom)
 *
 * Adjacent columns in the same row share a diagonal edge; the VERTICAL neighbor
 * lives in the row above (for UP) or below (for DOWN).
 */
data class TriangleCoordinate(val col: Int, val row: Int) : GridCoordinate

/** Returns true when this triangle is UP-pointing (even [TriangleCoordinate.col]). */
val TriangleCoordinate.isUp: Boolean get() = col % 2 == 0

/** Returns true when this triangle is DOWN-pointing (odd [TriangleCoordinate.col]). */
val TriangleCoordinate.isDown: Boolean get() = col % 2 != 0

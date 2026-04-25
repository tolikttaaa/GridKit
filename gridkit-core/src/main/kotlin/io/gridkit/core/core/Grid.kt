package io.gridkit.core.core

/**
 * Core abstraction for a tile-based game board.
 *
 * All topology-specific grids (square, hex, triangle) implement this interface,
 * enabling topology-agnostic game logic.
 *
 * @param C the coordinate type — must implement [GridCoordinate]
 * @param Dir the direction type — must implement [GridDirection]
 * @param D the optional cell-data payload type
 */
interface Grid<C : GridCoordinate, Dir : GridDirection, D> {

    /** All cells in the grid, keyed by their coordinate. */
    val cells: Map<C, Cell<C, D>>

    /** Returns the cell at [coordinate], or null if it does not exist. */
    fun getCell(coordinate: C): Cell<C, D>?

    /**
     * Returns all cells directly adjacent to [coordinate] as an unordered list.
     * Cells at the edge of the grid have fewer neighbors than interior cells.
     */
    fun getNeighbors(coordinate: C): List<Cell<C, D>>

    /**
     * Returns all existing neighbors keyed by their named direction.
     * Directions whose target coordinate does not exist are absent from the map.
     */
    fun getDirectedNeighbors(coordinate: C): Map<Dir, Cell<C, D>>

    /**
     * Returns the coordinate of the neighbor in [direction] from [coordinate],
     * or null when the target cell does not exist in the grid.
     */
    fun getNeighbor(coordinate: C, direction: Dir): C?

    /** Returns true when [coordinate] falls within the current grid. */
    fun isValidCoordinate(coordinate: C): Boolean

    /**
     * Finds the shortest path from [from] to [to] using the A* algorithm.
     *
     * @param passable decides whether a cell may be traversed; defaults to all
     *   non-[CellState.Blocked] cells being passable
     * @return ordered list of coordinates (including both endpoints),
     *   or null when no path exists
     */
    fun findPath(
        from: C,
        to: C,
        passable: (Cell<C, D>) -> Boolean = { it.state != CellState.Blocked }
    ): List<C>?

    /**
     * Returns all cells within [radius] steps of [center],
     * including [center] itself (radius 0).
     */
    fun getRange(center: C, radius: Int): List<Cell<C, D>>

    /**
     * Returns all cells forming the shortest straight line from [from] to [to],
     * both endpoints inclusive.
     */
    fun getLine(from: C, to: C): List<Cell<C, D>>

    /**
     * Returns all cells exactly [radius] steps away from [center]
     * (i.e. the perimeter of the range disc, excluding the interior).
     */
    fun getRing(center: C, radius: Int): List<Cell<C, D>>

    /** Returns the grid-distance (in steps) between [from] and [to]. */
    fun distance(from: C, to: C): Int

    /**
     * Places a new cell adjacent to [from] in the given [direction], or returns
     * the existing cell if the target coordinate is already present (idempotent).
     *
     * Expands the grid's bounding box when the new coordinate lies outside it.
     */
    fun placeNext(from: C, direction: Dir): Cell<C, D>

    /**
     * Returns the coordinate closest to the geometric centre of the
     * current bounding box (floor-based for even-sized grids).
     */
    fun arithmeticCenter(): C

    /**
     * Computes the physical centre of mass of all non-[CellState.Blocked] cells
     * and returns both the raw physical position and the nearest grid coordinate
     * as a single [GridCenter] value.
     */
    fun physicalCenter(): GridCenter<C>
}

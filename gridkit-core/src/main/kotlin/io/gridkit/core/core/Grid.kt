package io.gridkit.core.core

import io.gridkit.core.topology.Edge
import io.gridkit.core.topology.Vertex

/**
 * Core abstraction for a tile-based game board.
 *
 * All topology-specific grids (square, hex, triangle, diamond) implement this
 * interface, enabling topology-agnostic game logic.
 *
 * @param C the coordinate type — must implement [GridCoordinate]
 * @param Dir the direction type — must implement [GridDirection]
 * @param D the optional cell-data payload type
 * @param CellT the concrete [Cell] implementation stored by this grid
 * @param EdgeT the concrete [Edge] implementation used by this grid
 * @param VertexT the concrete [Vertex] implementation used by this grid
 */
interface Grid<
    C : GridCoordinate,
    Dir : GridDirection,
    D,
    CellT : Cell<C, Dir, D>,
    EdgeT : Edge<D>,
    VertexT : Vertex<D>
> {

    /** All cells in the grid, keyed by their coordinate. */
    val cells: Map<C, CellT>

    /** All shared edges currently referenced by this grid's cells. */
    val edges: Set<EdgeT>
        get() {
            // Grid implementations guarantee their cells are wired with the matching EdgeT type.
            @Suppress("UNCHECKED_CAST")
            return cells.values.flatMap { it.edges.values }.map { it as EdgeT }.toSet()
        }

    /** All shared vertices currently referenced by this grid's cells. */
    val vertices: Set<VertexT>
        get() {
            // Grid implementations guarantee their cells are wired with the matching VertexT type.
            @Suppress("UNCHECKED_CAST")
            return cells.values.flatMap { it.vertices.values }.map { it as VertexT }.toSet()
        }

    /** Returns the cell at [coordinate], or null if it does not exist. */
    fun getCell(coordinate: C): CellT?

    /**
     * Returns all cells directly adjacent to [coordinate] as an unordered list.
     * Cells at the edge of the grid have fewer neighbors than interior cells.
     */
    fun getNeighbors(coordinate: C): List<CellT>

    /**
     * Returns all existing neighbors keyed by their named direction.
     * Directions whose target coordinate does not exist are absent from the map.
     */
    fun getDirectedNeighbors(coordinate: C): Map<Dir, CellT>

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
     *   cells being passable
     * @return ordered list of coordinates (including both endpoints),
     *   or null when no path exists
     */
    fun findPath(
        from: C,
        to: C,
        passable: (CellT) -> Boolean = { true }
    ): List<C>?

    /**
     * Returns all cells within [radius] steps of [center],
     * including [center] itself (radius 0).
     */
    fun getRange(center: C, radius: Int): List<CellT>

    /**
     * Returns all cells forming the shortest straight line from [from] to [to],
     * both endpoints inclusive.
     */
    fun getLine(from: C, to: C): List<CellT>

    /**
     * Returns all cells exactly [radius] steps away from [center]
     * (i.e. the perimeter of the range disc, excluding the interior).
     */
    fun getRing(center: C, radius: Int): List<CellT>

    /** Returns the grid-distance (in steps) between [from] and [to]. */
    fun distance(from: C, to: C): Int

    /**
     * Places a new cell adjacent to [from] in the given [direction], or returns
     * the existing cell if the target coordinate is already present (idempotent).
     *
     * Correctly links shared [io.gridkit.core.topology.Vertex] and
     * [io.gridkit.core.topology.Edge] objects with all existing neighbors.
     * Expands the grid's bounding box when the new coordinate lies outside it.
     */
    fun placeNext(from: C, direction: Dir): CellT

    /**
     * Returns the coordinate closest to the geometric centre of the
     * current bounding box (floor-based for even-sized grids).
     */
    fun arithmeticCenter(): C

    /**
     * Computes the physical centre of mass of all cells and returns both the
     * raw physical position and the nearest grid coordinate as a single
     * [GridCenter] value.
     */
    fun physicalCenter(): GridCenter<C>

    /**
     * Returns an ASCII-art rendering of this grid with centered cell labels.
     *
     * Cells render [Cell.data] with `toString()`, or an empty label when the
     * data is null.
     *
     * @return box-art style ASCII representation for this topology
     */
    fun toAsciiString(): String
}

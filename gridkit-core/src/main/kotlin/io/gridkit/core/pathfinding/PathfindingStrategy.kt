package io.gridkit.core.pathfinding

import io.gridkit.core.core.Cell
import io.gridkit.core.core.Grid
import io.gridkit.core.core.GridCoordinate
import io.gridkit.core.core.GridDirection
import io.gridkit.core.topology.Edge
import io.gridkit.core.topology.Vertex

/**
 * Strategy interface for grid pathfinding algorithms.
 *
 * Implementations receive the grid and a passability predicate so they can be
 * swapped without touching grid or game-logic code.
 *
 * @param C the coordinate type used by the grid
 * @param Dir the direction type used by the grid
 * @param D the optional cell-data payload type
 * @param CellT the concrete [Cell] type exposed by the grid
 * @param EdgeT the concrete [Edge] type used by the grid
 * @param VertexT the concrete [Vertex] type used by the grid
 */
interface PathfindingStrategy<
    C : GridCoordinate,
    Dir : GridDirection,
    D,
    CellT : Cell<C, Dir, D>,
    EdgeT : Edge<D>,
    VertexT : Vertex<D>
> {
    /**
     * Finds a path from [from] to [to] on [grid].
     *
     * @param passable returns true for cells that may be traversed
     * @return ordered list of coordinates including both endpoints,
     *   or null if no path exists
     */
    fun findPath(
        grid: Grid<C, Dir, D, CellT, EdgeT, VertexT>,
        from: C,
        to: C,
        passable: (CellT) -> Boolean
    ): List<C>?
}

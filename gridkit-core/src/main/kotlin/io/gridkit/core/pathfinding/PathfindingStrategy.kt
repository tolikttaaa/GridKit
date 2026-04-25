package io.gridkit.core.pathfinding

import io.gridkit.core.core.Cell
import io.gridkit.core.core.Grid
import io.gridkit.core.core.GridCoordinate
import io.gridkit.core.core.GridDirection

/**
 * Strategy interface for grid pathfinding algorithms.
 *
 * Implementations receive the grid and a passability predicate so they can be
 * swapped without touching grid or game-logic code.
 */
interface PathfindingStrategy<C : GridCoordinate, Dir : GridDirection, D> {
    /**
     * Finds a path from [from] to [to] on [grid].
     *
     * @param passable returns true for cells that may be traversed
     * @return ordered list of coordinates including both endpoints,
     *   or null if no path exists
     */
    fun findPath(
        grid: Grid<C, Dir, D>,
        from: C,
        to: C,
        passable: (Cell<C, D>) -> Boolean
    ): List<C>?
}

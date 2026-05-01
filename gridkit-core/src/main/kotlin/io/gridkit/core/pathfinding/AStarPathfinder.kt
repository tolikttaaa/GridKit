package io.gridkit.core.pathfinding

import io.gridkit.core.core.Cell
import io.gridkit.core.core.Grid
import io.gridkit.core.core.GridCoordinate
import io.gridkit.core.core.GridDirection
import io.gridkit.core.topology.Edge
import io.gridkit.core.topology.Vertex
import java.util.PriorityQueue

/**
 * A* pathfinder that works across all grid topologies.
 *
 * Uses the grid's own [Grid.distance] as the admissible heuristic, which is
 * exact (or a lower bound) for all built-in coordinate types.
 *
 * @param C the coordinate type used by the grid
 * @param Dir the direction type used by the grid
 * @param D the optional cell-data payload type
 * @param CellT the concrete [Cell] type exposed by the grid
 * @param EdgeT the concrete [Edge] type used by the grid
 * @param VertexT the concrete [Vertex] type used by the grid
 */
class AStarPathfinder<
    C : GridCoordinate,
    Dir : GridDirection,
    D,
    CellT : Cell<C, Dir, D>,
    EdgeT : Edge<D>,
    VertexT : Vertex<D>
> : PathfindingStrategy<C, Dir, D, CellT, EdgeT, VertexT> {

    override fun findPath(
        grid: Grid<C, Dir, D, CellT, EdgeT, VertexT>,
        from: C,
        to: C,
        passable: (CellT) -> Boolean
    ): List<C>? {
        if (!grid.isValidCoordinate(from) || !grid.isValidCoordinate(to)) return null
        val toCell = grid.getCell(to) ?: return null
        if (!passable(toCell)) return null

        data class Node(val coord: C, val g: Int, val f: Int)

        val open = PriorityQueue<Node>(compareBy { it.f })
        val gScore = mutableMapOf(from to 0)
        val cameFrom = mutableMapOf<C, C>()

        open.add(Node(from, 0, grid.distance(from, to)))

        while (open.isNotEmpty()) {
            val current = open.poll()
            if (current.coord == to) return reconstruct(cameFrom, to)
            val currentG = gScore[current.coord] ?: continue
            for (neighbor in grid.getNeighbors(current.coord)) {
                if (!passable(neighbor) && neighbor.coordinate != to) continue
                val tentative = currentG + 1
                if (tentative < (gScore[neighbor.coordinate] ?: Int.MAX_VALUE)) {
                    gScore[neighbor.coordinate] = tentative
                    cameFrom[neighbor.coordinate] = current.coord
                    open.add(Node(neighbor.coordinate, tentative, tentative + grid.distance(neighbor.coordinate, to)))
                }
            }
        }
        return null
    }

    private fun reconstruct(cameFrom: Map<C, C>, end: C): List<C> {
        val path = mutableListOf(end)
        var cur = end
        while (cur in cameFrom) { cur = cameFrom[cur]!!; path.add(0, cur) }
        return path
    }
}

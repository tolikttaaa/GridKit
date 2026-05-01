package io.gridkit.core.topology

import io.gridkit.core.core.Cell
import io.gridkit.core.core.GridCoordinate
import io.gridkit.core.core.GridDirection

/**
 * A corner point shared by adjacent cells.
 *
 * A single [Vertex] instance is shared (by reference) between all cells that
 * meet at that corner.  Callers may store arbitrary [data] on each vertex.
 * Extend this class when a custom grid implementation needs additional
 * vertex-level state or behavior.
 *
 * @param D the user-defined payload type (same as the owning grid's data type)
 */
open class Vertex<D> {

    /** Optional application-defined payload attached to this corner. */
    var data: D? = null

    private val _cells: MutableSet<Cell<*, *, D>> = mutableSetOf()
    private val _edges: MutableSet<Edge<D>> = mutableSetOf()

    /** All cells that share this vertex. */
    val cells: Set<Cell<*, *, D>> get() = _cells

    /** All edges that connect to this vertex. */
    val edges: Set<Edge<D>> get() = _edges

    /** Registers [cell] as sharing this vertex.  Internal use only. */
    internal fun addCell(cell: Cell<*, *, D>) { _cells.add(cell) }

    /** Registers [edge] as connected to this vertex.  Internal use only. */
    internal fun addEdge(edge: Edge<D>) { _edges.add(edge) }
}

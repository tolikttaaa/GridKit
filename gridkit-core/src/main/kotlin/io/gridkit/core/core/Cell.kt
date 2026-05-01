package io.gridkit.core.core

import io.gridkit.core.topology.Edge
import io.gridkit.core.topology.Vertex

/**
 * A single cell on the grid.
 *
 * Each cell owns maps from topology-specific direction values to the shared
 * [Vertex] and [Edge] objects that form its boundary.  Adjacent cells always
 * hold the **same** [Vertex] and [Edge] instances (referential equality).
 * Extend this class when a custom [Grid] implementation needs additional
 * cell-level state or behavior.
 *
 * @param C the coordinate type of the owning grid
 * @param Dir the direction type used as map keys for vertices and edges
 * @param D the type of optional application-specific payload stored in this cell
 * @property coordinate the position of this cell
 * @property data optional payload; null when not set
 */
open class Cell<C : GridCoordinate, Dir : GridDirection, D>(
    val coordinate: C,
    data: D? = null
) {
    /** Optional application-defined payload; settable by grid internals during DSL updates. */
    var data: D? = data
        internal set
    private val _vertices: MutableMap<Dir, Vertex<D>> = mutableMapOf()
    private val _edges: MutableMap<Dir, Edge<D>> = mutableMapOf()

    /** Direction → corner vertex map; keys are vertex-direction values for this topology. */
    val vertices: Map<Dir, Vertex<D>> get() = _vertices

    /** Direction → edge map; keys are edge-direction values for this topology. */
    val edges: Map<Dir, Edge<D>> get() = _edges

    /** Stores [vertex] at [dir].  Internal use only. */
    internal fun putVertex(dir: Dir, vertex: Vertex<D>) { _vertices[dir] = vertex }

    /** Stores [edge] at [dir].  Internal use only. */
    internal fun putEdge(dir: Dir, edge: Edge<D>) { _edges[dir] = edge }

    /** Returns the vertex at corner [direction], or null if not populated. */
    fun getVertex(direction: Dir): Vertex<D>? = _vertices[direction]

    /** Returns the edge at side [direction], or null if not populated. */
    fun getEdge(direction: Dir): Edge<D>? = _edges[direction]

    /**
     * Returns all existing neighbors keyed by the edge direction they are
     * reached through.
     */
    @Suppress("UNCHECKED_CAST")
    fun getNeighbors(): Map<Dir, Cell<C, Dir, D>> =
        _edges.mapNotNull { (dir, edge) ->
            (edge.getOpposite(this) as? Cell<C, Dir, D>)?.let { dir to it }
        }.toMap()

    /** Returns all existing neighbors as a flat list. */
    fun getNeighborList(): List<Cell<C, Dir, D>> = getNeighbors().values.toList()

    /**
     * Returns the neighbor reachable through the edge at [direction], or null
     * when no edge exists there or when that edge is a border edge.
     */
    @Suppress("UNCHECKED_CAST")
    fun getNeighbor(direction: Dir): Cell<C, Dir, D>? =
        _edges[direction]?.getOpposite(this) as? Cell<C, Dir, D>

    override fun toString(): String = "Cell($coordinate)"
}

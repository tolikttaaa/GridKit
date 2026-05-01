package io.gridkit.core.topology

import io.gridkit.core.core.Cell

/**
 * A non-directional border shared by exactly two cells.
 *
 * An [Edge] separates two grid cells ([cellA] / [cellB]) and connects two
 * [vertices].  A single [Edge] instance is shared (by reference) between the
 * two cells on either side of it.  For border cells one of the two cell slots
 * is null.
 *
 * The edge is **non-directional**: [vertices] is a [Set] (no first/second
 * ordering), and [cellA]/[cellB] labels carry no positional meaning.
 * Extend this class when a custom grid implementation needs additional
 * edge-level state or behavior.
 *
 * @param D the user-defined payload type (same as the owning grid's data type)
 */
open class Edge<D>(vertices: Set<Vertex<D>>) {

    init {
        require(vertices.size == 2) { "An edge must connect exactly 2 vertices, got ${vertices.size}" }
    }

    /** The two corner vertices connected by this edge (unordered). */
    val vertices: Set<Vertex<D>> = vertices

    /** One of the two cells separated by this edge; may be null for border edges. */
    var cellA: Cell<*, *, D>? = null
        internal set

    /** The other cell separated by this edge; may be null for border edges. */
    var cellB: Cell<*, *, D>? = null
        internal set

    /** Both cell slots as a set; either or both may be null. */
    val cells: Set<Cell<*, *, D>?> get() = setOf(cellA, cellB)

    /** True when one cell slot is null (this edge lies on the grid border). */
    val isBorder: Boolean get() = cellA == null || cellB == null

    /**
     * Returns the cell on the opposite side of this edge from [cell].
     *
     * Returns null when [cell] is not part of this edge, or when the opposite
     * slot is empty (border edge).
     */
    fun getOpposite(cell: Cell<*, *, D>): Cell<*, *, D>? = when (cell) {
        cellA -> cellB
        cellB -> cellA
        else -> null
    }

    /** Assigns [cell] to the first available (null) cell slot.  Internal use only. */
    internal fun addCell(cell: Cell<*, *, D>) {
        when {
            cellA == null -> cellA = cell
            cellB == null -> cellB = cell
            else -> error("Edge already has two cells")
        }
    }
}

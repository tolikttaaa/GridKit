package io.gridkit.core.grid

import io.gridkit.core.core.*
import io.gridkit.core.direction.*
import io.gridkit.core.pathfinding.AStarPathfinder
import io.gridkit.core.topology.Edge
import io.gridkit.core.topology.Vertex
import kotlin.math.abs

/**
 * A rectangular square grid.
 *
 * Edges and vertices are shared between adjacent cells — two cells that touch
 * always hold the **same** [Edge] / [Vertex] instance (referential equality).
 *
 * @param D the optional cell-data payload type
 * @property diagonal when true, [getNeighbors] / [getDirectedNeighbors] also
 *   return the four diagonal neighbors (8-connected); vertex/edge topology is
 *   always 4-connected regardless of this flag
 */
class SquareGrid<D>(
    width: Int,
    height: Int,
    val diagonal: Boolean = false
) : Grid<
    SquareCoordinate,
    SquareDirection,
    D,
    Cell<SquareCoordinate, SquareDirection, D>,
    Edge<D>,
    Vertex<D>
> {

    private val _cells: MutableMap<SquareCoordinate, Cell<SquareCoordinate, SquareDirection, D>> = mutableMapOf()
    override val cells: Map<SquareCoordinate, Cell<SquareCoordinate, SquareDirection, D>> get() = _cells

    private var minCol = 0
    private var maxCol = width - 1
    private var minRow = 0
    private var maxRow = height - 1

    private val pathfinder = AStarPathfinder<
        SquareCoordinate,
        SquareDirection,
        D,
        Cell<SquareCoordinate, SquareDirection, D>,
        Edge<D>,
        Vertex<D>
    >()

    /** Physical size of each cell (default 1.0). */
    var cellSize: Double = 1.0

    init {
        for (row in 0 until height) {
            for (col in 0 until width) {
                addCell(SquareCoordinate(col, row))
            }
        }
    }

    // ── topology helpers ──────────────────────────────────────────────────────

    /** Creates a new cell at [coord], wires its shared vertices/edges, and registers it. */
    private fun addCell(coord: SquareCoordinate, data: D? = null): Cell<SquareCoordinate, SquareDirection, D> {
        val cell = Cell<SquareCoordinate, SquareDirection, D>(coord, data)
        _cells[coord] = cell
        buildTopology(cell)
        return cell
    }

    private fun buildTopology(cell: Cell<SquareCoordinate, SquareDirection, D>) {
        val col = cell.coordinate.col
        val row = cell.coordinate.row

        // ── vertices (corners) ────────────────────────────────────────────────
        // Each corner is shared with up to 3 neighbors.  We probe candidates in
        // order and reuse the first existing vertex we find; otherwise create one.
        val tlV = findVertex(col, row, SquareVertexDirection.TOP_LEFT) ?: Vertex()
        val trV = findVertex(col, row, SquareVertexDirection.TOP_RIGHT) ?: Vertex()
        val dlV = findVertex(col, row, SquareVertexDirection.DOWN_LEFT) ?: Vertex()
        val drV = findVertex(col, row, SquareVertexDirection.DOWN_RIGHT) ?: Vertex()

        tlV.addCell(cell); trV.addCell(cell); dlV.addCell(cell); drV.addCell(cell)
        cell.putVertex(SquareVertexDirection.TOP_LEFT,   tlV)
        cell.putVertex(SquareVertexDirection.TOP_RIGHT,  trV)
        cell.putVertex(SquareVertexDirection.DOWN_LEFT,  dlV)
        cell.putVertex(SquareVertexDirection.DOWN_RIGHT, drV)

        // ── edges (sides) ─────────────────────────────────────────────────────
        // Each edge is shared with exactly one neighbor.  If the neighbor already
        // exists, reuse its opposite-direction edge; otherwise create a new one.
        val topEdge = _cells[SquareCoordinate(col, row - 1)]?.getEdge(SquareEdgeDirection.BOTTOM)
            ?: newEdge(tlV, trV)
        topEdge.addCell(cell)
        cell.putEdge(SquareEdgeDirection.TOP, topEdge)

        val bottomEdge = _cells[SquareCoordinate(col, row + 1)]?.getEdge(SquareEdgeDirection.TOP)
            ?: newEdge(dlV, drV)
        bottomEdge.addCell(cell)
        cell.putEdge(SquareEdgeDirection.BOTTOM, bottomEdge)

        val leftEdge = _cells[SquareCoordinate(col - 1, row)]?.getEdge(SquareEdgeDirection.RIGHT)
            ?: newEdge(tlV, dlV)
        leftEdge.addCell(cell)
        cell.putEdge(SquareEdgeDirection.LEFT, leftEdge)

        val rightEdge = _cells[SquareCoordinate(col + 1, row)]?.getEdge(SquareEdgeDirection.LEFT)
            ?: newEdge(trV, drV)
        rightEdge.addCell(cell)
        cell.putEdge(SquareEdgeDirection.RIGHT, rightEdge)
    }

    /**
     * Probes the known neighbor cells that share the vertex at [vertexDir] of
     * coordinate ([col], [row]) and returns the first existing shared vertex.
     */
    private fun findVertex(col: Int, row: Int, vertexDir: SquareVertexDirection): Vertex<D>? =
        when (vertexDir) {
            SquareVertexDirection.TOP_LEFT -> listOf(
                SquareCoordinate(col - 1, row) to SquareVertexDirection.TOP_RIGHT,
                SquareCoordinate(col, row - 1) to SquareVertexDirection.DOWN_LEFT,
                SquareCoordinate(col - 1, row - 1) to SquareVertexDirection.DOWN_RIGHT
            )
            SquareVertexDirection.TOP_RIGHT -> listOf(
                SquareCoordinate(col + 1, row) to SquareVertexDirection.TOP_LEFT,
                SquareCoordinate(col, row - 1) to SquareVertexDirection.DOWN_RIGHT,
                SquareCoordinate(col + 1, row - 1) to SquareVertexDirection.DOWN_LEFT
            )
            SquareVertexDirection.DOWN_LEFT -> listOf(
                SquareCoordinate(col - 1, row) to SquareVertexDirection.DOWN_RIGHT,
                SquareCoordinate(col, row + 1) to SquareVertexDirection.TOP_LEFT,
                SquareCoordinate(col - 1, row + 1) to SquareVertexDirection.TOP_RIGHT
            )
            SquareVertexDirection.DOWN_RIGHT -> listOf(
                SquareCoordinate(col + 1, row) to SquareVertexDirection.DOWN_LEFT,
                SquareCoordinate(col, row + 1) to SquareVertexDirection.TOP_RIGHT,
                SquareCoordinate(col + 1, row + 1) to SquareVertexDirection.TOP_LEFT
            )
        }.firstNotNullOfOrNull { (coord, dir) -> _cells[coord]?.getVertex(dir) }

    /** Creates a new edge, registers it on both vertices, and returns it. */
    private fun newEdge(v1: Vertex<D>, v2: Vertex<D>): Edge<D> =
        Edge(setOf(v1, v2)).also { v1.addEdge(it); v2.addEdge(it) }

    // ── internal helpers used by DSL ──────────────────────────────────────────

    /** Updates the data on the cell at [coordinate]; creates the cell (with full topology) if absent. */
    internal fun setCell(coordinate: SquareCoordinate, data: D?) {
        val existing = _cells[coordinate]
        if (existing != null) {
            existing.data = data
        } else {
            addCell(coordinate, data)
            expandBounds(coordinate)
        }
    }

    private fun expandBounds(c: SquareCoordinate) {
        if (c.col < minCol) minCol = c.col
        if (c.col > maxCol) maxCol = c.col
        if (c.row < minRow) minRow = c.row
        if (c.row > maxRow) maxRow = c.row
    }

    // ── direction helpers ─────────────────────────────────────────────────────

    private val cardinalOffsets = listOf(
        SquareEdgeDirection.TOP    to (0 to -1),
        SquareEdgeDirection.BOTTOM to (0 to  1),
        SquareEdgeDirection.LEFT   to (-1 to 0),
        SquareEdgeDirection.RIGHT  to (1 to  0)
    )

    private val diagonalOffsets = listOf(
        SquareVertexDirection.TOP_LEFT    to (-1 to -1),
        SquareVertexDirection.TOP_RIGHT   to (1 to  -1),
        SquareVertexDirection.DOWN_LEFT   to (-1 to  1),
        SquareVertexDirection.DOWN_RIGHT  to (1 to   1)
    )

    private fun allOffsets(): List<Pair<SquareDirection, Pair<Int, Int>>> =
        if (diagonal) cardinalOffsets + diagonalOffsets else cardinalOffsets

    private fun directionOffset(direction: SquareDirection): Pair<Int, Int> = when (direction) {
        SquareEdgeDirection.TOP          ->  0 to -1
        SquareEdgeDirection.BOTTOM       ->  0 to  1
        SquareEdgeDirection.LEFT         -> -1 to  0
        SquareEdgeDirection.RIGHT        ->  1 to  0
        SquareVertexDirection.TOP_LEFT   -> -1 to -1
        SquareVertexDirection.TOP_RIGHT  ->  1 to -1
        SquareVertexDirection.DOWN_LEFT  -> -1 to  1
        SquareVertexDirection.DOWN_RIGHT ->  1 to  1
    }

    // ── Grid<SquareCoordinate, SquareDirection, D> ────────────────────────────

    override fun getCell(coordinate: SquareCoordinate): Cell<SquareCoordinate, SquareDirection, D>? =
        _cells[coordinate]

    override fun isValidCoordinate(coordinate: SquareCoordinate): Boolean = coordinate in _cells

    override fun getNeighbors(coordinate: SquareCoordinate): List<Cell<SquareCoordinate, SquareDirection, D>> =
        allOffsets().mapNotNull { (_, offset) ->
            _cells[SquareCoordinate(coordinate.col + offset.first, coordinate.row + offset.second)]
        }

    override fun getDirectedNeighbors(coordinate: SquareCoordinate): Map<SquareDirection, Cell<SquareCoordinate, SquareDirection, D>> =
        allOffsets().mapNotNull { (dir, offset) ->
            val nc = SquareCoordinate(coordinate.col + offset.first, coordinate.row + offset.second)
            _cells[nc]?.let { dir to it }
        }.toMap()

    override fun getNeighbor(coordinate: SquareCoordinate, direction: SquareDirection): SquareCoordinate? {
        val (dc, dr) = directionOffset(direction)
        val nc = SquareCoordinate(coordinate.col + dc, coordinate.row + dr)
        return if (nc in _cells) nc else null
    }

    override fun findPath(
        from: SquareCoordinate,
        to: SquareCoordinate,
        passable: (Cell<SquareCoordinate, SquareDirection, D>) -> Boolean
    ): List<SquareCoordinate>? = pathfinder.findPath(this, from, to, passable)

    override fun distance(from: SquareCoordinate, to: SquareCoordinate): Int =
        if (diagonal) maxOf(abs(to.col - from.col), abs(to.row - from.row))
        else abs(to.col - from.col) + abs(to.row - from.row)

    override fun getRange(center: SquareCoordinate, radius: Int): List<Cell<SquareCoordinate, SquareDirection, D>> =
        _cells.values.filter { distance(center, it.coordinate) <= radius }

    override fun getRing(center: SquareCoordinate, radius: Int): List<Cell<SquareCoordinate, SquareDirection, D>> =
        if (radius == 0) listOfNotNull(_cells[center])
        else _cells.values.filter { distance(center, it.coordinate) == radius }

    override fun getLine(from: SquareCoordinate, to: SquareCoordinate): List<Cell<SquareCoordinate, SquareDirection, D>> {
        val dx = to.col - from.col
        val dy = to.row - from.row
        val steps = maxOf(abs(dx), abs(dy))
        if (steps == 0) return listOfNotNull(_cells[from])
        return (0..steps).mapNotNull { i ->
            val col = from.col + (dx * i.toDouble() / steps).toInt()
            val row = from.row + (dy * i.toDouble() / steps).toInt()
            _cells[SquareCoordinate(col, row)]
        }
    }

    override fun placeNext(
        from: SquareCoordinate,
        direction: SquareDirection
    ): Cell<SquareCoordinate, SquareDirection, D> {
        val (dc, dr) = directionOffset(direction)
        val target = SquareCoordinate(from.col + dc, from.row + dr)
        _cells[target]?.let { return it }
        val cell = addCell(target)
        expandBounds(target)
        return cell
    }

    override fun arithmeticCenter(): SquareCoordinate =
        SquareCoordinate((minCol + maxCol).floorDiv(2), (minRow + maxRow).floorDiv(2))

    override fun physicalCenter(): GridCenter<SquareCoordinate> {
        val physical = if (_cells.isEmpty()) PhysicalCenter(0.0, 0.0) else PhysicalCenter(
            _cells.values.sumOf { toPhysical(it.coordinate).x } / _cells.size,
            _cells.values.sumOf { toPhysical(it.coordinate).y } / _cells.size
        )
        return GridCenter(physical, nearestCoordinate(physical))
    }

    /** Converts a coordinate to its physical (x, y) position. */
    fun toPhysical(coordinate: SquareCoordinate): PhysicalCenter =
        PhysicalCenter(coordinate.col * cellSize, coordinate.row * cellSize)

    /** Snaps an arbitrary physical position to the nearest existing coordinate. */
    fun nearestCoordinate(physical: PhysicalCenter): SquareCoordinate =
        _cells.keys.minByOrNull { c ->
            val p = toPhysical(c); val dx = p.x - physical.x; val dy = p.y - physical.y
            dx * dx + dy * dy
        } ?: SquareCoordinate(0, 0)

    /** Returns this square grid as shared-border ASCII box art. */
    override fun toAsciiString(): String {
        if (_cells.isEmpty()) return "(empty grid)"
        val colRange = minCol..maxCol
        val rowRange = minRow..maxRow
        val widths = colRange.associateWith { col ->
            maxOf(1, rowRange.maxOf { row -> _cells[SquareCoordinate(col, row)].asciiLabel().length }) + 4
        }
        fun border() = buildString {
            append('*')
            for (col in colRange) { append("-".repeat(widths.getValue(col))); append('*') }
        }
        return buildString {
            appendLine(border())
            for (row in rowRange) {
                append('|')
                for (col in colRange) {
                    val label = _cells[SquareCoordinate(col, row)].asciiLabel()
                    append(label.centered(widths.getValue(col))); append('|')
                }
                appendLine(); appendLine(border())
            }
        }.trimEnd()
    }

    private fun Cell<SquareCoordinate, SquareDirection, D>?.asciiLabel(): String =
        this?.data?.toString().orEmpty()

    private fun String.centered(width: Int): String {
        if (length >= width) return this
        val left = (width - length) / 2
        return " ".repeat(left) + this + " ".repeat(width - length - left)
    }
}

package io.gridkit.core.grid

import io.gridkit.core.core.*
import io.gridkit.core.direction.*
import io.gridkit.core.pathfinding.AStarPathfinder
import io.gridkit.core.topology.Edge
import io.gridkit.core.topology.Vertex

/**
 * A triangular grid.
 *
 * Each cell is identified by `TriangleCoordinate(col, row)` where col parity
 * encodes the pointing direction:
 * - **even col** → UP-pointing triangle (∆)
 * - **odd col**  → DOWN-pointing triangle (∇)
 *
 * Neighbor rules:
 * ```
 * LEFT     → (col - 1, row)          for both UP and DOWN
 * RIGHT    → (col + 1, row)          for both UP and DOWN
 * VERTICAL → (col + 1, row - 1)      for even col (UP-pointing)
 * VERTICAL → (col - 1, row + 1)      for odd  col (DOWN-pointing)
 * ```
 *
 * **Edge / vertex sharing (derived from edge-pairing rules):**
 *
 * For UP(col, row):
 * - `VERTICAL_v` = LEFT_v of DOWN(col+1, row) = RIGHT_v of DOWN(col-1, row)
 * - `LEFT_v`     = VERTICAL_v of DOWN(col-1, row) = LEFT_v of DOWN(col+1, row-1)
 * - `RIGHT_v`    = VERTICAL_v of DOWN(col+1, row) = RIGHT_v of DOWN(col+1, row-1)
 *
 * For DOWN(col, row):
 * - `VERTICAL_v` = RIGHT_v of UP(col-1, row) = LEFT_v of UP(col+1, row)
 * - `LEFT_v`     = VERTICAL_v of UP(col-1, row) = LEFT_v of UP(col-1, row+1)
 * - `RIGHT_v`    = VERTICAL_v of UP(col+1, row) = RIGHT_v of UP(col-1, row+1)
 *
 * @param D the optional cell-data payload type
 * @param cols total number of triangle columns
 * @param rows number of rows
 */
class TriangleGrid<D>(
    cols: Int,
    rows: Int
) : Grid<
    TriangleCoordinate,
    TriangleDirection,
    D,
    Cell<TriangleCoordinate, TriangleDirection, D>,
    Edge<D>,
    Vertex<D>
> {

    private val _cells: MutableMap<TriangleCoordinate, Cell<TriangleCoordinate, TriangleDirection, D>> = mutableMapOf()
    override val cells: Map<TriangleCoordinate, Cell<TriangleCoordinate, TriangleDirection, D>> get() = _cells

    private var minCol = 0; private var maxCol = cols - 1
    private var minRow = 0; private var maxRow = rows - 1

    /** Physical height of one triangle row (default 1.0). */
    var triHeight: Double = 1.0

    private val pathfinder = AStarPathfinder<
        TriangleCoordinate,
        TriangleDirection,
        D,
        Cell<TriangleCoordinate, TriangleDirection, D>,
        Edge<D>,
        Vertex<D>
    >()

    init {
        for (row in 0 until rows)
            for (col in 0 until cols)
                addCell(TriangleCoordinate(col, row))
    }

    // ── topology helpers ──────────────────────────────────────────────────────

    private fun addCell(coord: TriangleCoordinate, data: D? = null): Cell<TriangleCoordinate, TriangleDirection, D> {
        val cell = Cell<TriangleCoordinate, TriangleDirection, D>(coord, data)
        _cells[coord] = cell
        buildTopology(cell)
        return cell
    }

    private fun vc(col: Int, row: Int): TriangleCoordinate = TriangleCoordinate(col, row)

    /**
     * Returns the vertex at [vDir] from the cell at [coord] if that cell exists.
     * Helper shorthand for probing the vertex store of already-built cells.
     */
    private fun probe(coord: TriangleCoordinate, vDir: TriangleVertexDirection): Vertex<D>? =
        _cells[coord]?.getVertex(vDir)

    private fun buildTopology(cell: Cell<TriangleCoordinate, TriangleDirection, D>) {
        val col = cell.coordinate.col
        val row = cell.coordinate.row
        val isUp = col % 2 == 0

        // ── vertices ─────────────────────────────────────────────────────────
        val verticalV: Vertex<D>
        val leftV: Vertex<D>
        val rightV: Vertex<D>

        if (isUp) {
            // UP(col, row): apex = VERTICAL_v, base corners = LEFT_v, RIGHT_v
            verticalV = probe(vc(col + 1, row), TriangleVertexDirection.LEFT)
                ?: probe(vc(col - 1, row), TriangleVertexDirection.RIGHT)
                ?: Vertex()
            leftV = probe(vc(col - 1, row), TriangleVertexDirection.VERTICAL)
                ?: probe(vc(col + 1, row - 1), TriangleVertexDirection.LEFT)
                ?: Vertex()
            rightV = probe(vc(col + 1, row), TriangleVertexDirection.VERTICAL)
                ?: probe(vc(col + 1, row - 1), TriangleVertexDirection.RIGHT)
                ?: Vertex()
        } else {
            // DOWN(col, row): apex = VERTICAL_v, top corners = LEFT_v, RIGHT_v
            verticalV = probe(vc(col - 1, row), TriangleVertexDirection.RIGHT)
                ?: probe(vc(col + 1, row), TriangleVertexDirection.LEFT)
                ?: Vertex()
            leftV = probe(vc(col - 1, row), TriangleVertexDirection.VERTICAL)
                ?: probe(vc(col - 1, row + 1), TriangleVertexDirection.LEFT)
                ?: Vertex()
            rightV = probe(vc(col + 1, row), TriangleVertexDirection.VERTICAL)
                ?: probe(vc(col - 1, row + 1), TriangleVertexDirection.RIGHT)
                ?: Vertex()
        }

        listOf(verticalV, leftV, rightV).forEach { it.addCell(cell) }
        cell.putVertex(TriangleVertexDirection.VERTICAL, verticalV)
        cell.putVertex(TriangleVertexDirection.LEFT,     leftV)
        cell.putVertex(TriangleVertexDirection.RIGHT,    rightV)

        // ── edges ─────────────────────────────────────────────────────────────
        // For UP:   LEFT edge={VERTICAL_v,LEFT_v}, RIGHT={VERTICAL_v,RIGHT_v}, VERTICAL={LEFT_v,RIGHT_v}
        // For DOWN: LEFT edge={LEFT_v,VERTICAL_v}, RIGHT={VERTICAL_v,RIGHT_v}, VERTICAL={LEFT_v,RIGHT_v}
        // Opposite direction mapping: LEFT↔RIGHT, VERTICAL↔VERTICAL

        val leftNeighbor  = vc(col - 1, row)
        val rightNeighbor = vc(col + 1, row)
        val vertNeighbor  = if (isUp) vc(col + 1, row - 1) else vc(col - 1, row + 1)

        val leftEdge = _cells[leftNeighbor]?.getEdge(TriangleEdgeDirection.RIGHT)
            ?: newEdge(verticalV, leftV)
        leftEdge.addCell(cell)
        cell.putEdge(TriangleEdgeDirection.LEFT, leftEdge)

        val rightEdge = _cells[rightNeighbor]?.getEdge(TriangleEdgeDirection.LEFT)
            ?: newEdge(verticalV, rightV)
        rightEdge.addCell(cell)
        cell.putEdge(TriangleEdgeDirection.RIGHT, rightEdge)

        val verticalEdge = _cells[vertNeighbor]?.getEdge(TriangleEdgeDirection.VERTICAL)
            ?: newEdge(leftV, rightV)
        verticalEdge.addCell(cell)
        cell.putEdge(TriangleEdgeDirection.VERTICAL, verticalEdge)
    }

    private fun newEdge(v1: Vertex<D>, v2: Vertex<D>): Edge<D> =
        Edge(setOf(v1, v2)).also { v1.addEdge(it); v2.addEdge(it) }

    private fun neighborCoords(c: TriangleCoordinate): Map<TriangleEdgeDirection, TriangleCoordinate> {
        val vertical = if (c.isUp)
            TriangleCoordinate(c.col + 1, c.row - 1)
        else
            TriangleCoordinate(c.col - 1, c.row + 1)
        return mapOf(
            TriangleEdgeDirection.LEFT     to TriangleCoordinate(c.col - 1, c.row),
            TriangleEdgeDirection.RIGHT    to TriangleCoordinate(c.col + 1, c.row),
            TriangleEdgeDirection.VERTICAL to vertical
        )
    }

    internal fun setCell(coordinate: TriangleCoordinate, data: D?) {
        val existing = _cells[coordinate]
        if (existing != null) { existing.data = data; return }
        addCell(coordinate, data)
        expandBounds(coordinate)
    }

    private fun expandBounds(c: TriangleCoordinate) {
        if (c.col < minCol) minCol = c.col; if (c.col > maxCol) maxCol = c.col
        if (c.row < minRow) minRow = c.row; if (c.row > maxRow) maxRow = c.row
    }

    // ── Grid<TriangleCoordinate, TriangleDirection, D> ────────────────────────

    override fun getCell(coordinate: TriangleCoordinate): Cell<TriangleCoordinate, TriangleDirection, D>? =
        _cells[coordinate]

    override fun isValidCoordinate(coordinate: TriangleCoordinate): Boolean = coordinate in _cells

    override fun getNeighbors(coordinate: TriangleCoordinate): List<Cell<TriangleCoordinate, TriangleDirection, D>> =
        neighborCoords(coordinate).values.mapNotNull { _cells[it] }

    override fun getDirectedNeighbors(coordinate: TriangleCoordinate): Map<TriangleDirection, Cell<TriangleCoordinate, TriangleDirection, D>> =
        neighborCoords(coordinate).mapNotNull { (dir, nc) -> _cells[nc]?.let { dir to it } }.toMap()

    override fun getNeighbor(coordinate: TriangleCoordinate, direction: TriangleDirection): TriangleCoordinate? {
        if (direction !is TriangleEdgeDirection) return null
        val nc = neighborCoords(coordinate)[direction] ?: return null
        return if (nc in _cells) nc else null
    }

    override fun findPath(
        from: TriangleCoordinate,
        to: TriangleCoordinate,
        passable: (Cell<TriangleCoordinate, TriangleDirection, D>) -> Boolean
    ): List<TriangleCoordinate>? = pathfinder.findPath(this, from, to, passable)

    override fun distance(from: TriangleCoordinate, to: TriangleCoordinate): Int {
        if (from == to) return 0
        val visited = mutableSetOf(from)
        val queue = ArrayDeque<Pair<TriangleCoordinate, Int>>()
        queue.add(from to 0)
        while (queue.isNotEmpty()) {
            val (current, dist) = queue.removeFirst()
            for (nc in neighborCoords(current).values) {
                if (nc == to) return dist + 1
                if (nc !in visited && nc in _cells) { visited.add(nc); queue.add(nc to dist + 1) }
            }
        }
        return Int.MAX_VALUE
    }

    override fun getRange(center: TriangleCoordinate, radius: Int): List<Cell<TriangleCoordinate, TriangleDirection, D>> =
        _cells.values.filter { distance(center, it.coordinate) <= radius }

    override fun getRing(center: TriangleCoordinate, radius: Int): List<Cell<TriangleCoordinate, TriangleDirection, D>> =
        if (radius == 0) listOfNotNull(_cells[center])
        else _cells.values.filter { distance(center, it.coordinate) == radius }

    override fun getLine(from: TriangleCoordinate, to: TriangleCoordinate): List<Cell<TriangleCoordinate, TriangleDirection, D>> =
        (findPath(from, to) { true } ?: listOfNotNull(from)).mapNotNull { _cells[it] }

    override fun placeNext(from: TriangleCoordinate, direction: TriangleDirection): Cell<TriangleCoordinate, TriangleDirection, D> {
        if (direction !is TriangleEdgeDirection) error("placeNext requires a TriangleEdgeDirection")
        val target = neighborCoords(from)[direction]!!
        _cells[target]?.let { return it }
        val cell = addCell(target)
        expandBounds(target)
        return cell
    }

    override fun arithmeticCenter(): TriangleCoordinate =
        TriangleCoordinate((minCol + maxCol).floorDiv(2), (minRow + maxRow).floorDiv(2))

    override fun physicalCenter(): GridCenter<TriangleCoordinate> {
        val physical = if (_cells.isEmpty()) PhysicalCenter(0.0, 0.0) else PhysicalCenter(
            _cells.values.sumOf { toPhysical(it.coordinate).x } / _cells.size,
            _cells.values.sumOf { toPhysical(it.coordinate).y } / _cells.size
        )
        return GridCenter(physical, nearestCoordinate(physical))
    }

    fun toPhysical(coordinate: TriangleCoordinate): PhysicalCenter {
        val slot = coordinate.col / 2
        val x = slot * 0.5 + if (coordinate.isUp) 0.166 else 0.333
        val y = coordinate.row * triHeight + if (coordinate.isUp) 0.333 else 0.666
        return PhysicalCenter(x, y)
    }

    fun nearestCoordinate(physical: PhysicalCenter): TriangleCoordinate =
        _cells.keys.minByOrNull { c ->
            val p = toPhysical(c); val dx = p.x - physical.x; val dy = p.y - physical.y
            dx * dx + dy * dy
        } ?: TriangleCoordinate(0, 0)

    override fun toAsciiString(): String {
        if (_cells.isEmpty()) return "(empty grid)"
        val colRange = minCol..maxCol; val rowRange = minRow..maxRow
        val innerWidths = colRange.associateWith { col ->
            maxOf(5, rowRange.maxOf { row -> _cells[TriangleCoordinate(col, row)].asciiLabel().length } + 4)
        }
        fun isVisualUp(col: Int, row: Int): Boolean = (col + row) % 2 == 0
        fun top(row: Int) = buildString {
            for (col in colRange) {
                val width = innerWidths.getValue(col)
                if (isVisualUp(col, row)) {
                    if (col == colRange.first) append("  ")
                    append("*".repeat(if (col == colRange.first) width else width - 1))
                } else { append("-".repeat(width + 2)); append('*') }
            }
        }.trimEnd()
        fun middle(row: Int) = buildString {
            for (col in colRange) {
                val label = _cells[TriangleCoordinate(col, row)].asciiLabel()
                val body = label.centered(innerWidths.getValue(col))
                if (isVisualUp(col, row) && col == colRange.first) append(" /").append(body).append("\\")
                else if (isVisualUp(col, row)) append(body).append("\\")
                else if (col == colRange.first) append(" \\").append(body).append("/")
                else append(body).append("/")
            }
        }.trimEnd()
        fun bottom(row: Int) = buildString {
            for (col in colRange) {
                val width = innerWidths.getValue(col)
                if (isVisualUp(col, row)) {
                    if (col == colRange.first) append('*')
                    append("-".repeat(width + 2)); append('*')
                } else if (col == colRange.first) { append("  "); append("*".repeat(width)) }
                else append("*".repeat(width - 1))
            }
        }.trimEnd()
        return buildString {
            appendLine(top(rowRange.first))
            for (row in rowRange) { appendLine(middle(row)); appendLine(bottom(row)) }
        }.trimEnd()
    }

    private fun Cell<TriangleCoordinate, TriangleDirection, D>?.asciiLabel(): String =
        this?.data?.toString().orEmpty()
    private fun String.centered(width: Int): String {
        if (length >= width) return this
        val left = (width - length) / 2
        return " ".repeat(left) + this + " ".repeat(width - length - left)
    }
}

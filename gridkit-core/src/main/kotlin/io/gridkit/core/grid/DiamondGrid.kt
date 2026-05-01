package io.gridkit.core.grid

import io.gridkit.core.core.*
import io.gridkit.core.direction.*
import io.gridkit.core.pathfinding.AStarPathfinder
import io.gridkit.core.topology.Edge
import io.gridkit.core.topology.Vertex
import kotlin.math.abs

/**
 * A diamond grid laid out in an isometric (rotated-square) pattern.
 *
 * Physical coordinates use:
 * ```
 * x = (col - row) * diamondWidth  * 0.5
 * y = (col + row) * diamondHeight * 0.5
 * ```
 *
 * **Neighbour offsets** (derived geometrically):
 * ```
 * TOP_LEFT   → (row,   col-1)
 * TOP_RIGHT  → (row-1, col)
 * BOTTOM_LEFT  → (row+1, col)
 * BOTTOM_RIGHT → (row,   col+1)
 * ```
 *
 * **Edge opposites:**
 * - TOP_LEFT ↔ BOTTOM_RIGHT
 * - TOP_RIGHT ↔ BOTTOM_LEFT
 *
 * **Vertex sharing** (every vertex is shared by exactly 2 cells or fewer at borders):
 * - `TOP_v`   : (row, col-1).RIGHT_v  or  (row-1, col).LEFT_v
 * - `LEFT_v`  : (row, col-1).DOWN_v   or  (row+1, col).TOP_v
 * - `RIGHT_v` : (row-1, col).DOWN_v   or  (row,   col+1).TOP_v
 * - `DOWN_v`  : (row+1, col).RIGHT_v  or  (row,   col+1).LEFT_v
 *
 * @param D the optional cell-data payload type
 */
class DiamondGrid<D>(
    rows: Int,
    cols: Int
) : Grid<
    DiamondCoordinate,
    DiamondDirection,
    D,
    Cell<DiamondCoordinate, DiamondDirection, D>,
    Edge<D>,
    Vertex<D>
> {

    private val _cells: MutableMap<DiamondCoordinate, Cell<DiamondCoordinate, DiamondDirection, D>> = mutableMapOf()
    override val cells: Map<DiamondCoordinate, Cell<DiamondCoordinate, DiamondDirection, D>> get() = _cells

    private var minRow = 0; private var maxRow = rows - 1
    private var minCol = 0; private var maxCol = cols - 1

    /** Physical width of one diamond cell (default 1.0). */
    var diamondWidth: Double = 1.0

    /** Physical height of one diamond cell (default 1.0). */
    var diamondHeight: Double = 1.0

    private val pathfinder = AStarPathfinder<
        DiamondCoordinate,
        DiamondDirection,
        D,
        Cell<DiamondCoordinate, DiamondDirection, D>,
        Edge<D>,
        Vertex<D>
    >()

    init {
        for (row in 0 until rows)
            for (col in 0 until cols)
                addCell(DiamondCoordinate(row, col))
    }

    // ── topology helpers ──────────────────────────────────────────────────────

    private fun addCell(coord: DiamondCoordinate, data: D? = null): Cell<DiamondCoordinate, DiamondDirection, D> {
        val cell = Cell<DiamondCoordinate, DiamondDirection, D>(coord, data)
        _cells[coord] = cell
        buildTopology(cell)
        return cell
    }

    private fun probe(row: Int, col: Int, vDir: DiamondVertexDirection): Vertex<D>? =
        _cells[DiamondCoordinate(row, col)]?.getVertex(vDir)

    private fun buildTopology(cell: Cell<DiamondCoordinate, DiamondDirection, D>) {
        val row = cell.coordinate.row
        val col = cell.coordinate.col

        // ── vertices ─────────────────────────────────────────────────────────
        val topV   = probe(row, col - 1, DiamondVertexDirection.RIGHT)
            ?: probe(row - 1, col, DiamondVertexDirection.LEFT)
            ?: Vertex()
        val leftV  = probe(row, col - 1, DiamondVertexDirection.DOWN)
            ?: probe(row + 1, col, DiamondVertexDirection.TOP)
            ?: Vertex()
        val rightV = probe(row - 1, col, DiamondVertexDirection.DOWN)
            ?: probe(row, col + 1, DiamondVertexDirection.TOP)
            ?: Vertex()
        val downV  = probe(row + 1, col, DiamondVertexDirection.RIGHT)
            ?: probe(row, col + 1, DiamondVertexDirection.LEFT)
            ?: Vertex()

        listOf(topV, leftV, rightV, downV).forEach { it.addCell(cell) }
        cell.putVertex(DiamondVertexDirection.TOP,   topV)
        cell.putVertex(DiamondVertexDirection.LEFT,  leftV)
        cell.putVertex(DiamondVertexDirection.RIGHT, rightV)
        cell.putVertex(DiamondVertexDirection.DOWN,  downV)

        // ── edges ─────────────────────────────────────────────────────────────
        // TOP_LEFT edge ↔ BOTTOM_RIGHT of (row, col-1)
        val tlEdge = _cells[DiamondCoordinate(row, col - 1)]?.getEdge(DiamondEdgeDirection.BOTTOM_RIGHT)
            ?: newEdge(topV, leftV)
        tlEdge.addCell(cell)
        cell.putEdge(DiamondEdgeDirection.TOP_LEFT, tlEdge)

        // TOP_RIGHT edge ↔ BOTTOM_LEFT of (row-1, col)
        val trEdge = _cells[DiamondCoordinate(row - 1, col)]?.getEdge(DiamondEdgeDirection.BOTTOM_LEFT)
            ?: newEdge(topV, rightV)
        trEdge.addCell(cell)
        cell.putEdge(DiamondEdgeDirection.TOP_RIGHT, trEdge)

        // BOTTOM_LEFT edge ↔ TOP_RIGHT of (row+1, col)
        val blEdge = _cells[DiamondCoordinate(row + 1, col)]?.getEdge(DiamondEdgeDirection.TOP_RIGHT)
            ?: newEdge(leftV, downV)
        blEdge.addCell(cell)
        cell.putEdge(DiamondEdgeDirection.BOTTOM_LEFT, blEdge)

        // BOTTOM_RIGHT edge ↔ TOP_LEFT of (row, col+1)
        val brEdge = _cells[DiamondCoordinate(row, col + 1)]?.getEdge(DiamondEdgeDirection.TOP_LEFT)
            ?: newEdge(rightV, downV)
        brEdge.addCell(cell)
        cell.putEdge(DiamondEdgeDirection.BOTTOM_RIGHT, brEdge)
    }

    private fun newEdge(v1: Vertex<D>, v2: Vertex<D>): Edge<D> =
        Edge(setOf(v1, v2)).also { v1.addEdge(it); v2.addEdge(it) }

    private val neighborOffsets: Map<DiamondEdgeDirection, Pair<Int, Int>> = mapOf(
        DiamondEdgeDirection.TOP_LEFT    to ( 0 to -1),
        DiamondEdgeDirection.TOP_RIGHT   to (-1 to  0),
        DiamondEdgeDirection.BOTTOM_LEFT to ( 1 to  0),
        DiamondEdgeDirection.BOTTOM_RIGHT to (0 to  1)
    )

    internal fun setCell(coordinate: DiamondCoordinate, data: D?) {
        val existing = _cells[coordinate]
        if (existing != null) { existing.data = data; return }
        addCell(coordinate, data)
        expandBounds(coordinate)
    }

    private fun expandBounds(c: DiamondCoordinate) {
        if (c.row < minRow) minRow = c.row; if (c.row > maxRow) maxRow = c.row
        if (c.col < minCol) minCol = c.col; if (c.col > maxCol) maxCol = c.col
    }

    // ── Grid<DiamondCoordinate, DiamondDirection, D> ──────────────────────────────

    override fun getCell(coordinate: DiamondCoordinate): Cell<DiamondCoordinate, DiamondDirection, D>? =
        _cells[coordinate]

    override fun isValidCoordinate(coordinate: DiamondCoordinate): Boolean = coordinate in _cells

    override fun getNeighbors(coordinate: DiamondCoordinate): List<Cell<DiamondCoordinate, DiamondDirection, D>> =
        neighborOffsets.values.mapNotNull { (dr, dc) ->
            _cells[DiamondCoordinate(coordinate.row + dr, coordinate.col + dc)]
        }

    override fun getDirectedNeighbors(coordinate: DiamondCoordinate): Map<DiamondDirection, Cell<DiamondCoordinate, DiamondDirection, D>> =
        neighborOffsets.mapNotNull { (dir, off) ->
            _cells[DiamondCoordinate(coordinate.row + off.first, coordinate.col + off.second)]?.let { dir to it }
        }.toMap()

    override fun getNeighbor(coordinate: DiamondCoordinate, direction: DiamondDirection): DiamondCoordinate? {
        if (direction !is DiamondEdgeDirection) return null
        val (dr, dc) = neighborOffsets[direction]!!
        val nc = DiamondCoordinate(coordinate.row + dr, coordinate.col + dc)
        return if (nc in _cells) nc else null
    }

    override fun findPath(
        from: DiamondCoordinate,
        to: DiamondCoordinate,
        passable: (Cell<DiamondCoordinate, DiamondDirection, D>) -> Boolean
    ): List<DiamondCoordinate>? = pathfinder.findPath(this, from, to, passable)

    /** Manhattan distance — one step per edge traversal in any of the 4 directions. */
    override fun distance(from: DiamondCoordinate, to: DiamondCoordinate): Int =
        abs(to.row - from.row) + abs(to.col - from.col)

    override fun getRange(center: DiamondCoordinate, radius: Int): List<Cell<DiamondCoordinate, DiamondDirection, D>> =
        _cells.values.filter { distance(center, it.coordinate) <= radius }

    override fun getRing(center: DiamondCoordinate, radius: Int): List<Cell<DiamondCoordinate, DiamondDirection, D>> =
        if (radius == 0) listOfNotNull(_cells[center])
        else _cells.values.filter { distance(center, it.coordinate) == radius }

    override fun getLine(from: DiamondCoordinate, to: DiamondCoordinate): List<Cell<DiamondCoordinate, DiamondDirection, D>> {
        val dr = to.row - from.row; val dc = to.col - from.col
        val steps = maxOf(abs(dr), abs(dc)); if (steps == 0) return listOfNotNull(_cells[from])
        return (0..steps).mapNotNull { i ->
            val row = from.row + (dr * i.toDouble() / steps).toInt()
            val col = from.col + (dc * i.toDouble() / steps).toInt()
            _cells[DiamondCoordinate(row, col)]
        }
    }

    override fun placeNext(from: DiamondCoordinate, direction: DiamondDirection): Cell<DiamondCoordinate, DiamondDirection, D> {
        if (direction !is DiamondEdgeDirection) error("placeNext requires a DiamondEdgeDirection")
        val (dr, dc) = neighborOffsets[direction]!!
        val target = DiamondCoordinate(from.row + dr, from.col + dc)
        _cells[target]?.let { return it }
        val cell = addCell(target)
        expandBounds(target)
        return cell
    }

    override fun arithmeticCenter(): DiamondCoordinate =
        DiamondCoordinate((minRow + maxRow).floorDiv(2), (minCol + maxCol).floorDiv(2))

    override fun physicalCenter(): GridCenter<DiamondCoordinate> {
        val physical = if (_cells.isEmpty()) PhysicalCenter(0.0, 0.0) else PhysicalCenter(
            _cells.values.sumOf { toPhysical(it.coordinate).x } / _cells.size,
            _cells.values.sumOf { toPhysical(it.coordinate).y } / _cells.size
        )
        return GridCenter(physical, nearestCoordinate(physical))
    }

    /** Converts a coordinate to its physical (x, y) isometric position. */
    fun toPhysical(coordinate: DiamondCoordinate): PhysicalCenter =
        PhysicalCenter(
            (coordinate.col - coordinate.row) * diamondWidth  * 0.5,
            (coordinate.col + coordinate.row) * diamondHeight * 0.5
        )

    /** Snaps an arbitrary physical position to the nearest existing coordinate. */
    fun nearestCoordinate(physical: PhysicalCenter): DiamondCoordinate =
        _cells.keys.minByOrNull { c ->
            val p = toPhysical(c); val dx = p.x - physical.x; val dy = p.y - physical.y
            dx * dx + dy * dy
        } ?: DiamondCoordinate(0, 0)

    override fun toAsciiString(): String {
        if (_cells.isEmpty()) return "(empty grid)"
        val rowRange = minRow..maxRow; val colRange = minCol..maxCol
        val dashWidths = colRange.associateWith { col ->
            maxOf(1, rowRange.maxOf { row -> _cells[DiamondCoordinate(row, col)].asciiLabel().length })
        }
        val indent = " ".repeat((dashWidths.values.max() + 3) / 2)
        val staggerPadding = if (dashWidths.values.toSet().size == 1) {
            dashWidths.values.max() / 2
        } else {
            maxOf(0, dashWidths.values.max() / 2 - 3)
        }
        val wideIndent = indent + " ".repeat(staggerPadding)

        fun shiftedWidth(col: Int): Int =
            dashWidths.getValue(if (col < colRange.last) col + 1 else col)

        fun topOrBottom(row: Int) = buildString {
            if (row % 2 != 0) { append(wideIndent); append("  ") }
            for (col in colRange) {
                val d = if (row % 2 != 0) shiftedWidth(col) else dashWidths.getValue(col)
                append("  "); append("*".repeat(d + 2)); append(" ".repeat(shiftedWidth(col) + 2))
            }
        }.trimEnd()

        fun upper(row: Int) = buildString {
            if (row % 2 != 0) append(wideIndent)
            for (col in colRange) {
                val d = dashWidths.getValue(col)
                append(" /"); append(" ".repeat(d + 2)); append("\\"); append(" ".repeat(shiftedWidth(col) + 1))
            }
        }.trimEnd()

        fun transitionUpper() = buildString {
            for (col in colRange) {
                val d = dashWidths.getValue(col)
                append(" /"); append(" ".repeat(d + 2)); append("\\"); append(" ".repeat(shiftedWidth(col) + 1))
            }
            append(" /")
        }.trimEnd()

        fun middle(row: Int) = buildString {
            if (row % 2 != 0) append(wideIndent)
            for (col in colRange) {
                val label = _cells[DiamondCoordinate(row, col)].asciiLabel()
                val body = label.centered(dashWidths.getValue(col) + 4)
                if (col == colRange.first) append("*") else append("*".repeat(dashWidths.getValue(col) + 1))
                append(body); append("*")
            }
            if (row % 2 == 0 && minRow < maxRow) append("*".repeat(dashWidths.getValue(colRange.last) + 1))
        }

        fun lower(row: Int) = buildString {
            if (row % 2 != 0) { append(wideIndent); append("  ") }
            for (col in colRange) {
                val d = if (row % 2 != 0) shiftedWidth(col) else dashWidths.getValue(col)
                append(" \\"); append(" ".repeat(d + 2)); append("/"); append(" ".repeat(shiftedWidth(col) + 1))
            }
            if (row < maxRow && row % 2 == 0) append(" \\")
        }.trimEnd()

        fun sharedOddRowMiddle(row: Int) = buildString {
            append("  ")
            for (col in colRange) {
                val d = dashWidths.getValue(col)
                val label = _cells[DiamondCoordinate(row, col)].asciiLabel()
                append("*".repeat(d + 2)); append(label.centered(shiftedWidth(col) + 4))
            }
            append("*")
        }

        return buildString {
            for (row in rowRange) {
                if (row == minRow) { appendLine(topOrBottom(row)); appendLine(upper(row)); appendLine(middle(row)) }
                else if (row % 2 != 0) {
                    appendLine(sharedOddRowMiddle(row))
                    if (row == maxRow) { appendLine(lower(row)); appendLine(topOrBottom(row)) } else appendLine(transitionUpper())
                    continue
                } else { appendLine(middle(row)) }
                appendLine(lower(row))
                if (row == maxRow) {
                    appendLine(topOrBottom(row))
                }
            }
        }.trimEnd()
    }

    private fun Cell<DiamondCoordinate, DiamondDirection, D>?.asciiLabel(): String =
        this?.data?.toString().orEmpty()

    private fun String.centered(width: Int): String {
        if (length >= width) return this
        val left = (width - length) / 2
        return " ".repeat(left) + this + " ".repeat(width - length - left)
    }
}

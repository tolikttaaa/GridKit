package io.gridkit.core.grid

import io.gridkit.core.core.*
import io.gridkit.core.direction.*
import io.gridkit.core.pathfinding.AStarPathfinder
import io.gridkit.core.topology.Edge
import io.gridkit.core.topology.Vertex
import kotlin.math.abs

/**
 * A hexagonal grid using **even-r offset coordinates**.
 *
 * - Row 0 is the topmost row; row increases downward.
 * - Col 0 is the leftmost cell; col increases rightward.
 * - Even rows are not horizontally shifted; odd rows are shifted right by
 *   half a hex width (visually staggered).
 *
 * Edges and vertices are shared between adjacent cells.
 *
 * @param D the optional cell-data payload type
 */
class HexGrid<D>(
    rows: Int,
    cols: Int
) : Grid<
    HexCoordinate,
    HexDirection,
    D,
    Cell<HexCoordinate, HexDirection, D>,
    Edge<D>,
    Vertex<D>
> {

    private val _cells: MutableMap<HexCoordinate, Cell<HexCoordinate, HexDirection, D>> = mutableMapOf()
    override val cells: Map<HexCoordinate, Cell<HexCoordinate, HexDirection, D>> get() = _cells

    private var minRow = 0; private var maxRow = rows - 1
    private var minCol = 0; private var maxCol = cols - 1

    /** Physical width of one hex cell (default 1.0). */
    var hexWidth: Double = 1.0

    /** Physical height of one hex cell (default 1.0). */
    var hexHeight: Double = 1.0

    private val pathfinder = AStarPathfinder<
        HexCoordinate,
        HexDirection,
        D,
        Cell<HexCoordinate, HexDirection, D>,
        Edge<D>,
        Vertex<D>
    >()

    init {
        for (row in 0 until rows)
            for (col in 0 until cols)
                addCell(HexCoordinate(row, col))
    }

    // ── topology helpers ──────────────────────────────────────────────────────

    private fun addCell(coord: HexCoordinate, data: D? = null): Cell<HexCoordinate, HexDirection, D> {
        val cell = Cell<HexCoordinate, HexDirection, D>(coord, data)
        _cells[coord] = cell
        buildTopology(cell)
        return cell
    }

    /** Returns offset map for the 6 edge-direction neighbors, depending on row parity. */
    private fun neighborOffsets(row: Int): Map<HexEdgeDirection, Pair<Int, Int>> =
        if (row % 2 == 0) mapOf(
            HexEdgeDirection.TOP_LEFT   to (-1 to -1),
            HexEdgeDirection.TOP_RIGHT  to (-1 to  0),
            HexEdgeDirection.LEFT       to ( 0 to -1),
            HexEdgeDirection.RIGHT      to ( 0 to  1),
            HexEdgeDirection.DOWN_LEFT  to ( 1 to -1),
            HexEdgeDirection.DOWN_RIGHT to ( 1 to  0)
        ) else mapOf(
            HexEdgeDirection.TOP_LEFT   to (-1 to  0),
            HexEdgeDirection.TOP_RIGHT  to (-1 to  1),
            HexEdgeDirection.LEFT       to ( 0 to -1),
            HexEdgeDirection.RIGHT      to ( 0 to  1),
            HexEdgeDirection.DOWN_LEFT  to ( 1 to  0),
            HexEdgeDirection.DOWN_RIGHT to ( 1 to  1)
        )

    private fun neighborCoord(base: HexCoordinate, dir: HexEdgeDirection): HexCoordinate {
        val (dr, dc) = neighborOffsets(base.row)[dir]!!
        return HexCoordinate(base.row + dr, base.col + dc)
    }

    /** The edge direction on the opposite side (the shared edge from the neighbor's POV). */
    private fun oppositeEdge(dir: HexEdgeDirection): HexEdgeDirection = when (dir) {
        HexEdgeDirection.TOP_LEFT   -> HexEdgeDirection.DOWN_RIGHT
        HexEdgeDirection.TOP_RIGHT  -> HexEdgeDirection.DOWN_LEFT
        HexEdgeDirection.LEFT       -> HexEdgeDirection.RIGHT
        HexEdgeDirection.RIGHT      -> HexEdgeDirection.LEFT
        HexEdgeDirection.DOWN_LEFT  -> HexEdgeDirection.TOP_RIGHT
        HexEdgeDirection.DOWN_RIGHT -> HexEdgeDirection.TOP_LEFT
    }

    private fun buildTopology(cell: Cell<HexCoordinate, HexDirection, D>) {
        val coord = cell.coordinate
        val nb = neighborOffsets(coord.row).mapValues { (_, off) ->
            HexCoordinate(coord.row + off.first, coord.col + off.second)
        }

        // ── vertices ─────────────────────────────────────────────────────────
        // Sharing rules (derived from physical coordinates — same for even/odd rows):
        //   TOP      = TOP_LEFT_nb.DOWN_RIGHT   or TOP_RIGHT_nb.DOWN_LEFT
        //   TOP_LEFT = LEFT_nb.TOP_RIGHT        or TOP_LEFT_nb.DOWN
        //   TOP_RIGHT= TOP_RIGHT_nb.DOWN        or RIGHT_nb.TOP_LEFT
        //   DOWN_LEFT= LEFT_nb.DOWN_RIGHT       or DOWN_LEFT_nb.TOP
        //   DOWN_RIGHT= RIGHT_nb.DOWN_LEFT      or DOWN_RIGHT_nb.TOP
        //   DOWN     = DOWN_LEFT_nb.TOP_RIGHT   or DOWN_RIGHT_nb.TOP_LEFT
        //
        // Probe order: already-built neighbors first (TOP_LEFT, TOP_RIGHT, LEFT
        // are built before the current cell in row-major order).
        val topV    = findVertexIn(nb[HexEdgeDirection.TOP_LEFT]!!, HexVertexDirection.DOWN_RIGHT)
            ?: findVertexIn(nb[HexEdgeDirection.TOP_RIGHT]!!, HexVertexDirection.DOWN_LEFT)
            ?: Vertex()
        val topLV   = findVertexIn(nb[HexEdgeDirection.LEFT]!!, HexVertexDirection.TOP_RIGHT)
            ?: findVertexIn(nb[HexEdgeDirection.TOP_LEFT]!!, HexVertexDirection.DOWN)
            ?: Vertex()
        val topRV   = findVertexIn(nb[HexEdgeDirection.TOP_RIGHT]!!, HexVertexDirection.DOWN)
            ?: findVertexIn(nb[HexEdgeDirection.RIGHT]!!, HexVertexDirection.TOP_LEFT)
            ?: Vertex()
        val downLV  = findVertexIn(nb[HexEdgeDirection.LEFT]!!, HexVertexDirection.DOWN_RIGHT)
            ?: findVertexIn(nb[HexEdgeDirection.DOWN_LEFT]!!, HexVertexDirection.TOP)
            ?: Vertex()
        val downRV  = findVertexIn(nb[HexEdgeDirection.RIGHT]!!, HexVertexDirection.DOWN_LEFT)
            ?: findVertexIn(nb[HexEdgeDirection.DOWN_RIGHT]!!, HexVertexDirection.TOP)
            ?: Vertex()
        val downV   = findVertexIn(nb[HexEdgeDirection.DOWN_LEFT]!!, HexVertexDirection.TOP_RIGHT)
            ?: findVertexIn(nb[HexEdgeDirection.DOWN_RIGHT]!!, HexVertexDirection.TOP_LEFT)
            ?: Vertex()

        listOf(topV, topLV, topRV, downLV, downRV, downV).forEach { it.addCell(cell) }
        cell.putVertex(HexVertexDirection.TOP,        topV)
        cell.putVertex(HexVertexDirection.TOP_LEFT,   topLV)
        cell.putVertex(HexVertexDirection.TOP_RIGHT,  topRV)
        cell.putVertex(HexVertexDirection.DOWN_LEFT,  downLV)
        cell.putVertex(HexVertexDirection.DOWN_RIGHT, downRV)
        cell.putVertex(HexVertexDirection.DOWN,       downV)

        // ── edges ─────────────────────────────────────────────────────────────
        // Edge vertices (from physical geometry):
        //   TOP_LEFT  edge = {TOP,    TOP_LEFT}
        //   TOP_RIGHT edge = {TOP,    TOP_RIGHT}
        //   LEFT      edge = {TOP_LEFT,  DOWN_LEFT}
        //   RIGHT     edge = {TOP_RIGHT, DOWN_RIGHT}
        //   DOWN_LEFT  edge = {DOWN, DOWN_LEFT}
        //   DOWN_RIGHT edge = {DOWN, DOWN_RIGHT}
        fun edge(dir: HexEdgeDirection, v1: Vertex<D>, v2: Vertex<D>): Edge<D> =
            _cells[nb[dir]!!]?.getEdge(oppositeEdge(dir)) ?: newEdge(v1, v2)

        val tlE = edge(HexEdgeDirection.TOP_LEFT,   topV,  topLV)
        val trE = edge(HexEdgeDirection.TOP_RIGHT,  topV,  topRV)
        val lE  = edge(HexEdgeDirection.LEFT,        topLV, downLV)
        val rE  = edge(HexEdgeDirection.RIGHT,       topRV, downRV)
        val dlE = edge(HexEdgeDirection.DOWN_LEFT,   downV, downLV)
        val drE = edge(HexEdgeDirection.DOWN_RIGHT,  downV, downRV)

        listOf(tlE, trE, lE, rE, dlE, drE).forEach { it.addCell(cell) }
        cell.putEdge(HexEdgeDirection.TOP_LEFT,   tlE)
        cell.putEdge(HexEdgeDirection.TOP_RIGHT,  trE)
        cell.putEdge(HexEdgeDirection.LEFT,        lE)
        cell.putEdge(HexEdgeDirection.RIGHT,       rE)
        cell.putEdge(HexEdgeDirection.DOWN_LEFT,   dlE)
        cell.putEdge(HexEdgeDirection.DOWN_RIGHT,  drE)
    }

    private fun findVertexIn(coord: HexCoordinate, dir: HexVertexDirection): Vertex<D>? =
        _cells[coord]?.getVertex(dir)

    private fun newEdge(v1: Vertex<D>, v2: Vertex<D>): Edge<D> =
        Edge(setOf(v1, v2)).also { v1.addEdge(it); v2.addEdge(it) }

    internal fun setCell(coordinate: HexCoordinate, data: D?) {
        val existing = _cells[coordinate]
        if (existing != null) { existing.data = data; return }
        addCell(coordinate, data)
        expandBounds(coordinate)
    }

    private fun expandBounds(c: HexCoordinate) {
        if (c.row < minRow) minRow = c.row; if (c.row > maxRow) maxRow = c.row
        if (c.col < minCol) minCol = c.col; if (c.col > maxCol) maxCol = c.col
    }

    // ── Grid<HexCoordinate, HexDirection, D> ─────────────────────────────────

    override fun getCell(coordinate: HexCoordinate): Cell<HexCoordinate, HexDirection, D>? = _cells[coordinate]

    override fun isValidCoordinate(coordinate: HexCoordinate): Boolean = coordinate in _cells

    override fun getNeighbors(coordinate: HexCoordinate): List<Cell<HexCoordinate, HexDirection, D>> =
        neighborOffsets(coordinate.row).values.mapNotNull { (dr, dc) ->
            _cells[HexCoordinate(coordinate.row + dr, coordinate.col + dc)]
        }

    override fun getDirectedNeighbors(coordinate: HexCoordinate): Map<HexDirection, Cell<HexCoordinate, HexDirection, D>> =
        neighborOffsets(coordinate.row).mapNotNull { (dir, offset) ->
            _cells[HexCoordinate(coordinate.row + offset.first, coordinate.col + offset.second)]?.let { dir to it }
        }.toMap()

    override fun getNeighbor(coordinate: HexCoordinate, direction: HexDirection): HexCoordinate? {
        if (direction !is HexEdgeDirection) return null
        val (dr, dc) = neighborOffsets(coordinate.row)[direction]!!
        val nc = HexCoordinate(coordinate.row + dr, coordinate.col + dc)
        return if (nc in _cells) nc else null
    }

    override fun findPath(
        from: HexCoordinate,
        to: HexCoordinate,
        passable: (Cell<HexCoordinate, HexDirection, D>) -> Boolean
    ): List<HexCoordinate>? = pathfinder.findPath(this, from, to, passable)

    override fun distance(from: HexCoordinate, to: HexCoordinate): Int {
        val (fx, fy, fz) = toCube(from); val (tx, ty, tz) = toCube(to)
        return maxOf(abs(fx - tx), abs(fy - ty), abs(fz - tz))
    }

    private fun toCube(c: HexCoordinate): Triple<Int, Int, Int> {
        val x = c.col - (c.row - (c.row and 1)) / 2; val z = c.row
        return Triple(x, -x - z, z)
    }

    private fun cubeRound(x: Double, y: Double, z: Double): Triple<Int, Int, Int> {
        var rx = x.toLong().toInt(); var ry = y.toLong().toInt(); var rz = z.toLong().toInt()
        val xd = abs(rx - x); val yd = abs(ry - y); val zd = abs(rz - z)
        when { xd > yd && xd > zd -> rx = -ry - rz; yd > zd -> ry = -rx - rz; else -> rz = -rx - ry }
        return Triple(rx, ry, rz)
    }

    private fun fromCube(cube: Triple<Int, Int, Int>): HexCoordinate {
        val row = cube.third; return HexCoordinate(row, cube.first + (row - (row and 1)) / 2)
    }

    override fun getRange(center: HexCoordinate, radius: Int): List<Cell<HexCoordinate, HexDirection, D>> =
        _cells.values.filter { distance(center, it.coordinate) <= radius }

    override fun getRing(center: HexCoordinate, radius: Int): List<Cell<HexCoordinate, HexDirection, D>> =
        if (radius == 0) listOfNotNull(_cells[center])
        else _cells.values.filter { distance(center, it.coordinate) == radius }

    override fun getLine(from: HexCoordinate, to: HexCoordinate): List<Cell<HexCoordinate, HexDirection, D>> {
        val n = distance(from, to); if (n == 0) return listOfNotNull(_cells[from])
        val (fx, fy, fz) = toCube(from); val (tx, ty, tz) = toCube(to)
        return (0..n).mapNotNull { i ->
            val t = i.toDouble() / n
            _cells[fromCube(cubeRound(fx + (tx - fx) * t, fy + (ty - fy) * t, fz + (tz - fz) * t))]
        }
    }

    override fun placeNext(from: HexCoordinate, direction: HexDirection): Cell<HexCoordinate, HexDirection, D> {
        if (direction !is HexEdgeDirection) error("placeNext requires a HexEdgeDirection")
        val (dr, dc) = neighborOffsets(from.row)[direction]!!
        val target = HexCoordinate(from.row + dr, from.col + dc)
        _cells[target]?.let { return it }
        val cell = addCell(target)
        expandBounds(target)
        return cell
    }

    override fun arithmeticCenter(): HexCoordinate =
        HexCoordinate((minRow + maxRow).floorDiv(2), (minCol + maxCol).floorDiv(2))

    override fun physicalCenter(): GridCenter<HexCoordinate> {
        val physical = if (_cells.isEmpty()) PhysicalCenter(0.0, 0.0) else PhysicalCenter(
            _cells.values.sumOf { toPhysical(it.coordinate).x } / _cells.size,
            _cells.values.sumOf { toPhysical(it.coordinate).y } / _cells.size
        )
        return GridCenter(physical, nearestCoordinate(physical))
    }

    fun toPhysical(coordinate: HexCoordinate): PhysicalCenter {
        val x = coordinate.col * hexWidth + if (coordinate.row % 2 != 0) hexWidth / 2.0 else 0.0
        val y = coordinate.row * hexHeight * 0.75
        return PhysicalCenter(x, y)
    }

    fun nearestCoordinate(physical: PhysicalCenter): HexCoordinate =
        _cells.keys.minByOrNull { c ->
            val p = toPhysical(c); val dx = p.x - physical.x; val dy = p.y - physical.y
            dx * dx + dy * dy
        } ?: HexCoordinate(0, 0)

    override fun toAsciiString(): String {
        if (_cells.isEmpty()) return "(empty grid)"
        val rowRange = minRow..maxRow; val colRange = minCol..maxCol
        val dashWidths = colRange.associateWith { col ->
            maxOf(1, rowRange.maxOf { row -> _cells[HexCoordinate(row, col)].asciiLabel().length })
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
                val nextD = shiftedWidth(col)
                append("  *"); append("-".repeat(d)); append("*"); append(" ".repeat(nextD + 2))
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
                val label = _cells[HexCoordinate(row, col)].asciiLabel()
                val body = label.centered(dashWidths.getValue(col) + 4)
                if (col == colRange.first) append("*") else { append("-".repeat(dashWidths.getValue(col))); append("*") }
                append(body); append("*")
            }
            if (row % 2 == 0 && minRow < maxRow) {
                append("-".repeat(dashWidths.getValue(colRange.last))); append("*")
            }
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
                val label = _cells[HexCoordinate(row, col)].asciiLabel()
                append("*"); append("-".repeat(d)); append("*"); append(label.centered(shiftedWidth(col) + 4))
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
                if (row == maxRow) appendLine(topOrBottom(row))
            }
        }.trimEnd()
    }

    private fun Cell<HexCoordinate, HexDirection, D>?.asciiLabel(): String = this?.data?.toString().orEmpty()
    private fun String.centered(width: Int): String {
        if (length >= width) return this
        val left = (width - length) / 2
        return " ".repeat(left) + this + " ".repeat(width - length - left)
    }
}

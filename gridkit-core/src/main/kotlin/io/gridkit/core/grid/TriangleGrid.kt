package io.gridkit.core.grid

import io.gridkit.core.core.*
import io.gridkit.core.pathfinding.AStarPathfinder

/**
 * The three neighbor directions for a triangular cell.
 *
 * - **LEFT** / **RIGHT** — diagonal-edge neighbors in the same row (`col ± 1`)
 * - **VERTICAL** — the horizontal-edge neighbor in the adjacent row:
 *   UP-pointing triangles look to `row - 1`; DOWN-pointing to `row + 1`
 */
enum class TriangleNeighborDirection : GridDirection { LEFT, RIGHT, VERTICAL }

/**
 * A triangular grid.
 *
 * Each cell is identified by `TriangleCoordinate(col, row)` where col parity
 * encodes the pointing direction:
 * - **even col** → UP-pointing triangle
 * - **odd col**  → DOWN-pointing triangle
 *
 * Neighbor rules (both directions share the same LEFT/RIGHT formula):
 * ```
 * LEFT     → (col - 1, row)          for both UP and DOWN
 * RIGHT    → (col + 1, row)          for both UP and DOWN
 * VERTICAL → (col + 1, row - 1)      for even col (UP)
 * VERTICAL → (col - 1, row + 1)      for odd  col (DOWN)
 * ```
 *
 * @param D the optional cell-data payload type
 * @param cols total number of triangle columns (each column is one triangle;
 *   even = UP-pointing, odd = DOWN-pointing)
 * @param rows number of rows
 */
class TriangleGrid<D>(
    cols: Int,
    rows: Int
) : Grid<TriangleCoordinate, TriangleNeighborDirection, D> {

    private val _cells: MutableMap<TriangleCoordinate, Cell<TriangleCoordinate, D>> = mutableMapOf()
    override val cells: Map<TriangleCoordinate, Cell<TriangleCoordinate, D>> get() = _cells

    private var minCol = 0
    private var maxCol = cols - 1
    private var minRow = 0
    private var maxRow = rows - 1

    /** Physical height of one triangle row (default 1.0). */
    var triHeight: Double = 1.0

    private val pathfinder = AStarPathfinder<TriangleCoordinate, TriangleNeighborDirection, D>()

    init {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val coord = TriangleCoordinate(col, row)
                _cells[coord] = Cell(coord)
            }
        }
    }

    // ── internal helpers ──────────────────────────────────────────────────────

    internal fun setCell(coordinate: TriangleCoordinate, cell: Cell<TriangleCoordinate, D>) {
        _cells[coordinate] = cell
    }

    private fun neighborCoords(c: TriangleCoordinate): Map<TriangleNeighborDirection, TriangleCoordinate> {
        val vertical = if (c.isUp)
            TriangleCoordinate(c.col + 1, c.row - 1)
        else
            TriangleCoordinate(c.col - 1, c.row + 1)
        return mapOf(
            TriangleNeighborDirection.LEFT     to TriangleCoordinate(c.col - 1, c.row),
            TriangleNeighborDirection.RIGHT    to TriangleCoordinate(c.col + 1, c.row),
            TriangleNeighborDirection.VERTICAL to vertical
        )
    }

    // ── Grid<TriangleCoordinate, TriangleNeighborDirection, D> ───────────────

    override fun getCell(coordinate: TriangleCoordinate): Cell<TriangleCoordinate, D>? = _cells[coordinate]

    override fun isValidCoordinate(coordinate: TriangleCoordinate): Boolean = coordinate in _cells

    override fun getNeighbors(coordinate: TriangleCoordinate): List<Cell<TriangleCoordinate, D>> =
        neighborCoords(coordinate).values.mapNotNull { _cells[it] }

    override fun getDirectedNeighbors(coordinate: TriangleCoordinate): Map<TriangleNeighborDirection, Cell<TriangleCoordinate, D>> =
        neighborCoords(coordinate).mapNotNull { (dir, nc) -> _cells[nc]?.let { dir to it } }.toMap()

    override fun getNeighbor(coordinate: TriangleCoordinate, direction: TriangleNeighborDirection): TriangleCoordinate? {
        val nc = neighborCoords(coordinate)[direction] ?: return null
        return if (nc in _cells) nc else null
    }

    override fun findPath(
        from: TriangleCoordinate,
        to: TriangleCoordinate,
        passable: (Cell<TriangleCoordinate, D>) -> Boolean
    ): List<TriangleCoordinate>? = pathfinder.findPath(this, from, to, passable)

    /** BFS-based distance (no closed-form shortcut for triangular topology). */
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

    override fun getRange(center: TriangleCoordinate, radius: Int): List<Cell<TriangleCoordinate, D>> =
        _cells.values.filter { distance(center, it.coordinate) <= radius }

    override fun getRing(center: TriangleCoordinate, radius: Int): List<Cell<TriangleCoordinate, D>> =
        if (radius == 0) listOfNotNull(_cells[center])
        else _cells.values.filter { distance(center, it.coordinate) == radius }

    override fun getLine(from: TriangleCoordinate, to: TriangleCoordinate): List<Cell<TriangleCoordinate, D>> =
        (findPath(from, to) { true } ?: listOfNotNull(from)).mapNotNull { _cells[it] }

    override fun placeNext(from: TriangleCoordinate, direction: TriangleNeighborDirection): Cell<TriangleCoordinate, D> {
        val target = neighborCoords(from)[direction]!!
        _cells[target]?.let { return it }
        val cell = Cell<TriangleCoordinate, D>(target)
        _cells[target] = cell
        if (target.col < minCol) minCol = target.col
        if (target.col > maxCol) maxCol = target.col
        if (target.row < minRow) minRow = target.row
        if (target.row > maxRow) maxRow = target.row
        return cell
    }

    override fun arithmeticCenter(): TriangleCoordinate =
        TriangleCoordinate((minCol + maxCol) / 2, (minRow + maxRow) / 2)

    override fun physicalCenter(): GridCenter<TriangleCoordinate> {
        val physical = if (_cells.isEmpty()) PhysicalCenter(0.0, 0.0) else PhysicalCenter(
            _cells.values.sumOf { toPhysical(it.coordinate).x } / _cells.size,
            _cells.values.sumOf { toPhysical(it.coordinate).y } / _cells.size
        )
        return GridCenter(physical, nearestCoordinate(physical))
    }

    /** Returns this triangular grid as alternating ASCII box art. */
    override fun toAsciiString(): String {
        if (_cells.isEmpty()) return "(empty grid)"

        val colRange = minCol..maxCol
        val rowRange = minRow..maxRow
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
                } else {
                    append("-".repeat(width + 2))
                    append('*')
                }
            }
        }.trimEnd()

        fun middle(row: Int) = buildString {
            for (col in colRange) {
                val coordinate = TriangleCoordinate(col, row)
                val label = _cells[coordinate].asciiLabel()
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
                    append("-".repeat(width + 2))
                    append('*')
                } else if (col == colRange.first) {
                    append("  ")
                    append("*".repeat(width))
                } else {
                    append("*".repeat(width - 1))
                }
            }
        }.trimEnd()

        return buildString {
            appendLine(top(rowRange.first))
            for (row in rowRange) {
                appendLine(middle(row))
                appendLine(bottom(row))
            }
        }.trimEnd()
    }

    /** Snaps an arbitrary physical position to the nearest existing coordinate. */
    fun nearestCoordinate(physical: PhysicalCenter): TriangleCoordinate =
        _cells.keys.minByOrNull { c ->
            val p = toPhysical(c); val dx = p.x - physical.x; val dy = p.y - physical.y
            dx * dx + dy * dy
        } ?: TriangleCoordinate(0, 0)

    /**
     * Physical centroid of a triangle cell.
     *
     * The old-style slot index is `col / 2` (integer division); within each slot
     * the UP centroid is shifted right by 0.166 and the DOWN by 0.333:
     * - `x = (col / 2) * 0.5 + (if UP then 0.166 else 0.333)`
     * - `y = row * triHeight + (if UP then 0.333 else 0.666)`
     */
    fun toPhysical(coordinate: TriangleCoordinate): PhysicalCenter {
        val slot = coordinate.col / 2
        val x = slot * 0.5 + if (coordinate.isUp) 0.166 else 0.333
        val y = coordinate.row * triHeight + if (coordinate.isUp) 0.333 else 0.666
        return PhysicalCenter(x, y)
    }

    private fun Cell<TriangleCoordinate, D>?.asciiLabel(): String =
        this?.data?.toString().orEmpty()

    private fun String.centered(width: Int): String {
        if (length >= width) return this
        val left = (width - length) / 2
        val right = width - length - left
        return " ".repeat(left) + this + " ".repeat(right)
    }
}

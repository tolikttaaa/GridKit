package io.gridkit.core.grid

import io.gridkit.core.core.*
import io.gridkit.core.pathfinding.AStarPathfinder
import kotlin.math.sqrt

/**
 * The three neighbor directions for a triangular cell.
 *
 * - **LEFT** / **RIGHT** — the two diagonal edge neighbors
 * - **VERTICAL** — the neighbor sharing the horizontal edge
 *   (above for UP-pointing triangles, below for DOWN-pointing)
 */
enum class TriangleNeighborDirection : GridDirection { LEFT, RIGHT, VERTICAL }

/**
 * A triangular grid where each `(col, row)` slot contains two triangles:
 * an UP-pointing (apex at top) and a DOWN-pointing (apex at bottom) one.
 *
 * **Neighbor rules:**
 * - `UP(col, row)` → LEFT = `DOWN(col-1, row)`, RIGHT = `DOWN(col, row)`, VERTICAL = `DOWN(col, row-1)`
 * - `DOWN(col, row)` → LEFT = `UP(col, row)`, RIGHT = `UP(col+1, row)`, VERTICAL = `UP(col, row+1)`
 *
 * @param D the optional cell-data payload type
 * @param width number of column slots
 * @param height number of row slots
 */
class TriangleGrid<D>(
    width: Int,
    height: Int
) : Grid<TriangleCoordinate, TriangleNeighborDirection, D> {

    private val _cells: MutableMap<TriangleCoordinate, Cell<TriangleCoordinate, D>> = mutableMapOf()
    override val cells: Map<TriangleCoordinate, Cell<TriangleCoordinate, D>> get() = _cells

    private var minCol = 0
    private var maxCol = width - 1
    private var minRow = 0
    private var maxRow = height - 1

    /** Physical height of one triangle row (default 1.0). */
    var triHeight: Double = 1.0

    private val pathfinder = AStarPathfinder<TriangleCoordinate, TriangleNeighborDirection, D>()

    init {
        for (row in 0 until height) {
            for (col in 0 until width) {
                _cells[TriangleCoordinate(col, row, TriangleDirection.UP)]   = Cell(TriangleCoordinate(col, row, TriangleDirection.UP))
                _cells[TriangleCoordinate(col, row, TriangleDirection.DOWN)] = Cell(TriangleCoordinate(col, row, TriangleDirection.DOWN))
            }
        }
    }

    // ── internal helpers ──────────────────────────────────────────────────────

    internal fun setCell(coordinate: TriangleCoordinate, cell: Cell<TriangleCoordinate, D>) {
        _cells[coordinate] = cell
    }

    private fun neighborCoords(c: TriangleCoordinate): Map<TriangleNeighborDirection, TriangleCoordinate> =
        when (c.pointing) {
            TriangleDirection.UP -> mapOf(
                TriangleNeighborDirection.LEFT     to TriangleCoordinate(c.col - 1, c.row, TriangleDirection.DOWN),
                TriangleNeighborDirection.RIGHT    to TriangleCoordinate(c.col,     c.row, TriangleDirection.DOWN),
                TriangleNeighborDirection.VERTICAL to TriangleCoordinate(c.col,     c.row - 1, TriangleDirection.DOWN)
            )
            TriangleDirection.DOWN -> mapOf(
                TriangleNeighborDirection.LEFT     to TriangleCoordinate(c.col,     c.row, TriangleDirection.UP),
                TriangleNeighborDirection.RIGHT    to TriangleCoordinate(c.col + 1, c.row, TriangleDirection.UP),
                TriangleNeighborDirection.VERTICAL to TriangleCoordinate(c.col,     c.row + 1, TriangleDirection.UP)
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

    /** BFS-based distance (no cube-coordinate shortcut for triangular topology). */
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
        val target = neighborCoords(from)[direction] ?: error("No neighbor in direction $direction for $from")
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
        TriangleCoordinate((minCol + maxCol) / 2, (minRow + maxRow) / 2, TriangleDirection.UP)

    override fun physicalCenter(): GridCenter<TriangleCoordinate> {
        val active = _cells.values.filter { it.state != CellState.Blocked }
        val physical = if (active.isEmpty()) PhysicalCenter(0.0, 0.0) else {
            PhysicalCenter(
                active.sumOf { toPhysical(it.coordinate).x } / active.size,
                active.sumOf { toPhysical(it.coordinate).y } / active.size
            )
        }
        return GridCenter(physical, nearestCoordinate(physical))
    }

    /** Snaps an arbitrary physical position to the nearest existing coordinate. */
    fun nearestCoordinate(physical: PhysicalCenter): TriangleCoordinate =
        _cells.keys.minByOrNull { c ->
            val p = toPhysical(c)
            val dx = p.x - physical.x; val dy = p.y - physical.y
            dx * dx + dy * dy
        } ?: TriangleCoordinate(0, 0, TriangleDirection.UP)

    /**
     * Physical centroid of a triangle cell:
     * - `x = col * 0.5 + (if UP then 0.166 else 0.333)`
     * - `y = row * triHeight + (if UP then 0.333 else 0.666)`
     */
    fun toPhysical(coordinate: TriangleCoordinate): PhysicalCenter {
        val x = coordinate.col * 0.5 + if (coordinate.pointing == TriangleDirection.UP) 0.166 else 0.333
        val y = coordinate.row * triHeight + if (coordinate.pointing == TriangleDirection.UP) 0.333 else 0.666
        return PhysicalCenter(x, y)
    }
}

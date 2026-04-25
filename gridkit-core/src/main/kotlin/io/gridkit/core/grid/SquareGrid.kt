package io.gridkit.core.grid

import io.gridkit.core.core.*
import io.gridkit.core.pathfinding.AStarPathfinder
import kotlin.math.abs
import kotlin.math.sqrt

/** Cardinal and diagonal movement directions on a square grid. */
enum class SquareDirection : GridDirection {
    UP, DOWN, LEFT, RIGHT,
    UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
}

/**
 * A rectangular square grid.
 *
 * @param D the optional cell-data payload type
 * @property diagonal when true, includes diagonal neighbors (8-connected);
 *   when false, only 4-connected cardinal neighbors are returned
 */
class SquareGrid<D>(
    width: Int,
    height: Int,
    val diagonal: Boolean = false
) : Grid<SquareCoordinate, SquareDirection, D> {

    private val _cells: MutableMap<SquareCoordinate, Cell<SquareCoordinate, D>> = mutableMapOf()
    override val cells: Map<SquareCoordinate, Cell<SquareCoordinate, D>> get() = _cells

    private var minCol = 0
    private var maxCol = width - 1
    private var minRow = 0
    private var maxRow = height - 1

    private val pathfinder = AStarPathfinder<SquareCoordinate, SquareDirection, D>()

    /** Physical size of each cell (default 1.0). */
    var cellSize: Double = 1.0

    init {
        for (row in 0 until height) {
            for (col in 0 until width) {
                val coord = SquareCoordinate(col, row)
                _cells[coord] = Cell(coord)
            }
        }
    }

    // ── internal helpers ──────────────────────────────────────────────────────

    internal fun setCell(coordinate: SquareCoordinate, cell: Cell<SquareCoordinate, D>) {
        _cells[coordinate] = cell
    }

    private val cardinalOffsets = listOf(
        SquareDirection.UP       to ( 0 to -1),
        SquareDirection.DOWN     to ( 0 to  1),
        SquareDirection.LEFT     to (-1 to  0),
        SquareDirection.RIGHT    to ( 1 to  0)
    )

    private val diagonalOffsets = listOf(
        SquareDirection.UP_LEFT    to (-1 to -1),
        SquareDirection.UP_RIGHT   to ( 1 to -1),
        SquareDirection.DOWN_LEFT  to (-1 to  1),
        SquareDirection.DOWN_RIGHT to ( 1 to  1)
    )

    private fun allOffsets(): List<Pair<SquareDirection, Pair<Int, Int>>> =
        if (diagonal) cardinalOffsets + diagonalOffsets else cardinalOffsets

    private fun directionOffset(direction: SquareDirection): Pair<Int, Int> = when (direction) {
        SquareDirection.UP         ->  0 to -1
        SquareDirection.DOWN       ->  0 to  1
        SquareDirection.LEFT       -> -1 to  0
        SquareDirection.RIGHT      ->  1 to  0
        SquareDirection.UP_LEFT    -> -1 to -1
        SquareDirection.UP_RIGHT   ->  1 to -1
        SquareDirection.DOWN_LEFT  -> -1 to  1
        SquareDirection.DOWN_RIGHT ->  1 to  1
    }

    // ── Grid<SquareCoordinate, SquareDirection, D> ────────────────────────────

    override fun getCell(coordinate: SquareCoordinate): Cell<SquareCoordinate, D>? = _cells[coordinate]

    override fun isValidCoordinate(coordinate: SquareCoordinate): Boolean = coordinate in _cells

    override fun getNeighbors(coordinate: SquareCoordinate): List<Cell<SquareCoordinate, D>> =
        allOffsets().mapNotNull { (_, offset) ->
            _cells[SquareCoordinate(coordinate.col + offset.first, coordinate.row + offset.second)]
        }

    override fun getDirectedNeighbors(coordinate: SquareCoordinate): Map<SquareDirection, Cell<SquareCoordinate, D>> =
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
        passable: (Cell<SquareCoordinate, D>) -> Boolean
    ): List<SquareCoordinate>? = pathfinder.findPath(this, from, to, passable)

    override fun distance(from: SquareCoordinate, to: SquareCoordinate): Int =
        if (diagonal) maxOf(abs(to.col - from.col), abs(to.row - from.row))
        else abs(to.col - from.col) + abs(to.row - from.row)

    override fun getRange(center: SquareCoordinate, radius: Int): List<Cell<SquareCoordinate, D>> =
        _cells.values.filter { distance(center, it.coordinate) <= radius }

    override fun getRing(center: SquareCoordinate, radius: Int): List<Cell<SquareCoordinate, D>> =
        if (radius == 0) listOfNotNull(_cells[center])
        else _cells.values.filter { distance(center, it.coordinate) == radius }

    override fun getLine(from: SquareCoordinate, to: SquareCoordinate): List<Cell<SquareCoordinate, D>> {
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

    override fun placeNext(from: SquareCoordinate, direction: SquareDirection): Cell<SquareCoordinate, D> {
        val (dc, dr) = directionOffset(direction)
        val target = SquareCoordinate(from.col + dc, from.row + dr)
        _cells[target]?.let { return it }
        val cell = Cell<SquareCoordinate, D>(target)
        _cells[target] = cell
        if (target.col < minCol) minCol = target.col
        if (target.col > maxCol) maxCol = target.col
        if (target.row < minRow) minRow = target.row
        if (target.row > maxRow) maxRow = target.row
        return cell
    }

    override fun arithmeticCenter(): SquareCoordinate =
        SquareCoordinate((minCol + maxCol) / 2, (minRow + maxRow) / 2)

    override fun physicalCenter(): GridCenter<SquareCoordinate> {
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
    fun nearestCoordinate(physical: PhysicalCenter): SquareCoordinate =
        _cells.keys.minByOrNull { c ->
            val p = toPhysical(c)
            val dx = p.x - physical.x; val dy = p.y - physical.y
            dx * dx + dy * dy
        } ?: SquareCoordinate(0, 0)

    /** Converts a coordinate to its physical (x, y) position. */
    fun toPhysical(coordinate: SquareCoordinate): PhysicalCenter =
        PhysicalCenter(coordinate.col * cellSize, coordinate.row * cellSize)
}

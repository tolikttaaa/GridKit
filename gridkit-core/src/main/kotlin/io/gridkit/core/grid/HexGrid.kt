package io.gridkit.core.grid

import io.gridkit.core.core.*
import io.gridkit.core.pathfinding.AStarPathfinder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The six named neighbor directions for offset-coordinate hexagonal grids.
 *
 * ```
 *   TL  TR
 * L        R
 *   DL  DR
 * ```
 */
enum class HexDirection : GridDirection {
    TOP_LEFT, TOP_RIGHT,
    LEFT, RIGHT,
    DOWN_LEFT, DOWN_RIGHT
}

/**
 * A hexagonal grid using **even-r offset coordinates**.
 *
 * - Row 0 is the topmost row; row increases downward.
 * - Col 0 is the leftmost cell; col increases rightward.
 * - Even rows are not horizontally shifted; odd rows are shifted right by
 *   half a hex width (visually staggered).
 *
 * @param D the optional cell-data payload type
 * @param rows number of rows in the initial bounding box
 * @param cols number of columns in the initial bounding box
 */
class HexGrid<D>(
    rows: Int,
    cols: Int
) : Grid<HexCoordinate, HexDirection, D> {

    private val _cells: MutableMap<HexCoordinate, Cell<HexCoordinate, D>> = mutableMapOf()
    override val cells: Map<HexCoordinate, Cell<HexCoordinate, D>> get() = _cells

    private var minRow = 0
    private var maxRow = rows - 1
    private var minCol = 0
    private var maxCol = cols - 1

    /** Physical width of one hex cell (default 1.0). */
    var hexWidth: Double = 1.0

    /** Physical height of one hex cell (default 1.0). */
    var hexHeight: Double = 1.0

    private val pathfinder = AStarPathfinder<HexCoordinate, HexDirection, D>()

    init {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val coord = HexCoordinate(row, col)
                _cells[coord] = Cell(coord)
            }
        }
    }

    // ── internal helpers ──────────────────────────────────────────────────────

    internal fun setCell(coordinate: HexCoordinate, cell: Cell<HexCoordinate, D>) {
        _cells[coordinate] = cell
    }

    private fun neighborOffsets(row: Int): Map<HexDirection, Pair<Int, Int>> =
        if (row % 2 == 0) mapOf(
            HexDirection.TOP_LEFT   to (-1 to -1),
            HexDirection.TOP_RIGHT  to (-1 to  0),
            HexDirection.LEFT       to ( 0 to -1),
            HexDirection.RIGHT      to ( 0 to  1),
            HexDirection.DOWN_LEFT  to ( 1 to -1),
            HexDirection.DOWN_RIGHT to ( 1 to  0)
        ) else mapOf(
            HexDirection.TOP_LEFT   to (-1 to  0),
            HexDirection.TOP_RIGHT  to (-1 to  1),
            HexDirection.LEFT       to ( 0 to -1),
            HexDirection.RIGHT      to ( 0 to  1),
            HexDirection.DOWN_LEFT  to ( 1 to  0),
            HexDirection.DOWN_RIGHT to ( 1 to  1)
        )

    // ── Grid<HexCoordinate, HexDirection, D> ─────────────────────────────────

    override fun getCell(coordinate: HexCoordinate): Cell<HexCoordinate, D>? = _cells[coordinate]

    override fun isValidCoordinate(coordinate: HexCoordinate): Boolean = coordinate in _cells

    override fun getNeighbors(coordinate: HexCoordinate): List<Cell<HexCoordinate, D>> =
        neighborOffsets(coordinate.row).values.mapNotNull { (dr, dc) ->
            _cells[HexCoordinate(coordinate.row + dr, coordinate.col + dc)]
        }

    override fun getDirectedNeighbors(coordinate: HexCoordinate): Map<HexDirection, Cell<HexCoordinate, D>> =
        neighborOffsets(coordinate.row).mapNotNull { (dir, offset) ->
            val nc = HexCoordinate(coordinate.row + offset.first, coordinate.col + offset.second)
            _cells[nc]?.let { dir to it }
        }.toMap()

    override fun getNeighbor(coordinate: HexCoordinate, direction: HexDirection): HexCoordinate? {
        val (dr, dc) = neighborOffsets(coordinate.row)[direction]!!
        val nc = HexCoordinate(coordinate.row + dr, coordinate.col + dc)
        return if (nc in _cells) nc else null
    }

    override fun findPath(
        from: HexCoordinate,
        to: HexCoordinate,
        passable: (Cell<HexCoordinate, D>) -> Boolean
    ): List<HexCoordinate>? = pathfinder.findPath(this, from, to, passable)

    /** Hex distance via cube-coordinate conversion for correctness across offset rows. */
    override fun distance(from: HexCoordinate, to: HexCoordinate): Int {
        val (fx, fy, fz) = toCube(from)
        val (tx, ty, tz) = toCube(to)
        return maxOf(abs(fx - tx), abs(fy - ty), abs(fz - tz))
    }

    private fun toCube(c: HexCoordinate): Triple<Int, Int, Int> {
        val x = c.col - (c.row - (c.row and 1)) / 2
        val z = c.row
        return Triple(x, -x - z, z)
    }

    private fun cubeRound(x: Double, y: Double, z: Double): Triple<Int, Int, Int> {
        var rx = x.toLong().toInt(); var ry = y.toLong().toInt(); var rz = z.toLong().toInt()
        val xd = abs(rx - x); val yd = abs(ry - y); val zd = abs(rz - z)
        when { xd > yd && xd > zd -> rx = -ry - rz; yd > zd -> ry = -rx - rz; else -> rz = -rx - ry }
        return Triple(rx, ry, rz)
    }

    private fun fromCube(cube: Triple<Int, Int, Int>): HexCoordinate {
        val row = cube.third
        return HexCoordinate(row, cube.first + (row - (row and 1)) / 2)
    }

    override fun getRange(center: HexCoordinate, radius: Int): List<Cell<HexCoordinate, D>> =
        _cells.values.filter { distance(center, it.coordinate) <= radius }

    override fun getRing(center: HexCoordinate, radius: Int): List<Cell<HexCoordinate, D>> =
        if (radius == 0) listOfNotNull(_cells[center])
        else _cells.values.filter { distance(center, it.coordinate) == radius }

    /** Uses cube-coordinate interpolation for accurate straight-line traversal. */
    override fun getLine(from: HexCoordinate, to: HexCoordinate): List<Cell<HexCoordinate, D>> {
        val n = distance(from, to)
        if (n == 0) return listOfNotNull(_cells[from])
        val (fx, fy, fz) = toCube(from); val (tx, ty, tz) = toCube(to)
        return (0..n).mapNotNull { i ->
            val t = i.toDouble() / n
            _cells[fromCube(cubeRound(fx + (tx - fx) * t, fy + (ty - fy) * t, fz + (tz - fz) * t))]
        }
    }

    override fun placeNext(from: HexCoordinate, direction: HexDirection): Cell<HexCoordinate, D> {
        val (dr, dc) = neighborOffsets(from.row)[direction]!!
        val target = HexCoordinate(from.row + dr, from.col + dc)
        _cells[target]?.let { return it }
        val cell = Cell<HexCoordinate, D>(target)
        _cells[target] = cell
        if (target.row < minRow) minRow = target.row
        if (target.row > maxRow) maxRow = target.row
        if (target.col < minCol) minCol = target.col
        if (target.col > maxCol) maxCol = target.col
        return cell
    }

    override fun arithmeticCenter(): HexCoordinate =
        HexCoordinate((minRow + maxRow) / 2, (minCol + maxCol) / 2)

    override fun physicalCenter(): GridCenter<HexCoordinate> {
        val physical = if (_cells.isEmpty()) PhysicalCenter(0.0, 0.0) else {
            PhysicalCenter(
                _cells.values.sumOf { toPhysical(it.coordinate).x } / _cells.size,
                _cells.values.sumOf { toPhysical(it.coordinate).y } / _cells.size
            )
        }
        return GridCenter(physical, nearestCoordinate(physical))
    }

    /** Returns this hex grid as staggered ASCII box art. */
    override fun toAsciiString(): String {
        if (_cells.isEmpty()) return "(empty grid)"

        val rowRange = minRow..maxRow
        val colRange = minCol..maxCol
        val dashWidths = colRange.associateWith { col ->
            maxOf(1, rowRange.maxOf { row -> _cells[HexCoordinate(row, col)].asciiLabel().length })
        }
        val indent = " ".repeat((dashWidths.values.max() + 3) / 2)

        fun topOrBottom(row: Int) = buildString {
            if (row % 2 != 0) append(indent)
            if (row % 2 != 0) append("  ")
            for (col in colRange) {
                val dash = dashWidths.getValue(col)
                append("  *")
                append("-".repeat(dash))
                append("*   ")
            }
        }.trimEnd()

        fun upper(row: Int) = buildString {
            if (row % 2 != 0) append(indent)
            for (col in colRange) {
                append(" /")
                append(" ".repeat(dashWidths.getValue(col) + 2))
                append("\\  ")
            }
        }.trimEnd()

        fun transitionUpper() = buildString {
            for (col in colRange) {
                append(" /")
                append(" ".repeat(dashWidths.getValue(col) + 2))
                append("\\  ")
            }
            append(" /")
        }.trimEnd()

        fun middle(row: Int) = buildString {
            if (row % 2 != 0) append(indent)
            for (col in colRange) {
                val label = _cells[HexCoordinate(row, col)].asciiLabel()
                val body = label.centered(dashWidths.getValue(col) + 4)
                if (col == colRange.first) append("*") else append("-*")
                append(body)
                append("*")
            }
            if (row % 2 == 0 && minRow < maxRow) {
                append("-*")
            }
        }

        fun lower(row: Int) = buildString {
            if (row % 2 != 0) append(indent)
            if (row % 2 != 0) append("  ")
            for (col in colRange) {
                append(" \\")
                append(" ".repeat(dashWidths.getValue(col) + 2))
                append("/  ")
            }
            if (row < maxRow && row % 2 == 0) {
                append(" \\")
            }
        }.trimEnd()

        fun sharedOddRowMiddle(row: Int) = buildString {
            append("  ")
            for (col in colRange) {
                val dash = dashWidths.getValue(col)
                val label = _cells[HexCoordinate(row, col)].asciiLabel()
                append("*")
                append("-".repeat(dash))
                append("*")
                append(label.centered(dash + 4))
            }
            append("*")
        }

        return buildString {
            for (row in rowRange) {
                if (row == minRow) {
                    appendLine(topOrBottom(row))
                    appendLine(upper(row))
                    appendLine(middle(row))
                } else if (row % 2 != 0) {
                    appendLine(sharedOddRowMiddle(row))
                    if (row == maxRow) {
                        appendLine(lower(row))
                        appendLine(topOrBottom(row))
                    } else {
                        appendLine(transitionUpper())
                    }
                    continue
                } else {
                    appendLine(middle(row))
                }
                appendLine(lower(row))
                if (row == maxRow) {
                    appendLine(topOrBottom(row))
                }
            }
        }.trimEnd()
    }

    /** Snaps an arbitrary physical position to the nearest existing coordinate. */
    fun nearestCoordinate(physical: PhysicalCenter): HexCoordinate =
        _cells.keys.minByOrNull { c ->
            val p = toPhysical(c)
            val dx = p.x - physical.x; val dy = p.y - physical.y
            dx * dx + dy * dy
        } ?: HexCoordinate(0, 0)

    /**
     * Converts a hex coordinate to its physical (x, y) position using
     * flat-top offset mapping:
     * - `x = col * hexWidth + (if odd row then hexWidth / 2 else 0)`
     * - `y = row * hexHeight * 0.75`
     */
    fun toPhysical(coordinate: HexCoordinate): PhysicalCenter {
        val x = coordinate.col * hexWidth + if (coordinate.row % 2 != 0) hexWidth / 2.0 else 0.0
        val y = coordinate.row * hexHeight * 0.75
        return PhysicalCenter(x, y)
    }

    private fun Cell<HexCoordinate, D>?.asciiLabel(): String =
        this?.data?.toString().orEmpty()

    private fun String.centered(width: Int): String {
        if (length >= width) return this
        val left = (width - length) / 2
        val right = width - length - left
        return " ".repeat(left) + this + " ".repeat(right)
    }
}

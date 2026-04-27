package io.gridkit.playground.minesweeper

import io.gridkit.core.core.Grid
import io.gridkit.core.core.GridCoordinate
import io.gridkit.core.core.GridDirection
import kotlin.random.Random

/**
 * Topology-agnostic Minesweeper game.
 *
 * Works on any [Grid] — square, hex, or triangle — without knowing the
 * underlying coordinate or direction types. The grid is used purely for
 * topology (neighbour queries); game state is maintained separately.
 *
 * @param grid the board; only topology methods are used (no cell data required)
 * @param mineCount number of mines to place randomly
 * @param random source of randomness; supply a seeded instance for reproducibility
 */
class MinesweeperGame<C : GridCoordinate, Dir : GridDirection>(
    val grid: Grid<C, Dir, *>,
    mineCount: Int,
    random: Random = Random.Default
) {
    /** Visibility state of a single cell. */
    enum class Visibility { HIDDEN, REVEALED, FLAGGED }

    /** Full game state of a single cell. */
    data class CellInfo(
        val isMine: Boolean = false,
        val adjacentMines: Int = 0,
        val visibility: Visibility = Visibility.HIDDEN
    )

    /** Result returned by [reveal]. */
    sealed class RevealResult {
        /** The cell had no adjacent mines — a cascade reveal was performed. */
        object Cascade : RevealResult()
        /** The cell is safe with [count] adjacent mines. */
        data class Number(val count: Int) : RevealResult()
        /** A mine was hit — game is now lost. */
        object Mine : RevealResult()
        /** Cell was already revealed or flagged — nothing happened. */
        object NoOp : RevealResult()
    }

    private val state = mutableMapOf<C, CellInfo>()

    /** True after a mine has been revealed. */
    var isLost: Boolean = false
        private set

    init {
        val coords = grid.cells.keys.toMutableList().also { it.shuffle(random) }
        val mines = coords.take(mineCount).toSet()

        for (coord in grid.cells.keys) {
            state[coord] = CellInfo(isMine = coord in mines)
        }
        // Calculate adjacency counts
        for (coord in grid.cells.keys) {
            if (state[coord]!!.isMine) continue
            val adj = grid.getNeighbors(coord).count { state[it.coordinate]?.isMine == true }
            state[coord] = state[coord]!!.copy(adjacentMines = adj)
        }
    }

    /** Returns the current [CellInfo] for [coordinate]. */
    fun info(coordinate: C): CellInfo? = state[coordinate]

    /**
     * Reveals [coordinate].
     *
     * - Mine → marks game lost, returns [RevealResult.Mine].
     * - Number → reveals that cell only, returns [RevealResult.Number].
     * - Empty (0 adjacent mines) → cascade-reveals all connected empty cells,
     *   returns [RevealResult.Cascade].
     * - Already revealed or flagged → returns [RevealResult.NoOp].
     */
    fun reveal(coordinate: C): RevealResult {
        val info = state[coordinate] ?: return RevealResult.NoOp
        if (info.visibility != Visibility.HIDDEN) return RevealResult.NoOp

        if (info.isMine) {
            state[coordinate] = info.copy(visibility = Visibility.REVEALED)
            isLost = true
            return RevealResult.Mine
        }

        if (info.adjacentMines > 0) {
            state[coordinate] = info.copy(visibility = Visibility.REVEALED)
            return RevealResult.Number(info.adjacentMines)
        }

        // Cascade flood-fill for empty cells
        val queue = ArrayDeque<C>().also { it.add(coordinate) }
        val visited = mutableSetOf<C>()
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (!visited.add(cur)) continue
            val curInfo = state[cur] ?: continue
            if (curInfo.isMine) continue
            state[cur] = curInfo.copy(visibility = Visibility.REVEALED)
            if (curInfo.adjacentMines == 0) {
                grid.getNeighbors(cur)
                    .filter { state[it.coordinate]?.visibility == Visibility.HIDDEN }
                    .forEach { queue.add(it.coordinate) }
            }
        }
        return RevealResult.Cascade
    }

    /**
     * Toggles a flag on [coordinate].
     * HIDDEN → FLAGGED → HIDDEN. Has no effect on revealed cells.
     */
    fun flag(coordinate: C) {
        val info = state[coordinate] ?: return
        state[coordinate] = when (info.visibility) {
            Visibility.HIDDEN  -> info.copy(visibility = Visibility.FLAGGED)
            Visibility.FLAGGED -> info.copy(visibility = Visibility.HIDDEN)
            else               -> return
        }
    }

    /** True when all non-mine cells have been revealed. */
    fun isWon(): Boolean =
        !isLost && state.all { (_, info) -> info.isMine || info.visibility == Visibility.REVEALED }

    /** Number of mines not yet flagged. */
    fun remainingMines(): Int =
        state.values.count { it.isMine } - state.values.count {
            it.isMine && it.visibility == Visibility.FLAGGED
        }
}

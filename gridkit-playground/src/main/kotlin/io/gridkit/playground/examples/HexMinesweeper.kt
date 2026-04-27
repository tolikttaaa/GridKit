package io.gridkit.playground.examples

import io.gridkit.core.dsl.hexGrid
import io.gridkit.core.core.HexCoordinate
import io.gridkit.playground.minesweeper.MinesweeperGame
import io.gridkit.playground.minesweeper.MinesweeperGame.Visibility
import kotlin.random.Random

/** Runs a short Hex-grid Minesweeper demo and returns the transcript. */
fun hexMinesweeperDemo(): String = buildString {
    val grid = hexGrid<Nothing>(rows = 7, cols = 7)
    val game = MinesweeperGame(grid, mineCount = 8, random = Random(7))

    appendLine("=== Hex Minesweeper (7×7, 8 mines) ===")
    appendLine(renderHex(game))

    appendLine("Reveal (3,3)…")
    game.reveal(HexCoordinate(3, 3))
    appendLine(renderHex(game))

    appendLine("Flag (0,0) as suspected mine…")
    game.flag(HexCoordinate(0, 0))
    appendLine(renderHex(game))

    appendLine("Mines remaining: ${game.remainingMines()}")
    appendLine("Won: ${game.isWon()}  |  Lost: ${game.isLost}")
}

private fun renderHex(game: MinesweeperGame<HexCoordinate, *>): String = buildString {
    val coords = game.grid.cells.keys.filterIsInstance<HexCoordinate>()
    val maxRow = coords.maxOf { it.row }
    val maxCol = coords.maxOf { it.col }
    for (row in 0..maxRow) {
        if (row % 2 != 0) append(" ")       // odd-row stagger
        for (col in 0..maxCol) {
            append(cellGlyph(game.info(HexCoordinate(row, col))))
            append(' ')
        }
        append('\n')
    }
}.trimEnd()

private fun cellGlyph(info: MinesweeperGame.CellInfo?): String = when {
    info == null                           -> " "
    info.visibility == Visibility.FLAGGED  -> "F"
    info.visibility == Visibility.HIDDEN   -> "?"
    info.isMine                            -> "*"
    info.adjacentMines > 0                 -> info.adjacentMines.toString()
    else                                   -> "."
}

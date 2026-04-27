package io.gridkit.playground.examples

import io.gridkit.core.dsl.squareGrid
import io.gridkit.core.core.SquareCoordinate
import io.gridkit.playground.minesweeper.MinesweeperGame
import io.gridkit.playground.minesweeper.MinesweeperGame.Visibility
import kotlin.random.Random

/** Runs a short Square-grid Minesweeper demo and returns the transcript. */
fun squareMinesweeperDemo(): String = buildString {
    val grid = squareGrid<Nothing>(9, 9)
    val game = MinesweeperGame(grid, mineCount = 10, random = Random(42))

    appendLine("=== Square Minesweeper (9×9, 10 mines) ===")
    appendLine(renderSquare(game))

    appendLine("Reveal (4,4)…")
    game.reveal(SquareCoordinate(4, 4))
    appendLine(renderSquare(game))

    appendLine("Flag (0,0) as suspected mine…")
    game.flag(SquareCoordinate(0, 0))
    appendLine(renderSquare(game))

    appendLine("Mines remaining: ${game.remainingMines()}")
    appendLine("Won: ${game.isWon()}  |  Lost: ${game.isLost}")
}

private fun renderSquare(game: MinesweeperGame<SquareCoordinate, *>): String = buildString {
    val coords = game.grid.cells.keys.filterIsInstance<SquareCoordinate>()
    val maxCol = coords.maxOf { it.col }
    val maxRow = coords.maxOf { it.row }
    val renderGrid = squareGrid<String>(width = maxCol + 1, height = maxRow + 1) {
        for (coordinate in coords) {
            place(coordinate, data = cellLabel(game.info(coordinate)))
        }
    }
    append(renderGrid.toAsciiString())
}

private fun cellLabel(info: MinesweeperGame.CellInfo?): String = when {
    info == null                           -> " "
    info.visibility == Visibility.FLAGGED  -> "F"
    info.visibility == Visibility.HIDDEN   -> "?"
    info.isMine                            -> "*"
    info.adjacentMines > 0                 -> info.adjacentMines.toString()
    else                                   -> "."
}

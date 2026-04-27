package io.gridkit.playground.examples

import io.gridkit.core.dsl.triangleGrid
import io.gridkit.core.core.TriangleCoordinate
import io.gridkit.core.core.isUp
import io.gridkit.playground.minesweeper.MinesweeperGame
import io.gridkit.playground.minesweeper.MinesweeperGame.Visibility
import kotlin.random.Random

/** Runs a short Triangle-grid Minesweeper demo and returns the transcript. */
fun triangleMinesweeperDemo(): String = buildString {
    // 12 cols × 4 rows = 48 triangles
    val grid = triangleGrid<Nothing>(cols = 12, rows = 4)
    val game = MinesweeperGame(grid, mineCount = 6, random = Random(99))

    appendLine("=== Triangle Minesweeper (12 cols × 4 rows, 6 mines) ===")
    appendLine("  even col = UP /\\   odd col = DOWN \\/")
    appendLine(renderTriangle(game))

    appendLine("Reveal (6,2)…")
    game.reveal(TriangleCoordinate(6, 2))
    appendLine(renderTriangle(game))

    appendLine("Flag (0,0) as suspected mine…")
    game.flag(TriangleCoordinate(0, 0))
    appendLine(renderTriangle(game))

    appendLine("Mines remaining: ${game.remainingMines()}")
    appendLine("Won: ${game.isWon()}  |  Lost: ${game.isLost}")
}

private fun renderTriangle(game: MinesweeperGame<TriangleCoordinate, *>): String = buildString {
    val coords = game.grid.cells.keys.filterIsInstance<TriangleCoordinate>()
    val maxCol = coords.maxOf { it.col }
    val maxRow = coords.maxOf { it.row }
    for (row in 0..maxRow) {
        for (col in 0..maxCol) {
            val info = game.info(TriangleCoordinate(col, row))
            val up = col % 2 == 0
            append(cellGlyph(info, up))
        }
        append('\n')
    }
}.trimEnd()

private fun cellGlyph(info: MinesweeperGame.CellInfo?, isUp: Boolean): String = when {
    info == null                           -> " "
    info.visibility == Visibility.FLAGGED  -> "F"
    info.visibility == Visibility.HIDDEN   -> if (isUp) "^" else "v"
    info.isMine                            -> "*"
    info.adjacentMines > 0                 -> info.adjacentMines.toString()
    else                                   -> if (isUp) "/" else "\\"
}

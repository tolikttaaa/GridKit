package io.gridkit.playground

import io.gridkit.playground.examples.hexMinesweeperDemo
import io.gridkit.playground.examples.squareMinesweeperDemo
import io.gridkit.playground.examples.triangleMinesweeperDemo

/**
 * Runs Minesweeper demos on all three grid topologies.
 *
 * Each demo uses the same [MinesweeperGame] engine and delegates console
 * rendering to each grid's `toAsciiString()` implementation.
 */
fun main() {
    println(squareMinesweeperDemo())
    println()
    println(hexMinesweeperDemo())
    println()
    println(triangleMinesweeperDemo())
}

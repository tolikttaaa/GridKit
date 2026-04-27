package io.gridkit.playground

import io.gridkit.playground.examples.hexMinesweeperDemo
import io.gridkit.playground.examples.squareMinesweeperDemo
import io.gridkit.playground.examples.triangleMinesweeperDemo

/**
 * Runs Minesweeper demos on all three grid topologies.
 *
 * Each demo uses the same [MinesweeperGame] engine — only the grid and
 * the topology-specific renderer differ, showing that game logic is
 * fully decoupled from the underlying grid type.
 */
fun main() {
    println(squareMinesweeperDemo())
    println()
    println(hexMinesweeperDemo())
    println()
    println(triangleMinesweeperDemo())
}

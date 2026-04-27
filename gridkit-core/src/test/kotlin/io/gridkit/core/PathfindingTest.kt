package io.gridkit.core

import io.gridkit.core.core.*
import io.gridkit.core.dsl.hexGrid
import io.gridkit.core.dsl.squareGrid
import io.gridkit.core.grid.HexGrid
import io.gridkit.core.grid.SquareGrid
import io.gridkit.core.grid.TriangleGrid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathfindingTest {

    // ── Square pathfinding ────────────────────────────────────────────────────

    @Test
    fun `finds path on open square grid`() {
        val grid = SquareGrid<Nothing>(5, 5)
        val path = grid.findPath(SquareCoordinate(0, 0), SquareCoordinate(4, 4))
        assertNotNull(path)
        assertEquals(SquareCoordinate(0, 0), path.first())
        assertEquals(SquareCoordinate(4, 4), path.last())
    }

    @Test
    fun `path to same cell is length 1`() {
        val grid = SquareGrid<Nothing>(5, 5)
        assertEquals(1, grid.findPath(SquareCoordinate(2, 2), SquareCoordinate(2, 2))?.size)
    }

    @Test
    fun `path goes around obstacle`() {
        val grid = squareGrid<Nothing>(5, 5) {
            block(SquareCoordinate(1, 0))
            block(SquareCoordinate(1, 1))
            block(SquareCoordinate(1, 2))
        }
        val path = grid.findPath(SquareCoordinate(0, 0), SquareCoordinate(2, 0))
        assertNotNull(path)
        assertTrue(path.none { grid.getCell(it)?.state == CellState.Blocked })
        assertEquals(SquareCoordinate(0, 0), path.first())
        assertEquals(SquareCoordinate(2, 0), path.last())
    }

    @Test
    fun `returns null when destination is blocked`() {
        val grid = squareGrid<Nothing>(5, 5) { block(SquareCoordinate(4, 4)) }
        assertNull(grid.findPath(SquareCoordinate(0, 0), SquareCoordinate(4, 4)))
    }

    @Test
    fun `returns null when path is fully blocked`() {
        val grid = squareGrid<Nothing>(5, 1) { block(SquareCoordinate(2, 0)) }
        assertNull(grid.findPath(SquareCoordinate(0, 0), SquareCoordinate(4, 0)))
    }

    @Test
    fun `path respects custom passable predicate`() {
        val grid = SquareGrid<Nothing>(3, 3)
        val blocked = SquareCoordinate(1, 1)
        val path = grid.findPath(SquareCoordinate(0, 1), SquareCoordinate(2, 1)) { cell ->
            cell.coordinate != blocked
        }
        assertNotNull(path)
        assertTrue(path.none { it == blocked })
        assertEquals(SquareCoordinate(0, 1), path.first())
        assertEquals(SquareCoordinate(2, 1), path.last())
    }

    // ── Hex pathfinding ───────────────────────────────────────────────────────

    @Test
    fun `finds path on open hex grid`() {
        val grid = HexGrid<Nothing>(5, 5)
        val path = grid.findPath(HexCoordinate(0, 0), HexCoordinate(4, 4))
        assertNotNull(path)
        assertEquals(HexCoordinate(0, 0), path.first())
        assertEquals(HexCoordinate(4, 4), path.last())
    }

    @Test
    fun `hex path returns null through complete wall`() {
        val grid = hexGrid<Nothing>(5, 5) {
            block(HexCoordinate(2, 0)); block(HexCoordinate(2, 1))
            block(HexCoordinate(2, 2)); block(HexCoordinate(2, 3))
            block(HexCoordinate(2, 4))
        }
        assertNull(grid.findPath(HexCoordinate(0, 2), HexCoordinate(4, 2)))
    }

    // ── Triangle pathfinding ──────────────────────────────────────────────────

    @Test
    fun `finds path on triangle grid`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        val from = TriangleCoordinate(0, 0)  // even col = UP
        val to   = TriangleCoordinate(7, 3)  // odd  col = DOWN
        val path = grid.findPath(from, to)
        assertNotNull(path)
        assertEquals(from, path.first())
        assertEquals(to, path.last())
    }

    @Test
    fun `path length is at least manhattan distance`() {
        val grid = SquareGrid<Nothing>(10, 10)
        val from = SquareCoordinate(0, 0); val to = SquareCoordinate(5, 5)
        val path = grid.findPath(from, to)!!
        assertTrue(path.size - 1 >= grid.distance(from, to))
    }
}

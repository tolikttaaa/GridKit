package io.gridkit.core

import io.gridkit.core.core.*
import io.gridkit.core.direction.*
import io.gridkit.core.dsl.diamondGrid
import io.gridkit.core.extensions.isConnected
import io.gridkit.core.grid.DiamondGrid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiamondGridTest {

    // ── Initialisation ────────────────────────────────────────────────────────

    @Test
    fun `DiamondGrid(3,3) has 9 cells`() {
        assertEquals(9, DiamondGrid<Nothing>(3, 3).cells.size)
    }

    @Test
    fun `DiamondGrid(1,1) has 1 cell`() {
        assertEquals(1, DiamondGrid<Nothing>(1, 1).cells.size)
    }

    // ── Neighbor counts ───────────────────────────────────────────────────────

    @Test
    fun `interior cell has 4 neighbors`() {
        val grid = DiamondGrid<Nothing>(4, 4)
        assertEquals(4, grid.getNeighbors(DiamondCoordinate(1, 1)).size)
    }

    @Test
    fun `corner cell has 2 neighbors`() {
        val grid = DiamondGrid<Nothing>(4, 4)
        assertEquals(2, grid.getNeighbors(DiamondCoordinate(0, 0)).size)
    }

    @Test
    fun `edge cell has 3 neighbors`() {
        val grid = DiamondGrid<Nothing>(4, 4)
        assertEquals(3, grid.getNeighbors(DiamondCoordinate(0, 1)).size)
    }

    // ── Neighbor offsets ──────────────────────────────────────────────────────

    @Test
    fun `TOP_LEFT neighbor is (row, col-1)`() {
        val grid = DiamondGrid<Nothing>(4, 4)
        assertEquals(DiamondCoordinate(1, 0), grid.getNeighbor(DiamondCoordinate(1, 1), DiamondEdgeDirection.TOP_LEFT))
    }

    @Test
    fun `TOP_RIGHT neighbor is (row-1, col)`() {
        val grid = DiamondGrid<Nothing>(4, 4)
        assertEquals(DiamondCoordinate(0, 1), grid.getNeighbor(DiamondCoordinate(1, 1), DiamondEdgeDirection.TOP_RIGHT))
    }

    @Test
    fun `BOTTOM_LEFT neighbor is (row+1, col)`() {
        val grid = DiamondGrid<Nothing>(4, 4)
        assertEquals(DiamondCoordinate(2, 1), grid.getNeighbor(DiamondCoordinate(1, 1), DiamondEdgeDirection.BOTTOM_LEFT))
    }

    @Test
    fun `BOTTOM_RIGHT neighbor is (row, col+1)`() {
        val grid = DiamondGrid<Nothing>(4, 4)
        assertEquals(DiamondCoordinate(1, 2), grid.getNeighbor(DiamondCoordinate(1, 1), DiamondEdgeDirection.BOTTOM_RIGHT))
    }

    @Test
    fun `getNeighbor returns null at boundary`() {
        val grid = DiamondGrid<Nothing>(4, 4)
        assertNull(grid.getNeighbor(DiamondCoordinate(0, 0), DiamondEdgeDirection.TOP_LEFT))
        assertNull(grid.getNeighbor(DiamondCoordinate(0, 0), DiamondEdgeDirection.TOP_RIGHT))
    }

    // ── Distance ─────────────────────────────────────────────────────────────

    @Test
    fun `distance to self is 0`() {
        assertEquals(0, DiamondGrid<Nothing>(4, 4).distance(DiamondCoordinate(1, 1), DiamondCoordinate(1, 1)))
    }

    @Test
    fun `distance to direct neighbor is 1`() {
        val grid = DiamondGrid<Nothing>(4, 4)
        assertEquals(1, grid.distance(DiamondCoordinate(1, 1), DiamondCoordinate(1, 2)))
        assertEquals(1, grid.distance(DiamondCoordinate(1, 1), DiamondCoordinate(0, 1)))
    }

    @Test
    fun `distance is Manhattan — each step moves one row or one col`() {
        val grid = DiamondGrid<Nothing>(5, 5)
        // (0,0) to (2,1): 2 row-steps + 1 col-step = 3
        assertEquals(3, grid.distance(DiamondCoordinate(0, 0), DiamondCoordinate(2, 1)))
        // (0,0) to (2,2): 2 + 2 = 4
        assertEquals(4, grid.distance(DiamondCoordinate(0, 0), DiamondCoordinate(2, 2)))
    }

    // ── Range / Ring / Line ───────────────────────────────────────────────────

    @Test
    fun `getRange radius 0 returns only center`() {
        assertEquals(1, DiamondGrid<Nothing>(4, 4).getRange(DiamondCoordinate(1, 1), 0).size)
    }

    @Test
    fun `getRange radius 1 includes center and 4 neighbors`() {
        assertEquals(5, DiamondGrid<Nothing>(4, 4).getRange(DiamondCoordinate(1, 1), 1).size)
    }

    @Test
    fun `getRing radius 0 returns only center`() {
        assertEquals(1, DiamondGrid<Nothing>(4, 4).getRing(DiamondCoordinate(1, 1), 0).size)
    }

    @Test
    fun `getRing radius 1 returns 4 cells for interior cell`() {
        assertEquals(4, DiamondGrid<Nothing>(4, 4).getRing(DiamondCoordinate(1, 1), 1).size)
    }

    @Test
    fun `getLine returns straight path`() {
        val grid = DiamondGrid<Nothing>(5, 5)
        val line = grid.getLine(DiamondCoordinate(0, 0), DiamondCoordinate(0, 3))
        assertEquals(4, line.size)
        assertEquals(DiamondCoordinate(0, 0), line.first().coordinate)
        assertEquals(DiamondCoordinate(0, 3), line.last().coordinate)
    }

    // ── placeNext ─────────────────────────────────────────────────────────────

    @Test
    fun `placeNext creates new cell`() {
        val grid = DiamondGrid<Nothing>(1, 1)
        val cell = grid.placeNext(DiamondCoordinate(0, 0), DiamondEdgeDirection.BOTTOM_RIGHT)
        assertEquals(DiamondCoordinate(0, 1), cell.coordinate)
    }

    @Test
    fun `placeNext is idempotent`() {
        val grid = DiamondGrid<Nothing>(3, 3)
        val a = grid.placeNext(DiamondCoordinate(1, 1), DiamondEdgeDirection.BOTTOM_RIGHT)
        val b = grid.placeNext(DiamondCoordinate(1, 1), DiamondEdgeDirection.BOTTOM_RIGHT)
        assertTrue(a === b)
    }

    @Test
    fun `placeNext expands grid size`() {
        val grid = DiamondGrid<Nothing>(1, 1)
        grid.placeNext(DiamondCoordinate(0, 0), DiamondEdgeDirection.BOTTOM_LEFT)
        assertEquals(2, grid.cells.size)
    }

    // ── Center calculations ───────────────────────────────────────────────────

    @Test
    fun `arithmeticCenter of 4x4 diamond grid`() {
        assertEquals(DiamondCoordinate(1, 1), DiamondGrid<Nothing>(4, 4).arithmeticCenter())
    }

    @Test
    fun `physicalCenter coordinate is valid`() {
        val grid = DiamondGrid<Nothing>(3, 3)
        assertTrue(grid.isValidCoordinate(grid.physicalCenter().coordinate))
    }

    @Test
    fun `physicalCenter of symmetric grid is at origin`() {
        val grid = DiamondGrid<Nothing>(1, 1)
        val c = grid.physicalCenter()
        assertEquals(0.0, c.physical.x, 1e-9)
        assertEquals(0.0, c.physical.y, 1e-9)
    }

    // ── DSL ───────────────────────────────────────────────────────────────────

    @Test
    fun `DSL builder places cell data`() {
        val grid = diamondGrid<String>(3, 3) { place(DiamondCoordinate(1, 1), data = "X") }
        assertEquals("X", grid.getCell(DiamondCoordinate(1, 1))?.data)
    }

    @Test
    fun `sparse diamond grid via DSL`() {
        val grid = diamondGrid<String> {
            place(DiamondCoordinate(0, 0), data = "A")
            place(DiamondCoordinate(2, 2), data = "B")
        }
        assertEquals(2, grid.cells.size)
        assertNull(grid.getCell(DiamondCoordinate(1, 1)))
    }

    // ── Connectivity ──────────────────────────────────────────────────────────

    @Test
    fun `filled diamond grid is connected`() {
        assertTrue(DiamondGrid<Nothing>(4, 4).isConnected())
    }

    // ── findPath ──────────────────────────────────────────────────────────────

    @Test
    fun `findPath on diamond grid returns valid path`() {
        val grid = DiamondGrid<Nothing>(5, 5)
        val path = grid.findPath(DiamondCoordinate(0, 0), DiamondCoordinate(3, 3))
        assertNotNull(path)
        assertEquals(DiamondCoordinate(0, 0), path.first())
        assertEquals(DiamondCoordinate(3, 3), path.last())
    }

    @Test
    fun `findPath returns null when destination blocked`() {
        val grid = DiamondGrid<Nothing>(3, 3)
        val dest = DiamondCoordinate(2, 2)
        assertNull(grid.findPath(DiamondCoordinate(0, 0), dest) { it.coordinate != dest })
    }
}

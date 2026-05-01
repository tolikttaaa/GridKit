package io.gridkit.core

import io.gridkit.core.core.*
import io.gridkit.core.direction.*
import io.gridkit.core.dsl.squareGrid
import io.gridkit.core.grid.SquareGrid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SquareGridTest {

    @Test
    fun `grid is initialized with correct cell count`() {
        val grid = SquareGrid<Nothing>(4, 3)
        assertEquals(12, grid.cells.size)
    }

    @Test
    fun `interior cell has 4 neighbors in 4-connected grid`() {
        val grid = SquareGrid<Nothing>(5, 5)
        assertEquals(4, grid.getNeighbors(SquareCoordinate(2, 2)).size)
    }

    @Test
    fun `corner cell has 2 neighbors in 4-connected grid`() {
        val grid = SquareGrid<Nothing>(5, 5)
        assertEquals(2, grid.getNeighbors(SquareCoordinate(0, 0)).size)
    }

    @Test
    fun `edge cell has 3 neighbors in 4-connected grid`() {
        val grid = SquareGrid<Nothing>(5, 5)
        assertEquals(3, grid.getNeighbors(SquareCoordinate(2, 0)).size)
    }

    @Test
    fun `interior cell has 8 neighbors in 8-connected grid`() {
        val grid = SquareGrid<Nothing>(5, 5, diagonal = true)
        assertEquals(8, grid.getNeighbors(SquareCoordinate(2, 2)).size)
    }

    @Test
    fun `corner cell has 3 neighbors in 8-connected grid`() {
        val grid = SquareGrid<Nothing>(5, 5, diagonal = true)
        assertEquals(3, grid.getNeighbors(SquareCoordinate(0, 0)).size)
    }

    @Test
    fun `getNeighbor returns correct coordinate`() {
        val grid = SquareGrid<Nothing>(5, 5)
        assertEquals(SquareCoordinate(2, 1), grid.getNeighbor(SquareCoordinate(2, 2), SquareEdgeDirection.TOP))
        assertEquals(SquareCoordinate(2, 3), grid.getNeighbor(SquareCoordinate(2, 2), SquareEdgeDirection.BOTTOM))
        assertEquals(SquareCoordinate(1, 2), grid.getNeighbor(SquareCoordinate(2, 2), SquareEdgeDirection.LEFT))
        assertEquals(SquareCoordinate(3, 2), grid.getNeighbor(SquareCoordinate(2, 2), SquareEdgeDirection.RIGHT))
    }

    @Test
    fun `getNeighbor returns null at boundary`() {
        val grid = SquareGrid<Nothing>(5, 5)
        assertNull(grid.getNeighbor(SquareCoordinate(0, 0), SquareEdgeDirection.TOP))
        assertNull(grid.getNeighbor(SquareCoordinate(0, 0), SquareEdgeDirection.LEFT))
    }

    @Test
    fun `distance is Manhattan in 4-connected grid`() {
        val grid = SquareGrid<Nothing>(10, 10)
        assertEquals(6, grid.distance(SquareCoordinate(0, 0), SquareCoordinate(3, 3)))
        assertEquals(5, grid.distance(SquareCoordinate(1, 1), SquareCoordinate(4, 3)))
    }

    @Test
    fun `distance is Chebyshev in 8-connected grid`() {
        val grid = SquareGrid<Nothing>(10, 10, diagonal = true)
        assertEquals(3, grid.distance(SquareCoordinate(0, 0), SquareCoordinate(3, 3)))
        assertEquals(3, grid.distance(SquareCoordinate(1, 1), SquareCoordinate(4, 3)))
    }

    @Test
    fun `getRange includes center and cardinal neighbors`() {
        val grid = SquareGrid<Nothing>(5, 5)
        val range = grid.getRange(SquareCoordinate(2, 2), 1)
        assertEquals(5, range.size)
        assertTrue(range.any { it.coordinate == SquareCoordinate(2, 2) })
        assertTrue(range.any { it.coordinate == SquareCoordinate(2, 1) })
    }

    @Test
    fun `getRing radius 0 returns only center`() {
        val grid = SquareGrid<Nothing>(5, 5)
        val ring = grid.getRing(SquareCoordinate(2, 2), 0)
        assertEquals(1, ring.size)
        assertEquals(SquareCoordinate(2, 2), ring.first().coordinate)
    }

    @Test
    fun `getRing radius 1 returns 4 cells`() {
        val grid = SquareGrid<Nothing>(5, 5)
        assertEquals(4, grid.getRing(SquareCoordinate(2, 2), 1).size)
    }

    @Test
    fun `getLine returns straight horizontal path`() {
        val grid = SquareGrid<Nothing>(10, 10)
        val line = grid.getLine(SquareCoordinate(0, 0), SquareCoordinate(4, 0))
        assertEquals(5, line.size)
        assertEquals(SquareCoordinate(0, 0), line.first().coordinate)
        assertEquals(SquareCoordinate(4, 0), line.last().coordinate)
    }

    @Test
    fun `placeNext creates new cell`() {
        val grid = SquareGrid<Nothing>(1, 1)
        val newCell = grid.placeNext(SquareCoordinate(0, 0), SquareEdgeDirection.RIGHT)
        assertEquals(SquareCoordinate(1, 0), newCell.coordinate)
        assertNotNull(grid.getCell(SquareCoordinate(1, 0)))
    }

    @Test
    fun `placeNext is idempotent`() {
        val grid = SquareGrid<Nothing>(3, 3)
        val first  = grid.placeNext(SquareCoordinate(1, 1), SquareEdgeDirection.RIGHT)
        val second = grid.placeNext(SquareCoordinate(1, 1), SquareEdgeDirection.RIGHT)
        assertTrue(first === second)
        assertEquals(SquareCoordinate(2, 1), first.coordinate)
    }

    @Test
    fun `placeNext expands bounding box`() {
        val grid = SquareGrid<Nothing>(1, 1)
        grid.placeNext(SquareCoordinate(0, 0), SquareEdgeDirection.BOTTOM)
        assertNotNull(grid.getCell(SquareCoordinate(0, 1)))
        assertEquals(2, grid.cells.size)
    }

    @Test
    fun `DSL builder places cell data`() {
        val grid = squareGrid<String>(5, 5) { place(SquareCoordinate(2, 2), data = "X") }
        assertEquals("X", grid.getCell(SquareCoordinate(2, 2))?.data)
    }

    @Test
    fun `cell carries typed data payload`() {
        val grid = squareGrid<String>(3, 3) {
            place(SquareCoordinate(1, 1), data = "treasure")
        }
        assertEquals("treasure", grid.getCell(SquareCoordinate(1, 1))?.data)
        assertNull(grid.getCell(SquareCoordinate(0, 0))?.data)
    }

    @Test
    fun `getDirectedNeighbors returns direction-keyed map`() {
        val grid = SquareGrid<Nothing>(5, 5)
        val map = grid.getDirectedNeighbors(SquareCoordinate(2, 2))
        assertTrue(SquareEdgeDirection.TOP in map)
        assertTrue(SquareEdgeDirection.BOTTOM in map)
        assertTrue(SquareEdgeDirection.LEFT in map)
        assertTrue(SquareEdgeDirection.RIGHT in map)
        assertEquals(SquareCoordinate(2, 1), map[SquareEdgeDirection.TOP]?.coordinate)
    }

    @Test
    fun `isValidCoordinate returns false for out-of-bounds`() {
        val grid = SquareGrid<Nothing>(3, 3)
        assertTrue(grid.isValidCoordinate(SquareCoordinate(0, 0)))
        assertTrue(!grid.isValidCoordinate(SquareCoordinate(5, 5)))
    }
}

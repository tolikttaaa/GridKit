package io.gridkit.core

import io.gridkit.core.core.*
import io.gridkit.core.direction.*
import io.gridkit.core.dsl.hexGrid
import io.gridkit.core.dsl.squareGrid
import io.gridkit.core.extensions.isConnected
import io.gridkit.core.grid.SquareGrid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies sparse grid behaviour, negative coordinates, and dynamic tile placement.
 */
class SparseGridTest {

    // ── Sparse placement ──────────────────────────────────────────────────────

    @Test
    fun `sparse square grid via DSL places only named cells`() {
        val grid = squareGrid<String> {
            place(SquareCoordinate(0, 0), data = "start")
            place(SquareCoordinate(0, 2), data = "end")
        }
        assertEquals(2, grid.cells.size)
        assertEquals("start", grid.getCell(SquareCoordinate(0, 0))?.data)
        assertEquals("end",   grid.getCell(SquareCoordinate(0, 2))?.data)
        assertNull(grid.getCell(SquareCoordinate(0, 1)))
    }

    @Test
    fun `non-contiguous cells do not auto-fill the gap`() {
        val grid = squareGrid<Nothing> {
            place(SquareCoordinate(0, 0))
            place(SquareCoordinate(5, 5))
        }
        assertEquals(2, grid.cells.size)
        assertNull(grid.getCell(SquareCoordinate(2, 2)))
    }

    @Test
    fun `non-contiguous sparse grid is disconnected`() {
        val grid = squareGrid<Nothing> {
            place(SquareCoordinate(0, 0))
            place(SquareCoordinate(5, 5))
        }
        assertTrue(!grid.isConnected())
    }

    // ── Negative coordinates ──────────────────────────────────────────────────

    @Test
    fun `placeNext accepts negative coordinates`() {
        val grid = SquareGrid<Nothing>(1, 1)
        val cell = grid.placeNext(SquareCoordinate(0, 0), SquareEdgeDirection.TOP)
        assertEquals(SquareCoordinate(0, -1), cell.coordinate)
        assertNotNull(grid.getCell(SquareCoordinate(0, -1)))
    }

    @Test
    fun `can place cell at negative row and col`() {
        val grid = squareGrid<String> {
            place(SquareCoordinate(-2, -3), data = "negative")
        }
        assertEquals("negative", grid.getCell(SquareCoordinate(-2, -3))?.data)
    }

    @Test
    fun `arithmeticCenter updates correctly after negative placeNext`() {
        val grid = SquareGrid<Nothing>(1, 1) // cell at (0,0)
        grid.placeNext(SquareCoordinate(0, 0), SquareEdgeDirection.TOP) // adds (0,-1)
        // bounding box: col 0..0, row -1..0 → center (0, -1+0)/2 = (0, 0) floored
        val center = grid.arithmeticCenter()
        assertEquals(SquareCoordinate(0, -1), center)
    }

    @Test
    fun `negative coordinate cells share topology with adjacent cells`() {
        val grid = SquareGrid<Nothing>(1, 1) // (0,0)
        val neg = grid.placeNext(SquareCoordinate(0, 0), SquareEdgeDirection.TOP) // (0,-1)

        val edgeFromOrigin = grid.getCell(SquareCoordinate(0, 0))!!.getEdge(SquareEdgeDirection.TOP)
        val edgeFromNeg    = neg.getEdge(SquareEdgeDirection.BOTTOM)

        assertNotNull(edgeFromOrigin); assertNotNull(edgeFromNeg)
        assertTrue(edgeFromOrigin === edgeFromNeg)
    }

    // ── placeNext topology wiring ─────────────────────────────────────────────

    @Test
    fun `placeNext correctly links all existing neighbors`() {
        // Build an L-shape: (0,0), (1,0), then add (1,1) which should link to both
        val grid = squareGrid<Nothing> {
            place(SquareCoordinate(0, 0))
            place(SquareCoordinate(1, 0))
        }
        val newCell = grid.placeNext(SquareCoordinate(0, 0), SquareEdgeDirection.BOTTOM) // → (0,1)

        // (0,1) should now exist
        assertNotNull(grid.getCell(SquareCoordinate(0, 1)))

        // Not adjacent to (1,0), so the boundary remains
        val leftEdge = newCell.getEdge(SquareEdgeDirection.LEFT)
        assertNotNull(leftEdge)
        assertTrue(leftEdge!!.isBorder)
    }

    @Test
    fun `placeNext into an existing cell is idempotent and returns same instance`() {
        val grid = SquareGrid<Nothing>(3, 3)
        val existing = grid.getCell(SquareCoordinate(2, 1))!!
        val returned = grid.placeNext(SquareCoordinate(1, 1), SquareEdgeDirection.RIGHT)
        assertTrue(existing === returned)
    }

    @Test
    fun `placeNext with surrounding neighbors shares all reusable edges`() {
        val origin = squareGrid<Nothing> {
            place(SquareCoordinate(0, 0))
            place(SquareCoordinate(1, 0))
            place(SquareCoordinate(0, 1))
        }
        // Add (1,1) — shares TOP edge with (1,0) and LEFT edge with (0,1)
        val newCell = origin.placeNext(SquareCoordinate(0, 1), SquareEdgeDirection.RIGHT)

        val topEdge  = newCell.getEdge(SquareEdgeDirection.TOP)
        val leftEdge = newCell.getEdge(SquareEdgeDirection.LEFT)

        val expectedTop  = origin.getCell(SquareCoordinate(1, 0))!!.getEdge(SquareEdgeDirection.BOTTOM)
        val expectedLeft = origin.getCell(SquareCoordinate(0, 1))!!.getEdge(SquareEdgeDirection.RIGHT)

        assertTrue(topEdge  === expectedTop)
        assertTrue(leftEdge === expectedLeft)
    }

    // ── Hex sparse grid ───────────────────────────────────────────────────────

    @Test
    fun `sparse hex grid with negative col is valid`() {
        val grid = hexGrid<Nothing> {
            place(HexCoordinate(0, 0))
            place(HexCoordinate(2, -1))
        }
        assertEquals(2, grid.cells.size)
        assertNotNull(grid.getCell(HexCoordinate(2, -1)))
    }

    @Test
    fun `hex placeNext is idempotent`() {
        val grid = hexGrid<Nothing> {
            place(HexCoordinate(0, 0))
        }
        val a = grid.placeNext(HexCoordinate(0, 0), HexEdgeDirection.RIGHT)
        val b = grid.placeNext(HexCoordinate(0, 0), HexEdgeDirection.RIGHT)
        assertTrue(a === b)
    }
}

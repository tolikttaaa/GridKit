package io.gridkit.core

import io.gridkit.core.core.*
import io.gridkit.core.dsl.triangleGrid
import io.gridkit.core.grid.TriangleGrid
import io.gridkit.core.grid.TriangleNeighborDirection
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * col parity: even = UP-pointing, odd = DOWN-pointing.
 *
 * Neighbor rules:
 *   LEFT     → (col-1, row)        for both
 *   RIGHT    → (col+1, row)        for both
 *   VERTICAL → (col+1, row-1)      for even (UP)
 *   VERTICAL → (col-1, row+1)      for odd  (DOWN)
 */
class TriangleGridTest {

    // ── Initialisation ────────────────────────────────────────────────────────

    @Test fun `TriangleGrid(6,2) has 12 cells`() =
        assertEquals(12, TriangleGrid<Nothing>(6, 2).cells.size)

    @Test fun `every cell has at most 3 neighbors`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        grid.cells.keys.forEach { assertTrue(grid.getNeighbors(it).size <= 3) }
    }

    // ── Interior cells ────────────────────────────────────────────────────────

    @Test fun `interior UP cell (even col) has 3 neighbors`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        // col=2 is even → UP; row=2 is interior
        assertEquals(3, grid.getNeighbors(TriangleCoordinate(2, 2)).size)
    }

    @Test fun `interior DOWN cell (odd col) has 3 neighbors`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        // col=3 is odd → DOWN; row=2 is interior
        assertEquals(3, grid.getNeighbors(TriangleCoordinate(3, 2)).size)
    }

    // ── Edge cells ────────────────────────────────────────────────────────────

    @Test fun `UP cell at top row (row=0) has no VERTICAL neighbor`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        // VERTICAL of UP = (col+1, row-1) = (3, -1) → out of bounds
        assertEquals(2, grid.getNeighbors(TriangleCoordinate(2, 0)).size)
    }

    @Test fun `DOWN cell at bottom row has no VERTICAL neighbor`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        // VERTICAL of DOWN = (col-1, row+1) = (2, 4) → out of bounds
        assertEquals(2, grid.getNeighbors(TriangleCoordinate(3, 3)).size)
    }

    @Test fun `leftmost cell (col=0) has no LEFT neighbor`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        val map = grid.getDirectedNeighbors(TriangleCoordinate(0, 1))
        assertTrue(TriangleNeighborDirection.LEFT !in map)
    }

    @Test fun `rightmost DOWN cell has no RIGHT neighbor`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        // cols 0..7; rightmost odd col = 7 (DOWN); RIGHT = (8, row) → out of bounds
        val map = grid.getDirectedNeighbors(TriangleCoordinate(7, 1))
        assertTrue(TriangleNeighborDirection.RIGHT !in map)
    }

    // ── getNeighbor correctness ───────────────────────────────────────────────

    @Test fun `UP cell neighbors are correct`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        val coord = TriangleCoordinate(4, 2) // even col → UP
        assertEquals(TriangleCoordinate(3, 2), grid.getNeighbor(coord, TriangleNeighborDirection.LEFT))
        assertEquals(TriangleCoordinate(5, 2), grid.getNeighbor(coord, TriangleNeighborDirection.RIGHT))
        assertEquals(TriangleCoordinate(5, 1), grid.getNeighbor(coord, TriangleNeighborDirection.VERTICAL))
    }

    @Test fun `DOWN cell neighbors are correct`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        val coord = TriangleCoordinate(5, 2) // odd col → DOWN
        assertEquals(TriangleCoordinate(4, 2), grid.getNeighbor(coord, TriangleNeighborDirection.LEFT))
        assertEquals(TriangleCoordinate(6, 2), grid.getNeighbor(coord, TriangleNeighborDirection.RIGHT))
        assertEquals(TriangleCoordinate(4, 3), grid.getNeighbor(coord, TriangleNeighborDirection.VERTICAL))
    }

    @Test fun `getNeighbor returns null at boundary`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        // UP at row 0: VERTICAL → (col+1, -1) — out of bounds
        assertNull(grid.getNeighbor(TriangleCoordinate(2, 0), TriangleNeighborDirection.VERTICAL))
    }

    // ── isUp / isDown extensions ──────────────────────────────────────────────

    @Test fun `even col is UP`()  = assertTrue(TriangleCoordinate(4, 2).isUp)
    @Test fun `odd col is DOWN`() = assertTrue(TriangleCoordinate(5, 2).isDown)

    // ── Distance ─────────────────────────────────────────────────────────────

    @Test fun `distance to itself is 0`() {
        val c = TriangleCoordinate(4, 2)
        assertEquals(0, TriangleGrid<Nothing>(8, 4).distance(c, c))
    }

    @Test fun `distance between adjacent cells is 1`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        assertEquals(1, grid.distance(TriangleCoordinate(4, 2), TriangleCoordinate(5, 2)))
    }

    // ── placeNext ────────────────────────────────────────────────────────────

    @Test fun `placeNext RIGHT from last col expands grid`() {
        val grid = TriangleGrid<Nothing>(4, 2)
        val cell = grid.placeNext(TriangleCoordinate(3, 0), TriangleNeighborDirection.RIGHT)
        assertEquals(TriangleCoordinate(4, 0), cell.coordinate)
        assertNotNull(grid.getCell(TriangleCoordinate(4, 0)))
    }

    @Test fun `placeNext is idempotent`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        val coord = TriangleCoordinate(4, 2)
        assertEquals(
            grid.placeNext(coord, TriangleNeighborDirection.RIGHT),
            grid.placeNext(coord, TriangleNeighborDirection.RIGHT)
        )
    }

    @Test fun `placeNext expands bounding box`() {
        val grid = TriangleGrid<Nothing>(2, 1)
        val before = grid.cells.size
        // DOWN(1,0).VERTICAL = UP(0, 1) — new row
        grid.placeNext(TriangleCoordinate(1, 0), TriangleNeighborDirection.VERTICAL)
        assertTrue(grid.cells.size > before)
    }

    // ── Range / Ring ──────────────────────────────────────────────────────────

    @Test fun `getRange radius 0 returns only center`() =
        assertEquals(1, TriangleGrid<Nothing>(8, 4).getRange(TriangleCoordinate(4, 2), 0).size)

    @Test fun `getRing radius 0 returns only center`() =
        assertEquals(1, TriangleGrid<Nothing>(8, 4).getRing(TriangleCoordinate(4, 2), 0).size)

    // ── DSL / data ────────────────────────────────────────────────────────────

    @Test fun `DSL builder blocks cells`() {
        val grid = triangleGrid<Nothing>(8, 4) { block(TriangleCoordinate(3, 2)) }
        assertEquals(CellState.Blocked, grid.getCell(TriangleCoordinate(3, 2))?.state)
    }

    @Test fun `cell carries typed data payload`() {
        data class Terrain(val name: String)
        val grid = triangleGrid<Terrain>(8, 4) {
            place(TriangleCoordinate(4, 2), data = Terrain("forest"))
        }
        assertEquals(Terrain("forest"), grid.getCell(TriangleCoordinate(4, 2))?.data)
    }
}

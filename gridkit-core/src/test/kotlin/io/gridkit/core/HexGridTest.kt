package io.gridkit.core

import io.gridkit.core.core.*
import io.gridkit.core.dsl.hexGrid
import io.gridkit.core.grid.HexDirection
import io.gridkit.core.grid.HexGrid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HexGridTest {

    @Test
    fun `grid is initialized with correct cell count`() {
        assertEquals(12, HexGrid<Nothing>(3, 4).cells.size)
    }

    @Test
    fun `interior cell has 6 neighbors`() {
        assertEquals(6, HexGrid<Nothing>(5, 5).getNeighbors(HexCoordinate(2, 2)).size)
    }

    @Test
    fun `corner cell has fewer than 6 neighbors`() {
        assertTrue(HexGrid<Nothing>(5, 5).getNeighbors(HexCoordinate(0, 0)).size < 6)
    }

    // ── Even-row neighbor offsets ─────────────────────────────────────────────

    @Test fun `even-row TOP_LEFT  → (row-1, col-1)`() =
        assertEquals(HexCoordinate(1, 1), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(2, 2), HexDirection.TOP_LEFT))

    @Test fun `even-row TOP_RIGHT → (row-1, col)`() =
        assertEquals(HexCoordinate(1, 2), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(2, 2), HexDirection.TOP_RIGHT))

    @Test fun `even-row LEFT      → (row, col-1)`() =
        assertEquals(HexCoordinate(2, 1), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(2, 2), HexDirection.LEFT))

    @Test fun `even-row RIGHT     → (row, col+1)`() =
        assertEquals(HexCoordinate(2, 3), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(2, 2), HexDirection.RIGHT))

    @Test fun `even-row DOWN_LEFT  → (row+1, col-1)`() =
        assertEquals(HexCoordinate(3, 1), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(2, 2), HexDirection.DOWN_LEFT))

    @Test fun `even-row DOWN_RIGHT → (row+1, col)`() =
        assertEquals(HexCoordinate(3, 2), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(2, 2), HexDirection.DOWN_RIGHT))

    // ── Odd-row neighbor offsets ──────────────────────────────────────────────

    @Test fun `odd-row TOP_LEFT  → (row-1, col)`() =
        assertEquals(HexCoordinate(0, 2), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(1, 2), HexDirection.TOP_LEFT))

    @Test fun `odd-row TOP_RIGHT → (row-1, col+1)`() =
        assertEquals(HexCoordinate(0, 3), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(1, 2), HexDirection.TOP_RIGHT))

    @Test fun `odd-row LEFT      → (row, col-1)`() =
        assertEquals(HexCoordinate(1, 1), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(1, 2), HexDirection.LEFT))

    @Test fun `odd-row RIGHT     → (row, col+1)`() =
        assertEquals(HexCoordinate(1, 3), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(1, 2), HexDirection.RIGHT))

    @Test fun `odd-row DOWN_LEFT  → (row+1, col)`() =
        assertEquals(HexCoordinate(2, 2), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(1, 2), HexDirection.DOWN_LEFT))

    @Test fun `odd-row DOWN_RIGHT → (row+1, col+1)`() =
        assertEquals(HexCoordinate(2, 3), HexGrid<Nothing>(5, 5).getNeighbor(HexCoordinate(1, 2), HexDirection.DOWN_RIGHT))

    @Test
    fun `getNeighbor returns null at boundary`() {
        val grid = HexGrid<Nothing>(5, 5)
        assertNull(grid.getNeighbor(HexCoordinate(0, 0), HexDirection.TOP_LEFT))
        assertNull(grid.getNeighbor(HexCoordinate(0, 0), HexDirection.TOP_RIGHT))
    }

    // ── Distance ─────────────────────────────────────────────────────────────

    @Test
    fun `distance from cell to itself is 0`() =
        assertEquals(0, HexGrid<Nothing>(5, 5).distance(HexCoordinate(2, 2), HexCoordinate(2, 2)))

    @Test
    fun `distance to direct neighbor is 1`() =
        assertEquals(1, HexGrid<Nothing>(5, 5).distance(HexCoordinate(2, 2), HexCoordinate(2, 3)))

    @Test
    fun `distance is positive across grid`() {
        assertTrue(HexGrid<Nothing>(6, 6).distance(HexCoordinate(0, 0), HexCoordinate(2, 2)) > 0)
    }

    // ── Range & Ring ──────────────────────────────────────────────────────────

    @Test
    fun `getRange radius 0 returns only center`() =
        assertEquals(1, HexGrid<Nothing>(5, 5).getRange(HexCoordinate(2, 2), 0).size)

    @Test
    fun `getRange radius 1 returns 7 cells for interior hex`() =
        assertEquals(7, HexGrid<Nothing>(5, 5).getRange(HexCoordinate(2, 2), 1).size)

    @Test
    fun `getRing radius 0 returns only center`() =
        assertEquals(1, HexGrid<Nothing>(5, 5).getRing(HexCoordinate(2, 2), 0).size)

    @Test
    fun `getRing radius 1 returns 6 cells for interior hex`() =
        assertEquals(6, HexGrid<Nothing>(5, 5).getRing(HexCoordinate(2, 2), 1).size)

    // ── Line ─────────────────────────────────────────────────────────────────

    @Test
    fun `getLine between adjacent cells returns 2 cells`() =
        assertEquals(2, HexGrid<Nothing>(5, 5).getLine(HexCoordinate(2, 2), HexCoordinate(2, 3)).size)

    @Test
    fun `getLine to itself returns single cell`() =
        assertEquals(1, HexGrid<Nothing>(5, 5).getLine(HexCoordinate(2, 2), HexCoordinate(2, 2)).size)

    // ── placeNext ─────────────────────────────────────────────────────────────

    @Test
    fun `placeNext even-row RIGHT creates (0,1)`() {
        val grid = HexGrid<Nothing>(1, 1)
        val cell = grid.placeNext(HexCoordinate(0, 0), HexDirection.RIGHT)
        assertEquals(HexCoordinate(0, 1), cell.coordinate)
        assertNotNull(grid.getCell(HexCoordinate(0, 1)))
    }

    @Test
    fun `placeNext is idempotent`() {
        val grid = HexGrid<Nothing>(3, 3)
        assertEquals(grid.placeNext(HexCoordinate(1, 1), HexDirection.RIGHT),
                     grid.placeNext(HexCoordinate(1, 1), HexDirection.RIGHT))
    }

    @Test
    fun `placeNext expands bounding box`() {
        val grid = HexGrid<Nothing>(1, 1)
        grid.placeNext(HexCoordinate(0, 0), HexDirection.DOWN_RIGHT)
        assertEquals(2, grid.cells.size)
    }

    @Test
    fun `placeNext all 6 directions succeed`() {
        val grid = HexGrid<Nothing>(5, 5)
        HexDirection.values().forEach { dir ->
            assertNotNull(grid.placeNext(HexCoordinate(2, 2), dir))
        }
    }

    // ── DSL ───────────────────────────────────────────────────────────────────

    @Test
    fun `DSL builder blocks cells`() {
        val grid = hexGrid<Nothing>(5, 5) { block(HexCoordinate(2, 2)) }
        assertEquals(CellState.Blocked, grid.getCell(HexCoordinate(2, 2))?.state)
    }

    @Test
    fun `cell carries typed data payload`() {
        val grid = hexGrid<Int>(4, 4) {
            place(HexCoordinate(1, 1), data = 42)
        }
        assertEquals(42, grid.getCell(HexCoordinate(1, 1))?.data)
        assertNull(grid.getCell(HexCoordinate(0, 0))?.data)
    }

    @Test
    fun `getDirectedNeighbors returns all 6 directions for interior cell`() {
        val grid = HexGrid<Nothing>(5, 5)
        val map = grid.getDirectedNeighbors(HexCoordinate(2, 2))
        assertEquals(6, map.size)
        assertTrue(HexDirection.LEFT in map)
        assertTrue(HexDirection.RIGHT in map)
    }
}

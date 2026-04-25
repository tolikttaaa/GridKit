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

class TriangleGridTest {

    @Test
    fun `grid is initialized with correct cell count`() {
        // width=3, height=2 → 3*2*2 triangles (UP + DOWN per slot)
        assertEquals(12, TriangleGrid<Nothing>(3, 2).cells.size)
    }

    @Test
    fun `every cell has at most 3 neighbors`() {
        val grid = TriangleGrid<Nothing>(4, 4)
        grid.cells.keys.forEach { assertTrue(grid.getNeighbors(it).size <= 3) }
    }

    @Test
    fun `interior UP cell has 3 neighbors`() {
        val grid = TriangleGrid<Nothing>(4, 4)
        assertEquals(3, grid.getNeighbors(TriangleCoordinate(1, 1, TriangleDirection.UP)).size)
    }

    @Test
    fun `interior DOWN cell has 3 neighbors`() {
        val grid = TriangleGrid<Nothing>(4, 4)
        assertEquals(3, grid.getNeighbors(TriangleCoordinate(1, 1, TriangleDirection.DOWN)).size)
    }

    @Test
    fun `UP cell at top row has 2 neighbors - no VERTICAL above`() {
        val grid = TriangleGrid<Nothing>(4, 4)
        assertEquals(2, grid.getNeighbors(TriangleCoordinate(1, 0, TriangleDirection.UP)).size)
    }

    @Test
    fun `UP(col=0) has no LEFT neighbor`() {
        val grid = TriangleGrid<Nothing>(4, 4)
        val map = grid.getDirectedNeighbors(TriangleCoordinate(0, 1, TriangleDirection.UP))
        assertTrue(TriangleNeighborDirection.LEFT !in map)
    }

    @Test
    fun `getNeighbor correct coords for UP cell`() {
        val grid = TriangleGrid<Nothing>(4, 4)
        val coord = TriangleCoordinate(2, 2, TriangleDirection.UP)
        assertEquals(TriangleCoordinate(1, 2, TriangleDirection.DOWN),
            grid.getNeighbor(coord, TriangleNeighborDirection.LEFT))
        assertEquals(TriangleCoordinate(2, 2, TriangleDirection.DOWN),
            grid.getNeighbor(coord, TriangleNeighborDirection.RIGHT))
        assertEquals(TriangleCoordinate(2, 1, TriangleDirection.DOWN),
            grid.getNeighbor(coord, TriangleNeighborDirection.VERTICAL))
    }

    @Test
    fun `getNeighbor correct coords for DOWN cell`() {
        val grid = TriangleGrid<Nothing>(4, 4)
        val coord = TriangleCoordinate(2, 2, TriangleDirection.DOWN)
        assertEquals(TriangleCoordinate(2, 2, TriangleDirection.UP),
            grid.getNeighbor(coord, TriangleNeighborDirection.LEFT))
        assertEquals(TriangleCoordinate(3, 2, TriangleDirection.UP),
            grid.getNeighbor(coord, TriangleNeighborDirection.RIGHT))
        assertEquals(TriangleCoordinate(2, 3, TriangleDirection.UP),
            grid.getNeighbor(coord, TriangleNeighborDirection.VERTICAL))
    }

    @Test
    fun `getNeighbor returns null at boundary`() {
        val grid = TriangleGrid<Nothing>(3, 3)
        assertNull(grid.getNeighbor(TriangleCoordinate(1, 0, TriangleDirection.UP), TriangleNeighborDirection.VERTICAL))
    }

    @Test
    fun `distance from cell to itself is 0`() {
        val coord = TriangleCoordinate(2, 2, TriangleDirection.UP)
        assertEquals(0, TriangleGrid<Nothing>(4, 4).distance(coord, coord))
    }

    @Test
    fun `distance between adjacent cells is 1`() {
        val grid = TriangleGrid<Nothing>(4, 4)
        assertEquals(1, grid.distance(
            TriangleCoordinate(2, 2, TriangleDirection.UP),
            TriangleCoordinate(2, 2, TriangleDirection.DOWN)
        ))
    }

    @Test
    fun `placeNext creates cell outside grid`() {
        val grid = TriangleGrid<Nothing>(1, 1)
        val down = TriangleCoordinate(0, 0, TriangleDirection.DOWN)
        val newCell = grid.placeNext(down, TriangleNeighborDirection.VERTICAL)
        assertEquals(TriangleCoordinate(0, 1, TriangleDirection.UP), newCell.coordinate)
        assertNotNull(grid.getCell(TriangleCoordinate(0, 1, TriangleDirection.UP)))
    }

    @Test
    fun `placeNext is idempotent`() {
        val grid = TriangleGrid<Nothing>(4, 4)
        val coord = TriangleCoordinate(2, 2, TriangleDirection.UP)
        assertEquals(grid.placeNext(coord, TriangleNeighborDirection.RIGHT),
                     grid.placeNext(coord, TriangleNeighborDirection.RIGHT))
    }

    @Test
    fun `placeNext expands bounding box`() {
        val grid = TriangleGrid<Nothing>(1, 1)
        val before = grid.cells.size
        grid.placeNext(TriangleCoordinate(0, 0, TriangleDirection.DOWN), TriangleNeighborDirection.VERTICAL)
        assertTrue(grid.cells.size > before)
    }

    @Test
    fun `getRange radius 0 returns center only`() =
        assertEquals(1, TriangleGrid<Nothing>(4, 4)
            .getRange(TriangleCoordinate(2, 2, TriangleDirection.UP), 0).size)

    @Test
    fun `getRing radius 0 returns center only`() =
        assertEquals(1, TriangleGrid<Nothing>(4, 4)
            .getRing(TriangleCoordinate(2, 2, TriangleDirection.UP), 0).size)

    @Test
    fun `DSL builder blocks cells`() {
        val grid = triangleGrid<Nothing>(4, 4) {
            block(TriangleCoordinate(1, 1, TriangleDirection.UP))
        }
        assertEquals(CellState.Blocked, grid.getCell(TriangleCoordinate(1, 1, TriangleDirection.UP))?.state)
    }

    @Test
    fun `cell carries typed data payload`() {
        data class Terrain(val name: String)
        val grid = triangleGrid<Terrain>(3, 3) {
            place(TriangleCoordinate(1, 1, TriangleDirection.UP), data = Terrain("forest"))
        }
        assertEquals(Terrain("forest"), grid.getCell(TriangleCoordinate(1, 1, TriangleDirection.UP))?.data)
    }
}

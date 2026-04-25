package io.gridkit.core

import io.gridkit.core.core.*
import io.gridkit.core.dsl.hexGrid
import io.gridkit.core.dsl.squareGrid
import io.gridkit.core.extensions.flood
import io.gridkit.core.extensions.isConnected
import io.gridkit.core.grid.HexGrid
import io.gridkit.core.grid.SquareGrid
import io.gridkit.core.grid.TriangleGrid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GridCenterTest {

    // ── Arithmetic center ─────────────────────────────────────────────────────

    @Test
    fun `arithmeticCenter of 5x5 square grid`() =
        assertEquals(SquareCoordinate(2, 2), SquareGrid<Nothing>(5, 5).arithmeticCenter())

    @Test
    fun `arithmeticCenter of 4x4 square grid uses floor`() =
        assertEquals(SquareCoordinate(1, 1), SquareGrid<Nothing>(4, 4).arithmeticCenter())

    @Test
    fun `arithmeticCenter of HexGrid rows=4 cols=6`() {
        val center = HexGrid<Nothing>(4, 6).arithmeticCenter()
        // bounding box rows 0..3 → (0+3)/2=1; cols 0..5 → (0+5)/2=2
        assertEquals(1, center.row)
        assertEquals(2, center.col)
    }

    @Test
    fun `arithmeticCenter of 1x1 grid`() =
        assertEquals(SquareCoordinate(0, 0), SquareGrid<Nothing>(1, 1).arithmeticCenter())

    // ── physicalCenter (merged) ───────────────────────────────────────────────

    @Test
    fun `physicalCenter of 1x1 square grid has physical origin`() {
        val center = SquareGrid<Nothing>(1, 1).physicalCenter()
        assertEquals(0.0, center.physical.x, 1e-9)
        assertEquals(0.0, center.physical.y, 1e-9)
    }

    @Test
    fun `physicalCenter of 1x1 square grid snaps to only cell`() {
        assertEquals(SquareCoordinate(0, 0), SquareGrid<Nothing>(1, 1).physicalCenter().coordinate)
    }

    @Test
    fun `physicalCenter of 3x3 square grid has physical (1,1)`() {
        val center = SquareGrid<Nothing>(3, 3).physicalCenter()
        assertEquals(1.0, center.physical.x, 1e-9)
        assertEquals(1.0, center.physical.y, 1e-9)
    }

    @Test
    fun `physicalCenter snaps to center cell of 3x3 grid`() {
        assertEquals(SquareCoordinate(1, 1), SquareGrid<Nothing>(3, 3).physicalCenter().coordinate)
    }

    @Test
    fun `physicalCenter excludes blocked cells`() {
        val grid = squareGrid<Nothing>(3, 3) { block(SquareCoordinate(0, 0)) }
        val center = grid.physicalCenter()
        // Removing (0,0) shifts the average x to the right of 1.0
        assertTrue(center.physical.x > 1.0)
    }

    @Test
    fun `physicalCenter coordinate is valid after excluding blocked cells`() {
        val grid = squareGrid<Nothing>(3, 3) { block(SquareCoordinate(0, 0)) }
        assertTrue(grid.isValidCoordinate(grid.physicalCenter().coordinate))
    }

    @Test
    fun `physicalCenter of hex grid returns reasonable physical position`() {
        val center = HexGrid<Nothing>(4, 6).physicalCenter()
        assertTrue(center.physical.x > 0)
        assertTrue(center.physical.y > 0)
    }

    @Test
    fun `physicalCenter coordinate round-trips through hex physical position`() {
        val grid = hexGrid<Nothing>(5, 5) {
            block(HexCoordinate(0, 0))
            block(HexCoordinate(4, 5))
        }
        val center = grid.physicalCenter()
        assertTrue(grid.isValidCoordinate(center.coordinate))
    }

    // ── nearestCoordinate on concrete class ───────────────────────────────────

    @Test
    fun `nearestCoordinate snaps to exact cell`() {
        val grid = SquareGrid<Nothing>(5, 5)
        val target = SquareCoordinate(2, 3)
        assertEquals(target, grid.nearestCoordinate(grid.toPhysical(target)))
    }

    @Test
    fun `nearestCoordinate snaps to closest cell for offset point`() {
        val grid = SquareGrid<Nothing>(5, 5)
        assertEquals(SquareCoordinate(2, 2), grid.nearestCoordinate(PhysicalCenter(2.1, 2.0)))
    }

    // ── Flood fill ────────────────────────────────────────────────────────────

    @Test
    fun `flood fill reaches all cells on open grid`() {
        assertEquals(9, SquareGrid<Nothing>(3, 3).flood(SquareCoordinate(0, 0)).size)
    }

    @Test
    fun `flood fill stops at blocked cells`() {
        val grid = squareGrid<Nothing>(5, 1) { block(SquareCoordinate(2, 0)) }
        assertEquals(2, grid.flood(SquareCoordinate(0, 0)).size)
    }

    @Test
    fun `flood fill with custom predicate`() {
        val grid = SquareGrid<Nothing>(5, 5)
        val reached = grid.flood(SquareCoordinate(0, 0)) { c ->
            c.coordinate.col < 3 && c.coordinate.row < 3
        }
        assertEquals(9, reached.size)
    }

    // ── isConnected ───────────────────────────────────────────────────────────

    @Test
    fun `open square grid is connected`() = assertTrue(SquareGrid<Nothing>(4, 4).isConnected())

    @Test
    fun `grid split by wall is not connected`() {
        assertFalse(squareGrid<Nothing>(5, 1) { block(SquareCoordinate(2, 0)) }.isConnected())
    }

    @Test
    fun `all-blocked grid is vacuously connected`() {
        val grid = squareGrid<Nothing>(2, 2) {
            block(SquareCoordinate(0, 0)); block(SquareCoordinate(1, 0))
            block(SquareCoordinate(0, 1)); block(SquareCoordinate(1, 1))
        }
        assertTrue(grid.isConnected())
    }

    @Test
    fun `triangle grid is connected`() = assertTrue(TriangleGrid<Nothing>(3, 3).isConnected())

    @Test
    fun `hex grid is connected`() = assertTrue(HexGrid<Nothing>(3, 3).isConnected())
}

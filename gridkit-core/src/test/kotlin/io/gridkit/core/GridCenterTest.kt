package io.gridkit.core

import io.gridkit.core.core.*
import io.gridkit.core.extensions.flood
import io.gridkit.core.extensions.isConnected
import io.gridkit.core.grid.HexGrid
import io.gridkit.core.grid.SquareGrid
import io.gridkit.core.grid.TriangleGrid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridCenterTest {

    // ── Arithmetic center ─────────────────────────────────────────────────────

    @Test fun `arithmeticCenter of 5x5 square grid`() =
        assertEquals(SquareCoordinate(2, 2), SquareGrid<Nothing>(5, 5).arithmeticCenter())

    @Test fun `arithmeticCenter of 4x4 square grid uses floor`() =
        assertEquals(SquareCoordinate(1, 1), SquareGrid<Nothing>(4, 4).arithmeticCenter())

    @Test fun `arithmeticCenter of HexGrid rows=4 cols=6`() {
        val c = HexGrid<Nothing>(4, 6).arithmeticCenter()
        assertEquals(1, c.row); assertEquals(2, c.col)
    }

    @Test fun `arithmeticCenter of 1x1 square grid`() =
        assertEquals(SquareCoordinate(0, 0), SquareGrid<Nothing>(1, 1).arithmeticCenter())

    @Test fun `arithmeticCenter of TriangleGrid(8,4)`() {
        val c = TriangleGrid<Nothing>(8, 4).arithmeticCenter()
        // cols 0..7 → (0+7)/2=3; rows 0..3 → (0+3)/2=1
        assertEquals(TriangleCoordinate(3, 1), c)
    }

    // ── physicalCenter ────────────────────────────────────────────────────────

    @Test fun `physicalCenter of 1x1 square grid`() {
        val c = SquareGrid<Nothing>(1, 1).physicalCenter()
        assertEquals(0.0, c.physical.x, 1e-9); assertEquals(0.0, c.physical.y, 1e-9)
        assertEquals(SquareCoordinate(0, 0), c.coordinate)
    }

    @Test fun `physicalCenter of 3x3 square grid`() {
        val c = SquareGrid<Nothing>(3, 3).physicalCenter()
        assertEquals(1.0, c.physical.x, 1e-9); assertEquals(1.0, c.physical.y, 1e-9)
        assertEquals(SquareCoordinate(1, 1), c.coordinate)
    }

    @Test fun `physicalCenter includes all cells`() {
        val grid = SquareGrid<Nothing>(3, 3)
        assertEquals(1.0, grid.physicalCenter().physical.x, 1e-9)
    }

    @Test fun `physicalCenter coordinate is always valid`() {
        val grid = SquareGrid<Nothing>(3, 3)
        assertTrue(grid.isValidCoordinate(grid.physicalCenter().coordinate))
    }

    @Test fun `physicalCenter of hex grid has positive position`() {
        val c = HexGrid<Nothing>(4, 6).physicalCenter()
        assertTrue(c.physical.x > 0); assertTrue(c.physical.y > 0)
    }

    @Test fun `physicalCenter of triangle grid has valid coordinate`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        assertTrue(grid.isValidCoordinate(grid.physicalCenter().coordinate))
    }

    // ── nearestCoordinate on concrete class ───────────────────────────────────

    @Test fun `nearestCoordinate snaps to exact cell`() {
        val grid = SquareGrid<Nothing>(5, 5)
        val target = SquareCoordinate(2, 3)
        assertEquals(target, grid.nearestCoordinate(grid.toPhysical(target)))
    }

    @Test fun `nearestCoordinate snaps to closest cell for offset point`() {
        val grid = SquareGrid<Nothing>(5, 5)
        assertEquals(SquareCoordinate(2, 2), grid.nearestCoordinate(PhysicalCenter(2.1, 2.0)))
    }

    // ── Flood fill ────────────────────────────────────────────────────────────

    @Test fun `flood fill reaches all cells on open grid`() =
        assertEquals(9, SquareGrid<Nothing>(3, 3).flood(SquareCoordinate(0, 0)).size)

    @Test fun `flood fill respects custom predicate`() {
        val grid = SquareGrid<Nothing>(5, 1)
        assertEquals(2, grid.flood(SquareCoordinate(0, 0)) { cell ->
            cell.coordinate != SquareCoordinate(2, 0)
        }.size)
    }

    @Test fun `flood fill with custom predicate`() {
        val grid = SquareGrid<Nothing>(5, 5)
        assertEquals(9, grid.flood(SquareCoordinate(0, 0)) { c ->
            c.coordinate.col < 3 && c.coordinate.row < 3
        }.size)
    }

    // ── isConnected ───────────────────────────────────────────────────────────

    @Test fun `open square grid is connected`() = assertTrue(SquareGrid<Nothing>(4, 4).isConnected())

    @Test fun `triangle grid is connected`() = assertTrue(TriangleGrid<Nothing>(6, 3).isConnected())

    @Test fun `hex grid is connected`() = assertTrue(HexGrid<Nothing>(3, 3).isConnected())
}

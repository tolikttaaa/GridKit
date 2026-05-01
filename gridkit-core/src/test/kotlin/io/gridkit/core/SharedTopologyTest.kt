package io.gridkit.core

import io.gridkit.core.core.*
import io.gridkit.core.direction.*
import io.gridkit.core.grid.HexGrid
import io.gridkit.core.grid.DiamondGrid
import io.gridkit.core.grid.SquareGrid
import io.gridkit.core.grid.TriangleGrid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies the shared-topology invariants for all four grid types:
 * - Adjacent cells hold **the same** Edge and Vertex instances (referential equality ===)
 * - edge.vertices is a Set of exactly 2 unordered vertices
 * - edge.getOpposite() works regardless of cellA/cellB assignment order
 * - edge.isBorder is true on grid boundaries, false for interior edges
 * - vertex.cells contains every cell that shares that vertex
 * - vertex.edges contains every edge connected to that vertex
 */
class SharedTopologyTest {

    // ── Square grid ───────────────────────────────────────────────────────────

    @Test
    fun `square adjacent cells share the same Edge instance`() {
        val grid = SquareGrid<Nothing>(3, 3)
        val a = grid.getCell(SquareCoordinate(1, 1))!!
        val b = grid.getCell(SquareCoordinate(1, 2))!! // BOTTOM neighbor of a

        val edgeFromA = a.getEdge(SquareEdgeDirection.BOTTOM)
        val edgeFromB = b.getEdge(SquareEdgeDirection.TOP)

        assertNotNull(edgeFromA)
        assertNotNull(edgeFromB)
        assertTrue(edgeFromA === edgeFromB, "Shared edge must be the same object")
    }

    @Test
    fun `square adjacent cells share the same Vertex instance at TOP_RIGHT corner`() {
        val grid = SquareGrid<Nothing>(3, 3)
        val a = grid.getCell(SquareCoordinate(1, 1))!!
        val b = grid.getCell(SquareCoordinate(2, 1))!! // RIGHT neighbor of a

        val vFromA = a.getVertex(SquareVertexDirection.TOP_RIGHT)
        val vFromB = b.getVertex(SquareVertexDirection.TOP_LEFT)

        assertNotNull(vFromA)
        assertNotNull(vFromB)
        assertTrue(vFromA === vFromB, "Shared corner vertex must be the same object")
    }

    @Test
    fun `square edge has exactly 2 vertices`() {
        val cell = SquareGrid<Nothing>(3, 3).getCell(SquareCoordinate(1, 1))!!
        val edge = cell.getEdge(SquareEdgeDirection.RIGHT)!!
        assertEquals(2, edge.vertices.size)
    }

    @Test
    fun `square edge vertices are unordered (Set semantics)`() {
        val grid = SquareGrid<Nothing>(3, 3)
        val a = grid.getCell(SquareCoordinate(1, 1))!!
        val edge = a.getEdge(SquareEdgeDirection.RIGHT)!!
        val topRight = a.getVertex(SquareVertexDirection.TOP_RIGHT)!!
        val downRight = a.getVertex(SquareVertexDirection.DOWN_RIGHT)!!
        assertTrue(edge.vertices.containsAll(setOf(topRight, downRight)))
    }

    @Test
    fun `square edge getOpposite works in both directions`() {
        val grid = SquareGrid<Nothing>(3, 3)
        val a = grid.getCell(SquareCoordinate(1, 1))!!
        val b = grid.getCell(SquareCoordinate(2, 1))!!
        val edge = a.getEdge(SquareEdgeDirection.RIGHT)!!

        assertTrue(edge.getOpposite(a) === b)
        assertTrue(edge.getOpposite(b) === a)
    }

    @Test
    fun `square boundary edge isBorder is true`() {
        val grid = SquareGrid<Nothing>(3, 3)
        val corner = grid.getCell(SquareCoordinate(0, 0))!!
        assertTrue(corner.getEdge(SquareEdgeDirection.TOP)!!.isBorder)
        assertTrue(corner.getEdge(SquareEdgeDirection.LEFT)!!.isBorder)
    }

    @Test
    fun `square interior edge isBorder is false`() {
        val grid = SquareGrid<Nothing>(3, 3)
        val center = grid.getCell(SquareCoordinate(1, 1))!!
        assertTrue(!center.getEdge(SquareEdgeDirection.TOP)!!.isBorder)
        assertTrue(!center.getEdge(SquareEdgeDirection.RIGHT)!!.isBorder)
    }

    @Test
    fun `square vertex cells set contains all sharing cells`() {
        val grid = SquareGrid<Nothing>(3, 3)
        // The DOWN_RIGHT corner of (1,1) is shared by (1,1), (2,1), (1,2), (2,2)
        val v = grid.getCell(SquareCoordinate(1, 1))!!.getVertex(SquareVertexDirection.DOWN_RIGHT)!!
        assertEquals(4, v.cells.size)
        val cellCoords = v.cells.map { it.coordinate }.toSet()
        assertTrue(SquareCoordinate(1, 1) in cellCoords)
        assertTrue(SquareCoordinate(2, 1) in cellCoords)
        assertTrue(SquareCoordinate(1, 2) in cellCoords)
        assertTrue(SquareCoordinate(2, 2) in cellCoords)
    }

    @Test
    fun `square vertex edges set contains all connected edges`() {
        val grid = SquareGrid<Nothing>(3, 3)
        // TOP_RIGHT vertex of (1,1) connects RIGHT edge of (1,1), TOP edge of (1,1),
        // LEFT edge of (2,1), and BOTTOM edge of (1,0) — 4 edges total for interior vertex
        val v = grid.getCell(SquareCoordinate(1, 1))!!.getVertex(SquareVertexDirection.TOP_RIGHT)!!
        assertEquals(4, v.edges.size)
    }

    @Test
    fun `square boundary vertex has fewer cells`() {
        val grid = SquareGrid<Nothing>(3, 3)
        val cornerV = grid.getCell(SquareCoordinate(0, 0))!!.getVertex(SquareVertexDirection.TOP_LEFT)!!
        assertEquals(1, cornerV.cells.size)
    }

    @Test
    fun `square edge getOpposite returns null for unrelated cell`() {
        val grid = SquareGrid<Nothing>(3, 3)
        val a = grid.getCell(SquareCoordinate(1, 1))!!
        val c = grid.getCell(SquareCoordinate(0, 0))!!
        val edge = a.getEdge(SquareEdgeDirection.RIGHT)!!
        assertNull(edge.getOpposite(c))
    }

    // ── Hex grid ──────────────────────────────────────────────────────────────

    @Test
    fun `hex adjacent cells share the same Edge instance`() {
        val grid = HexGrid<Nothing>(5, 5)
        val a = grid.getCell(HexCoordinate(2, 2))!!
        val b = grid.getCell(HexCoordinate(2, 3))!! // RIGHT neighbor

        val edgeFromA = a.getEdge(HexEdgeDirection.RIGHT)
        val edgeFromB = b.getEdge(HexEdgeDirection.LEFT)

        assertNotNull(edgeFromA); assertNotNull(edgeFromB)
        assertTrue(edgeFromA === edgeFromB)
    }

    @Test
    fun `hex edge has exactly 2 vertices`() {
        val cell = HexGrid<Nothing>(3, 3).getCell(HexCoordinate(1, 1))!!
        assertEquals(2, cell.getEdge(HexEdgeDirection.LEFT)!!.vertices.size)
    }

    @Test
    fun `hex boundary edge isBorder is true`() {
        val grid = HexGrid<Nothing>(5, 5)
        val corner = grid.getCell(HexCoordinate(0, 0))!!
        assertTrue(corner.getEdge(HexEdgeDirection.TOP_LEFT)!!.isBorder)
    }

    @Test
    fun `hex interior edge isBorder is false`() {
        val grid = HexGrid<Nothing>(5, 5)
        val center = grid.getCell(HexCoordinate(2, 2))!!
        HexEdgeDirection.entries.forEach { dir ->
            assertTrue(!center.getEdge(dir)!!.isBorder, "Edge $dir should not be border")
        }
    }

    @Test
    fun `hex adjacent cells share the same Vertex instance`() {
        val grid = HexGrid<Nothing>(5, 5)
        val a = grid.getCell(HexCoordinate(2, 2))!!
        val b = grid.getCell(HexCoordinate(2, 3))!! // RIGHT neighbor

        // The TOP_RIGHT vertex of a = the TOP_LEFT vertex of b
        val vFromA = a.getVertex(HexVertexDirection.TOP_RIGHT)
        val vFromB = b.getVertex(HexVertexDirection.TOP_LEFT)

        assertNotNull(vFromA); assertNotNull(vFromB)
        assertTrue(vFromA === vFromB)
    }

    // ── Triangle grid ─────────────────────────────────────────────────────────

    @Test
    fun `triangle adjacent cells share the same Edge instance`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        val up   = grid.getCell(TriangleCoordinate(4, 2))!! // UP (even col)
        val down = grid.getCell(TriangleCoordinate(5, 2))!! // RIGHT neighbor (DOWN)

        val edgeFromUp   = up.getEdge(TriangleEdgeDirection.RIGHT)
        val edgeFromDown = down.getEdge(TriangleEdgeDirection.LEFT)

        assertNotNull(edgeFromUp); assertNotNull(edgeFromDown)
        assertTrue(edgeFromUp === edgeFromDown)
    }

    @Test
    fun `triangle edge has exactly 2 vertices`() {
        val cell = TriangleGrid<Nothing>(8, 4).getCell(TriangleCoordinate(4, 2))!!
        assertEquals(2, cell.getEdge(TriangleEdgeDirection.VERTICAL)!!.vertices.size)
    }

    @Test
    fun `triangle boundary edge isBorder is true`() {
        val grid = TriangleGrid<Nothing>(8, 4)
        val topLeft = grid.getCell(TriangleCoordinate(0, 0))!!
        assertTrue(topLeft.getEdge(TriangleEdgeDirection.LEFT)!!.isBorder)
        assertTrue(topLeft.getEdge(TriangleEdgeDirection.VERTICAL)!!.isBorder)
    }

    // ── Diamond grid ──────────────────────────────────────────────────────────

    @Test
    fun `diamond adjacent cells share the same Edge instance`() {
        val grid = DiamondGrid<Nothing>(3, 3)
        val a = grid.getCell(DiamondCoordinate(1, 1))!!
        val b = grid.getCell(DiamondCoordinate(1, 2))!! // BOTTOM_RIGHT neighbor

        val edgeFromA = a.getEdge(DiamondEdgeDirection.BOTTOM_RIGHT)
        val edgeFromB = b.getEdge(DiamondEdgeDirection.TOP_LEFT)

        assertNotNull(edgeFromA); assertNotNull(edgeFromB)
        assertTrue(edgeFromA === edgeFromB)
    }

    @Test
    fun `diamond adjacent cells share the same Vertex instance`() {
        val grid = DiamondGrid<Nothing>(3, 3)
        val a = grid.getCell(DiamondCoordinate(1, 1))!!
        val b = grid.getCell(DiamondCoordinate(1, 2))!! // BOTTOM_RIGHT neighbor

        val vFromA = a.getVertex(DiamondVertexDirection.RIGHT)
        val vFromB = b.getVertex(DiamondVertexDirection.TOP)

        assertNotNull(vFromA); assertNotNull(vFromB)
        assertTrue(vFromA === vFromB)
    }

    @Test
    fun `diamond edge has exactly 2 vertices`() {
        val cell = DiamondGrid<Nothing>(3, 3).getCell(DiamondCoordinate(1, 1))!!
        assertEquals(2, cell.getEdge(DiamondEdgeDirection.TOP_LEFT)!!.vertices.size)
    }

    @Test
    fun `diamond boundary edge isBorder is true`() {
        val grid = DiamondGrid<Nothing>(3, 3)
        val corner = grid.getCell(DiamondCoordinate(0, 0))!!
        assertTrue(corner.getEdge(DiamondEdgeDirection.TOP_LEFT)!!.isBorder)
        assertTrue(corner.getEdge(DiamondEdgeDirection.TOP_RIGHT)!!.isBorder)
    }

    @Test
    fun `diamond interior edge isBorder is false`() {
        val grid = DiamondGrid<Nothing>(3, 3)
        val center = grid.getCell(DiamondCoordinate(1, 1))!!
        DiamondEdgeDirection.entries.forEach { dir ->
            assertTrue(!center.getEdge(dir)!!.isBorder, "Edge $dir should not be border")
        }
    }

    // ── Cell.getNeighbors via edges ───────────────────────────────────────────

    @Test
    fun `Cell getNeighbors via edges matches grid getNeighbors for square`() {
        val grid = SquareGrid<Nothing>(5, 5)
        val cell = grid.getCell(SquareCoordinate(2, 2))!!
        assertEquals(
            grid.getNeighbors(SquareCoordinate(2, 2)).map { it.coordinate }.toSet(),
            cell.getNeighborList().map { it.coordinate }.toSet()
        )
    }

    @Test
    fun `Cell getNeighbor via edge returns correct cell for square`() {
        val grid = SquareGrid<Nothing>(5, 5)
        val cell = grid.getCell(SquareCoordinate(2, 2))!!
        val right = cell.getNeighbor(SquareEdgeDirection.RIGHT)
        assertEquals(SquareCoordinate(3, 2), right?.coordinate)
    }
}

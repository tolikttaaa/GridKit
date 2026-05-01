package io.gridkit.core

import io.gridkit.core.core.*
import io.gridkit.core.dsl.hexGrid
import io.gridkit.core.dsl.diamondGrid
import io.gridkit.core.dsl.squareGrid
import io.gridkit.core.dsl.triangleGrid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AsciiStringTest {

    @Test
    fun `single square cell renders box art`() {
        val grid = squareGrid<String>(1, 1) {
            place(SquareCoordinate(0, 0), data = "X")
        }

        assertEquals(
            """
            *-----*
            |  X  |
            *-----*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `single hex cell renders box art`() {
        val grid = hexGrid<String>(1, 1) {
            place(HexCoordinate(0, 0), data = "42")
        }

        assertEquals(
            """
              *--*
             /    \
            *  42  *
             \    /
              *--*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `single triangle cell renders up-pointing box art`() {
        val grid = triangleGrid<String>(1, 1) {
            place(TriangleCoordinate(0, 0), data = "X")
        }

        assertEquals(
            """
              *****
             /  X  \
            *-------*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `square columns use the longest value in each column`() {
        val grid = squareGrid<String>(2, 2) {
            place(SquareCoordinate(0, 0), data = "A")
            place(SquareCoordinate(1, 0), data = "B")
            place(SquareCoordinate(0, 1), data = "long")
            place(SquareCoordinate(1, 1), data = "C")
        }

        assertEquals(
            """
            *--------*-----*
            |   A    |  B  |
            *--------*-----*
            |  long  |  C  |
            *--------*-----*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `empty square cell renders empty body`() {
        val grid = squareGrid<Nothing>(1, 1)

        assertEquals(
            """
            *-----*
            |     |
            *-----*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `three by three square grid shares edges`() {
        val grid = squareGrid<String>(3, 3) {
            place(SquareCoordinate(0, 0), data = "M")
            place(SquareCoordinate(1, 0), data = "1")
            place(SquareCoordinate(0, 1), data = "1")
            place(SquareCoordinate(1, 1), data = "1")
            place(SquareCoordinate(1, 2), data = "1")
            place(SquareCoordinate(2, 2), data = "M")
        }

        assertEquals(
            """
            *-----*-----*-----*
            |  M  |  1  |     |
            *-----*-----*-----*
            |  1  |  1  |     |
            *-----*-----*-----*
            |     |  1  |  M  |
            *-----*-----*-----*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `two by three hex grid uses odd row offset staggering`() {
        val grid = hexGrid<String>(2, 3) {
            place(HexCoordinate(0, 0), data = "A")
            place(HexCoordinate(0, 1), data = "B")
            place(HexCoordinate(0, 2), data = "C")
            place(HexCoordinate(1, 0), data = "D")
            place(HexCoordinate(1, 1), data = "E")
            place(HexCoordinate(1, 2), data = "F")
        }

        assertEquals(
            """
              *-*     *-*     *-*
             /   \   /   \   /   \
            *  A  *-*  B  *-*  C  *-*
             \   /   \   /   \   /   \
              *-*  D  *-*  E  *-*  F  *
                 \   /   \   /   \   /
                  *-*     *-*     *-*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `triangle grid alternates up and down cells`() {
        val grid = triangleGrid<String>(2, 1) {
            place(TriangleCoordinate(0, 0), data = "A")
            place(TriangleCoordinate(1, 0), data = "B")
        }

        assertEquals(
            """
              *****-------*
             /  A  \  B  /
            *-------*****
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `two by three diamond grid uses isometric row indentation`() {
        val grid = diamondGrid<String>(2, 3) {
            place(DiamondCoordinate(0, 0), data = "A")
            place(DiamondCoordinate(0, 1), data = "B")
            place(DiamondCoordinate(0, 2), data = "C")
            place(DiamondCoordinate(1, 0), data = "D")
            place(DiamondCoordinate(1, 1), data = "E")
            place(DiamondCoordinate(1, 2), data = "F")
        }

        assertEquals(
            """
              ***     ***     ***
             /   \   /   \   /   \
            *  A  ***  B  ***  C  ***
             \   /   \   /   \   /   \
              ***  D  ***  E  ***  F  *
                 \   /   \   /   \   /
                  ***     ***     ***
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `four by four diamond grid renders diamond box art`() {
        val grid = diamondGrid<String>(4, 4) {
            val labels = listOf(
                listOf("A", "B", "C", "D"),
                listOf("E", "F", "G", "H"),
                listOf("I", "J", "K", "L"),
                listOf("M", "N", "O", "P")
            )
            labels.forEachIndexed { row, values ->
                values.forEachIndexed { col, value ->
                    place(DiamondCoordinate(row, col), data = value)
                }
            }
        }

        assertEquals(
            """
              ***     ***     ***     ***
             /   \   /   \   /   \   /   \
            *  A  ***  B  ***  C  ***  D  ***
             \   /   \   /   \   /   \   /   \
              ***  E  ***  F  ***  G  ***  H  *
             /   \   /   \   /   \   /   \   /
            *  I  ***  J  ***  K  ***  L  ***
             \   /   \   /   \   /   \   /   \
              ***  M  ***  N  ***  O  ***  P  *
                 \   /   \   /   \   /   \   /
                  ***     ***     ***     ***
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `four by four square grid expands for long values`() {
        val grid = squareGrid<String>(4, 4) {
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    place(SquareCoordinate(col, row), data = LONG_VALUE)
                }
            }
        }

        assertEquals(
            """
            *--------------*--------------*--------------*--------------*
            |  LONG_VALUE  |  LONG_VALUE  |  LONG_VALUE  |  LONG_VALUE  |
            *--------------*--------------*--------------*--------------*
            |  LONG_VALUE  |  LONG_VALUE  |  LONG_VALUE  |  LONG_VALUE  |
            *--------------*--------------*--------------*--------------*
            |  LONG_VALUE  |  LONG_VALUE  |  LONG_VALUE  |  LONG_VALUE  |
            *--------------*--------------*--------------*--------------*
            |  LONG_VALUE  |  LONG_VALUE  |  LONG_VALUE  |  LONG_VALUE  |
            *--------------*--------------*--------------*--------------*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `four by four hex grid expands for long values`() {
        val grid = hexGrid<String>(4, 4) {
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    place(HexCoordinate(row, col), data = LONG_VALUE)
                }
            }
        }

        assertEquals(
            """
              *----------*              *----------*              *----------*              *----------*
             /            \            /            \            /            \            /            \
            *  LONG_VALUE  *----------*  LONG_VALUE  *----------*  LONG_VALUE  *----------*  LONG_VALUE  *----------*
             \            /            \            /            \            /            \            /            \
              *----------*  LONG_VALUE  *----------*  LONG_VALUE  *----------*  LONG_VALUE  *----------*  LONG_VALUE  *
             /            \            /            \            /            \            /            \            /
            *  LONG_VALUE  *----------*  LONG_VALUE  *----------*  LONG_VALUE  *----------*  LONG_VALUE  *----------*
             \            /            \            /            \            /            \            /            \
              *----------*  LONG_VALUE  *----------*  LONG_VALUE  *----------*  LONG_VALUE  *----------*  LONG_VALUE  *
                          \            /            \            /            \            /            \            /
                           *----------*              *----------*              *----------*              *----------*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `four by four triangle grid expands for long values`() {
        val grid = triangleGrid<String>(4, 4) {
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    place(TriangleCoordinate(col, row), data = LONG_VALUE)
                }
            }
        }

        assertEquals(
            """
              **************----------------**************----------------*
             /  LONG_VALUE  \  LONG_VALUE  /  LONG_VALUE  \  LONG_VALUE  /
            *----------------**************----------------**************
             \  LONG_VALUE  /  LONG_VALUE  \  LONG_VALUE  /  LONG_VALUE  \
              **************----------------**************----------------*
             /  LONG_VALUE  \  LONG_VALUE  /  LONG_VALUE  \  LONG_VALUE  /
            *----------------**************----------------**************
             \  LONG_VALUE  /  LONG_VALUE  \  LONG_VALUE  /  LONG_VALUE  \
              **************----------------**************----------------*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `four by four diamond grid expands for long values`() {
        val grid = diamondGrid<String>(4, 4) {
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    place(DiamondCoordinate(row, col), data = LONG_VALUE)
                }
            }
        }

        assertEquals(
            """
              ************              ************              ************              ************
             /            \            /            \            /            \            /            \
            *  LONG_VALUE  ************  LONG_VALUE  ************  LONG_VALUE  ************  LONG_VALUE  ************
             \            /            \            /            \            /            \            /            \
              ************  LONG_VALUE  ************  LONG_VALUE  ************  LONG_VALUE  ************  LONG_VALUE  *
             /            \            /            \            /            \            /            \            /
            *  LONG_VALUE  ************  LONG_VALUE  ************  LONG_VALUE  ************  LONG_VALUE  ************
             \            /            \            /            \            /            \            /            \
              ************  LONG_VALUE  ************  LONG_VALUE  ************  LONG_VALUE  ************  LONG_VALUE  *
                          \            /            \            /            \            /            \            /
                           ************              ************              ************              ************
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `four by four square grid expands for coordinate-sized values`() {
        val grid = squareGrid<String>(4, 4) {
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    place(SquareCoordinate(col, row), data = coordinateSizedValue(row, col))
                }
            }
        }

        assertEquals(
            """
            *----------*-----------*------------*-------------*
            |   r_c    |   r_cc    |   r_ccc    |   r_cccc    |
            *----------*-----------*------------*-------------*
            |   rr_c   |   rr_cc   |   rr_ccc   |   rr_cccc   |
            *----------*-----------*------------*-------------*
            |  rrr_c   |  rrr_cc   |  rrr_ccc   |  rrr_cccc   |
            *----------*-----------*------------*-------------*
            |  rrrr_c  |  rrrr_cc  |  rrrr_ccc  |  rrrr_cccc  |
            *----------*-----------*------------*-------------*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `four by four hex grid expands for coordinate-sized values`() {
        val grid = hexGrid<String>(4, 4) {
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    place(HexCoordinate(row, col), data = coordinateSizedValue(row, col))
                }
            }
        }

        assertEquals(
            """
              *------*           *-------*            *--------*             *---------*
             /        \         /         \          /          \           /           \
            *   r_c    *-------*   r_cc    *--------*   r_ccc    *---------*   r_cccc    *---------*
             \        /         \         /          \          /           \           /           \
              *------*   rr_c    *-------*   rr_cc    *--------*   rr_ccc    *---------*   rr_cccc   *
             /        \         /         \          /          \           /           \           /
            *  rrr_c   *-------*  rrr_cc   *--------*  rrr_ccc   *---------*  rrr_cccc   *---------*
             \        /         \         /          \          /           \           /           \
              *------*  rrrr_c   *-------*  rrrr_cc   *--------*  rrrr_ccc   *---------*  rrrr_cccc  *
                      \         /         \          /          \           /           \           /
                       *-------*           *--------*            *---------*             *---------*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `four by four triangle grid expands for coordinate-sized values`() {
        val grid = triangleGrid<String>(4, 4) {
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    place(TriangleCoordinate(col, row), data = coordinateSizedValue(row, col))
                }
            }
        }

        assertEquals(
            """
              **********-------------************---------------*
             /   r_c    \   r_cc    /   r_ccc    \   r_cccc    /
            *------------***********--------------*************
             \   rr_c   /   rr_cc   \   rr_ccc   /   rr_cccc   \
              **********-------------************---------------*
             /  rrr_c   \  rrr_cc   /  rrr_ccc   \  rrr_cccc   /
            *------------***********--------------*************
             \  rrrr_c  /  rrrr_cc  \  rrrr_ccc  /  rrrr_cccc  \
              **********-------------************---------------*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `four by four diamond grid expands for coordinate-sized values`() {
        val grid = diamondGrid<String>(4, 4) {
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    place(DiamondCoordinate(row, col), data = coordinateSizedValue(row, col))
                }
            }
        }

        assertEquals(
            """
              ********           *********            **********             ***********
             /        \         /         \          /          \           /           \
            *   r_c    *********   r_cc    **********   r_ccc    ***********   r_cccc    ***********
             \        /         \         /          \          /           \           /           \
              ********   rr_c    *********   rr_cc    **********   rr_ccc    ***********   rr_cccc   *
             /        \         /         \          /          \           /           \           /
            *  rrr_c   *********  rrr_cc   **********  rrr_ccc   ***********  rrr_cccc   ***********
             \        /         \         /          \          /           \           /           \
              ********  rrrr_c   *********  rrrr_cc   **********  rrrr_ccc   ***********  rrrr_cccc  *
                      \         /         \          /          \           /           \           /
                       *********           **********            ***********             ***********
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `complex square minesweeper grid renders shared box art`() {
        val grid = squareGrid<String>(4, 4) {
            place(SquareCoordinate(0, 0), data = "M")
            place(SquareCoordinate(1, 0), data = "1")
            place(SquareCoordinate(0, 1), data = "1")
            place(SquareCoordinate(1, 1), data = "1")
            place(SquareCoordinate(1, 2), data = "1")
            place(SquareCoordinate(2, 2), data = "2")
            place(SquareCoordinate(3, 2), data = "2")
            place(SquareCoordinate(1, 3), data = "1")
            place(SquareCoordinate(2, 3), data = "M")
            place(SquareCoordinate(3, 3), data = "M")
        }

        assertEquals(
            """
            *-----*-----*-----*-----*
            |  M  |  1  |     |     |
            *-----*-----*-----*-----*
            |  1  |  1  |     |     |
            *-----*-----*-----*-----*
            |     |  1  |  2  |  2  |
            *-----*-----*-----*-----*
            |     |  1  |  M  |  M  |
            *-----*-----*-----*-----*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `complex triangle minesweeper grid renders shared box art`() {
        val grid = triangleGrid<String>(5, 5) {
            val labels = listOf(
                listOf("M", "1", "1", null, null),
                listOf("1", "2", "2", "1", null),
                listOf("1", "1", "M", "1", "1"),
                listOf("1", "1", "1", "2", "2"),
                listOf(null, null, null, "1", "M")
            )
            labels.forEachIndexed { row, values ->
                values.forEachIndexed { col, value ->
                    if (value != null) {
                        place(TriangleCoordinate(col, row), data = value)
                    }
                }
            }
        }

        assertEquals(
            """
              *****-------*****-------*****
             /  M  \  1  /  1  \     /     \
            *-------*****-------*****-------*
             \  1  /  2  \  2  /  1  \     /
              *****-------*****-------*****
             /  1  \  1  /  M  \  1  /  1  \
            *-------*****-------*****-------*
             \  1  /  1  \  1  /  2  \  2  /
              *****-------*****-------*****
             /     \     /     \  1  /  M  \
            *-------*****-------*****-------*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    @Test
    fun `complex hex minesweeper grid renders shared box art`() {
        val grid = hexGrid<String>(11, 6) {
            val labels = listOf(
                listOf("M", "1", null, null, null, null),
                listOf("2", "1", null, null, null, null),
                listOf("1", "M", null, null, null, null),
                listOf("1", "1", null, null, null, null),
                listOf(null, "1", null, "1", "1", null),
                listOf(null, null, "1", "2", "1", null),
                listOf(null, null, null, "M", "M", null),
                listOf(null, null, "1", "3", "1", null),
                listOf("1", null, null, "2", "2", null),
                listOf("1", null, null, "M", null, null),
                listOf("M", null, null, "1", "1", null)
            )
            labels.forEachIndexed { row, values ->
                values.forEachIndexed { col, value ->
                    if (value != null) {
                        place(HexCoordinate(row, col), data = value)
                    }
                }
            }
        }

        assertEquals(
            """
              *-*     *-*     *-*     *-*     *-*     *-*
             /   \   /   \   /   \   /   \   /   \   /   \
            *  M  *-*  1  *-*     *-*     *-*     *-*     *-*
             \   /   \   /   \   /   \   /   \   /   \   /   \
              *-*  2  *-*  1  *-*     *-*     *-*     *-*     *
             /   \   /   \   /   \   /   \   /   \   /   \   /
            *  1  *-*  M  *-*     *-*     *-*     *-*     *-*
             \   /   \   /   \   /   \   /   \   /   \   /   \
              *-*  1  *-*  1  *-*     *-*     *-*     *-*     *
             /   \   /   \   /   \   /   \   /   \   /   \   /
            *     *-*  1  *-*     *-*  1  *-*  1  *-*     *-*
             \   /   \   /   \   /   \   /   \   /   \   /   \
              *-*     *-*     *-*  1  *-*  2  *-*  1  *-*     *
             /   \   /   \   /   \   /   \   /   \   /   \   /
            *     *-*     *-*     *-*  M  *-*  M  *-*     *-*
             \   /   \   /   \   /   \   /   \   /   \   /   \
              *-*     *-*     *-*  1  *-*  3  *-*  1  *-*     *
             /   \   /   \   /   \   /   \   /   \   /   \   /
            *  1  *-*     *-*     *-*  2  *-*  2  *-*     *-*
             \   /   \   /   \   /   \   /   \   /   \   /   \
              *-*  1  *-*     *-*     *-*  M  *-*     *-*     *
             /   \   /   \   /   \   /   \   /   \   /   \   /
            *  M  *-*     *-*     *-*  1  *-*  1  *-*     *-*
             \   /   \   /   \   /   \   /   \   /   \   /
              *-*     *-*     *-*     *-*     *-*     *-*
            """.trimIndent(),
            grid.toAsciiString()
        )
    }

    private companion object {
        const val LONG_VALUE = "LONG_VALUE"

        fun coordinateSizedValue(row: Int, col: Int): String =
            "r".repeat(row + 1) + "_" + "c".repeat(col + 1)
    }

}

package io.gridkit.core.direction

import io.gridkit.core.core.GridDirection

/** Union of all hex-grid direction kinds — edge sides and vertex corners. */
sealed interface HexDirection : GridDirection

/**
 * The six edge/neighbor directions for offset-coordinate hexagonal grids.
 *
 * ```
 *   TOP_LEFT  TOP_RIGHT
 * LEFT          RIGHT
 *   DOWN_LEFT DOWN_RIGHT
 * ```
 */
enum class HexEdgeDirection : HexDirection {
    TOP_LEFT, TOP_RIGHT,
    LEFT, RIGHT,
    DOWN_LEFT, DOWN_RIGHT
}

/**
 * The six vertex corner directions for a pointy-top hexagonal cell.
 *
 * Vertices are listed clockwise from the top point:
 * ```
 *        TOP
 *   TL *     * TR
 *      |     |
 *   DL *     * DR
 *        DOWN
 * ```
 */
enum class HexVertexDirection : HexDirection {
    TOP,
    TOP_RIGHT, DOWN_RIGHT,
    DOWN,
    DOWN_LEFT, TOP_LEFT
}

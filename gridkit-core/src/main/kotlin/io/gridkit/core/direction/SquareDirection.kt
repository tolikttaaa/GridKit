package io.gridkit.core.direction

import io.gridkit.core.core.GridDirection

/** Union of all square-grid direction kinds — edge sides and vertex corners. */
sealed interface SquareDirection : GridDirection

/**
 * The four cardinal edge/neighbor directions on a square grid.
 *
 * ```
 *        TOP
 *    *---*---*
 *    |       |
 * LEFT  cell  RIGHT
 *    |       |
 *    *---*---*
 *       BOTTOM
 * ```
 */
enum class SquareEdgeDirection : SquareDirection { TOP, BOTTOM, LEFT, RIGHT }

/**
 * The four corner vertex directions on a square grid.
 *
 * ```
 * TOP_LEFT *---* TOP_RIGHT
 *          |   |
 * DOWN_LEFT*---* DOWN_RIGHT
 * ```
 */
enum class SquareVertexDirection : SquareDirection { TOP_LEFT, TOP_RIGHT, DOWN_LEFT, DOWN_RIGHT }

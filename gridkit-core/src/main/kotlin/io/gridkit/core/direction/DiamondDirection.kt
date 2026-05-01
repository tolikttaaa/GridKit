package io.gridkit.core.direction

import io.gridkit.core.core.GridDirection

/** Union of all diamond-grid direction kinds — edge sides and vertex corners. */
sealed interface DiamondDirection : GridDirection

/**
 * The four edge/neighbor directions for a diamond cell.
 *
 * ```
 *               *
 *    TOP_LEFT  / \  TOP_RIGHT
 *             *   *
 * BOTTOM_LEFT  \ /  BOTTOM_RIGHT
 *               *
 * ```
 */
enum class DiamondEdgeDirection : DiamondDirection { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/**
 * The four vertex corner directions for a diamond cell.
 *
 * ```
 *      * TOP
 *     / \
 * LEFT   RIGHT
 *     \ /
 *      * DOWN
 * ```
 */
enum class DiamondVertexDirection : DiamondDirection { TOP, LEFT, RIGHT, DOWN }

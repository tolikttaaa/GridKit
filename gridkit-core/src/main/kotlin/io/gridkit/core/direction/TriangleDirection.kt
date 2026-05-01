package io.gridkit.core.direction

import io.gridkit.core.core.GridDirection

/** Union of all triangle-grid direction kinds — edge sides and vertex corners. */
sealed interface TriangleDirection : GridDirection

/**
 * The three edge/neighbor directions for a triangular cell.
 *
 * - **LEFT** / **RIGHT** — diagonal-edge neighbors in the same row
 * - **VERTICAL** — the horizontal-edge neighbor across a row boundary
 */
enum class TriangleEdgeDirection : TriangleDirection { LEFT, RIGHT, VERTICAL }

/**
 * The three vertex corner directions for a triangular cell.
 *
 * - **LEFT** / **RIGHT** — the base corners
 * - **VERTICAL** — the apex: top for UP-pointing (∆), bottom for DOWN-pointing (∇)
 */
enum class TriangleVertexDirection : TriangleDirection { LEFT, RIGHT, VERTICAL }

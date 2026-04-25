package io.gridkit.core.core

/**
 * The result of computing a grid's physical center of mass.
 *
 * Combines what were previously two separate interface methods:
 * `physicalCenter()` (the raw x/y) and `nearestCoordinate()` (the snapped cell).
 *
 * @property physical raw physical position (average of all non-Blocked cell positions)
 * @property coordinate the grid cell nearest to [physical]
 */
data class GridCenter<out C : GridCoordinate>(
    val physical: PhysicalCenter,
    val coordinate: C
)

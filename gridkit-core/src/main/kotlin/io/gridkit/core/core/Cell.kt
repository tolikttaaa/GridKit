package io.gridkit.core.core

/**
 * A single cell on the grid.
 *
 * @param C the coordinate type of the owning grid
 * @param D the type of optional application-specific payload stored in this cell
 * @property coordinate the position of this cell
 * @property state the current occupancy state
 * @property data optional payload; null when not set
 */
data class Cell<C : GridCoordinate, D>(
    val coordinate: C,
    val state: CellState = CellState.Empty,
    val data: D? = null
)

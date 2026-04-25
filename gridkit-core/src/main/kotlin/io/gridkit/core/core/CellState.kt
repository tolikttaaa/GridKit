package io.gridkit.core.core

/** The occupancy state of a grid cell. */
enum class CellState {
    /** Cell exists and is unoccupied. */
    Empty,
    /** Cell is occupied by a game piece. */
    Occupied,
    /** Cell is permanently impassable (wall, void, etc.). */
    Blocked
}

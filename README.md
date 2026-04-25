# GridKit

![GridKit Logo](GridKit.png)

[![CI](https://github.com/tolikttaaa/GridKit/actions/workflows/ci.yml/badge.svg)](https://github.com/tolikttaaa/GridKit/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/tolikttaaa/GridKit/branch/main/graph/badge.svg)](https://codecov.io/gh/tolikttaaa/GridKit)

A Kotlin library providing a unified abstraction layer for tile-based game boards.
GridKit supports square, hexagonal, and triangular grid topologies through a single,
topology-agnostic API — game logic written against `Grid<C>` works identically
regardless of the underlying grid type.

---

## Quick Start

```kotlin
// Square grid — chess / minesweeper style
val board = squareGrid(width = 8, height = 8) {
    block(SquareCoordinate(3, 3))
}

// Hexagonal grid — Catan / hex-strategy style
val hexBoard = hexGrid(rows = 5, cols = 6) {
    block(HexCoordinate(row = 1, col = 2))
}

// Triangular grid — triomino / puzzle style
val triBoard = triangleGrid(width = 8, height = 4) {}

// Pathfinding — works the same on all topologies
val path = board.findPath(SquareCoordinate(0, 0), SquareCoordinate(7, 7))
println(path)  // [SquareCoordinate(col=0, row=0), ..., SquareCoordinate(col=7, row=7)]

// Flood fill
val reachable = board.flood(SquareCoordinate(0, 0))
println("Reachable cells: ${reachable.size}")

// Connectivity check
println("Board is connected: ${board.isConnected()}")

// ASCII map
println(board.toAsciiMap())
```

---

## Grid Topologies

### Square Grid

```
(0,0) (1,0) (2,0) (3,0) (4,0)
(0,1) (1,1) (2,1) (3,1) (4,1)
(0,2) (1,2) (2,2) (3,2) (4,2)
(0,3) (1,3) (2,3) (3,3) (4,3)
(0,4) (1,4) (2,4) (3,4) (4,4)
```

`SquareCoordinate(col, row)` — col increases rightward, row increases downward.

- `diagonal = false` → 4-connected (cardinal only)
- `diagonal = true` → 8-connected (cardinal + diagonal)

```kotlin
val grid = SquareGrid(5, 5, diagonal = false)
grid.getNeighbor(SquareCoordinate(2, 2), SquareDirection.UP)   // → SquareCoordinate(2, 1)
grid.getNeighbor(SquareCoordinate(2, 2), SquareDirection.DOWN) // → SquareCoordinate(2, 3)
```

Available directions: `UP`, `DOWN`, `LEFT`, `RIGHT`, `UP_LEFT`, `UP_RIGHT`, `DOWN_LEFT`, `DOWN_RIGHT`

---

### Hexagonal Grid (Offset Coordinates)

GridKit uses **even-r offset coordinates** — NOT cube coordinates.
Row increases downward; col increases rightward.
Odd rows are visually shifted right by half a hex width.

```
Even row:    .  .  .  .  .
Odd row:      .  .  .  .  .
Even row:    .  .  .  .  .
```

**Neighbor offsets:**

| Direction  | Even row         | Odd row          |
|------------|------------------|------------------|
| TOP_LEFT   | (row-1, col-1)   | (row-1, col)     |
| TOP_RIGHT  | (row-1, col)     | (row-1, col+1)   |
| LEFT       | (row,   col-1)   | (row,   col-1)   |
| RIGHT      | (row,   col+1)   | (row,   col+1)   |
| DOWN_LEFT  | (row+1, col-1)   | (row+1, col)     |
| DOWN_RIGHT | (row+1, col)     | (row+1, col+1)   |

```kotlin
val hex = HexGrid(5, 5)
// Even row: (2,2)
hex.getNeighbor(HexCoordinate(2, 2), HexDirection.TOP_LEFT)   // → HexCoordinate(1, 1)
hex.getNeighbor(HexCoordinate(2, 2), HexDirection.DOWN_RIGHT) // → HexCoordinate(3, 2)

// Odd row: (1,2)
hex.getNeighbor(HexCoordinate(1, 2), HexDirection.TOP_LEFT)   // → HexCoordinate(0, 2)
hex.getNeighbor(HexCoordinate(1, 2), HexDirection.DOWN_RIGHT) // → HexCoordinate(2, 3)
```

**Physical coordinate mapping** (flat-top, hexWidth=1.0, hexHeight=1.0):
```
x = col * hexWidth + (if oddRow then hexWidth/2 else 0.0)
y = row * hexHeight * 0.75
```

---

### Triangular Grid

Each `(col, row)` slot contains two triangles: `UP` (apex at top) and `DOWN` (apex at bottom).
Each triangle has exactly 3 neighbors.

```
col:  0    1    2    3
    /\UP /\UP /\UP /\UP
   /DN\/DN\/DN\/DN\
   \UP/\UP/\UP/\UP/
    \/DN\/DN\/DN\/
```

**Neighbor rules:**

| Cell              | LEFT             | RIGHT             | VERTICAL          |
|-------------------|------------------|-------------------|-------------------|
| UP(col, row)      | DOWN(col-1, row) | DOWN(col, row)    | DOWN(col, row-1)  |
| DOWN(col, row)    | UP(col, row)     | UP(col+1, row)    | UP(col, row+1)    |

```kotlin
val tri = TriangleGrid(4, 4)
tri.getNeighbor(TriangleCoordinate(2, 2, UP), TriangleNeighborDirection.RIGHT)
// → TriangleCoordinate(2, 2, DOWN)
```

---

## Dynamic Tile Placement

Boards can grow during play — useful for Carcassonne-style games:

```kotlin
val grid = hexGrid(rows = 1, cols = 1) {}
// Start with just HexCoordinate(0, 0)

val newCell = grid.placeNext(HexCoordinate(0, 0), HexDirection.RIGHT)
// → creates and registers HexCoordinate(0, 1)
// Bounding box expands automatically

val sameCell = grid.placeNext(HexCoordinate(0, 0), HexDirection.RIGHT)
// → returns existing HexCoordinate(0, 1)  (idempotent)
```

`placeNext` is available on all three grid types with their respective direction enums.

---

## Center Calculations

### Arithmetic Center

Returns the coordinate closest to the geometric middle of the bounding box (floor division):

```kotlin
val sq = SquareGrid(5, 5)
sq.arithmeticCenter()  // → SquareCoordinate(col=2, row=2)

val sq4 = SquareGrid(4, 4)
sq4.arithmeticCenter() // → SquareCoordinate(col=1, row=1)  (floor of 3/2 = 1)
```

### Physical Center

Returns the average physical (x, y) position of all non-Blocked cells:

```kotlin
val hex = hexGrid(rows = 4, cols = 6) {
    block(HexCoordinate(0, 0))
    block(HexCoordinate(3, 5))
}
val pc = hex.physicalCenter()  // → PhysicalCenter(x≈2.87, y≈1.65)
```

Blocked cells are excluded from the average.

### Nearest Coordinate

Snaps a physical position back to the nearest grid cell:

```kotlin
val nc = hex.nearestCoordinate(pc)  // → HexCoordinate(row=2, col=3)
```

---

## Pathfinding

GridKit uses A* with the grid's own `distance()` function as heuristic:

```kotlin
// Default: treats Blocked cells as impassable
val path = grid.findPath(from, to)

// Custom predicate
val path = grid.findPath(from, to) { cell ->
    cell.state != CellState.Blocked && cell.state != CellState.Occupied
}
// Returns null when no path exists
```

---

## Extensions

```kotlin
import io.gridkit.core.extensions.*

// Flood fill — BFS from start, collecting all reachable cells
val reachable: Set<SquareCoordinate> = grid.flood(SquareCoordinate(0, 0))

// With custom predicate
val reachable = grid.flood(start) { cell -> cell.state == CellState.Empty }

// Connectivity check — true when all traversable cells form one component
val connected: Boolean = grid.isConnected()

// ASCII debug map
println(grid.toAsciiMap())
// . . . . .
// . . # . .
// . . . . .
```

---

## API Reference

### Grid Interface

| Method                                   | Description                                                   |
|------------------------------------------|---------------------------------------------------------------|
| `cells: Map<C, Cell<C>>`                 | All cells in the grid                                         |
| `getCell(C): Cell<C>?`                   | Cell at coordinate, or null                                   |
| `getNeighbors(C): List<Cell<C>>`         | Adjacent cells (topology-specific)                            |
| `isValidCoordinate(C): Boolean`          | Whether coordinate exists in grid                             |
| `findPath(C, C, predicate): List<C>?`    | A* path; null if unreachable                                  |
| `getRange(C, Int): List<Cell<C>>`        | All cells within radius steps                                 |
| `getRing(C, Int): List<Cell<C>>`         | Cells exactly radius steps away                               |
| `getLine(C, C): List<Cell<C>>`           | Straight line between two cells                               |
| `distance(C, C): Int`                    | Step distance between two coordinates                         |
| `placeNext(C, direction): Cell<C>`       | Create or return adjacent cell, expanding grid if needed      |
| `arithmeticCenter(): C`                  | Coordinate closest to bounding box center                     |
| `physicalCenter(): PhysicalCenter`       | Average (x,y) of all non-Blocked cells                       |
| `nearestCoordinate(PhysicalCenter): C`   | Snap physical position to nearest coordinate                  |

### Grid-Specific APIs

| Class         | Extra method                                              |
|---------------|-----------------------------------------------------------|
| `SquareGrid`  | `getNeighbor(C, SquareDirection): C?`                    |
|               | `getDirectedNeighbors(C): Map<SquareDirection, Cell<C>>` |
|               | `placeNext(C, SquareDirection): Cell<C>`                 |
|               | `toPhysical(C): PhysicalCenter`                          |
| `HexGrid`     | `getNeighbor(C, HexDirection): C?`                       |
|               | `getDirectedNeighbors(C): Map<HexDirection, Cell<C>>`    |
|               | `placeNext(C, HexDirection): Cell<C>`                    |
|               | `toPhysical(C): PhysicalCenter`                          |
| `TriangleGrid`| `getNeighbor(C, TriangleNeighborDirection): C?`          |
|               | `getDirectedNeighbors(C): Map<TriangleNeighborDirection, Cell<C>>` |
|               | `placeNext(C, TriangleNeighborDirection): Cell<C>`       |
|               | `toPhysical(C): PhysicalCenter`                          |

---

## Module Structure

```
gridkit/
├── gridkit-core/           ← Pure grid logic, zero UI dependencies
│   └── io.gridkit.core
│       ├── core/           GridCoordinate, Cell, CellState, Grid, PhysicalCenter
│       ├── grid/           SquareGrid, HexGrid, TriangleGrid (+ direction enums)
│       ├── pathfinding/    PathfindingStrategy, AStarPathfinder
│       ├── dsl/            squareGrid {}, hexGrid {}, triangleGrid {}
│       └── extensions/     flood(), isConnected(), toAsciiMap()
│
└── gridkit-visualization/  ← Placeholder for future renderers (Compose, JavaFX, SVG)
    └── io.gridkit.visualization
```

`gridkit-visualization` depends on `gridkit-core`; `gridkit-core` has zero dependency
on `gridkit-visualization`, keeping the core renderer-agnostic.

---

## Building

```bash
./gradlew :gridkit-core:test        # run tests
./gradlew :gridkit-core:build       # compile + test + jar
./gradlew build                     # build all modules
```

Requires JDK 11+. No external runtime dependencies (stdlib only).

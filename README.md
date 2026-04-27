# GridKit

![GridKit Logo](GridKit.png)

[![CI](https://github.com/tolikttaaa/GridKit/actions/workflows/ci.yml/badge.svg)](https://github.com/tolikttaaa/GridKit/actions/workflows/ci.yml)
[![Coverage](https://raw.githubusercontent.com/tolikttaaa/GridKit/badges/.github/badges/jacoco.svg)](https://github.com/tolikttaaa/GridKit/actions/workflows/ci.yml)

A Kotlin library providing a unified abstraction layer for tile-based game boards.
GridKit supports square, hexagonal, and triangular grid topologies through a single,
topology-agnostic API — game logic written against `Grid<C, Dir, D>` works identically
regardless of the underlying grid type.

---

## Quick Start

```kotlin
// Square grid — chess / minesweeper style
val board = squareGrid<String>(width = 8, height = 8) {
    place(SquareCoordinate(3, 3), data = "M")
}

// Hexagonal grid — Catan / hex-strategy style
val hexBoard = hexGrid<String>(rows = 5, cols = 6) {
    place(HexCoordinate(row = 1, col = 2), data = "forest")
}

// Triangular grid — even col = UP /\, odd col = DOWN \/
val triBoard = triangleGrid<Nothing>(cols = 12, rows = 4) {}

// Attach typed data to cells
val labeled = squareGrid<String>(5, 5) {
    place(SquareCoordinate(2, 2), data = "treasure")
}

// Pathfinding — works the same on all topologies
val path = board.findPath(SquareCoordinate(0, 0), SquareCoordinate(7, 7))

// Flood fill & connectivity
val reachable = board.flood(SquareCoordinate(0, 0))
println("Connected: ${board.isConnected()}")

// ASCII box-art renderer
println(board.toAsciiString())

// Compact ASCII debug map
println(board.toAsciiMap())
```

---

## Grid Topologies

### Square Grid

```
(0,0) (1,0) (2,0) (3,0) (4,0)
(0,1) (1,1) (2,1) (3,1) (4,1)
(0,2) (1,2) (2,2) (3,2) (4,2)
```

`SquareCoordinate(col, row)` — col increases rightward, row increases downward.

- `diagonal = false` → 4-connected (cardinal only)
- `diagonal = true`  → 8-connected (cardinal + diagonal)

```kotlin
val grid = SquareGrid<Nothing>(5, 5)
grid.getNeighbor(SquareCoordinate(2, 2), SquareDirection.UP)   // → SquareCoordinate(2, 1)
grid.getDirectedNeighbors(SquareCoordinate(2, 2))              // → Map<SquareDirection, Cell<…>>
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

| Direction  | Even row       | Odd row        |
|------------|----------------|----------------|
| TOP_LEFT   | (row-1, col-1) | (row-1, col)   |
| TOP_RIGHT  | (row-1, col)   | (row-1, col+1) |
| LEFT       | (row,   col-1) | (row,   col-1) |
| RIGHT      | (row,   col+1) | (row,   col+1) |
| DOWN_LEFT  | (row+1, col-1) | (row+1, col)   |
| DOWN_RIGHT | (row+1, col)   | (row+1, col+1) |

**Physical coordinate mapping** (flat-top, hexWidth=1.0, hexHeight=1.0):
```
x = col * hexWidth + (if oddRow then hexWidth/2 else 0.0)
y = row * hexHeight * 0.75
```

---

### Triangular Grid

Each cell is a `TriangleCoordinate(col, row)`. The pointing direction is encoded
directly in the **parity of `col`** — no separate direction field:

- **even `col`** → UP-pointing triangle `/ \` (apex at top)
- **odd `col`**  → DOWN-pointing triangle `\ /` (apex at bottom)

```
col:  0  1  2  3  4  5  6  7
      /\ \/ /\ \/ /\ \/ /\ \/   row 0
      /\ \/ /\ \/ /\ \/ /\ \/   row 1
```

**Neighbor rules** (unified formula):

| Direction | Formula              |
|-----------|----------------------|
| LEFT      | `(col-1, row)`       |
| RIGHT     | `(col+1, row)`       |
| VERTICAL  | `(col+1, row-1)` if even col (UP) |
| VERTICAL  | `(col-1, row+1)` if odd  col (DOWN) |

```kotlin
val grid = TriangleGrid<Nothing>(cols = 8, rows = 4)

// Check orientation via extension properties
TriangleCoordinate(4, 2).isUp   // true  — even col
TriangleCoordinate(5, 2).isDown // true  — odd col

grid.getNeighbor(TriangleCoordinate(4, 2), TriangleNeighborDirection.VERTICAL)
// UP triangle → (5, 1)

grid.getNeighbor(TriangleCoordinate(5, 2), TriangleNeighborDirection.VERTICAL)
// DOWN triangle → (4, 3)
```

**Physical centroid mapping** (`slot = col / 2`):
```
x = slot * 0.5 + (if UP then 0.166 else 0.333)
y = row  * triHeight + (if UP then 0.333 else 0.666)
```

---

## Cell Data Payloads

Every cell carries an optional typed payload `D` (null when not set):

```kotlin
data class Terrain(val elevation: Int, val biome: String)

val map = hexGrid<Terrain>(rows = 5, cols = 6) {
    place(HexCoordinate(2, 3), data = Terrain(elevation = 500, biome = "forest"))
}

map.getCell(HexCoordinate(2, 3))?.data   // Terrain(500, "forest")
```

Use `<Nothing>` when you don't need per-cell data.

---

## Dynamic Tile Placement

Boards can grow at runtime — useful for Carcassonne-style games:

```kotlin
val grid = hexGrid<Nothing>(rows = 1, cols = 1) {}
val newCell = grid.placeNext(HexCoordinate(0, 0), HexDirection.RIGHT)
// → creates HexCoordinate(0, 1); bounding box expands automatically

val same = grid.placeNext(HexCoordinate(0, 0), HexDirection.RIGHT)
// → returns existing cell (idempotent)
```

`placeNext` is available on all three grid types with their typed direction enums.

---

## Center Calculations

### Arithmetic Center

Returns the coordinate closest to the geometric middle of the bounding box:

```kotlin
SquareGrid<Nothing>(5, 5).arithmeticCenter()  // SquareCoordinate(col=2, row=2)
SquareGrid<Nothing>(4, 4).arithmeticCenter()  // SquareCoordinate(col=1, row=1)  (floor)
```

### Physical Center

`physicalCenter()` returns a `GridCenter<C>` combining both the raw position and
the nearest coordinate in one call — no need to call two separate methods:

```kotlin
val center = hexGrid<Nothing>(rows = 4, cols = 6) {
}.physicalCenter()

center.physical.x   // average x of all cells
center.physical.y   // average y of all cells
center.coordinate   // nearest HexCoordinate to that position
```

---

## Pathfinding

A* with the grid's own `distance()` as heuristic — works on all topologies:

```kotlin
val path = grid.findPath(from, to)                   // default: every cell is passable
val path = grid.findPath(from, to) { cell ->         // custom predicate
    cell.data?.passable == true
}
// Returns null when no path exists
```

---

## Extensions

```kotlin
import io.gridkit.core.extensions.*

// Flood fill
val reachable: Set<C> = grid.flood(start)
val reachable = grid.flood(start) { cell -> cell.data?.passable == true }

// Connectivity
val ok: Boolean = grid.isConnected()

// ASCII box-art renderer
println(grid.toAsciiString())

// ASCII debug map
println(grid.toAsciiMap())
```

`toAsciiString()` is implemented by each concrete grid type and renders square,
hexagonal, and triangular grids with shared ASCII borders. Cells use
`cell.data?.toString().orEmpty()` as their center label.

---

## Playground — Minesweeper Examples

The `gridkit-playground` module demonstrates library usage with a topology-agnostic
Minesweeper implementation that runs on all three grid types and renders each
demo through the grid's `toAsciiString()` implementation:

```
gridkit-playground/
└── src/main/kotlin/io/gridkit/playground/
    ├── minesweeper/
    │   └── MinesweeperGame.kt   ← works on any Grid<C, Dir, *>
    ├── examples/
    │   ├── SquareMinesweeper.kt
    │   ├── HexMinesweeper.kt
    │   └── TriangleMinesweeper.kt
    └── Main.kt                  ← runs all three demos
```

Run with:
```bash
./gradlew :gridkit-playground:run
```

---

## API Reference

### Grid Interface `Grid<C, Dir, D>`

| Method | Description |
|--------|-------------|
| `cells: Map<C, Cell<C, D>>` | All cells |
| `getCell(C): Cell<C, D>?` | Cell at coordinate |
| `getNeighbors(C): List<Cell<C, D>>` | Adjacent cells |
| `getDirectedNeighbors(C): Map<Dir, Cell<C, D>>` | Neighbors keyed by direction |
| `getNeighbor(C, Dir): C?` | Single neighbour coordinate |
| `isValidCoordinate(C): Boolean` | Bounds check |
| `findPath(C, C, predicate): List<C>?` | A* pathfinding |
| `getRange(C, Int): List<Cell<C, D>>` | All cells within radius |
| `getRing(C, Int): List<Cell<C, D>>` | Cells at exact radius |
| `getLine(C, C): List<Cell<C, D>>` | Straight line |
| `distance(C, C): Int` | Step distance |
| `placeNext(C, Dir): Cell<C, D>` | Add adjacent cell, expand grid |
| `arithmeticCenter(): C` | Bounding-box centre coordinate |
| `physicalCenter(): GridCenter<C>` | Physical centre + nearest coordinate |
| `toAsciiString(): String` | ASCII box-art renderer |

### Type Parameters

| Parameter | Bound | Role |
|-----------|-------|------|
| `C` | `GridCoordinate` | Coordinate type (`SquareCoordinate`, `HexCoordinate`, `TriangleCoordinate`) |
| `Dir` | `GridDirection` | Direction type (`SquareDirection`, `HexDirection`, `TriangleNeighborDirection`) |
| `D` | — | Optional per-cell data payload; use `Nothing` for topology-only grids |

---

## Module Structure

```
gridkit/
├── gridkit-core/           ← Pure grid logic, zero UI dependencies
│   └── io.gridkit.core
│       ├── core/           GridCoordinate, GridDirection, Cell,
│       │                   Grid, GridCenter, PhysicalCenter
│       ├── grid/           SquareGrid, HexGrid, TriangleGrid (+ direction enums)
│       ├── pathfinding/    PathfindingStrategy, AStarPathfinder
│       ├── dsl/            squareGrid {}, hexGrid {}, triangleGrid {}
│       └── extensions/     flood(), isConnected(), toAsciiMap()
│
├── gridkit-visualization/  ← Placeholder for future renderers
│
└── gridkit-playground/     ← Usage examples; Minesweeper on all 3 grid types
    └── io.gridkit.playground
```

---

## Building

```bash
./gradlew :gridkit-core:test              # run core tests
./gradlew :gridkit-playground:run         # run Minesweeper demos
./gradlew build                           # compile + test all modules
```

Requires JDK 21+. No external runtime dependencies (stdlib only).

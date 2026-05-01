# GridKit

![GridKit Logo](GridKit.png)

[![CI](https://github.com/tolikttaaa/GridKit/actions/workflows/ci.yml/badge.svg)](https://github.com/tolikttaaa/GridKit/actions/workflows/ci.yml)
[![Coverage](https://raw.githubusercontent.com/tolikttaaa/GridKit/badges/.github/badges/jacoco.svg)](https://github.com/tolikttaaa/GridKit/actions/workflows/ci.yml)

A Kotlin library providing a unified abstraction layer for tile-based game boards.
GridKit supports **square, hexagonal, triangular, and diamond** grid topologies
through a single topology-agnostic API. Built-in grids keep concise constructors
such as `SquareGrid<D>`, while custom grid implementations can specify their
own cell, edge, and vertex subclasses in the `Grid` type parameters.

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
val triBoard = triangleGrid<Nothing>(cols = 12, rows = 4)

// Diamond/diamond grid — isometric layout
val diamondBoard = diamondGrid<Nothing>(rows = 4, cols = 4)

// Pathfinding — works the same on all topologies
val path = board.findPath(SquareCoordinate(0, 0), SquareCoordinate(7, 7))

// Flood fill & connectivity
val reachable = board.flood(SquareCoordinate(0, 0))
println("Connected: ${board.isConnected()}")

// ASCII box-art renderer
println(board.toAsciiString())
```

---

## Vertex / Edge Topology Model

Every cell is a structural node in a shared topology graph. Adjacent cells hold
**the same** `Vertex` and `Edge` instances (referential equality `===`):

```
          Vertex (corner)
         /       \
  Edge ——— Cell ——— Edge
         \       /
          Vertex
```

```kotlin
val grid = SquareGrid<Nothing>(3, 3)
val a = grid.getCell(SquareCoordinate(1, 1))!!
val b = grid.getCell(SquareCoordinate(2, 1))!!   // RIGHT neighbor of a

// Same Edge instance
val edge = a.getEdge(SquareEdgeDirection.RIGHT)!!
assert(edge === b.getEdge(SquareEdgeDirection.LEFT))

// Same Vertex instance at the shared corner
assert(a.getVertex(SquareVertexDirection.TOP_RIGHT) ===
       b.getVertex(SquareVertexDirection.TOP_LEFT))

// Navigate via edges (no coordinate arithmetic)
val neighbor: Cell<…>? = a.getNeighbor(SquareEdgeDirection.RIGHT)

// Edge properties
edge.isBorder          // true when one side has no cell
edge.getOpposite(a)    // returns b (or null for border edges)
edge.vertices          // Set<Vertex> of exactly 2, unordered

// Vertex properties
edge.vertices.first().cells   // all cells sharing this corner
edge.vertices.first().edges   // all edges meeting at this corner
```

---

## Grid Topologies

### Square Grid

```
col→  0     1     2     3     4
row↓
0   (0,0) (1,0) (2,0) (3,0) (4,0)
1   (0,1) (1,1) (2,1) (3,1) (4,1)
2   (0,2) (1,2) (2,2) (3,2) (4,2)
```

`SquareCoordinate(col, row)` — col increases rightward, row increases downward.

- `diagonal = false` → 4-connected (cardinal only, **vertex/edge topology**)
- `diagonal = true`  → 8-connected neighbors via `getNeighbors()`/`getDirectedNeighbors()`

**Edge directions:** `SquareEdgeDirection` — `TOP`, `BOTTOM`, `LEFT`, `RIGHT`

**Vertex directions:** `SquareVertexDirection` — `TOP_LEFT`, `TOP_RIGHT`, `DOWN_LEFT`, `DOWN_RIGHT`

```kotlin
val grid = SquareGrid<Nothing>(5, 5)
grid.getNeighbor(SquareCoordinate(2, 2), SquareEdgeDirection.TOP)   // → (2, 1)
grid.getNeighbor(SquareCoordinate(2, 2), SquareEdgeDirection.RIGHT) // → (3, 2)
```

---

### Hexagonal Grid (Offset Coordinates)

GridKit uses **even-r offset coordinates** — NOT cube coordinates.
Row increases downward; col increases rightward.
Odd rows are visually shifted right by half a hex width.

```
col→   0     1     2     3     4
row↓
0   [0,0] [0,1] [0,2] [0,3] [0,4]
1      [1,0] [1,1] [1,2] [1,3] [1,4]   ← odd row offset
2   [2,0] [2,1] [2,2] [2,3] [2,4]
```

**Edge directions:** `HexEdgeDirection` — `TOP_LEFT`, `TOP_RIGHT`, `LEFT`, `RIGHT`, `DOWN_LEFT`, `DOWN_RIGHT`

**Vertex directions:** `HexVertexDirection` — `TOP`, `TOP_LEFT`, `TOP_RIGHT`, `DOWN_LEFT`, `DOWN`, `DOWN_RIGHT`

**Neighbor offsets:**

| Direction  | Even row       | Odd row        |
|------------|----------------|----------------|
| TOP_LEFT   | (row-1, col-1) | (row-1, col)   |
| TOP_RIGHT  | (row-1, col)   | (row-1, col+1) |
| LEFT       | (row,   col-1) | (row,   col-1) |
| RIGHT      | (row,   col+1) | (row,   col+1) |
| DOWN_LEFT  | (row+1, col-1) | (row+1, col)   |
| DOWN_RIGHT | (row+1, col)   | (row+1, col+1) |

**Physical coordinate mapping** (hexWidth=1.0, hexHeight=1.0):
```
x = col * hexWidth + (if oddRow then hexWidth/2 else 0.0)
y = row * hexHeight * 0.75
```

---

### Triangular Grid

Each cell is a `TriangleCoordinate(col, row)`. The pointing direction is encoded
in the **parity of `col`**:

- **even `col`** → UP-pointing triangle `∆` (apex at top)
- **odd `col`**  → DOWN-pointing triangle `∇` (apex at bottom)

```
col:  0  1  2  3  4  5  6  7
      /\ \/ /\ \/ /\ \/ /\ \/   row 0
      /\ \/ /\ \/ /\ \/ /\ \/   row 1
```

**Edge directions:** `TriangleEdgeDirection` — `LEFT`, `RIGHT`, `VERTICAL`

**Vertex directions:** `TriangleVertexDirection` — `LEFT`, `RIGHT`, `VERTICAL`
(VERTICAL = apex: top for UP-pointing, bottom for DOWN-pointing)

**Neighbor rules:**

| Direction | Formula                             |
|-----------|-------------------------------------|
| LEFT      | `(col-1, row)`                      |
| RIGHT     | `(col+1, row)`                      |
| VERTICAL  | `(col+1, row-1)` if even col (UP)   |
| VERTICAL  | `(col-1, row+1)` if odd  col (DOWN) |

```kotlin
val grid = TriangleGrid<Nothing>(cols = 8, rows = 4)

TriangleCoordinate(4, 2).isUp   // true  — even col
TriangleCoordinate(5, 2).isDown // true  — odd col

grid.getNeighbor(TriangleCoordinate(4, 2), TriangleEdgeDirection.VERTICAL) // → (5, 1)
grid.getNeighbor(TriangleCoordinate(5, 2), TriangleEdgeDirection.VERTICAL) // → (4, 3)
```

**Physical centroid mapping** (`slot = col / 2`):
```
x = slot * 0.5 + (if UP then 0.166 else 0.333)
y = row  * triHeight + (if UP then 0.333 else 0.666)
```

---

### Diamond / Diamond Grid

Diamond-shaped cells laid out in an isometric (rotated-square) pattern.

```
col→   0    1    2    3
row↓
0    (0,0)
1    (1,0)(1,1)
2    (2,0)(2,1)(2,2)(2,3)
3    (3,0)(3,1)(3,2)(3,3)
```

**Edge directions:** `DiamondEdgeDirection` — `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`

**Vertex directions:** `DiamondVertexDirection` — `TOP`, `LEFT`, `RIGHT`, `DOWN`

**Neighbor offsets:**

| Direction    | Offset       |
|--------------|--------------|
| TOP_LEFT     | (row, col-1) |
| TOP_RIGHT    | (row-1, col) |
| BOTTOM_LEFT  | (row+1, col) |
| BOTTOM_RIGHT | (row, col+1) |

**Physical coordinate mapping** (diamondWidth=1.0, diamondHeight=1.0):
```
x = (col - row) * diamondWidth  * 0.5
y = (col + row) * diamondHeight * 0.5
```

`toAsciiString()` renders diamond grids as shared-border diamond box art with
odd rows staggered horizontally and connector spans scaled to label width.

---

## Sparse Grids and Negative Coordinates

The grid is **sparse by default** — only explicitly placed cells exist.
Negative coordinates are fully valid in all directions.

```kotlin
// Sparse square grid — only named cells are placed
val board = squareGrid<String> {
    place(SquareCoordinate(0, 0), data = "start")
    place(SquareCoordinate(0, 2), data = "end")
    // (0,1) does NOT exist
}

// Grow organically with placeNext
val hexBoard = hexGrid<Int> {
    place(HexCoordinate(0, 0), data = 1)
    placeNext(HexCoordinate(0, 0), HexEdgeDirection.RIGHT)
    placeNext(HexCoordinate(0, 0), HexEdgeDirection.DOWN_RIGHT)
}

// Negative coordinates work everywhere
val grid = SquareGrid<Nothing>(1, 1)
grid.placeNext(SquareCoordinate(0, 0), SquareEdgeDirection.TOP) // → SquareCoordinate(0, -1)
```

`placeNext` is **idempotent** — if the target cell already exists it is returned unchanged.
When a new cell is placed next to existing neighbors, all shared `Vertex` and `Edge` instances
are automatically wired up.

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

## Custom Topology Objects

`Cell`, `Edge`, and `Vertex` are open base classes. Custom `Grid`
implementations can expose subclasses through the `Grid<C, Dir, D, CellT, EdgeT, VertexT>`
contract so callers keep typed access to additional fields or methods:

```kotlin
class TerrainCell(
    coordinate: SquareCoordinate,
    data: Terrain? = null
) : Cell<SquareCoordinate, SquareDirection, Terrain>(coordinate, data) {
    var movementCost: Int = 1
}
```

The built-in `SquareGrid`, `HexGrid`, `TriangleGrid`, and `DiamondGrid` continue
to use the base `Cell`, `Edge`, and `Vertex` types.

---

## Dynamic Tile Placement

Boards can grow at runtime — useful for Carcassonne-style games:

```kotlin
val grid = hexGrid<Nothing>(rows = 1, cols = 1)
val newCell = grid.placeNext(HexCoordinate(0, 0), HexEdgeDirection.RIGHT)
// → creates HexCoordinate(0, 1); shared edges/vertices wired; bounding box expands

val same = grid.placeNext(HexCoordinate(0, 0), HexEdgeDirection.RIGHT)
// → returns the existing cell (idempotent)
```

---

## Center Calculations

### Arithmetic Center

Returns the coordinate closest to the geometric middle of the bounding box
(floor-based for even-sized or negative-range grids):

```kotlin
SquareGrid<Nothing>(5, 5).arithmeticCenter()  // SquareCoordinate(col=2, row=2)
SquareGrid<Nothing>(4, 4).arithmeticCenter()  // SquareCoordinate(col=1, row=1)
```

### Physical Center

`physicalCenter()` returns a `GridCenter<C>` combining both the raw physical
position and the nearest grid coordinate:

```kotlin
val center = hexGrid<Nothing>(rows = 4, cols = 6).physicalCenter()

center.physical.x   // average x of all cells
center.physical.y   // average y of all cells
center.coordinate   // nearest HexCoordinate to that position
```

---

## Pathfinding

A* with the grid's own `distance()` as heuristic — works on all topologies:

```kotlin
val path = grid.findPath(from, to)                   // default: all cells passable
val path = grid.findPath(from, to) { cell ->         // custom predicate
    cell.data?.passable == true
}
// Returns null when no path exists
```

---

## Extensions

```kotlin
import io.gridkit.core.extensions.*

// Flood fill — BFS from start, optional predicate
val reachable: Set<C> = grid.flood(start)
val reachable = grid.flood(start) { cell -> cell.data?.passable == true }

// Connectivity — true if all cells are mutually reachable
val ok: Boolean = grid.isConnected()

// Single-character ASCII debug map
println(grid.toAsciiMap())
```

---

## API Reference

### Grid Interface `Grid<C, Dir, D, CellT, EdgeT, VertexT>`

| Method | Description |
|--------|-------------|
| `cells: Map<C, CellT>` | All cells |
| `edges: Set<EdgeT>` | All shared edges |
| `vertices: Set<VertexT>` | All shared vertices |
| `getCell(C): CellT?` | Cell at coordinate |
| `getNeighbors(C): List<CellT>` | Adjacent cells (unordered) |
| `getDirectedNeighbors(C): Map<Dir, CellT>` | Neighbors keyed by direction |
| `getNeighbor(C, Dir): C?` | Single neighbour coordinate |
| `isValidCoordinate(C): Boolean` | Presence check |
| `findPath(C, C, predicate): List<C>?` | A* pathfinding |
| `getRange(C, Int): List<CellT>` | All cells within radius |
| `getRing(C, Int): List<CellT>` | Cells at exact radius |
| `getLine(C, C): List<CellT>` | Straight line |
| `distance(C, C): Int` | Step distance |
| `placeNext(C, Dir): CellT` | Add adjacent cell, expand grid |
| `arithmeticCenter(): C` | Bounding-box centre coordinate |
| `physicalCenter(): GridCenter<C>` | Physical centre + nearest coordinate |
| `toAsciiString(): String` | ASCII box-art renderer |

### Cell `Cell<C, Dir, D>`

| Property / Method | Description |
|-------------------|-------------|
| `coordinate: C` | Grid position |
| `data: D?` | Optional payload |
| `edges: Map<Dir, Edge<D>>` | Direction → shared edge |
| `vertices: Map<Dir, Vertex<D>>` | Direction → shared vertex |
| `getEdge(Dir): Edge<D>?` | Edge in a direction |
| `getVertex(Dir): Vertex<D>?` | Vertex at a corner |
| `getNeighbors(): Map<Dir, Cell>` | Neighbors via edges |
| `getNeighborList(): List<Cell>` | Flat neighbor list |
| `getNeighbor(Dir): Cell?` | Single neighbor via edge |

### Edge `Edge<D>`

| Property / Method | Description |
|-------------------|-------------|
| `vertices: Set<Vertex<D>>` | Exactly 2 vertices (unordered) |
| `cellA: Cell?` / `cellB: Cell?` | The two cells separated by this edge |
| `isBorder: Boolean` | True when one cell slot is null |
| `getOpposite(Cell): Cell?` | Cell on the other side |

### Vertex `Vertex<D>`

| Property | Description |
|----------|-------------|
| `data: D?` | Optional payload |
| `cells: Set<Cell>` | All cells sharing this corner |
| `edges: Set<Edge<D>>` | All edges connected here |

### Type Parameters

| Parameter | Bound | Role |
|-----------|-------|------|
| `C` | `GridCoordinate` | `SquareCoordinate`, `HexCoordinate`, `TriangleCoordinate`, `DiamondCoordinate` |
| `Dir` | `GridDirection` | `SquareDirection`, `HexDirection`, `TriangleDirection`, `DiamondDirection` |
| `D` | — | Optional per-cell data; use `Nothing` for topology-only grids |
| `CellT` | `Cell<C, Dir, D>` | Concrete cell implementation exposed by a grid |
| `EdgeT` | `Edge<D>` | Concrete edge implementation used by a grid |
| `VertexT` | `Vertex<D>` | Concrete vertex implementation used by a grid |

---

## Module Structure

```
gridkit/
├── gridkit-core/           ← Pure grid logic, zero UI dependencies
│   └── io.gridkit.core
│       ├── core/           GridCoordinate, GridDirection, Cell, Grid,
│       │                   GridCenter, PhysicalCenter
│       ├── topology/       Vertex, Edge  (shared structural objects)
│       ├── direction/      SquareDirection, HexDirection, TriangleDirection,
│       │                   DiamondDirection (sealed interfaces + enums)
│       ├── grid/           SquareGrid, HexGrid, TriangleGrid, DiamondGrid
│       ├── pathfinding/    PathfindingStrategy, AStarPathfinder
│       ├── dsl/            squareGrid{}, hexGrid{}, triangleGrid{}, diamondGrid{}
│       └── extensions/     flood(), isConnected(), toAsciiMap()
│
└── gridkit-visualization/  ← Placeholder for future renderers
```

---

## Building

```bash
./gradlew :gridkit-core:test   # run core tests
./gradlew build                # compile + test all modules
```

Requires JDK 21+. No external runtime dependencies (stdlib only).

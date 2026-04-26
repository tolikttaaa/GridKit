# GridKit — Agent Instructions

This file is the authoritative guide for all AI agents (Claude Code, Codex, etc.)
working inside this repository. Follow every rule here without exception.

---

## Project Overview

GridKit is a Kotlin library providing a unified abstraction layer for tile-based
game boards. It supports square, hexagonal, and triangular grid topologies through
a single, topology-agnostic `Grid<C, Dir, D>` interface.

**Modules:**

| Module                  | Purpose                                             |
|-------------------------|-----------------------------------------------------|
| `gridkit-core`          | Pure grid logic, zero UI dependencies               |
| `gridkit-visualization` | Placeholder for future renderers (Compose/JavaFX/SVG) |

**Key packages inside `gridkit-core`:**

| Package         | Contents                                                         |
|-----------------|------------------------------------------------------------------|
| `core/`         | `GridCoordinate`, `Cell`, `CellState`, `Grid`, `PhysicalCenter` |
| `grid/`         | `SquareGrid`, `HexGrid`, `TriangleGrid` + direction enums        |
| `pathfinding/`  | `PathfindingStrategy`, `AStarPathfinder`                         |
| `dsl/`          | `squareGrid {}`, `hexGrid {}`, `triangleGrid {}` builders        |
| `extensions/`   | `flood()`, `isConnected()`, `toAsciiMap()`                       |

---

## Build & Test Commands

```bash
./gradlew :gridkit-core:test              # run unit tests
./gradlew :gridkit-core:build             # compile + test + jar
./gradlew build                           # build all modules
./gradlew :gridkit-core:test --info       # verbose test output
./gradlew clean build                     # clean then full build
```

Requires JDK 21+. No external runtime dependencies (stdlib only).

---

## Code Commenting Standards

**These rules are mandatory. Do not skip or abbreviate comments on public API.**

### KDoc on every public declaration

Every `public` class, interface, function, property, and enum must have a KDoc
block (`/** ... */`). If a declaration is self-evident from its name alone, the
KDoc may be a single line; otherwise use full multi-line form.

```kotlin
/** All cells in the grid, keyed by their coordinate. */
val cells: Map<C, Cell<C, D>>

/**
 * Returns the shortest path from [from] to [to] using A*.
 *
 * @param passable decides whether a cell may be traversed
 * @return ordered list of coordinates, or null when no path exists
 */
fun findPath(from: C, to: C, passable: (Cell<C, D>) -> Boolean): List<C>?
```

### KDoc conventions

- Use `[TypeName]` cross-references for all types mentioned in prose.
- Use `@param` for every non-obvious parameter.
- Use `@return` when the return value has a non-trivial contract.
- Use `@throws` when a function can throw (document the condition, not just the type).
- Omit `@param` / `@return` only when a one-line KDoc already fully covers them.

### Section separators inside long files

Use aligned `// ──` banners to separate logical sections within a class:

```kotlin
// ── internal helpers ──────────────────────────────────────────────────────

// ── Grid<SquareCoordinate, SquareDirection, D> ────────────────────────────
```

### Inline comments

Use inline `//` comments only when the **why** is non-obvious — a hidden
constraint, a subtle invariant, or a workaround. Never describe what the code
already says clearly by itself.

### No comment anti-patterns

- No `// TODO` left in committed code (open a GitHub issue instead).
- No commented-out dead code.
- No docstrings that just restate the method name.
- No multi-paragraph prose blocks inside function bodies.

---

## README Maintenance Rules

**After every change to source code, DSL, public API, or module structure,
you must review and update `README.md` before completing the task.**

This is non-negotiable. A PR with code changes but a stale README is incomplete.

### README section → what triggers an update

| README section              | Update when…                                                       |
|-----------------------------|--------------------------------------------------------------------|
| **Quick Start**             | DSL builder signatures change; new top-level feature added         |
| **Grid Topologies**         | Coordinate type, neighbor rules, or direction enum changes         |
| **Dynamic Tile Placement**  | `placeNext` contract or return type changes                        |
| **Center Calculations**     | `arithmeticCenter`, `physicalCenter`, or `nearestCoordinate` change |
| **Pathfinding**             | `findPath` signature, predicate semantics, or algorithm changes    |
| **Extensions**              | `flood`, `isConnected`, `toAsciiMap` signatures or behavior change |
| **API Reference tables**    | Any method added, removed, or renamed in `Grid` interface          |
| **Grid-Specific APIs table**| Any method added/removed on `SquareGrid`, `HexGrid`, `TriangleGrid` |
| **Module Structure**        | New module, new package, or package restructuring                  |
| **Building**                | Gradle task names, JDK requirement, or dependency changes          |

### How to update README

1. Read the current README in full before editing.
2. Make the smallest accurate change — do not rewrite sections that are still correct.
3. Keep all code examples in the README compilable and consistent with the
   actual API (coordinate order, parameter names, return types).
4. Do not add marketing language, vague phrases, or filler sentences.
5. Update the **API Reference** tables to match the actual `Grid` interface
   and the three grid classes exactly — no extra rows, no missing rows.

---

## General Agent Behaviour

- Always run `./gradlew :gridkit-core:test` after touching any `.kt` source file
  and verify that no tests regressed.
- Prefer editing existing files over creating new ones.
- Do not add features, abstractions, or refactors beyond what the task requires.
- Do not leave half-finished implementations.
- Do not add `@Suppress` annotations without explaining why in a comment.

# ASCII Rendering

`Grid.toAsciiString()` renders grids as ASCII art for console output or any
other string-based display. It is GridKit's first renderer and the reference
visualization for square, hexagonal, and triangular grids.

The visual style follows these rules:

- Cell corners are marked with `*`.
- Cell edges are drawn with ASCII characters.
- Cell values are displayed in the center of each cell.
- Cell width adapts to the length of the value string.
- Adjacent cells share border characters instead of double-drawing edges.

Cell values come from `cell.data?.toString().orEmpty()`. A cell with null data
renders as an empty body while preserving its structure.

---

## Square Cells

```text
*---------*
|  value  |
*---------*
```

- Top and bottom edges are `*` plus a `-` line plus `*`.
- Left and right edges are `|`.
- The value is surrounded by two spaces of padding on each side.
- Horizontal size is driven by the longest value in the same column.

Example, value `hello`:

```text
*---------*
|  hello  |
*---------*
```

Example, value `X`:

```text
*-----*
|  X  |
*-----*
```

---

## Hex Cells

```text
  *-----*
 /       \
*  value  *
 \       /
  *-----*
```

- Top and bottom edges are `*` plus a `-` line plus `*`.
- Diagonal edges use `/` and `\`.
- Width adapts to the value length.
- Neighboring cells connect edge-to-edge through shared `*` corner characters.
- Odd rows follow offset staggering.

Example, value `42`:

```text
  *--*
 /    \
*  42  *
 \    /
  *--*
```

Example, value `center`:

```text
  *------*
 /        \
*  center  *
 \        /
  *------*
```

---

## Triangle Cells

Down-pointing triangle:

```text
*-----------*
 \  value  /
  *********
```

Up-pointing triangle:

```text
  *********
 /  value  \
*-----------*
```

- Diagonal edges use `/` and `\`.
- Width adapts to the value length.
- Adjacent triangles share edges and corners.

---

## Grid Rules

1. Shared edges: adjacent cells share border characters.
2. Alignment: cells in the same row are horizontally aligned.
3. Hex staggering: odd rows are offset horizontally.
4. Value centering: labels are centered in the cell body.
5. Empty cells: cells with null data render an empty body.
6. Dynamic width: larger labels expand their column or cell footprint.

---

## Minesweeper Examples

Legend:

- `M` means mine.
- A number means adjacent mine count.
- Empty body means zero adjacent mines.

### Square Grid

```text
*-----*-----*-----*-----*
|  M  |  1  |     |     |
*-----*-----*-----*-----*
|  1  |  1  |     |     |
*-----*-----*-----*-----*
|     |  1  |  2  |  2  |
*-----*-----*-----*-----*
|     |  1  |  M  |  M  |
*-----*-----*-----*-----*
```

### Triangle Grid

```text
  *****-------*****-------*****
 /  M  \  1  /  1  \     /     \
*-------*****-------*****-------*
 \  1  /  2  \  2  /  1  \     /
  *****-------*****-------*****
 /  1  \  1  /  M  \  1  /  1  \
*-------*****-------*****-------*
 \  1  /  1  \  1  /  2  \  2  /
  *****-------*****-------*****
 /     \     /     \  1  /  M  \
*-------*****-------*****-------*
```

### Hexagonal Grid

```text
  *-*     *-*     *-*     *-*     *-*     *-*
 /   \   /   \   /   \   /   \   /   \   /   \
*  M  *-*  1  *-*     *-*     *-*     *-*     *-*
 \   /   \   /   \   /   \   /   \   /   \   /   \
  *-*  2  *-*  1  *-*     *-*     *-*     *-*     *
 /   \   /   \   /   \   /   \   /   \   /   \   /
*  1  *-*  M  *-*     *-*     *-*     *-*     *-*
 \   /   \   /   \   /   \   /   \   /   \   /   \
  *-*  1  *-*  1  *-*     *-*     *-*     *-*     *
 /   \   /   \   /   \   /   \   /   \   /   \   /
*     *-*  1  *-*     *-*  1  *-*  1  *-*     *-*
 \   /   \   /   \   /   \   /   \   /   \   /   \
  *-*     *-*     *-*  1  *-*  2  *-*  1  *-*     *
 /   \   /   \   /   \   /   \   /   \   /   \   /
*     *-*     *-*     *-*  M  *-*  M  *-*     *-*
 \   /   \   /   \   /   \   /   \   /   \   /   \
  *-*     *-*     *-*  1  *-*  3  *-*  1  *-*     *
 /   \   /   \   /   \   /   \   /   \   /   \   /
*  1  *-*     *-*     *-*  2  *-*  2  *-*     *-*
 \   /   \   /   \   /   \   /   \   /   \   /   \
  *-*  1  *-*     *-*     *-*  M  *-*     *-*     *
 /   \   /   \   /   \   /   \   /   \   /   \   /
*  M  *-*     *-*     *-*  1  *-*  1  *-*     *-*
 \   /   \   /   \   /   \   /   \   /   \   /
  *-*     *-*     *-*     *-*     *-*     *-*
```

# Desktop — Do Not Arrange Before Measuring

Status: **Implemented** (2026-08-24)

Icons are auto-placed only once the grid knows how tall it is. Amends
[`../shell/grid-layouts.md`](../shell/grid-layouts.md), which established column-major placement
without saying when it may be computed.

## Context

Icons visibly arrange themselves **horizontally and then vertically** — a row across the top that
snaps into columns a frame later. It is most obvious when something makes the desktop recompose,
such as opening the context menu.

**Auto-placement runs before the grid has been measured.** `firstFreeCell` fills column-major within
`rowsPerColumn`, and until the desktop has been laid out that value is `1` — the grid coerces its
unmeasured height to a single row, and the screen state starts at `1` too. One row per column
*is* a horizontal arrangement: every icon takes a new column at row 0. The correct vertical order
only appears after the height arrives and everything is placed again.

**And the first measurement reports the old value.** `onGridMetrics` is called from
`onGloballyPositioned` but passes the `rows` computed during the *current* composition — from the
height before this measurement. So even the measuring pass hands back `1`, and the real row count
takes another composition to arrive. The wrong arrangement therefore survives longer than a single
frame, which is why it is visible rather than theoretical.

**A coerced `1` is a wrong answer dressed as a safe one.** `coerceAtLeast(1)` was there to avoid a
zero, but it turns "not measured yet" into "one row", and one row is a layout the user can see. The
honest value for an unmeasured grid is *nothing*, and the honest response to it is to place nothing
— SRS §12 asks for skeletons rather than a wrong screen.

## Requirement

1. **No auto-placement happens before the grid is measured.**
2. **An unmeasured grid reports no row count** rather than a plausible-looking one.
3. **The measuring pass reports the count it just measured**, not the one from before it.
4. **An icon with no placement is not drawn** — it appears when it has a cell, rather than being
   stacked at the origin.
5. **Placement, once made, does not change** because of a later recomposition.
6. **Stored placements are unaffected** — this is about icons that have never been placed.
7. **The placement function tolerates a zero or negative row count** rather than producing a row.

## Acceptance criteria

- [x] Icons never appear in a horizontal row: with every placement cleared, the first arrangement
      drawn is the final column-major one.
- [x] Opening the desktop context menu does not rearrange anything — a pixel diff of the icon area
      across opening and closing the menu reports **zero** changed pixels.
- [x] Icons the user has positioned stay where they were put: stored placements are untouched by the
      row count, which is asserted directly.
- [x] The desktop shows wallpaper rather than misplaced icons for the frame before measurement — an
      icon with no cell is skipped instead of drawn at the origin.
- [x] `withAutoPlacement` with a zero or negative row count places nothing.
- [x] `./gradlew test lint assembleDebug` green.

**The stale report was half the reason it was visible.** `onGridMetrics` passed the `rows` computed
during the current composition — from the height *before* the measurement it was reporting — so even
the measuring pass handed back `1`, and the horizontal arrangement survived an extra composition
rather than a single frame. The count is now derived from the size just measured.

## Notes

- **Why not simply pick a sensible default row count.** Any default is a guess at the display's
  height, and being wrong by one row reorders every icon after the first column. Waiting one frame
  costs nothing the user can perceive; guessing costs the arrangement.
- **Not in this slice:** persisting the auto-placed arrangement so it survives a change in grid
  height, which is a separate decision about whose arrangement wins.

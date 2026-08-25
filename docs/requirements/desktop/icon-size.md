# Desktop — Icon Size in the Cell

Status: **Implemented** (2026-08-24)

Desktop and dock icons drawn large enough to fill the space they occupy. Amends the sizing left
implicit by [`icon-grid.md`](icon-grid.md) and interacts with
[`../design/icon-gloss.md`](../design/icon-gloss.md), which is why the artwork is smaller than it
looks like it should be.

## Context

A desktop icon is drawn into a 52 dp box inside a 96 dp cell, and the visible tile is smaller still.
The treatment reserves room inside the cached bitmap for the drop shadow and the outer glow — the
tile is inset by `shadowRadius + shadowOffset + glowRadius`, about 24% on every side — so a 52 dp
draw yields roughly a 27 dp tile. Nearly three quarters of each cell is empty, and the icons read as
small even after the gloss work made them read as *icons*.

**The inset is not waste, and shrinking it is not the fix.** The shadow and glow are drawn outside
the tile deliberately: clipped by the bitmap edge they would read as a band rather than as light.
The bitmap has to be bigger than the tile. What has to change is how large that bitmap is drawn.

**The cell has room the icon is not using.** The grid is 96 dp wide and 104 dp tall, and the label
below takes two lines. The icon can grow substantially before the two collide.

**The dock has the same arithmetic.** Its 40 dp draw yields about a 21 dp tile in a 44 dp touch
target inside a 56 dp bar, which is why the dock reads as sparse next to a bar sized for it.

## Requirement

1. **A desktop icon fills materially more of its cell**, without the label colliding with the icon
   above it or overflowing the cell.
2. **A dock icon grows to match**, staying inside the bar at rest and inside its touch target.
3. **The label keeps its two lines** and stays legible; the icon takes space from the cell's
   emptiness, not from the label.
4. **Nothing overlaps.** At the largest size, adjacent cells' artwork does not touch.
5. **The sizes are tokens**, not literals at the call site, so the grid and the dock cannot drift
   apart again.
6. **The relationship between the drawn box and the visible tile is stated once**, so a future
   change to the shadow or glow does not silently shrink every icon again.

## Acceptance criteria

Checked on the `Pixel_Tablet` AVD at 320 dpi.

- [x] The desktop icon's box goes 52 dp → 72 dp; the visible tile goes from ~28% of the 96 dp cell
      to ~39%.
- [x] The label still shows two lines and is not clipped.
- [x] Icons in adjacent cells do not touch.
- [x] The dock icon goes 40 dp → 48 dp and still fits the bar at rest (56 dp) and its touch target,
      which now takes the larger of the icon and the 44 dp minimum.
- [x] The sizes live in `PcSize` (`DesktopIcon`, `DockIcon`, `VisibleTileFraction`).
- [x] `VisibleTileTest` pins the fraction: no style may draw a smaller tile than the sizing assumes,
      and the worst case is the one the constant describes.
- [x] `./gradlew test lint assembleDebug` green.

**The fraction is per style, and the constant is the worst case.** Clay reserves more room than
glass — 0.52 against 0.61 — because its shadow and glow radii are larger. Sizing has to hold for the
smaller of the two, so the constant is clay's, and the test asserts glass meets it rather than
asserting both are equal.

## Notes

- **Why a test on the fraction rather than on the dp.** The dp is a design choice and will change
  again; the *ratio of visible artwork to reserved margin* is the thing that silently regressed and
  is the thing worth holding.
- **The derived figure was wrong, and the measurement corrected it.** The inset arithmetic
  (`1 − 2 × (shadowRadius + shadowOffset + glowRadius)`) predicts 0.52 for clay, which held, but
  0.61 for glass came out of the pixels rather than the formula. Pinning a measured number beats
  restating a derivation that only happens to be right for one style.
- **Not in this slice:** a user-adjustable icon size (SRS §7.5 puts it in Settings), `Ctrl`+scroll
  resizing, or changing the 96 dp grid pitch itself.

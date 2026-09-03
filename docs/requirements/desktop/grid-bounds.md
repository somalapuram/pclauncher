# Desktop — Grid Bounds Against the Overlay Bar

Status: **Accepted · Implemented** (2026-09-03)

The desktop's usable area ends where the taskbar begins, and the rows that fit are centred in it.
SRS §6.6 and §12. Depends on [`../shell/overlay-service.md`](../shell/overlay-service.md) for the
two-window split that caused this, and reuses the clearance named in
[`../shell/tray-popover-host.md`](../shell/tray-popover-host.md).

## Context

Moving the chrome into an overlay window took the bar out of the activity. That was right — it is
what keeps the bar above app windows — but it removed the only thing that had been reserving the
bar's space.

While the activity drew the bar, `HomeScreen`'s `Column` gave the desktop `weight(1f)` and the bar
the remainder, so the grid could not reach the bar by construction. With the chrome in the overlay
that `ShellBar` is not composed at all, the `Column` holds only the desktop, and the desktop takes
the whole window. The bar is a *separate window* the activity cannot see: it contributes no window
insets, so `safeDrawing` does not describe it either.

The result, measured on the emulator at 2560×1600 / 320 dpi:

- Activity window `[0,0][2560,1600]` — the entire display.
- Overlay bar window `[0,1432][2560,1600]` — the bottom **84 dp**.
- Cell height 104 dp (208 px), so the full-height window holds **7 rows**, measured as a cell block
  spanning `y = 84..1540` — row 7 running `y = 1332..1540` against a bar starting at `y = 1432`.

So the last row overhangs the taskbar by **108 px**, and its label lands at `y = 1506`, entirely
underneath the bar. This is not a cosmetic overlap: `cellAt` will happily return that row, so an
icon can be *dropped* into a cell that is permanently obscured.

**The leftover space belongs at both ends.** Rows are whole; the height rarely divides exactly, and
today the remainder falls entirely below the last row. That reads as a grid pushed against the top
edge rather than one placed in its area. Splitting it puts equal air above the first row and below
the last.

## Requirement

1. **The desktop reserves the overlay bar's height** whenever the chrome is in the overlay, so no
   cell is ever drawn beneath the bar.
2. **The reservation reuses `ShellMenuBarClearance`** rather than naming the bar's height a second
   time. It is already the shell's single answer to "how far must this clear the bar", and the two
   menu hosts place against it.
3. **While the activity owns the chrome, nothing changes.** The `Column` already reserves the bar
   by construction, and reserving it twice would leave a bar-sized band of dead wallpaper.
4. **The rows that fit are centred vertically** in the usable area: the space above the first row
   equals the space below the last, to within a pixel of rounding.
5. **Centring moves hit-testing with the pixels.** The reported grid origin, the drop arithmetic
   and the desktop's own context-menu `cellAt` all shift by the same amount, so a drop lands in the
   cell the user sees under the pointer.
6. **Icons and widgets shift together.** Both are positioned from the same cell arithmetic in one
   container; a widget must not part company with the icons around it.
7. **The row count still comes from the measured height**, so the centring offset must not feed
   back into the measurement that produced it.

## Acceptance criteria

Measured on the emulator at 2560×1600 / 320 dpi, from label bands in a screenshot strip carrying no
bar chrome (`x = 460..780`); the row pitch reads 207–209 px against a 208 px cell, which is the
antialiasing bias on a glyph's top edge.

- [x] No cell is drawn under the bar. **Before:** 7 rows, cell block `y 84..1540` — 108 px past the
      bar top at 1432, with row 7's label at `y 1506` entirely underneath it. **After:** 6 rows,
      cell block `y 119..1367`, no label below the bar.
- [x] Row count drops from 7 to 6 at 2560×1600.
- [x] The space above the first row equals the space below the last — measured 71 px above, 65 px
      below, against the arithmetic's exact 68/68 (`16 dp padding + 26 dp` — here 32 px + 36 px).
      The 6 px spread is the same glyph-edge bias as the pitch.
- [x] A drop still lands in the cell under the pointer. Dragged Settings from `(512,1260)` to
      `(896,636)`; the store wrote `com.android.settings/.Settings|4|2` — column 4, row 2.
- [x] Widgets and icons move by the same offset — both are positioned inside the one centred
      container, so there is no second offset that could disagree.
- [x] Centring arithmetic is pure and unit-tested — exact fit, remainder, odd remainder, a height
      below one cell, unmeasured and degenerate sizes, and a sweep proving the offset never reaches
      half a cell (9 tests).
- [x] Layout regression tested at the Compose level: cells centred, and the first cell's top at
      padding + half the leftover (3 tests).
- [x] `./gradlew test lint assembleDebug` green — 540 tests, 0 failures, lint clean.

## Notes

- **The overlay bar's 84 dp is arithmetic, not a guess:** 12 dp magnification headroom (the window
  is exactly as tall as its content, and a magnified dock icon stands above the bar) + 56 dp
  `PcSize.DockHeightAtRest` + 16 dp bottom margin. That is the same 84 dp `ShellMenuBarClearance`
  already carries, which is why it is reused rather than recomputed.
- **Centring cannot be done with padding.** Padding changes the measured height, which changes the
  row count, which changes the remainder — the value oscillates between frames. The offset is
  applied to a child container instead, leaving the measurement that produced it untouched.
- **This is the second bug caused by the window split.** Cross-window drag between dock and desktop
  was the first (`shell/overlay-service.md`). Both have the same shape: something the single-window
  layout gave for free, which the split silently stopped providing. Worth checking anything else
  that assumed the bar was in the activity.
- **Not in this slice:** a user-configurable margin, horizontal centring of the used columns, or
  reserving space for a bar on another screen edge.

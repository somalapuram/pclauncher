# Desktop — A Drag Starts Where the Pointer Is

Status: **Implemented** (2026-08-24)

The position reported when a desktop icon drag begins is the pointer's, in root coordinates.
Amends [`../shell/direct-manipulation.md`](../shell/direct-manipulation.md).

## Context

Dragging any desktop icon puts the ghost near the top-left of the grid rather than under the
pointer, and drops it into a cell computed from there. The further the icon is from the first cell,
the further wrong it is.

**Two origins, and the wrong one is used.** `DesktopIcon` captures its own position in root
coordinates and reports only the offset *within itself*; the grid then adds the **grid's** origin.
So the reported point is `gridOrigin + offsetInsideIcon`, and the icon's own cell offset — the part
that says *which* icon is being dragged — is dropped. Every drag reports a point a few dozen pixels
from the grid's corner.

The icon's captured origin is never read. An unused value that exists for exactly this purpose is
the tell: the pieces were both there and one was not connected.

**The dock does it correctly**, which is why only the desktop is wrong: `DockIcon` adds *its own*
root position before reporting. The two surfaces disagreed and nothing compared them.

**It hid behind the first icon.** Dragging the icon in cell (0, 0) — the top-left, the obvious one
to reach for when testing — has a cell offset of zero, so the bug produces exactly the right answer
there. Every check I ran happened to use it.

## Requirement

1. **A drag reports the pointer's position in root coordinates**, wherever the icon sits in the
   grid.
2. **The icon reports its own position**, since it is the only participant that knows where it is;
   the grid passes it through unchanged.
3. **Drops land in the cell under the pointer**, for an icon in any cell.
4. **The dock is unchanged** — it was already right.
5. **The equivalence is tested against an icon that is not in the first cell**, because the first
   cell cannot show the fault.

## Acceptance criteria

Measured on device by logging the reported positions, then confirmed by where icons actually land.

- [x] A drag from a far cell begins at the pointer: the icon at cell (2,5) reported
      `origin(416,1120) + local(96,88) = (512,1208)`, exactly the touch point.
- [x] The drop position equals the pointer: `(1500, 500)` for a drag ending there, where it had been
      `(1228, 695)`.
- [x] Dropping lands in the cell under the pointer — a drop at `(1500,900)` put the icon in
      column 7, row 3, which is that point's cell.
- [x] A drop onto an occupied cell is still refused: dropping at `(1500,500)` left the icon where it
      was, because the widget covers columns 7–9 of row 2.
- [x] Dock drags are unaffected — `DockIcon` already added its own root position.
- [x] The relationship is pinned in `DragTravelTest`, including the case that hides the fault.
- [x] `./gradlew test lint assembleDebug` green.

**There were two faults, not one.** Fixing the origin still left the drop 272 px short, because the
gesture loop returned on release *without* applying that event's movement — and the release carries
the last segment of the swipe. Six deltas were reported for a 1200 ms drag; the missing 154 px in x
was almost exactly one of them. The same shape existed in the widget's own drag loop and is fixed
with it.

## Notes

- **Why the icon reports rather than the grid computing.** The grid would have to reconstruct the
  cell offset it already handed the icon, and the two would then have to agree about padding,
  spacing and any transform between them. The icon is measured where it is drawn; asking it is both
  shorter and impossible to get subtly wrong.
- **Not in this slice:** grabbing an icon by the point that was pressed rather than centring the
  ghost on the pointer.

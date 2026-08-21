# Desktop — Icon Placement & Context Menu

Status: **Accepted · Implemented** (2026-08-20)

Icons the user can arrange, and a right-click menu on the desktop itself. SRS §14 Phase 7; derives
from SRS §6.6 and §10 (`desktop_layout`). Supersedes the auto-flowing grid in
[`../shell/grid-layouts.md`](../shell/grid-layouts.md). Depends on
[`../shell/direct-manipulation.md`](../shell/direct-manipulation.md) for the drag gesture.

## Context

The desktop currently *flows*: icons fill column-major and their position is a consequence of
alphabetical order. That is a list wearing a grid's clothes. A desktop's defining property is that
**the user decides where things go** — it is a surface you arrange, and the arrangement is meaning:
work things here, the thing I am using today near the corner I look at.

So position becomes data. Each icon has a cell, cells persist, and a drag moves one.

**Auto-placement still has to be good**, because most icons will never be moved. A newly installed
app must land somewhere sensible without asking, and "sensible" is the first free cell scanning
column-major — the same order the flowing grid used, so today's arrangement survives the change
rather than being scrambled by it.

**Dropping needs to be forgiving.** A pointer released between cells must land in the nearest one,
and a drop onto an occupied cell must not silently destroy what was there. Refusing the drop and
leaving the icon where it started is honest; overwriting is not.

**The desktop's own right-click is a separate gesture from an icon's.** `direct-manipulation.md`
gave icons a context menu; the empty desktop needs its own, and it is where wallpaper and widgets
are reached from — the two things SRS §6.6 lists that have no other entry point.

## Requirement

1. **Every icon has a persisted grid cell** in a `desktop_layout` store, keyed by
   `(component, user)` like everything else.
2. **Unplaced icons auto-place** into the first free cell scanning column-major, so a fresh install
   and a newly installed app both look deliberate.
3. **Dragging an icon on the desktop moves it** to the cell under the pointer, and the move
   persists.
4. **A drop between cells snaps to the nearest**; a drop on an occupied cell is refused and the icon
   returns to where it came from.
5. **Dragging to the taskbar still pins** — the existing behaviour must not be broken by adding
   within-desktop movement.
6. **The empty desktop has a context menu** on right-click and long-press offering at least
   **Change Wallpaper** and **Add Widget**.
7. **Change Wallpaper opens the system picker**, rather than pclauncher implementing wallpaper
   selection.
8. **Cell arithmetic is pure and tested**: position→cell, cell→position, nearest-free-cell,
   occupancy, and out-of-bounds.
9. **A corrupt or unreadable layout costs the arrangement, not the desktop** — icons fall back to
   auto-placement (GATE 4).

## Acceptance criteria

- [x] Icon cells persist in a `desktop_layout` store — verified on device, a dragged icon writes
      `...calendar/...AllInOneActivity|8|2`.
- [x] Unplaced icons auto-place column-major into the first free cell.
- [x] Dragging an icon to an empty cell moves it and the move is written to the store.
- [~] **Snap-to-nearest is not implemented.** A drop maps to the cell it is *inside*
      (`cellAt` truncates); it does not round to the closest cell centre. Off-grid drops are
      rejected outright rather than snapped.
- [x] A drop on an occupied cell is refused and the icon stays where it was (`moved` returns null,
      store untouched).
- [x] Dragging an icon to the taskbar still pins it.
- [x] Long-press on empty desktop opens a menu with Change Wallpaper and Add Widget, anchored at
      the press.
- [x] Change Wallpaper launches the system picker.
- [x] Cell arithmetic is pure and unit-tested — bounds, occupancy, refusal, off-grid, degenerate
      sizes, codec round-trip and corruption (19 tests).
- [x] A corrupt layout falls back to auto-placement rather than an empty desktop.
- [x] `./gradlew test` green (237 project-wide); `./gradlew lint` clean; verified on device.

## Notes

- **The layout stops being a lazy grid.** Free placement means absolute cells, so the desktop
  becomes positioned children rather than `LazyHorizontalGrid`. A desktop holds tens of icons, not
  thousands, so nothing is lost by dropping laziness — and the flowing grid could never have
  expressed "this icon lives here".
- **Auto-placement deliberately matches the old flow order** so this change does not scramble an
  arrangement the user has already got used to.
- **A parent watching the same events as its children will steal them.** The desktop container has
  its own long-press for the context menu, and it fired *mid-drag* — opening the menu, which took
  the pointer and killed the drag four events in. The container now stands down the moment it sees
  a change a child has consumed. This is the kind of bug that looks like "drag is flaky".
- **Where nothing scrolls, a drag should not wait.** The long-press gate exists so a scrollable
  ancestor keeps its gesture; the desktop has no scroll, so gating there bought nothing and cost
  everything — a quick drag was abandoned into empty space and simply did nothing. The gate is now
  a parameter, on inside scrollables and off on the desktop.
- **Not in this slice:** folders, multi-select and marquee, dragging several icons at once, sorting
  commands, icon size options, or free (non-grid) pixel placement.

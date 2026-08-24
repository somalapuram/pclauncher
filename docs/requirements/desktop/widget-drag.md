# Desktop — Arranging Widgets by Drag

Status: **Implemented** (2026-08-24)

Drag a widget to move it around the desktop grid, the way icons already move. Closes the "moving a
widget by drag (it is placed where it lands and stays)" exclusion in
[`widget-resize.md`](widget-resize.md). Depends on [`icon-grid.md`](icon-grid.md) for the grid and
its drop rules, on [`widget-host.md`](widget-host.md) for hosting, and on
[`../shell/direct-manipulation.md`](../shell/direct-manipulation.md) for the gesture vocabulary.

## Context

An icon can be dragged anywhere on the desktop; a widget lands wherever the first free cell happened
to be and stays there for good. The desktop's whole claim over a list of apps is that position is
something the user chooses, and half the things on it cannot be positioned. A widget is also the
larger, more deliberate object — the one most worth putting somewhere specific.

**The widget must move, not a ghost of it.** Icons drag a small ghost because the icon is 52 dp and
the ghost reads clearly under a finger. A widget is several cells wide; a ghost of it would be
either a misleading thumbnail or a full second copy rendered every frame on a CPU renderer (SRS
§4.3). Moving the widget itself is cheaper — one offset, no extra layer — and it shows exactly what
will land where.

**The gesture is already spoken for twice.** A press on a widget can mean tap-the-widget,
open-its-menu, or move-it, and the widget's own view must keep the first of those — an
`AppWidgetHostView` handling its clicks is the point of a widget. So the three have to be separated
by what the finger does rather than by where it lands: still and held is the menu, moved past the
touch slop is a drag, and anything shorter is the widget's own tap.

**A drag that is stolen too early breaks the widget.** The container must watch the press without
claiming it, and take the gesture only at the moment movement passes the slop — before that, every
event still belongs to the widget.

**Dropping follows the same rules as an icon.** A widget dropped where it does not fit is refused
and springs back, because silently displacing whatever was there is worse than declining the drop
(`icon-grid.md` requirement 4). A widget is a rectangle, so "does it fit" is the span-aware overlap
test spans already introduced.

**Where a widget lands is decided by the widget, not the finger.** With a 3×1 widget the finger may
be anywhere along it; snapping the cell under the *pointer* would move the widget by an amount that
depends on where the user happened to grab it. Snapping the widget's own top-left corner to the
nearest cell is what makes the drop land where it looks like it will.

## Requirement

1. **Dragging a widget moves it**: past the touch slop, the widget follows the pointer.
2. **A tap still reaches the widget.** Nothing is consumed until the drag actually starts.
3. **A held, still press opens the widget's context menu** rather than starting a drag
   ([`widget-removal.md`](widget-removal.md)).
4. **The widget's top-left snaps to the nearest cell** on drop, not the cell under the pointer.
5. **The drop is clamped to the grid** — no part of the widget may land off-grid.
6. **A drop that overlaps another placement is refused** and the widget returns to where it was.
7. **The span is preserved** across a move.
8. **The move persists** and survives restart.
9. **A widget in resize mode does not drag** — its handles own the gesture until resize mode ends.
10. **The snap arithmetic is pure and tested**: rounding, clamping at each edge, and preserving the
    span.

## Acceptance criteria

Verified on the `pclauncher_desktop_api34` emulator with the Google Search widget (span 5×1).

- [x] Dragging a widget moves it under the pointer and it lands on release.
- [x] A tap on the widget still reaches the widget — tapping the search box launched Chrome.
- [x] A held, still press opens the widget menu and does not move the widget.
- [x] The widget's top-left snaps to the nearest cell, and consecutive drags compose:
      `0|5 → 2|5 → 4|5 → 4|3`.
- [x] A drag toward an edge clamps rather than putting part of the widget off-grid
      (`WidgetDragTest`, all four edges).
- [x] A drop onto another *stored* placement is refused and the widget springs back — dragging the
      5-wide widget onto the pinned Phone icon at `7|1` left it at `4|3`.
- [x] The span is unchanged by a move (`5|1` throughout every drag above).
- [x] The new position survives a force-stop and relaunch.
- [x] Handles still resize while resize mode is on, and the widget does not move.
- [x] Snap arithmetic unit-tested: rounding both directions, clamping at all four edges, span
      preserved, degenerate cell size (`WidgetDragTest`, 15 cases).
- [x] A second drag is measured from where the first one landed (`WidgetDragWiringTest`).
- [x] `./gradlew test lint assembleDebug` green.

**Behaviour worth naming, which the criteria above do not capture:** a widget dropped on
*auto-placed* icons is **not** refused. Only placements the user has made are in the store, so the
drop is accepted and the icons flow around the widget on the next auto-placement pass. Observed:
dropping the widget across columns 1–5 pushed `pclauncher` down a row rather than overlapping it.
That is the better answer — a widget refused by icons nobody positioned would be unmovable across
most of the desktop — but it means requirement 6 holds for stored placements, not for every glyph
on screen.

## Notes

- **Why the widget itself moves rather than a shared `DragState`:** the shared state models a drag
  of an `AppEntry` between the desktop and the dock, where dropping means pin or unpin. A widget
  has no dock meaning and no pin meaning, so putting it through that path would mean a nullable
  entry and a `DragResult` that has to be ignored half the time. The move is local to the desktop
  grid, and the drop calls the same store operation an icon's drop calls.
- **A scrollable widget loses its scroll.** Taking the gesture at the slop means a swipe inside a
  widget that scrolls its own content will drag the widget instead. Accepted for now: the
  alternative — requiring a hold before the drag — is the gesture the context menu already uses.
  Revisit if a widget that genuinely scrolls turns out to matter.
- **The gesture node must not move with the widget.** Translating the node the drag is measured
  from slides the coordinate space out from under the finger: local deltas shrink, and a drag
  registered only ~87% of its true distance, so a 1.5-cell drag rounded down to one. The cell offset
  stays on the layout node and the drag is drawn by a child's `graphicsLayer` instead.
- **`pointerInput` keys are part of the contract.** Keyed only on `enabled`, the block kept the
  placement from the first composition and every drag after the first was measured from a cell the
  widget no longer occupied — it looked like the row changing on a purely horizontal drag.
  `WidgetDragWiringTest` fails if the keys are dropped.
- **Not in this slice:** dragging a widget onto the taskbar, dragging between displays, drag
  autoscroll at the screen edge, or a drop preview outline showing the target cells.

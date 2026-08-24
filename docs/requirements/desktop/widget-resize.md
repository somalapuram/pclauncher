# Desktop — Widget Resize

Status: **Implemented** (2026-08-24)

Long-press a widget to resize it by dragging its edges. Supersedes the "resizing widgets by drag"
exclusion in [`widget-host.md`](widget-host.md). Depends on that doc for hosting and on
[`icon-grid.md`](icon-grid.md) for the cells a widget occupies, and on
[`../shell/direct-manipulation.md`](../shell/direct-manipulation.md) for the gesture vocabulary.

## Context

Every widget currently occupies a fixed 2×2 block regardless of what it is. A calendar wants to be
wide, a clock wants to be small, and a provider that declares it can be 4×1 is being shown at a
size it never asked for. Resizing is not a refinement here — it is the difference between widgets
being usable and being decoration.

**The provider decides what is allowed, and we have to ask.** `AppWidgetProviderInfo.resizeMode`
says whether a widget may be resized horizontally, vertically, both, or not at all, and
`minResizeWidth`/`minResizeHeight` give a floor that is often smaller than the widget's default.
Ignoring either produces a widget squeezed into a shape its layout cannot render, which looks like
our bug rather than a misuse of theirs.

**Resizing is not just moving the frame.** A hosted widget must be *told* its new size or it keeps
rendering for the old one — `updateAppWidgetSize` is what triggers the provider to re-lay-out, and
without it a resized widget is a stretched picture of its former self.

**Long-press has to be taken from the widget without breaking it.** An `AppWidgetHostView` handles
its own taps — that is the whole point of a widget — so the resize gesture cannot simply consume
everything. The container watches for a long press without claiming the gesture, and only takes
over once the press has been held. Taps continue to reach the widget.

**A resize can be refused.** Growing into a cell occupied by an icon or another widget, or past the
grid edge, must leave the widget as it was rather than overlapping. This is the same rule as
dropping an icon on an occupied cell, and it must give the same answer.

## Requirement

1. **A placement carries a span** — columns and rows — persisted alongside its cell.
2. **Old stored placements still load.** Entries written before spans existed decode as 1×1 rather
   than being discarded.
3. **Long-press on a widget enters resize mode**, showing a frame with drag handles. Taps before
   that reach the widget normally.
4. **Only the provider's permitted axes offer handles.** `RESIZE_NONE` offers none and the widget
   cannot enter resize mode at all.
5. **Dragging a handle changes the span in whole cells**, clamped to the provider's minimum and to
   the grid.
6. **A resize that would overlap another placement is refused**, leaving the span unchanged.
7. **The widget is told its new size** via `updateAppWidgetSize`, so the provider re-renders rather
   than stretching.
8. **Resize mode ends** on a tap outside, on Escape, or on the widget itself again.
9. **The span maths is pure and tested**: clamping to min, clamping to grid, refusal on overlap,
   each permitted axis, and a `RESIZE_NONE` provider.

## Acceptance criteria

Verified on the `pclauncher_desktop_api34` emulator against the Google Search widget
(`resizeMode=horizontal`, `minResizeWidth` = 3 cells) and the Chrome Dino widget (`RESIZE_NONE`).

- [x] `DesktopPlacement` carries a span and it persists across restart — stored as
      `widget:6|2|3|4|1` and re-read after a force-stop.
- [x] Placements stored without a span decode as 1×1 (`DesktopLayoutTest`).
- [x] Long-press on a widget enters resize mode; a tap before that still reaches the widget — the
      frame is watched on `PointerEventPass.Initial` and never consumes, so the hosted view keeps
      its own clicks.
- [x] `RESIZE_NONE` widgets cannot enter resize mode (Chrome Dino refuses); a horizontal-only
      provider offers left and right handles and no vertical ones (observed on device).
- [x] Dragging a handle resizes in whole cells and persists: a 100 px drag on a 96 dp grid took the
      widget from `3×1` to `4×1`, and the reverse drag took it back to `3×1`.
- [x] A resize below the provider's minimum is refused, not applied — a further shrink from 3 to 2
      columns produced no write and left the span at 3.
- [x] A resize past the grid edge is refused (`WidgetResizeTest`).
- [x] A resize overlapping another placement is refused and the span is unchanged
      (`WidgetResizeTest`).
- [x] `updateAppWidgetSize` is called after a resize — the search bar re-laid-out at the new width
      rather than stretching.
- [x] Resize mode ends on a tap outside.
- [x] Span arithmetic is pure and unit-tested across all the above (`WidgetResizeTest`, 18 cases),
      and the controller that feeds it is tested for cumulative reports
      (`WidgetResizeControllerTest`).
- [x] `./gradlew test lint assembleDebug` green.

**Not verified:** Escape to leave resize mode — the shell has no key handling yet, so the criterion
above covers only the outside tap. Right-click and secondary-button paths remain unverified on the
emulator, which cannot synthesise a secondary button (see `direct-manipulation.md`).

## Notes

- **Occupancy now means a rectangle, not a cell.** With spans, "is this cell free" becomes "does
  this rectangle intersect any other". Every existing placement becomes a 1×1 rectangle, so icon
  behaviour is unchanged — but the overlap test has to move from a set of cells to an intersection,
  and doing that in one place is what keeps icons and widgets agreeing.
- **Why long-press rather than a handle that is always visible:** permanent handles on every widget
  would clutter a desktop whose whole point is the user's own content, and a widget is resized once
  and then left alone for months.
- **A handle reports cumulative pixels, not per-frame deltas.** The first implementation applied
  each report as a fresh increment against the span the previous report had just written, so one
  107 px drag arrived as ten reports and compounded a 3-cell widget to 5 before the finger lifted.
  A resize is measured against the placement captured at `onDragStart`; `WidgetResizeControllerTest`
  fails if that base is dropped.
- **Not in this slice:** reconfiguring a widget, moving a widget by drag (it is placed where it
  lands and stays), resize animations, or per-widget size presets.

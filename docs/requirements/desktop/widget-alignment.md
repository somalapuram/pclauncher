# Desktop — A Widget Fills the Cells It Is Placed On

Status: **Implemented** (2026-08-24)

A widget's edges land on the grid, not inside it. Amends
[`widget-sizing.md`](widget-sizing.md), which got the size right and left the inset.

## Context

A widget placed on a 5×4 block of cells draws a rectangle of 912 × 784 px inside a cell rectangle
of 960 × 832 — inset by about 24 px on every side, symmetrically. It is *centred*, so nothing looks
broken in isolation, but its edges do not line up with the icon columns beside it and the gap
between a widget and its neighbours is visibly larger than the gap between two icons. On a grid,
that reads as the widget being off the grid.

**The inset is the host's, not ours.** `AppWidgetHostView` applies a default padding around the
widget it hosts — `getDefaultPaddingForWidget`, meant to keep widgets built for older platforms from
touching each other on a home screen that does no spacing of its own. We *do* spacing of our own:
the grid is the spacing. So the host's padding is applied on top of a layout that already accounts
for it, twice over.

**Zeroing it is what launchers do.** A launcher that positions widgets on a grid takes
responsibility for the gaps between them, and the widget's own layout supplies its internal padding.
Leaving both in place means neither is in charge of the result.

**The size fix did not cover this.** `widget-sizing.md` made the provider lay out for the right
number of dp, and it does — the content is correctly sized *for the box it was given*. The box was
smaller than the cells by the host's padding, so both can be right and the widget still not reach
the grid lines.

## Requirement

1. **A hosted widget's view occupies the full cell rectangle** it is placed on, with no padding of
   the host's own.
2. **The widget's own internal padding is untouched** — spacing inside the widget belongs to its
   author.
3. **The cell rectangle is unchanged.** This removes an inset; it does not move or resize the
   placement.
4. **It applies to every hosted view**, including ones re-created after a restart.
5. **A view that refuses the change still draws** (GATE 4).

## Acceptance criteria

Measured on the `Pixel_Tablet` AVD, 5×4 widget at column 3.

- [x] The drawn rectangle matches the cell rectangle: 960 px wide spanning x 608–1567, against a
      cell rectangle of 960 px spanning 607–1567. It was 912 px, inset 25 px left and 24 px right.
- [x] Its left edge lines up with the icon column at the same grid column.
- [x] The widget still renders its content, with its own internal spacing unchanged.
- [x] The same holds after a force-stop and relaunch — the padding is removed where the view is
      created, which is also where it is re-created.
- [x] Tested against a real `AppWidgetHostView` rather than asserted in prose, for symmetric and
      asymmetric starting padding (`WidgetPaddingTest`).
- [x] `./gradlew test lint assembleDebug` green.

## Notes

- **Why measure rather than eyeball.** The inset is symmetric, so the widget looks deliberate and
  merely *small* — which is exactly the kind of thing that survives a visual check and shows up when
  someone puts an icon next to it.
- **Not in this slice:** a configurable gutter between grid items, or honouring a provider's
  requested margins where it declares any.

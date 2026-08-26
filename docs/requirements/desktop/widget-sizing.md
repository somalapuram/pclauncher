# Desktop — Telling a Widget How Big It Is

Status: **Implemented** (2026-08-24)

A hosted widget is told the size it is drawn at, whenever it is drawn at one. Amends
[`widget-resize.md`](widget-resize.md), which introduced the call and wired it to one path only.

## Context

A widget placed at 4×3 renders its content as a narrow pill in the middle of a large empty
rectangle. Dragging a resize handle fixes it — which is the clue.

`AppWidgetHostView.updateAppWidgetSize` is how a provider learns the space it has; until it is
called, the provider lays out for its own default and the host view centres that inside whatever
bounds it was given. The call exists and is correct, and it has exactly **one** caller: the resize
path. So a widget learns its size only if the user happens to resize it, and never when it is first
placed or when it is re-created after a restart.

**It looked like it worked because of which widgets were tested.** `widget-resize.md` was verified
with the Google Search widget, whose default size matches the cell it lands in, and the Dino widget,
whose content is a fixed picture that looks the same at any size. Neither could show the gap. A
widget with a genuinely responsive layout shows it immediately.

**The restart case is the worse half.** After a process restart every widget view is re-created from
the store, and nothing tells any of them their size — so a desktop that looked right before a reboot
comes back with every responsive widget collapsed to its minimum, and stays that way until each is
resized by hand.

**The size is a property of the placement, not of an event.** Deriving it from "the user just
resized" is what made it conditional on the user doing something. It follows from the span and the
cell size, both of which the surface drawing the widget already knows.

## Requirement

1. **A widget is told its size whenever the size it is drawn at is established** — when placed, when
   resized, and when re-created after a restart.
2. **The size reported is the one it is actually drawn at**: the span multiplied by the cell size.
3. **It is reported from one place.** The resize path must not need its own call, or the two can
   disagree.
4. **Reporting is idempotent and cheap** — repeating the same size does no work worth avoiding, and
   nothing recomputes per frame.
5. **A provider that refuses or fails the call costs the size, not the desktop** (GATE 4).

## Acceptance criteria

Verified on the `Pixel_Tablet` AVD with the Chrome bookmarks widget, whose layout is genuinely
responsive — which is what made it able to show the fault at all.

- [x] A newly placed 4×3 widget fills its rectangle: it rendered its "Mobile bookmarks" content
      instead of the narrow pill it drew before.
- [x] The same widget fills its rectangle after a force-stop and relaunch — the case that was
      broken for *every* widget, since nothing re-reported a size on re-creation.
- [x] Resizing still updates the content, with no call in the resize path: dragging the bottom
      handle took it 4×3 → 4×4 and the content followed.
- [x] A provider that ignores or refuses the call still draws — the call is guarded, as before.
- [x] The dp conversion from span and cell size is unit-tested, including a zero and a negative cell
      (`WidgetSizeTest`).
- [x] `./gradlew test lint assembleDebug` green.

## Notes

- **Why this hid for so long.** Every widget used to verify the resize work was one whose appearance
  is insensitive to size — a search bar at its default width, and a picture. The acceptance criteria
  said "the provider re-renders rather than stretching", and it did, for the one transition that was
  exercised. The missing case was never a transition at all.
- **Not in this slice:** honouring a provider's `minResizeWidth` when *placing* it (as opposed to
  when resizing), or `OPTION_APPWIDGET_MIN/MAX` ranges distinct from the exact size.

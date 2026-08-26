# Shell — Where the Tray Popover Sits, and How It Presses

Status: **Implemented** (2026-08-24)

The quick-settings popover lined up with the bar that opens it, and a tray control that presses in
its own shape. Amends [`quick-settings-surface.md`](quick-settings-surface.md), which specified the
panel's contents and treatment but never said where it goes.

## Context

**The popover floats free of the bar.** It is anchored `BottomEnd` to the tray *control*, and the
tray is not the last thing in the bar — the Show Desktop handle and two spacers sit to its right.
So the panel's right edge lands 44 px inside the bar's right edge on a 2560 px panel: near enough
to look like a mistake rather than a margin, and far enough to be obvious. Vertically it is placed
by a fixed 64 px nudge that has no relationship to the bar's height.

Desktops align a flyout to the *screen edge*, at the same margin the bar itself keeps, sitting just
above it. That reads as one piece of furniture. Anchoring to whatever control happened to be clicked
does not.

**The tray control still presses square.** The tiles inside the panel were fixed by clipping them
to their shape, but the control that *opens* the panel has the same defect and was missed: its
`clickable` sits before its rounded background and nothing clips it, so the press and hover
indication draw as a rectangle across the whole tray. This is the third time this ordering has bitten
— the shape has to be clipped above the click, not merely painted below it.

**The margin has to come from the same place as the bar's.** Restating 16 dp in the popover is how
the two drift apart the next time the bar's inset changes. The bar is padded by `PcSpacing.Large`;
the popover has to be positioned from that same token.

## Requirement

1. **The popover's right edge lines up with the bar's right edge** — the same margin from the
   window, taken from the same token the bar uses.
2. **The popover sits directly above the bar**, separated by one deliberate gap rather than a
   number chosen to look right at one screen size.
3. **The popover never leaves the window.** A panel taller or wider than the space available is
   clamped on screen rather than drawn off the edge.
4. **Placement is a pure function** of the anchor, the window and the popover's own size, so it can
   be asserted without a device.
5. **The tray control presses in its own shape** — the rounded highlight, not a rectangle.
6. **Behaviour is unchanged**: the same control opens and closes the panel, with the same
   accessible name, and outside-click and Back still dismiss it.

## Acceptance criteria

Measured on the `Pixel_Tablet` AVD (2560 px wide, 320 dpi).

- [x] The popover's right edge and the bar's right edge sit at the same distance from the window
      edge — both at x = 2527, 32 px in, where the popover was previously 76 px in.
- [x] The popover's bottom sits one gap above the bar's top.
- [x] A popover wider than the window is clamped to x = 0 (`TrayPopoverPositionTest`).
- [x] A popover taller than the space above the anchor is clamped to y = 0.
- [x] The placement arithmetic is unit-tested at both edges, in the ordinary case, and for the
      property that actually failed: moving the anchor horizontally must not move the popover.
- [x] Pressing the tray shows a rounded highlight.
- [x] The tray still opens and closes the panel and still reports its state as its description.
- [x] `./gradlew test lint assembleDebug` green.

## Notes

- **Why not simply anchor to the bar.** The tray composable does not know the bar's bounds, and
  handing them down would couple a control to its container for the sake of one offset. Positioning
  from the *window* achieves the same alignment because the bar's margin is a known token, and it
  keeps the tray usable in the overlay window that `overlay-service.md` will eventually host it in.
- **The margin is read from `PcSpacing.Large`, the token the bar is padded with.** Restating 16 dp
  in the popover is exactly how the two would drift apart the next time that padding changes, and
  the drift would be small enough to survive review.
- **Not in this slice:** an animated open, a pointer-anchored popover, or repositioning when the
  bar moves to the left or right edge (SRS §6.3 makes that a setting; it will need its own pass).

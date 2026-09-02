# Shell — the chrome uses one window per size, not one window that resizes

## Context

The chrome moved into a `TYPE_APPLICATION_OVERLAY` window (`overlay-service.md`) and the bar did
appear above app windows. But on the device the user reported: *"I see start menu flicker :( and
task bar flicker"*.

Measured cause, from `dumpsys window` sampled across a Start click. One window hosted both the bar
and the menu, sized `WRAP_CONTENT`:

- at rest — `Requested w=2560 h=144`, `fl=... NOT_FOCUSABLE`
- menu open — `Requested w=2560 h=1488`, `fl=... WATCH_OUTSIDE_TOUCH` (focusable)

So each Start click re-laid-out the overlay window **twice**: once for the size change as the menu
composed into it, and again for the focus-flag change that lets the menu take keys. An overlay
window's surface is re-created on those, and the re-creation is what the eye reads as the bar and
the menu blinking.

The same single window also clips: it is exactly as tall as the bar, and a dock icon at peak
magnification (`DockMagnification.MAX_SCALE`) stands taller than the bar it sits in, so the window
edge cut the top off the icon under the pointer.

Separately, the home activity dropped its own bar the moment it *asked* the service to start,
before the overlay window existed — a gap with no bar at all, seen as a blink on every launch.

## Requirement

1. **The bar window never changes size or focusability** once it is added. It is added at bar
   height plus magnification headroom, non-focusable, and stays that way for its whole life.
2. **A menu opens in a window of its own** — added when it opens, removed when it closes. That
   window is full-screen (its click-catcher has to cover the screen) and focusable.
3. **Focusability is fixed per window at construction.** No `updateViewLayout` that changes size or
   flags, because that is the thing that blinks.
4. **The bar window leaves headroom** for a magnified dock icon, so the window edge never crops it.
5. **The menu's open state is shared, not duplicated.** One flow drives both windows: the bar draws
   the Start button pressed from it, and the service adds and removes the menu window from it.
6. **A menu window that cannot be added must not leave the Start button stuck pressed** (GATE 4).
7. **The home activity hides its own bar only once the overlay bar is actually on screen**, not
   when the service is asked to start.

## Acceptance criteria

- [ ] Opening and closing the Start menu leaves the bar's window size and flags unchanged —
      `Requested w/h` and `fl=` identical before, during, and after (checked with `dumpsys window`).
- [ ] The Start menu appears above app windows, clear of the bar.
- [ ] A dock icon at peak magnification is not cropped by the window edge.
- [ ] The Start button is not left pressed when the menu window cannot be shown.
- [ ] There is never a moment with no bar on screen while the shell starts.
- [ ] `./gradlew test lint assembleDebug` green; checked on device.

## Notes

- **Cost accepted:** the headroom strip above the bar belongs to the bar window, so touches there
  do not reach the app behind. It is `MagnificationHeadroom` (12 dp) at the very bottom edge of the
  screen, directly above a bar that already occupies that region.
- **Why not one full-screen window all the time.** A full-screen non-focusable overlay still
  consumes every touch inside its bounds, which would make the app behind it unusable. Restricting
  the touchable area to the bar needs `ViewTreeObserver.OnComputeInternalInsetsListener` and
  `TOUCHABLE_INSETS_REGION`, which are hidden APIs — out of scope in Stage A (GATE 3).
- The tray popover is a Compose `Popup`, which is already its own window, so it is not clipped by
  the bar window and needs no third window here.
- Supersedes nothing; extends `overlay-service.md`.

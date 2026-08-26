# Shell — Magnification That Stays Put and Lands on the Right Icon

Status: **Implemented, hover unverified on device** (2026-08-24)

Hovering the dock magnifies the icon under the pointer, and does not resize the bar. Amends
[`dock-taskbar.md`](dock-taskbar.md).

## Context

Two faults, both visible the moment a pointer crosses the bar.

**The bar resizes.** Hovering grows the bar by 14 dp and shrinks it again on exit, so the whole
piece of furniture — and everything measuring against its top edge — moves whenever the pointer
passes over it. The reasoning was that a magnified icon drawn outside the bar "would float over the
wallpaper with no background and read as a glitch". That is what a dock does: on macOS the dock's
own background stays put and the icons rise above it. Growing the bar is the more distracting of the
two by a wide margin, because it moves things the user is not pointing at.

**Magnification lands on the wrong icon.** `dockOriginX` is declared, read, and never assigned — a
`onSizeChanged` block carries a comment promising the origin is "captured below via position", and
no such capture exists. So the pointer is mapped against a dock that starts at x = 0 while the real
dock is centred around the middle of a 2560 px bar. The scale is applied to whatever index that
arithmetic lands on, which is not the icon under the pointer.

**And the pitch is stale.** The index is computed with a pitch of `MinTouchTarget`, but a dock item
now sizes to the larger of the touch minimum and the icon — 48 dp since the icons were grown. Even
with the origin fixed, every item past the first would drift by 4 dp more than the last.

A dropped assignment is invisible in review: the value is declared, the call site reads it, and
nothing warns. This is the same shape as the widget resolver that was accepted and never forwarded.

## Requirement

1. **The bar's height does not change on hover.** It rests at its resting height whatever the
   pointer is doing.
2. **A magnified icon may extend above the bar**, the way a dock's icons do.
3. **The icon under the pointer is the one that magnifies**, at any position along the bar.
4. **The pointer index is computed in one coordinate space** — the pointer position and the dock's
   origin must be measured against the same origin, not two different ones.
5. **The item pitch matches the item's real width**, derived from the same sizes the dock lays out
   with rather than restated.
6. **Magnification still falls off with distance** and still turns off with the setting.

## Acceptance criteria

- [x] The bar's height is identical with the pointer over it and away from it — the growth path is
      gone, and `BarAlignmentTest` pins the bar to its resting height.
- [ ] Hovering the leftmost dock icon magnifies the leftmost icon; the same for the rightmost.
      **Not verified on device:** `adb shell input motionevent` throws on this system image, so a
      hover cannot be synthesised. The coordinate mapping is unit-tested instead, and the behaviour
      needs a human with a mouse.
- [x] The pointer index is derived from a dock origin that is actually assigned — the dock row now
      reports its position in root coordinates, and the pointer is lifted into the same space.
- [x] The pitch used for the index equals the width a dock item lays out at
      (`maxOf(MinTouchTarget, DockIcon)`), not the touch minimum it assumed.
- [x] `DockMagnificationTest` covers all three: the icon under the pointer is the largest, a default
      origin picks a different icon entirely, and the two pitches disagree.
- [x] Magnification still decays with distance and is disabled by the setting.
- [x] `./gradlew test lint assembleDebug` green.

## Notes

- **Why the origin is stored rather than derived.** The dock is centred on the *screen*
  (`BarLayout.dockStartX`), so its left edge depends on the width of both neighbouring zones. Asking
  the layout where it actually put the dock is honest; recomputing the same formula in a second
  place is how the two get to disagree.
- **The icon magnification itself is untouched.** What was removed is the *bar's* height growth and
  the `barHeightFor` helper that sized it; `DockMagnification.scaleAt` still runs per icon at
  `MAX_SCALE = 1.62` through the same graphics layer. A 48 dp icon is drawn at about 78 dp and rises
  above a 56 dp bar, which is what a dock does.
- **The scale is not time-animated, and was not before either.** It is a continuous function of
  pointer position, so it is smooth while the pointer moves through the falloff — the only thing
  that was ever animated here was the bar's height, and that is the part that had to go.
- **Not in this slice:** magnification on touch (there is no hover to track), or animating the
  running indicator with the icon.

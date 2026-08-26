# Shell — The Pointer's State, Told Truthfully

Status: **Implemented, hover unverified on device** (2026-08-24)

Magnification that lets go when the pointer leaves, and a drag ghost that sits under the pointer
rather than near it. Amends [`dock-magnification-fixes.md`](dock-magnification-fixes.md) and
[`direct-manipulation.md`](direct-manipulation.md).

## Context

**Magnification sticks.** The bar reads the pointer with a loop that assigns
`pointerX = event.changes.last().position.x` for *every* event it receives. An `Exit` event carries
a position like any other, so leaving the bar records one last coordinate and then goes quiet — and
the icon under that stale coordinate stays enlarged indefinitely. Clicking an icon has the same
shape: the launched app takes the pointer, no further events arrive, and the dock is left frozen
mid-magnification.

The state is not wrong so much as *abandoned*. Nothing tells the bar the pointer is gone, because
the code never asked what kind of event it was holding.

**The drag ghost is offset from the pointer.** Drag positions are recorded in **root** coordinates —
every producer builds them as `positionInRoot() + local`, and the drop test converts back the same
way, which is why drops land on the right cell. The ghost, though, is drawn inside the shell's Box,
and that Box is inset by the window's safe-drawing padding. So the ghost is placed at a root
coordinate inside a space whose origin is not the root's: it renders low and right by exactly the
inset, which on this device is the status bar's height.

Drops are correct and the picture is wrong, which is the confusing combination — the icon lands
where the user meant, but never looked like it would.

## Requirement

1. **Leaving the bar ends magnification.** Every icon returns to rest when the pointer exits.
2. **Clicking an icon does not strand the dock magnified** — if the pointer stops being reported,
   the dock returns to rest rather than holding its last frame.
3. **A pointer inside the bar but outside the dock's span leaves the icons at rest**, which the
   index mapping already decides; the loop must not defeat it by holding a stale position.
4. **The drag ghost is centred on the pointer**, in whatever coordinate space it is drawn in.
5. **The space conversion is explicit**, not implied by where the composable happens to sit — the
   ghost is told the origin of its own space so the correction cannot silently disappear if the
   layout is rearranged.
6. **Drops are unaffected.** They already work; the fix is to the picture only.

## Acceptance criteria

- [ ] Moving the pointer off the bar returns every icon to rest. **Not verified on device:**
      `adb shell input motionevent` throws on this system image, so an exit cannot be synthesised.
      `PointerReleaseTest` covers the decision that was wrong.
- [ ] Moving the pointer past the ends of the dock returns the icons to rest — same limitation; the
      index mapping already returned null there, and the loop no longer defeats it with a stale
      position.
- [ ] Launching an app from the dock leaves the dock at rest — same limitation.
- [x] The drag ghost's centre tracks the pointer with no constant offset — checked with an injected
      touch drag, the ghost riding under the finger.
- [x] Dropping still lands on the cell under the pointer: drops were always in root space, and that
      path is untouched.
- [x] The ghost's placement arithmetic is unit-tested, including a non-zero space origin and the
      round-trip that its centre returns the pointer (`DragGhostTest`).
- [x] `./gradlew test lint assembleDebug` green.

**The device check on the ghost is approximate.** The injected swipe's start time cannot be pinned
precisely, so the pointer's position at capture is known to within a few tens of pixels — enough to
show the ghost is no longer displaced by a whole status bar, not enough to call it pixel-exact. The
arithmetic is what pins that.

## Notes

- **Why the loop has to read the event type.** `awaitPointerEvent()` reports enters, moves, presses,
  releases and exits alike, and a position is present on all of them. Treating them as
  interchangeable is what turned "the pointer left" into "the pointer is still there, at the door".
- **Not in this slice:** a spring on the scale as it settles back to rest, or magnification driven
  by a pointer that has left the window entirely.

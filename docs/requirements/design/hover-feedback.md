# Design — Hover Feedback Everywhere You Can Point

Status: **Implemented, hover unverified on device** (2026-08-24)

Desktop icons, desktop widgets and Start menu entries respond to the pointer, the way the dock
already does. Derives from SRS §6.1 principle 3 (pointer-first, touch-tolerant) and §6.1 principle 4
(motion is macOS: spring-based, short, degradable).

## Context

The dock magnifies under the pointer and the Start button lights up. Everything else on the desktop
is inert: an icon, a widget and a Start menu entry all look exactly the same whether the pointer is
on them or on the wallpaper. On a machine driven by a mouse, that is the single most common question
the interface has to answer — *what am I about to click?* — and three of the four surfaces do not
answer it.

**This is what pointer-first means in practice.** SRS §6.1 asks for hover states as the first-class
affordance and a long-press equivalent for touch. The dock got them; the rest never did.

**Different surfaces want different answers.** An icon is a small object on a wallpaper and can
afford to grow. A widget is a large piece of someone else's UI — growing it would reflow its
contents and fight the provider — so it needs an outline rather than a size change. A menu row is
a list item and wants the row highlight every list has had for forty years.

**A widget's own hover must still work.** Widgets are interactive: a hover treatment on the
container cannot consume the pointer, or the widget's own buttons stop responding — the same
constraint the long-press and drag gestures already work under.

**It has to cost nothing on a software renderer.** SRS §4.3 rules out per-frame blurs and stacked
translucency. A scale is a graphics-layer transform on an already-composited bitmap and an outline
is one stroke; both are affordable where a shadow or a blur would not be.

## Requirement

1. **A desktop icon responds to hover** — it grows slightly and takes a soft background behind icon
   and label together, so the pair reads as one target.
2. **A desktop widget responds to hover** with an outline, not a size change, so the provider's
   layout is untouched.
3. **A Start menu entry responds to hover** with a row highlight distinct from its selected state,
   so hovering and keyboard selection do not look identical.
4. **Hover never consumes the pointer.** Clicks, drags, long-presses and a widget's own touches all
   behave exactly as before.
5. **Transitions are animated**, short and spring-based (SRS §6.1 principle 4), not instant jumps.
6. **Touch is unaffected.** A finger produces no hover state, and nothing about the touch paths
   changes.
7. **The visual decision is a pure function** of the interaction state, shared by all three
   surfaces so they cannot drift apart.

## Acceptance criteria

The hover states themselves cannot be exercised here: `adb shell input motionevent` throws on this
system image, so no pointer enter can be synthesised. What is checked below is the decision, and
that nothing on the touch paths regressed.

- [ ] Hovering a desktop icon grows it and shows a background behind icon and label. **Not verified
      on device** — needs a real pointer.
- [ ] Leaving it returns it to rest — same limitation; `PointerReleaseTest` covers the equivalent
      release for the dock, and `hoverable` reports exits directly.
- [ ] Hovering a widget outlines it without changing its size — same limitation.
- [ ] Hovering a Start menu entry highlights the row — same limitation. The *selected* row's wash
      was confirmed on device and is a different value, so the two cannot look identical.
- [x] A widget's own clickable areas still respond: `hoverable` observes without consuming, the same
      discipline the long-press and drag already work under.
- [x] Dragging an icon still works — checked on device, an icon moved two cells.
- [x] Clicking still works — checked on device, an app launched from the desktop.
- [x] The Start menu still opens and renders.
- [x] The scale and wash decisions are unit-tested for hovered, pressed and at-rest, including that
      a press outranks the hover it always arrives with (`HoverFeedbackTest`).
- [x] A hovered icon's growth is asserted to stay inside its cell, so it cannot reach a neighbour's
      artwork.
- [x] `./gradlew test lint assembleDebug` green.

## Notes

- **Why one shared function.** Three surfaces implementing "a bit bigger, a bit lighter" separately
  is how they end up with three different bits. The numbers live in one place and each surface picks
  which of them apply to it.
- **Why the widget gets an outline instead of a scale.** Scaling a hosted view resamples someone
  else's rendered UI, and at the sizes widgets occupy that is visible as blur. An outline says the
  same thing without touching the content.
- **The transform goes *after* the gesture modifier, not before.** A graphics layer applies to
  everything inside it, including a pointer node — so scaling above the gesture would scale the
  coordinates a drag reports, by 8% exactly while hovering, which is when a mouse drag begins. The
  hit area stays put and only the picture grows.
- **Not in this slice:** hover previews on taskbar chips (`dock-taskbar.md` has its own plan for
  those), a cursor change per surface, or hover sounds.

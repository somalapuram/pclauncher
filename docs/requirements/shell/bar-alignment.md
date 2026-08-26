# Shell — Bar Alignment and the Start Glyph

Status: **Implemented** (2026-08-24)

The dock's icons on the same line as everything else in the bar, and a Start glyph that does not
look like Windows. Amends [`dock-taskbar.md`](dock-taskbar.md).

## Context

**The dock sits lower than the rest of the bar.** Everything in the bar — the Start button, the
window chips, the tray, the Show Desktop handle — is centred vertically. The dock alone is
bottom-aligned, at two levels: the row is `Alignment.Bottom` in the bar, and inside each dock item
the icon is `BottomCenter` within its touch target. The result is a Start button whose centre is
visibly above the row of icons beside it, which is the first thing the eye lands on because the two
are adjacent.

**Bottom alignment was for magnification, and it is not needed for it.** Dock icons grow upward on
hover so they do not push through the bar's lower edge — but that is done with a `graphicsLayer`
transform origin, which is a draw-time effect and independent of where layout puts the icon.
Centring the row changes where the icon rests without changing how it grows.

**The Start glyph is a Windows logo.** Four panes with the top-left one detached — the code calls it
"a nod to the Windows four-pane Start without copying it". On a device that *is* Android, running
Android apps, the nod reads as the wrong operating system. SRS §1 asks for a hybrid that "must feel
like neither a phone launcher nor a copy of either desktop", and a Windows mark is the most literal
possible copy of one of them.

The Windows *organisation* — a Start button at the left of the bar opening a searchable pinned/all
apps menu — is what §6.4 asks for and is unchanged. Only the mark on the button is at issue.

## Requirement

1. **The dock's icons are vertically centred in the bar**, on the same line as the Start button, the
   tray and the chips.
2. **Magnification still grows upward** from near the icon's base, unchanged.
3. **The running indicator stays at the bottom of the item** and does not push the icon off centre.
4. **The Start glyph is Android-style** — an app-grid mark, not a four-pane window.
5. **The glyph reads at 20 dp** in both themes, filled with a single colour like the rest of the
   shell's marks.
6. **The button's behaviour is unchanged** — same target size, same open/hover/pressed states, same
   accessible name.

## Acceptance criteria

- [x] The Start button's centre and the dock icons' centres sit on the same line — asserted within
      1 dp by `BarAlignmentTest`, which failed at a 6 dp gap before the fix.
- [x] Hover magnification still grows the icon upward: it is a `graphicsLayer` transform origin and
      was never coupled to layout alignment.
- [x] A running app's indicator sits below its icon and no longer shifts it — the indicator is
      aligned inside the item rather than stacked in flow.
- [x] The Start glyph is a three-by-three grid of dots.
- [x] The glyph is legible at 20 dp in light and dark.
- [x] The Start button still reports itself as "Start" and still opens the menu.
- [x] Alignment is asserted, not eyeballed, in three cases: at rest, with an app running, and
      against the tray.
- [x] `./gradlew test lint assembleDebug` green; checked on device.

**There were two misalignments, not one.** Centring the dock row inside its box left a 6 dp gap
still, because `weight` sizes a Row child's *width* — the box kept the Row's `Alignment.Bottom` and
its content-sized height rode the bar's lower edge. The test caught the remainder; by eye the first
fix looked done.

## Notes

- **Why a dot grid.** It is the app-drawer mark Android launchers have used for a decade, it is
  unmistakably not a window, and it survives being drawn at 20 dp — which a robot silhouette or
  anything with fine detail would not.
- **Not in this slice:** a user-selectable Start mark, an app logo in the button, or moving the
  Start button's position (SRS §6.3 puts it at the left).

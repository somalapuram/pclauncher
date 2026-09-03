# Shell — Start Glyph Size

Status: **Accepted · Implemented** (2026-09-03)

The grid of boxes inside the Start button is drawn larger, and larger again while the menu is open.
SRS §6.1 principle 1, §6.3. Sits on top of
[`start-button-gloss.md`](start-button-gloss.md), which gave the tile behind it a surface.

## Context

The Start button is a 48 dp tile carrying a 3×3 grid glyph. The glyph was drawn at 20 dp — 42% of
the tile's width — which leaves it looking like a small mark centred in a large empty square. Now
that the tile is glossy and reads as a real object, the undersized glyph is the thing that stands
out: the button is the visual anchor of the whole shell and the first thing the eye lands on
(§6.3), and its content should fill it with the same confidence the dock icons fill theirs.

**Growing it on open is state, not decoration.** The button already signals "I own the menu that is
up" through the accent fill. Size is a second, independent channel for the same fact, and it is the
one that survives a wallpaper whose colours leave the accent close to the tile beneath it. It also
matches the shell's motion language — spatial and short (§6.1 principle 4), the menu growing from
the button that spawned it.

## Requirement

1. **The glyph is 30% larger at rest** than the 20 dp it was drawn at.
2. **It grows further while the menu is open**, so open is distinguishable from rest by size alone,
   with the tile's colour ignored.
3. **The growth is animated**, on the shell's existing spring, not an instant jump.
4. **The glyph stays inside its tile** at every size, with margin — a glyph touching the tile's
   border would read as clipped.
5. **The size decision is pure and tested**, like the fill decision beside it.
6. **Contrast is unaffected** — SRS §12's 4.5:1 floor still holds at both sizes.

## Acceptance criteria

- [x] The glyph is 30% larger at rest — 20 dp → **26 dp**, exactly 1.3×.
- [x] It is larger again when open — **30 dp**, a further 15%.
- [x] Open and rest differ by size alone: 30 dp vs 26 dp, asserted without reference to colour.
- [x] The growth animates on `PcMotion.DockMagnify`, the spring the dock and the button's own
      press-scale already use.
- [x] The glyph never reaches the tile edge — 30 dp inside a 48 dp tile leaves 9 dp a side, and the
      1 dp border sits outside that.
- [x] Verified on device (2560×1600 at 320 dpi, so 1 dp = 2 px), measuring the dot grid inside the
      tile's full-width band: **36 px (18.0 dp) square at rest, 40×41 px (20.0×20.5 dp) open** — a
      1.11–1.14× growth against the layout's nominal 1.154×, the shortfall being the antialiased
      dot edge the ink threshold trims.
- [x] The dots themselves grew 30%: the vector carries ~31% internal padding, so the 20 dp box drew
      a 13.8 dp grid and the 26 dp box draws the measured 18.0 dp — exactly 1.30×.
- [x] Contrast holds at both sizes, measured per glyph row against the tile's real colour beside the
      dots: worst row **5.97:1** at rest, **5.18:1** open. Both clear SRS §12's 4.5:1.
- [x] `startGlyphSize` is pure and unit-tested (6 tests).
- [x] `./gradlew test lint assembleDebug` green — 565 tests, 0 failures, lint clean.

## Notes

- **30% is measured from the original 20 dp**, not compounded with the open-state step. Rest is
  26 dp; open adds its own 15% on top of that, which is a visible change without the glyph
  crowding the tile.
- **Size is a redundant channel, deliberately.** The accent fill already says the menu is open.
  Redundancy is the point: SRS §6.1 principle 5 asks that state be distinguished by more than
  colour alone, and a wallpaper-derived accent is not guaranteed to contrast with the bar.
- **Not in this slice:** scaling the glyph on hover, a user-facing size setting, or changing the
  glyph artwork itself.

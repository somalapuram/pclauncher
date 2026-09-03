# Shell — The Start Button Dims Under the Finger

Status: **Accepted · Implemented** (2026-09-03)

Pressing the Start button makes it darker, not brighter. SRS §6.1 principles 1 and 4, §12.
Adjusts the press state introduced in [`start-button-gloss.md`](start-button-gloss.md).

## Context

The gloss slice gave every state of the Start button a lit top and a shaded underside. That is
right for rest and for open, but it made **press** the brightest thing the button ever does: the
pressed fill is the accent at 45%, its top lifted 34% toward white, with a white specular over it.
Holding the button down flashes it lighter than the menu it is about to open.

That is backwards. A surface pushed under a finger goes *away* from the light — every physical
control and every desktop convention darkens on press. The button already shrinks to 92% on press
(`start-scale`), which reads as depression; brightening at the same moment fights it, and the two
together read as a flicker rather than a press.

**Brightness is the channel to change, not alpha.** Dropping the fill's alpha would make the tile
more transparent, which on a dark bar happens to look dimmer but on a light one would look
*lighter*. The gloss profile already shades colour (start-button-gloss.md), and that is the channel
that means "less light falls here" at either polarity.

## Requirement

1. **The pressed tile is dimmer than the tile at rest**, at every point down its height.
2. **The dimming is a black scrim over the finished tile**, not a change of alpha and not a shift in
   the gloss profile. It darkens whatever the profile produced, so the press and the gloss can be
   tuned independently, and it reads as *less light* at either polarity.
3. **The gloss survives the press.** The tile still has a lit top and a shaded underside; it is the
   same object under less light, not a flat dark chip.
4. **The specular dims with it.** A full-strength white highlight on a tile that has just gone dark
   is the brightest thing on it and undoes the effect.
5. **Press still outranks hover and is outranked by open** — the precedence from
   `start-button-gloss.md` is unchanged.
6. **The glyph stays legible while pressed** — SRS §12's 4.5:1 still holds.
7. **The dimming is pure and tested.**

## Acceptance criteria

Measured on device at 2560×1600 / 320 dpi, sampling the tile column clear of the glyph (`x = 62`,
every 8 px down the tile), holding the button with a long `input swipe` to capture the pressed
frame. The bar behind reads 13.

- [x] The pressed tile is dimmer than rest at **every** sampled depth —
      **`41, 39, 38, 36, 35, 34, 31, 27, 23`** pressed against
      **`74, 63, 52, 40, 36, 38, 40, 41, 43`** at rest.
- [x] The regression this replaced is recorded: before any dim, pressed measured
      `97, 89, 81, 72, 66, 62, 58, 53, 49` — the *brightest* state the button had.
- [x] The dimming is a scrim, not an alpha change — `startTileFill`'s pressed alpha is unchanged at
      0.45, asserted in `StartPressDimTest`.
- [x] The dimming does not touch the gloss profile, so the two tune apart — asserted directly
      against `AccentGlossLift` / `AccentInnerShade`.
- [x] The tile still reads as glossy while pressed: lit top over a shaded underside, `41 → 23`.
- [x] The specular dims with the tile — `StartSpecularAlpha` 0.30 → 0.14 while pressed.
- [x] Precedence unchanged — open still beats press, press still beats hover.
- [x] The glyph stays legible while pressed — **9.26:1**. Dimming the tile *improves* the light
      glyph's contrast.
- [x] `pressScrimAlpha` is pure and unit-tested (8 tests).
- [x] `./gradlew test lint assembleDebug` green — 573 tests, 0 failures, lint clean.

## Notes

- **The press already had a scale.** The button shrinks to 92% under the pointer, and the two
  signals now agree: it gets smaller *and* darker. Before this they disagreed, which is why the
  press read as a flash.
- **Dimming is a scrim rather than a fourth role.** Press is not a different kind of surface — it
  is the same surface under less light.
- **Two measurements changed the design here, and both were invisible without the device.**
  First, a 0.22 dim left the press *brighter* than rest — pressing also switches the fill from a
  12%-alpha wash to a 45%-alpha accent, so the dim must overcome a role change, not soften a
  shade. Second, once the dim was a shift in the gloss profile, deepening the open state's gloss
  pushed the pressed tile straight back up. That coupling is why the dim became a scrim over the
  finished tile: it cannot be undone by tuning the gloss. The scrim then needed 0.45 → 0.58 → 0.62
  as the gloss and the rest-state sheen moved, each step measured rather than reasoned.
- **Not in this slice:** a dim on the dock icons or the tray under press, a press ripple, or a
  configurable press strength.

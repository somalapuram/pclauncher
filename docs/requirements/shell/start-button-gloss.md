# Shell — A Glossy Start Button

Status: **Accepted · Implemented** (2026-09-03)

The Start button reads as a piece of glass like the icons beside it, in every state. SRS §6.1
principles 1 and 2, §6.3. Extends [`../design/icon-gloss.md`](../design/icon-gloss.md)'s treatment
to the one tile in the bar that is drawn live rather than baked.

## Context

The Start button is the visual anchor of the whole shell — the leftmost thing in the bar and the
first thing the eye lands on. It is drawn as a flat fill behind a glyph: `Color.Transparent` at
rest, a flat wash on hover, flat accent when open.

Everything around it is glossy. The dock icons are composited tiles with a gloss, a specular band,
a lit rim and an inner shade (`design/icon-gloss.md`). The bar underneath them is a vertical sheen
rather than a flat colour, precisely so it does not look flat next to them
(`surfaceSheen`, SurfaceTreatment.kt). The Start button sits between the two and is the only
element of the set that never got the treatment — at rest it is a hairline outline around nothing.

**The gloss must be live, not baked.** `IconStyle` is a compositor for bitmaps: it runs once per
icon and is cached on disk under `TREATMENT_VERSION`. The Start button changes appearance on hover,
press and open, so it cannot be a cached bitmap. The live vocabulary is `surfaceSheen`, already used
by the bar and the quick-settings panel — one linear gradient, a single shader on a fill that was
happening anyway, which is what §4.3 can afford on a software renderer.

**A blur or a per-frame layer is not available.** Software rendering (§4.3) rules out
`RenderEffect`, stacked translucent layers, and large alpha-composited shadows. Whatever makes the
button read as glass has to come out of the fill and at most one overlay.

## Requirement

1. **The Start button carries a gloss in every state**, including at rest — it is a tile, not an
   empty outline.
2. **The gloss is the shell's existing one.** It uses `surfaceSheen` rather than a second gradient
   vocabulary, so the button, the bar and the tray panel stay one design.
3. **A specular highlight over the upper part of the tile**, because a single soft gradient reads
   as a tinted fill; it is the tight bright band that makes a surface read as *curved glass*. This
   is the same reasoning `IconStyle.specular` records for the baked tiles.
4. **The opaque state is glossy too, and differently.** `surfaceSheen` varies alpha, which an
   opaque accent has none of — it collapses to a flat chip. That state shades its **colour**
   instead: lit on top, shaded underneath, so it reads as an object with a body rather than a
   stronger copy of the rest state's wash.
5. **State still reads.** Open outranks hover, which outranks rest, and the difference between them
   must remain obvious — gloss may not flatten the states into each other.
6. **The glyph stays legible** on the glossy tile at both polarities: contrast ≥ 4.5:1 per SRS §12.
7. **No new per-frame cost.** No blur, no extra composited layer beyond one overlay brush, and no
   allocation of a `Shape` per recomposition.
8. **The state→fill decision is pure and tested**, not buried in the composable.

## Acceptance criteria

Measured on the emulator at 2560×1600 / 320 dpi, in the dark polarity, sampling a column through
the tile clear of the glyph (`x = 62`). The bar behind it reads 13.

- [x] The button is glossy at rest. **Before:** the tile column reads a flat `14` — indistinguishable
      from the bar at `13`, which is what made it an outline around nothing. **After:** `81, 70, 59,
      48, 38` down the specular's fall-off, then `40, 42, 43, 45` as the sheen lifts toward the
      bottom edge.
- [x] Gloss uses `surfaceSheen`, not a second gradient helper — one call, same as `ShellBar` and
      `QuickSettingsPanel`.
- [x] The specular band is brighter than the fill beneath it — `81` at the top against `38–45`
      through the body.
- [x] Open, pressed, hovered and rest remain distinguishable — the four fills differ in role or
      alpha, asserted as a set-size equality in `StartTileFillTest`.
- [x] The open state is glossy, not flat. **Originally:** `192 → 186` top-to-bottom — a 6-level
      spread that reads as a flat chip. **First fix:** `201 → 137` (1.47×) — a gradient, but still
      read flat beside the composited icon tiles. **Now:** `210, 205, 200, 193, 189, 186, 168, 144,
      121` — a **1.74×** spread, a lit top over a genuinely shaded underside.
- [x] The two states are glossed differently in kind, not strength: rest is a translucent wash
      falling into the bar, open is an opaque tile lit from above. Asserted in `StartTileGlossTest`.
- [x] The glyph stays legible, measured per glyph row against the tile's real colour beside the
      dots: **5.31:1** at rest, **9.26:1** pressed, **4.62:1** open. All clear SRS §12's 4.5:1 —
      but see the margin note below; open has only 0.12 to spare.
- [~] **The light polarity was not measured on device.** The emulator is themed from a dark
      wallpaper, so only the dark tile was sampled; the light one is covered by unit tests and
      reasoning, not by a screenshot.
- [x] No blur and no second layer: one gradient background plus one overlay brush, and the shape is
      `remember`ed rather than allocated per recomposition.
- [x] `startTileFill` and `startTileGloss` are pure and unit-tested — every state, precedence,
      alpha range, the sheen's stops staying legal, and that shading preserves alpha (19 tests).
- [x] `./gradlew test lint assembleDebug` green — 565 tests, 0 failures, lint clean.

## Notes

- **Rest is where the change is visible.** The other three states already had a fill; rest had
  none, which is why the button looked like an outline rather than a member of the icon set.
- **Precedence is unchanged** from the original: open beats pressed beats hovered. The gloss is
  applied to whatever that decision produces, so the state machine and the appearance stay
  separable — which is what makes the decision testable without a composition.
- **The specular is drawn over the fill, not under it.** `IconStyle` records the same finding for
  the baked tiles: highlights painted before the artwork survive only in the margin, and the tile
  measured flat despite having two highlight layers. Here the glyph is small and centred, so the
  band is placed above it and kept subtle enough not to wash the glyph out.
- **The open state was flat, and the cause was the mechanism, not the strength.** At alpha 1 the
  sheen's three stops collapse to `0.93, 1.0, 1.0`: an opaque fill has no alpha headroom, so
  varying alpha does nothing to it. Turning the gloss up would not have helped. Shading the
  *colour* — lit top, shaded underside, the `IconStyle.innerShade` idea — is what gives an opaque
  tile a body. Worth remembering for any other opaque surface that wants the shell's gloss.
- **The open state's contrast margin is thin, and the accent is wallpaper-derived.** Deepening the
  gloss took it from 5.18:1 to 4.62:1 against a 4.5:1 floor. Two things were tried to buy margin
  back: reducing the inner shade (0.42 → 0.32) moved contrast only 4.62 → 4.72 while costing real
  depth (1.74× → 1.62×), so the shade is not what costs contrast; placing the gradient's midpoint
  at 0.62 instead of the centre, below the glyph, recovered the depth at no contrast cost and was
  kept. **Re-measure this against a wallpaper that yields a darker accent** — the floor is a hard
  SRS §12 requirement and this is the value nearest to it.
- **Not in this slice:** a glossy treatment for the taskbar window chips, the tray icons, or the
  Show Desktop handle; a user-facing gloss toggle; animating the specular under the pointer.

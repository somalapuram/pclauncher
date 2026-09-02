# Design — a white app icon must not land on a white tile

## Context

`light-icon-separation.md` gave every light tile a shadow, and recorded what it did not fix:

> The Clock tile is still the palest of the set, and the reason is its own artwork rather than the
> treatment: Android's stock Clock is a white face with thin hands, so a white glyph lands on a
> white tile.

That is now the ask. The tile is built by blending the app's own colour into a near-white base
(`tileColorFor`, `tileTint = 0.52`). For an app whose icon is genuinely white — Clock's adaptive
background is white and its foreground is a pale face — the blend has nothing to move toward, so the
tile stays near-white and the glyph disappears into it. Measured: Clock's tile reads 235 against a
250 panel after the shadow fix, still the palest of the set, and the face itself is white on white.

The reference is consistent about this. Where a glyph is white — Contacts, Messages, Phone, Email —
its tile is a saturated colour; where a glyph is colourful — Gmail, Chrome, Photos, Play Store — its
tile is white. The tile supplies whatever the glyph lacks. Those apps happen to have a brand colour
in their artwork for us to find; Clock does not, and something still has to be chosen.

## Requirement

1. **A tile is never so light that a pale glyph vanishes into it.** There is a ceiling on how light
   a light-theme tile may be, and a tile above it is brought down to it.
2. **Hue survives the correction.** A pale yellow tile that is too light becomes a deeper yellow,
   not a grey. Only a tile with no hue at all — a white app — becomes neutral.
3. **Tiles already below the ceiling are untouched**, so the colours tuned against the reference in
   `icon-gloss.md` keep their values and the set still reads as one design.
4. **The dark style is unaffected.** Its tiles are near-black; a lightness ceiling is meaningless
   there and must not be applied.
5. **The corrected tile still separates from the panel** by at least as much as the rest of the set.
6. **The cached artwork is invalidated** (`TREATMENT_VERSION`), or existing installs keep the old
   white-on-white tiles for good.

## Acceptance criteria

- [x] The Clock tile is visibly a tile. Separation from the 250 panel across the three states:
      −9 originally, −15 after the shadow, **−17** now, with a defined edge and a cool body.
- [x] The Clock face is discernible against its own tile.
- [x] Calendar, Chrome, Contacts and Camera are unchanged to the pixel: −29, −33, −35, −19 before
      and after the ceiling.
- [x] The dark theme is unchanged — `DarkGlass.maxTileLuminance` is 1, which disables the ceiling,
      and a test asserts it.
- [x] A tile with hue that is too light keeps its hue (unit test).
- [x] `./gradlew test lint assembleDebug` green; checked on device.

## Residual

The ceiling applies to the tile's *fill*, and the gloss, specular and glaze are painted over it —
so at the lit top-left corner a clamped tile still washes back towards white (measured 248 → 244 at
that corner, against 235 → 224 for the fill). The tile reads as a tile now, and the lower two-thirds
carry the correction; going further would mean weakening the highlights for every light tile, which
is a change to the style rather than to this rule.

## Notes

- **A ceiling, not a floor of colour.** The alternative — inventing a hue for a colourless app — is
  worse: any hue we picked would be a claim about the app that its own artwork does not make. Making
  the tile a *darker neutral* says only "this is a surface", which is true.
- **Why not simply darken `tileBase`.** That would move every light tile, including the ones tuned
  against the reference. The ceiling touches only the tiles that are actually too light.
- **Scaled in linear light**, not on the sRGB components, so the result lands on the luminance asked
  for rather than near it.

# Design — light-theme icons must separate from the surface they sit on

## Context

Now that a pale wallpaper puts the whole shell in its light theme
(`wallpaper-theme-alignment.md`), the icon treatment is visible there for the first time, and
several icons all but disappear. Measured on the Start menu, whose panel surface is luminance 250:

| tile | Calendar | Camera | Chrome | Clock | Contacts |
|---|---|---|---|---|---|
| tile luminance | 226 | 236 | 222 | 241 | 219 |
| separation from the panel | −24 | −14 | −28 | **−9** | −31 |

Clock separates by 9 of 255 and Camera by 14. A vertical profile straight through the Clock tile
runs `250 250 250 … 249 248 247 242 241 …` and back to `250` — the tile is a barely-there dip in a
flat field, and **there is no shadow band below it at all**.

The same profile through `ref-img/icons3.png`, the design this style is drawn from, is a different
picture. Its ground is 246; its white tiles read **255** — *brighter* than the ground — and below
each tile is a clear shadow band at about **220**. The reference separates a white tile from a pale
ground the way a physical object does: it is lighter than its surroundings and it casts a shadow.

Ours does neither. `SoftClay.shadow` is `0x3D000000` — 24% black — blurred over 9% of the icon, and
at that strength it does not survive as a visible band. The dark style gets away with the same
weakness because a near-black tile on a dark panel is separated by its bright rim instead; a white
tile has no such edge, and its white rim is invisible against white.

This is a separation problem, not a colour problem. The tile colours are tuned against the reference
and are not reopened here.

## Requirement

1. **Every light-theme tile is discernible from the surface behind it**, whatever the app's own
   colour — including an app whose icon is white.
2. **Separation comes from the shadow**, as it does in the reference, not from darkening the tiles.
   `tileBase` and `tileTint` keep their tuned values (`icon-gloss.md`).
3. **The shadow band below a tile is comparable to the reference's** — of the order of 20 or more
   luminance below the surrounding surface, rather than the 0–3 measured today.
4. **The dark theme is untouched.** Its tiles separate by a bright rim on a dark ground and measure
   correctly already; this changes the light style only.
5. **Within the software-rendering budget** (SRS §4.3): the shadow is already composited once into
   the cached bitmap and stays that way. No new layer, no per-frame cost.
6. **The cached artwork is invalidated**, or every existing install keeps the old flat tiles
   (`TREATMENT_VERSION`).

## Acceptance criteria

- [x] On the light theme, every tile separates from the panel by a visible shadow. Measured against
      the 250 panel, before → after: Calendar −24 → −29, Camera −14 → −19, Chrome −28 → −33,
      Clock −9 → −15, Contacts −31 → −35.
- [x] A profile below a light tile shows a band at least 20 below the surrounding surface: under the
      Clock tile it now reads 180, 206, 233 against a 250 panel — a −44 trough where before it ran
      213, 231, 247 and back to 250.
- [x] The dark theme measures as it does today — `DarkGlass` is untouched, and its test still
      asserts the tile stays under 150 luminance.
- [x] The icon set still reads as one design: `tileBase` and `tileTint` are unchanged, so no tile
      moved colour.
- [x] `./gradlew test lint assembleDebug` green; checked on device.

## Residual, and not fixed here

The Clock tile is still the palest of the set, and the reason is its own artwork rather than the
treatment: Android's stock Clock is a white face with thin hands, so a white glyph lands on a white
tile. The reference gets away with the same white tile because its Clock is drawn with a grey
outline. A launcher cannot add contrast inside someone else's artwork; what it can do is stop
compounding it, which would mean tinting the tile when the app's own colour is near-white. That is a
change to `tileTint`'s rule rather than to separation, and `icon-gloss.md` warns that moving it is
how every tile ended up a different mid-grey. Left alone deliberately, and recorded here.

## Notes

- **Why not darken the tiles instead.** The reference's white tiles are *brighter* than its ground,
  not darker. Darkening ours to force separation would make the light set contradict the design it
  is drawn from, and would flatten the distinction between the pale tiles and the saturated ones.
- **Why the dark style does not need this.** Its rim is `0xE6FFFFFF` against a `0x0F1116` tile: the
  lit edge does the separating. On white clay the rim is white-on-white and contributes nothing,
  which is why the shadow has to.

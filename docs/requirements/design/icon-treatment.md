# Design — Icon Treatment

Status: **Accepted · Implemented** (2026-08-20)

Makes every app icon read as one designed set: a squircle tile with gloss, rim light and shadow,
in dark-glass or soft-clay depending on the theme. Reference: `ref-img/icons2.png` (dark) and
`ref-img/icons3.png` (light). Derives from SRS §6.1 (principles 2, 5, 6) and §4.3 (software
rendering). Depends on [`../launch/app-inventory.md`](../launch/app-inventory.md).

## Context

Android app icons are a mess by construction: every app ships its own artwork at its own visual
weight, some adaptive, some a flat legacy bitmap, some already a circle, some a full-bleed square.
Dropped straight into a dock they look like a ransom note. Every desktop that reads as designed —
macOS, Windows 11, ChromeOS — imposes a **consistent silhouette** on top of whatever the app
provides, and that is what the reference images are.

pclauncher cannot change the artwork an app ships. What it can do is take the layers the app
*does* expose and re-composite them: mask the foreground to a squircle, put it on a tile we
generate, and light the result consistently. For an `AdaptiveIconDrawable` — foreground and
background as separate layers, which is most modern apps — this is a genuine restyle. For a legacy
single-layer icon there is nothing to separate, so the best available answer is to centre it on a
tile coloured from the icon itself.

**The two reference styles are one design at two polarities.** `icons2.png` is near-black glass with
a coloured rim; `icons3.png` is white/pastel clay with a soft shadow. Same silhouette, same glyph
placement, same construction — inverted lighting. Building them as one parameterised treatment is
both less code and the only way SRS §6.1 principle 6 ("light and dark both first-class") survives
contact with the icon set.

**This has to be free at draw time.** SRS §4.3: `pc_x86_64` composites on the CPU. A per-frame
gradient, blur and shadow per icon across a full app grid is not affordable. The treatment is
therefore applied **once, when an icon is loaded**, and the composited result is what the cache
stores — so drawing an icon stays a single bitmap blit no matter how elaborate the treatment is.

## Requirement

When this is done, the following must be true:

1. **A squircle, not a rounded rectangle.** The mask is a superellipse (continuous curvature), which
   is what makes the reference tiles read as iOS/macOS-modern rather than as Android's rounded
   square. The exponent and corner ratio are design tokens, not literals.
2. **One treatment, two palettes.** A single pipeline parameterised by an `IconStyle` carrying tile
   fill, gloss, rim and shadow. `DarkGlass` and `SoftClay` are the two shipped instances, selected
   by the theme. Adding a third must not touch the pipeline.
3. **Adaptive icons are genuinely re-composited.** Foreground and background layers are separated;
   the foreground is scaled and inset per token onto our tile, and the app's own background layer
   is used to derive the tile colour rather than being drawn as-is.
4. **Legacy icons degrade honestly.** A single-layer icon is centred at a smaller inset on a tile
   whose colour is **sampled from the icon** (dominant non-transparent colour). It must never be
   stretched to fill, and must never be mistaken for having been properly re-composited.
5. **Monochrome icons are honoured where present.** An adaptive icon exposing a monochrome layer
   (API 33+) may be tinted to the accent, so a themed-icon look is available without a second
   pipeline.
6. **Lighting is consistent and directional.** One light source, top-left, across every icon: a
   gloss highlight on the tile, a rim along the lit edges, and a drop shadow on the opposite side.
   Inconsistent lighting is what makes a mixed icon set look wrong even when every icon is masked
   the same.
7. **Baked once, never per frame.** The treatment runs inside the icon loader; the cache stores the
   finished bitmap. Drawing is one blit. No `RenderEffect`, no runtime shadow layer, no gradient
   recomputed during scroll or dock magnification.
8. **The cache key covers the treatment.** `IconCacheKey` gains the style and a **treatment
   version**, so switching theme does not serve dark tiles on a light desktop, and changing the
   pipeline invalidates every cached icon instead of leaving a mix of old and new on screen.
9. **Pure and testable.** Squircle geometry, inset maths, dominant-colour extraction, and style
   selection are pure functions with no `Canvas` in their signatures, tested without a device.
10. **It can be turned off.** A setting renders the app's icon untouched. Any user whose icon pack
    or accessibility need conflicts with the treatment gets out with one toggle.

## Acceptance criteria

- [x] Squircle path is a superellipse driven by `SQUIRCLE_EXPONENT`; every sampled point satisfies
      |x|^n + |y|^n = 1 (test), and the corner is provably fuller than a circle's.
- [x] `IconStyle` parameterises fill, gloss, rim, shadow and insets; `DarkGlass` and `SoftClay` ship
      and are chosen by theme.
- [x] An adaptive icon is split into foreground/background and re-composited onto our tile, with
      the foreground **overscaled 1.5×** to recover the optical size the 108/72 safe zone hides.
- [x] A legacy icon is centred at the larger legacy inset and never stretched.
- [x] Dominant-colour extraction ignores transparent pixels, weights by saturation, and falls back
      on an all-transparent input (tests, including greyscale and near-transparent cases).
- [ ] A monochrome layer can be tinted to the accent. **Not implemented** — see Notes.
- [x] The composited bitmap is produced in the loader and cached; drawing is one blit and dock
      magnification is a graphics-layer scale over it.
- [x] `IconCacheKey` includes style id and treatment version; both feed `diskName()`, so a theme
      switch or a pipeline change cannot serve a stale tile (tests).
- [x] Geometry, insets, colour extraction, content detection and style selection are pure and
      unit-tested without a device.
- [x] The treatment can be disabled — an unrecognised style id (`"none"`) returns the app's own
      icon untouched.
- [x] `./gradlew test` green (124 tests project-wide); `./gradlew lint` clean.

## Notes

- **Depends-on / blocks:** depends on `launch/app-inventory.md` (the icon cache it plugs into).
  Blocks nothing, but every surface that draws an icon — dock, Start, palette, desktop grid,
  taskbar — inherits it for free, which is the point of doing it in the loader.
- **Why the loader and not a Compose modifier:** a modifier would re-run the whole treatment on
  every recomposition and every dock magnification frame. On SwiftShader that is the difference
  between a dock that animates and one that does not.
- **Cost of the bake:** the treatment runs once per (component, density, style) and lands in the
  disk tier, so it is paid on first sight of an app and never again — including across restarts.
- **The honest limit:** a legacy icon that is a full-bleed square with its own baked-in corner
  radius will look like a picture of an icon sitting on our tile, because that is what it is. No
  amount of masking recovers layers the app never shipped. The opt-out in requirement 10 exists
  partly for this.
- **Monochrome tinting is deferred.** Requirement 5 is the one thing here that did not land: it
  needs an accent to tint *to*, and the accent is a setting that does not exist until the settings
  store does. Nothing else depends on it, so it waits rather than being guessed at.
- **An empty adaptive foreground falls back to the whole icon.** Some adaptive icons render nothing
  from the foreground layer alone — an empty `InsetDrawable`, or artwork that only paints once the
  parent has bounds. Drawing that produced a blank tile in the first build. The pipeline now checks
  for visible content at bake time and treats such icons as legacy: a correctly-drawn whole icon on
  a tile beats a blank one.
- **The tile is tinted from the foreground, not the background.** The first build sampled the
  background layer, which is usually one flat fill chosen to sit behind a mask; averaging it gave
  muddy mid-greys across the whole dock. The colour a person associates with an app lives in its
  logo, so the foreground is what tints the tile.
- **Not in this slice:** icon packs, user-supplied icon overrides, per-app tile colour choice,
  animated icons, badge/notification-dot rendering.

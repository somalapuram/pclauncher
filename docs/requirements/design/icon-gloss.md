# Design — Making the Gloss Actually Read

Status: **Implemented** (2026-08-24)

The icon treatment produces matte tiles in the light theme and a sheen the artwork hides in both.
Amends [`icon-treatment.md`](icon-treatment.md), which specified the layers but not that the result
has to read as glossy at the size it is drawn.

## Context

On a Pixel Tablet in the light theme the desktop and taskbar tiles are near-white and flat. Measured
against the taskbar's own flat background, the luminance falls by **4 units out of 255** across the
top half of a tile — the shading is, for practical purposes, absent. `ref-img/icons3.png` — the
reference this style was built from — shows a pronounced bevel, a bright top-left specular, and
tiles that carry real pastel colour from the app.

Three separate causes, and only one of them is a value being too low:

**The sheen is painted under the glyph.** `IconCompositor` draws gloss and specular, then draws the
glyph on top. So the highlight survives only in the margin around the artwork, which on a
well-filled adaptive icon is almost nothing. In the reference the sheen passes *over* the artwork —
that is what makes it read as glass covering the icon rather than a gradient behind it. This is
structural: no amount of alpha fixes a layer that is painted over.

**Clay was tuned to be matte on purpose, and the purpose was wrong.** `SoftClay` carries a comment
saying a hard specular would "turn the light set plastic". Comparing against the reference, plastic
is exactly what it is — glossy, saturated, dimensional. The tile tint is also low enough (0.26) that
every tile lands on near-white, so the set reads as a grid of identical white squares, which is the
thing the tint was supposed to prevent.

**Nothing draws the underside.** The reference tiles are pillowed: light along the top edge, and a
soft *darkening* along the bottom inside the shape. The compositor has a drop shadow beneath the
tile but no inner shade within it, so the tile has a lit edge and no body.

**A cached bitmap outlives a style change.** The treatment is baked once and cached on disk, keyed
in part by a treatment version. Changing the style without bumping that version serves every
existing user the old flat tile forever.

**The bar is flat for the same reason the tiles are.** It is a single scrim colour with a hairline.
That is correct for cost (SRS §4.3 rules out blur by default) but it means the one surface framing
every glossy icon has no dimension itself. A single linear gradient is affordable where a blur is
not.

## Requirement

1. **A glaze layer is drawn over the glyph**, clipped to the tile, so the highlight sits above the
   artwork rather than behind it.
2. **The tile has an underside.** A soft inner darkening along the bottom edge, inside the shape.
3. **A tile's shading is measurable.** Top-to-bottom luminance across a tile differs enough to read
   as a curved surface rather than a flat fill, in both styles.
4. **Light tiles carry the app's colour.** The light style's tint is raised until tiles read as
   pastel rather than as white squares, while staying one coherent set.
5. **The dark style stays coherent.** It gets the same two new layers, tuned for near-black glass;
   it must not become washed out.
6. **The treatment version is bumped**, so every cached bitmap is rebaked rather than served flat.
7. **The bar gets a sheen** — one cheap linear gradient, no blur — so the surface framing the icons
   has dimension too.
8. **Cost does not grow per frame.** Both new layers are baked into the cached bitmap exactly like
   the existing ones; the bar's sheen is one gradient in a surface that already draws a background.
9. **The new parameters are style data, not compositor constants**, so a third style stays
   expressible without touching the compositor (`icon-treatment.md`'s original rule).

## Acceptance criteria

Measured in `IconGlossTest` over the composited bitmap; device checks on the `Pixel_Tablet` AVD
(Android 17, 2560×1600 @ 320 dpi) in both themes.

- [x] A composited tile's top half is measurably brighter than its bottom half in both styles —
      asserted at >35 for clay and >20 for glass, against a flat fill's 0.
- [x] That margin survives an opaque, tile-filling glyph (>35).
- [x] Removing the glaze makes the assertion fail (spread drops under 8).
- [x] The light style's tiles take visible colour from the app — two dominant colours produce
      measurably different tiles.
- [x] The dark style remains dark: mid-tile luminance stays under 150.
- [x] `IconStyle.TREATMENT_VERSION` raised 3 → 5, so every cached bitmap is rebaked. (4 was the
      first calibration; 5 is the stronger one asked for after seeing it on device.)
- [x] The bar draws a vertical sheen (`surfaceSheen`, 3 tests).
- [x] On device the tiles read as glossy in both themes: the sheen now crosses the artwork, the
      underside is visible, and light tiles are pastel rather than white.
- [x] `./gradlew test lint assembleDebug` green.

**The numbers that made the diagnosis.** With the highlight painted under the glyph, an *opaque*
gloss at full strength lifted a covered tile from 90 to 98 of 255. The identical colour applied as
a glaze above the glyph lifted it to 184. The layer's position, not its alpha, was the whole
problem.

## Notes

- **Why measure rather than eyeball.** "Looks glossy" is not checkable and drifted once already —
  the style was tuned by eye against a reference and ended up flat on a real device at a real
  density. A luminance-difference assertion over the composited bitmap is the only form of this
  that a test can hold.
- **Why not simply raise the alphas.** That was the first instinct and it would not have worked:
  with the sheen painted under the glyph, raising its alpha brightens the margin and leaves the
  centre of every well-filled icon exactly as flat as before.
- **The stop is a fraction of the bitmap, not of the tile.** The tile is inset inside the bitmap to
  leave room for the shadow and the glow, so a diagonal gradient with a stop under about 0.5 expires
  before it reaches the tile at all. The first calibration used 0.46 and produced no sheen
  whatsoever, which read exactly like the bug being fixed.
- **The rim is the one lit edge a glyph cannot cover**, since it is drawn at the tile's boundary and
  the glyph is inset. On a light tile, where white-on-near-white does little, it does more work than
  the gloss does.
- **Tiles were also invisible against the bar.** At the old tint every tile landed on near-white and
  the taskbar is light grey, so the tile edge vanished and only the artwork read. Raising the tint
  fixed the silhouette as much as the shading did.
- **Calibrated twice, on device.** The first pass cleared the thresholds and still read as
  understated at 320 dpi, so glaze, rim, specular and the underside were all pushed again and the
  test thresholds raised with them — a threshold left at the old value would have let the stronger
  look silently regress.
- **Not in this slice:** per-app style overrides, a themed/monochrome icon path, animated
  specular that tracks the pointer, or restyling the Start menu and popovers to match.

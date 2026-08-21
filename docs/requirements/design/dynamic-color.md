# Design — Dynamic Colour

Status: **Accepted · Implemented** (2026-08-20)

Make the shell adopt Android's system colours (Material You) instead of a hardcoded palette.
Implements the SRS §13 default — *"accent from wallpaper (Material You) with a manual override"* —
which `foundation/scaffolding.md` shipped as a fixed palette placeholder. Extends
[`icon-treatment.md`](icon-treatment.md), which reads the same tokens.

## Context

`core/design/Color.kt` currently hardcodes two palettes and a fixed accent (`#2F6FED`). That was
right for phase 1 — the tokens had to exist before anything could draw — but it means the shell
looks like a stranger on the user's own device. Every other Android surface picks up the system's
generated colours; a launcher that does not is the one thing on screen that ignores the wallpaper
it is sitting on.

**`minSdk` is 31, which makes this unusually clean.** Dynamic colour arrived in Android 12, so
there is no version gate, no fallback branch for older devices, and no support library — the API is
simply available. That was a consequence of the `minSdk` chosen for freeform windowing, and it pays
off here.

**It must stay overridable.** SRS §13 says "with a manual override", and there are real reasons to
want one: a user whose wallpaper generates a palette they dislike, and the `pc_x86_64` bring-up
device, where the wallpaper is whatever the ROM shipped and the generated accent may be arbitrary.
So dynamic is the *default*, not the only option.

**The shell's tokens stay the shell's tokens.** Nothing outside `core:design` should learn about
Material 3 colour roles. The mapping from a system `ColorScheme` to `PcColors` happens in one place,
so every surface keeps reading `LocalPcColors` and none of them care where the values came from.

## Requirement

1. **The palette derives from the system's dynamic colour scheme** when available and enabled,
   following light/dark as it does today.
2. **The mapping is one pure function** from a Material 3 `ColorScheme` to `PcColors`. No surface
   outside `core:design` references a Material colour role.
3. **The static palettes remain** as the override and as the fallback, unchanged in appearance.
4. **A single switch** chooses between dynamic and static, defaulting to dynamic.
5. **Contrast survives the swap.** The mapping must pick roles that keep text legible on the shell's
   translucent surfaces in both themes — foreground roles paired with the surface they sit on, not
   assembled from unrelated roles.
6. **The accent flows everywhere it already does** — Start's open state, the focused-window rim,
   running indicators, selection — with no per-surface change.
7. **Pure and tested:** the mapping and the dynamic/static decision are tested without a device.

## Acceptance criteria

- [x] `PcTheme` uses the system dynamic scheme by default, honouring light/dark.
- [x] One pure `pcColorsFrom(ColorScheme)` performs the mapping; no Material role names appear
      outside `core:design`.
- [x] The static palettes still exist unchanged and are asserted so by a test.
- [x] A `dynamicColor` switch selects between them and defaults to true.
- [x] Foreground/background pairs come from matching roles (`surface`/`onSurface`,
      `onSurfaceVariant` for muted, `primary`/`onPrimary`), tested.
- [x] No surface outside `core:design` changed to accommodate this.
- [x] Mapping and fallback are unit-tested.
- [x] `./gradlew test` green; `./gradlew lint` clean. Verified on device in both themes: the
      surface reads `(250,248,255)` light and `(18,19,24)` dark, neither of which is the static
      palette — the system scheme is genuinely in use.

## Notes

- **Why map rather than adopt `MaterialTheme` wholesale:** the shell is deliberately *not* a
  Material app — SRS §3 says Material 3 is "a base only, restyled per §6", and §6 specifies a
  macOS/Windows hybrid. Taking the system's *colours* while keeping our own shape, spacing and
  motion is the point. Adopting `MaterialTheme` outright would drag component styling with it.
- **The icon treatment is unaffected.** Tile colour comes from each app's own artwork
  (`icon-treatment.md`), which is what makes a set of icons recognisable; the system accent has no
  business overriding it. The one place the accent will matter is monochrome-icon tinting, which
  that doc left deferred for exactly this reason — there was no accent to tint to.
- **Not in this slice:** a colour picker in Settings, per-surface accent overrides, contrast
  themes, or extracting a palette from a specific wallpaper region.

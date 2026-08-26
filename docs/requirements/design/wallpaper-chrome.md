# Design — Chrome That Belongs to the Wallpaper

Status: **Implemented** (2026-08-24)

The bar, the Start menu and the popovers take their light/dark polarity from the wallpaper rather
than from the system theme setting. Amends [`dynamic-color.md`](dynamic-color.md), which took the
*hues* from the wallpaper and left the polarity to the system.

## Context

On a dark wallpaper in the light system theme the shell renders as bright near-white slabs: a heavy
bar across the bottom and hard-edged panels floating over a near-black desktop. SRS §6.1 asks for
chrome that is "thin and quiet" and surfaces that are "layered and translucent"; what is there is
the loudest thing on screen.

**The system theme is answering the wrong question.** `isSystemInDarkTheme()` says what the *user's
apps* should look like. The shell does not sit on a page of its own — it sits directly on the
wallpaper, and what it needs to know is what is *behind* it. Those two answers coincide often enough
that the mistake is easy to make and obvious once seen: a light theme with a dark wallpaper is an
ordinary configuration, and it is exactly where the shell looks worst.

**The wallpaper already says.** `WallpaperManager.getWallpaperColors` needs no permission, returns
the wallpaper's dominant colours, and carries `HINT_SUPPORTS_DARK_TEXT` — the system's own judgement
about whether dark text is legible on it. That hint is precisely the question the chrome has to
answer, computed by the platform from the actual pixels.

**Hues are already wallpaper-derived; polarity is not.** Material You feeds `pcColorsFrom`, so the
tints already come from the wallpaper. Only the light-or-dark decision is taken from elsewhere,
which is why the surfaces are the right *colour* and the wrong *weight*.

**It has to survive a wallpaper that says nothing.** Live wallpapers, a wallpaper still loading, and
a device that returns no colours at all are all ordinary. Each falls back to the system theme, which
is what the shell does today — so the failure mode is the current behaviour, not a broken one
(GATE 4).

## Requirement

1. **Chrome polarity follows the wallpaper.** A dark wallpaper gives dark chrome with light text; a
   light wallpaper gives light chrome with dark text.
2. **The platform's own hint decides it** where one is available, rather than a luminance threshold
   of ours.
3. **Where no hint is available, the wallpaper's dominant colour decides**, by luminance.
4. **Where neither is available, the system theme decides** — today's behaviour, unchanged.
5. **Changing the wallpaper updates the chrome** without restarting the shell.
6. **The icon treatment follows the same polarity**, so glass icons sit on dark chrome and clay on
   light — the set and the surfaces cannot disagree.
7. **The decision is a pure function** of the hint, the dominant colour and the system setting.
8. **Reading the wallpaper never blocks the first frame** and never takes the desktop down.

## Acceptance criteria

- [x] On a dark wallpaper in the **light** system theme, the bar and panels are dark with light
      text — which is the configuration that proves the wallpaper outranks the system setting.
- [ ] On a light wallpaper, they are light with dark text. **Not verified on device:** the emulator
      has one wallpaper and adb offers no way to set another. The branch is unit-tested.
- [ ] Changing the wallpaper flips the chrome without a restart — same limitation; the listener is
      registered and disposed, but the event cannot be triggered here.
- [x] With no wallpaper colours available the shell renders as before: the decision falls through to
      the system setting, which was the previous behaviour exactly.
- [x] Desktop icons use the treatment matching the chrome — dark glass appeared with the dark
      chrome, where the system theme alone would have kept clay.
- [x] Text stays legible in the dark polarity, checked on the bar, Start menu and popover.
- [x] The polarity decision is unit-tested across hint present, hint absent with light and dark
      dominant colours, mid-grey, and nothing at all.
- [x] `./gradlew test lint assembleDebug` green.

**A second fault surfaced on the way.** `PcTheme` never wrapped `MaterialTheme` — it provided only
its own tokens. Material's components read `MaterialTheme` and nothing else, so every dropdown menu
and the volume slider kept the default light scheme. Invisible while the shell was also light; the
moment polarity followed the wallpaper, a dark shell was serving light menus. The theme now wraps
Material with the same scheme it derives its own tokens from.

## Notes

- **Why the hint rather than our own luminance test.** The platform computes it from the wallpaper's
  actual pixels, including the regions text will sit on. A single dominant colour cannot see that a
  mostly-light wallpaper is dark exactly where the bar goes.
- **Not in this slice:** per-surface polarity (a dark bar with a light Start menu), sampling the
  wallpaper region beneath each surface separately, or a user override — that belongs with the
  Settings appearance page (SRS §7.5).

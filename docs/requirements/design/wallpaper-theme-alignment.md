# Design — the whole shell follows one theme, taken from the wallpaper

> Supersedes the preference order in [`wallpaper-chrome.md`](wallpaper-chrome.md). That doc's
> premise — the chrome follows the wallpaper, not the system theme — stands unchanged; only which
> wallpaper signal decides is replaced.

## Context

Asked for directly: *"when the theam is changed from dar to light or litght ot dark, all text and
start meun and task bar need to aligh to the theams."*

Found first as a legibility failure. With a pale wallpaper set, every desktop icon label was drawn
white on a light ground. Measured screen luminance directly behind each label band (0–255):

| label row | Calendar | Camera | Chrome | Clock | Contacts | Drive |
|---|---|---|---|---|---|---|
| behind the text | 180 | 185 | 187 | 201 | 203 | 208 |

White glyphs on that give roughly **1.3:1**. SRS §12 requires **4.5:1** — missed by a factor of
three. The taskbar and Start menu stayed dark on the same pale wallpaper.

**Why.** `chromeIsDark` prefers `WallpaperColors.HINT_SUPPORTS_DARK_TEXT`, and for this wallpaper
the platform reports `hints=4` — `HINT_FROM_BITMAP` alone, no dark-text support. The colours the
same call returns are `#b88545`, `#cdb68b` and `#f0f1f2`: every one of them light. The hint is a
single judgement over the whole bitmap, and this wallpaper has a dark tan arch through its middle
that neither the labels nor the bar sit on.

`wallpaper-chrome.md` preferred the hint because it "sees the whole wallpaper including the regions
text will sit on, which a single dominant colour cannot". That is a good argument and this is a
measured counter-example: the hint spoke for the darker middle while every region the shell occupies
was pale. The colours are the better signal precisely because there is more than one of them.

Everything downstream already follows one theme — labels take `colors.onSurface`, the bar and menus
take their surface from the same scheme — so this is one decision, not four.

## Requirement

1. **One polarity for the whole shell.** The taskbar, Start menu, tray, menus and desktop labels are
   all light, or all dark, together. There is no state where the labels disagree with the bar.
2. **The wallpaper's reported colours decide it**, by mean luminance across every colour the
   platform returns — not by the dark-text hint.
3. **The hint remains the fallback**, for a wallpaper that reports a hint but no colours.
4. **The system theme remains the last resort**, for a wallpaper that reports nothing at all — so a
   wallpaper that says nothing changes nothing (GATE 4).
5. **A wallpaper change re-themes everything live**, with no restart, as it does today.
6. **Text meets SRS §12's 4.5:1** against the ground it is drawn on, on both wallpapers.
7. **No new per-label surface.** `visual-pass.md` removed the per-label pill deliberately; the
   shadow stays as the safety net for a wallpaper that is pale under one icon and dark under
   another, and is drawn in the opposite polarity to the text.

## Acceptance criteria

- [x] On the pale wallpaper: bar, Start menu and labels are all light-themed, and labels are dark.
      Measured glyph-against-ground contrast per label row, before and after:

      | row | Calendar | Camera | Chrome | Clock | Contacts | Drive |
      |---|---|---|---|---|---|---|
      | before (white on pale) | 1.70 | 1.54 | 1.56 | 1.27 | 1.24 | 1.19 |
      | after (dark on pale) | 7.3 | 7.7 | 8.1 | 9.0 | 9.3 | 9.6 |

- [x] On the dark wallpaper: all three are dark-themed and labels are light — 6.0:1 on the row over
      the wallpaper's pale corner, 15.2:1 on the rest.
- [x] Never a mix — the polarity is one value read by every surface, so a mix is not representable.
- [x] Changing the wallpaper flips all of it live, without a restart: applied both wallpapers
      through the system picker with the shell running, pid unchanged across both changes.
- [x] A wallpaper reporting a hint but no colours still follows the hint (unit test).
- [x] A wallpaper reporting nothing still follows the system theme (unit test).
- [x] `./gradlew test lint assembleDebug` green; checked on device on both wallpapers.

## Notes

- **Mean, not dominant.** The dominant colour is the *most common* one, which on a wallpaper with a
  large mid-tone feature is not the colour the shell sits on. Averaging the reported colours is a
  cheap way to stop one big feature speaking for the whole picture.
- **Still one global answer.** This does not sample the wallpaper per label or per bar segment —
  reading wallpaper pixels needs a permission the shell deliberately does not take
  (`WallpaperTone.kt`). It replaces a signal that is wrong for this whole wallpaper with one that is
  right for it, and leaves the shadow to cover what a single answer cannot.

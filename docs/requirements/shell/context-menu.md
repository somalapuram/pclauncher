# Shell — The Context Menu, Steady and In Keeping

Status: **Implemented** (2026-08-24)

Opening a context menu disturbs nothing, and the menu looks like part of this shell rather than a
stock Material component. Amends [`../desktop/placement-timing.md`](../desktop/placement-timing.md)
and [`../design/visual-pass.md`](../design/visual-pass.md).

## Context

**The desktop still flickers when a menu opens**, though it no longer rearranges. That is a fault I
introduced fixing the rearrangement: auto-placement is now skipped while the row count is unknown,
and an icon with no cell is skipped rather than drawn. So where the grid previously *reflowed* on a
transient re-measure, it now *blanks* — the icons vanish for a frame and come back. Both symptoms
have the same cause: a momentary measurement is being treated as new information about the grid.

A height of zero is not a measurement. It is the absence of one, and the arrangement that was
already correct should survive it untouched.

**The menus are stock Material.** They now follow the shell's polarity, since the theme wraps
`MaterialTheme`, but they are still a default container with default corners and no relationship to
the bar and panels beside them. Everything else the shell floats over the wallpaper — the bar, the
Start menu, the quick-settings popover — shares one treatment: a surface, a hairline, rounded
corners and a shadow. The menus are the only surfaces that do not.

**There are five of them** — the desktop's own menu, an icon's, a widget's, the Start menu's pin
menu and the power menu — and styling each at its call site is how they drift apart.

## Requirement

1. **A transient measurement does not change the arrangement.** Once the grid's height is known, a
   zero or missing measurement leaves it as it was.
2. **Opening or closing a context menu changes nothing else on screen** — no reflow, no blink.
3. **Every context menu shares one surface treatment** with the bar and the panels: the shell's
   surface colour, a hairline, the popover corner radius and a shadow.
4. **It is defined once** and used by all five menus, so they cannot drift.
5. **Menu behaviour is unchanged** — same items, same actions, same dismissal.

## Acceptance criteria

Checked by pixel diff of the icon area, since the flicker is far shorter than a screen capture.

- [x] Opening the desktop context menu leaves the icon area untouched **during** the press —
      captured mid-press, the diff against the resting desktop reports no changed region at all.
- [x] And once the menu is open.
- [x] Icons do not disappear for a frame: a zero-sized measurement no longer replaces a known one.
- [x] All five menus render with the shell's surface, hairline, corners and shadow, from one
      definition.
- [x] Each menu's items and actions behave as before.
- [x] `./gradlew test lint assembleDebug` green.

**The flicker was mine.** Fixing the horizontal-then-vertical reflow made auto-placement wait for a
measurement and skip icons that have no cell — so a transient zero-height measurement stopped
reflowing the grid and started blanking it instead. Same cause, different symptom: a momentary
measurement was being treated as news. It is now ignored, because a height of zero is the absence of
a measurement rather than a smaller one.

## Notes

- **Why sticky rather than debounced.** A debounce still lets a wrong value through, just later. The
  row count only ever *becomes* known; treating zero as "no news" is both simpler and exactly true.
- **Not in this slice:** icons or separators in the menus, or a menu that animates open from the
  point it was summoned.

# Design — The Polish Pass

Status: **Implemented** (2026-08-24)

Three things that make the shell look unfinished: labels in boxes, panels with no depth, and a
Start menu that is cramped where it should breathe. Follows
[`wallpaper-chrome.md`](wallpaper-chrome.md), which fixed the weight of the chrome and left these.

## Context

**Every desktop label sits in a grey pill.** Seventeen opaque rounded rectangles of ragged widths,
one per icon. The code explains why: "a readable label over an arbitrary wallpaper needs its own
ground; a text shadow would cost a layer per icon on a CPU renderer." The first half is right and
the second is the wrong trade. Now that chrome polarity follows the wallpaper the label *colour* is
already correct for the wallpaper overall — but a wallpaper is not one colour, and this one is dark
on the right and pale blue at the top left, which is precisely where two of the pills are still
visible as boxes. A shadow costs a blur on static text that nothing animates; the pills cost the
desktop's appearance all of the time.

**Panels have no depth.** The Start menu and the quick-settings popover meet the wallpaper at a hard
1 px edge. Nothing says they are above the desktop rather than cut into it, and on a busy wallpaper
the eye has to work out the boundary from colour alone.

**The Start menu is cramped where it counts.** Its disabled footer controls wrap to three lines,
"All apps" is jammed against the search field, and the grid's rows are loose while the footer is
tight — the opposite of the emphasis it should have.

## Requirement

1. **Desktop labels have no box.** They stay legible over any wallpaper, light or dark, by their own
   contrast rather than by a rectangle behind them.
2. **The label treatment follows the chrome's polarity**, so it is correct on both.
3. **Floating panels read as floating** — the Start menu and the popover carry a soft shadow rather
   than meeting the wallpaper at a hard edge.
4. **Shadows stay within the software-rendering budget** (SRS §4.3): one per panel, no per-frame
   blur of anything large, nothing stacked.
5. **The Start menu's spacing is deliberate**: the section label reads as a heading, the grid has
   room, and the footer is not crushed.
6. **No footer control wraps to three lines.**
7. **Nothing changes behaviourally** — every gesture, action and target size is as it was.

## Acceptance criteria

- [x] Desktop labels show no rectangle anywhere on the wallpaper.
- [x] Labels are legible over both the pale top-left and the near-black right of the same wallpaper.
- [x] The Start menu and the quick-settings popover cast a shadow and read as above the desktop.
- [x] No footer control wraps: the four text buttons became two icon buttons, and the power actions
      moved behind one of them where a menu row has space for its reason.
- [x] The section label reads as a heading, with room above it and the grid beneath.
- [x] Icons, drag, hover, menus and the footer's actions behave as before — the power menu still
      offers Restart shell enabled and the two privileged actions disabled with their reason.
- [x] `./gradlew test lint assembleDebug` green.

**The footer was rebuilt rather than tightened.** Shortening the labels would have kept a row of
four text buttons each carrying its own unavailability note, which is a form, not furniture.
Grouping the power actions behind one button is what Windows 11 does, and it is also what makes the
disabled ones explicable: an icon has no room for a reason and a menu row has plenty.

## Notes

- **Why the shadow is affordable now and was not before.** The judgement was made when the label was
  a `Text` on a wallpaper and the cost was weighed against a rectangle that was free. It is a blur
  on twenty short, static strings; the alternative is visible on every frame regardless. If it ever
  shows up in a trace on the target device, the honest fix is a cheaper shadow, not a box.
- **Not in this slice:** a label that hides on hover, two-line eliding rules, or per-icon label
  colour sampled from the wallpaper beneath it.

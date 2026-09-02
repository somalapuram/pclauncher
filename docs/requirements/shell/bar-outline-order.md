# Shell — The Bar's Outline Goes Behind Its Icons

Status: **Implemented, overlap unverified on device** (2026-08-24)

A magnified dock icon rises above the bar without the bar's edge drawn across it. Amends
[`dock-magnification-fixes.md`](dock-magnification-fixes.md), which stopped the bar resizing and
let the icons overflow it instead.

## Context

Dock icons magnify by rising above the bar — that was the point of fixing the bar's height, and it
is what a dock does. But the bar's top edge is drawn *over* the icon that rises through it, so the
enlarged icon has a hairline running across it.

**`Modifier.border` draws after content.** Compose's implementation is explicit: `drawContent()`
first, then the border path. So the outline is painted on top of every child, including one that
extends beyond the bar's bounds. Nothing about the modifier order at the call site changes that —
the border modifier always draws last within its node.

**Only the bar has content that leaves it.** Every other surface's children stay inside, so their
borders are unaffected and should stay as they are; this is not a reason to change how the shell
draws edges generally.

## Requirement

1. **The bar's outline is drawn behind its children**, so anything overflowing the bar is drawn over
   the edge rather than under it.
2. **The outline looks the same at rest** — same colour, weight, corner radius and inset.
3. **The drop-target state still reads**: the accent outline that appears while a drag hovers the
   bar keeps its weight and colour.
4. **Other surfaces are unchanged.**

## Acceptance criteria

- [ ] A magnified dock icon shows no line across it. **Not verified on device:** magnification needs
      a pointer hover and `input motionevent` throws on this system image, so the overlapping state
      cannot be produced from adb.
- [x] The bar at rest is visually identical — same outline, weight, corners and inset.
- [x] The drop-target state keeps its heavier accent outline; the branch is unchanged, only where it
      is drawn.
- [x] Other surfaces are untouched.
- [x] `./gradlew test lint assembleDebug` green.

**The cause is from Compose's source, not inference.** `Modifier.border` is implemented as
`drawContent()` followed by drawing the outline, so it always paints on top of the node's children —
no ordering at the call site can change it. The outline is now a `drawBehind` stroke, which runs
before the content.

## Notes

- **Not verified on device.** Magnification needs a pointer hover, and `input motionevent` throws on
  this system image, so the overlapping state cannot be produced from adb. The cause is confirmed
  from Compose's source rather than inferred, and the fix is a draw-order change with nothing else
  in it — but the visual confirmation is the user's.
- **Not in this slice:** clipping the bar's children, or a shadow under a magnified icon.

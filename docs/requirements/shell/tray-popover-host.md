# Shell — the quick-settings panel is placed by the host, like the Start menu

> Supersedes the *mechanism* in [`tray-popover-placement.md`](tray-popover-placement.md). Its two
> placement rules — right-aligned at the bar's own margin, one gap clear above the bar — are kept
> exactly; only what computes them changes.

## Context

Reported on the device: the tray is *"not aligned like start menu"*.

The Start menu is drawn by the host — the home activity's full-screen surface, or the overlay's
full-screen menu window — and positioned by alignment within it: bottom-start, inset by the bar's
margin and clear of the bar's height. The quick-settings panel is not. It is a Compose `Popup`
created *inside* the bar, positioned by `TrayPopoverPosition` against the window the bar is in.

That worked while the bar sat in a full-screen window. `overlay-window-split.md` made the bar a
window of its own, about 168 px tall, and the arithmetic collapsed:

```
verticalPosition(anchorTop, popupHeight, gap) = (anchorTop - popupHeight - gap).coerceAtLeast(0)
```

In the bar window the tray's `anchorTop` is around 20, and the panel is several hundred px tall, so
the expression is deeply negative and clamps to **0** — the top of the bar window. The panel is
pinned onto the bar instead of sitting above it. The clamp is doing exactly what it was written to
do; the assumption it rests on — that there is room above the anchor inside this window — stopped
being true.

Anchoring the panel to a control in one window while it must be placed against the screen is the
same cross-window problem the Start menu already solved by being drawn somewhere else.

## Requirement

1. **The panel is drawn by the host, not by the bar** — the same surface that draws the Start menu,
   in both hosts.
2. **Placement is by alignment within that surface**, not by arithmetic against an anchor: bottom
   and end, inset by the bar's own margin and clear of the bar's height. This is the same expression
   the Start menu uses, so the two cannot drift apart.
3. **The two placement rules survive unchanged**: the panel's right edge lines up with the bar's,
   and it sits one gap clear above the bar rather than touching it.
4. **The tray still owns its own state visually** — the indicators show pressed while the panel is
   open — but the open state is hoisted so the host can draw the panel.
5. **One menu at a time.** Opening quick settings closes the Start menu and the reverse, because
   both are drawn in the same surface and two overlapping panels is not a state worth having.
6. **Both hosts behave identically.** The activity's panel and the overlay's are the same
   composable, placed by the same rule.

## Acceptance criteria

- [x] With the chrome in the overlay, the panel opens **above** the bar, not on top of it —
      checked on device, where it previously sat on the bar.
- [x] Its right edge lines up with the bar's right edge, and it clears the bar by the Start menu's
      gap: both come from `ShellMenuEdgeInset` and `ShellMenuBarClearance`, one expression each.
- [x] The tray indicators show pressed while it is open.
- [x] Opening Start with quick settings open closes quick settings, and the reverse — `ShellMenu`
      holds one value, so the state does not exist (unit test).
- [x] The volume slider does not dismiss it mid-drag — `dismissesPanel`, now named once and tested,
      rather than the same condition written in each host.
- [ ] **Not yet verified on device:** Ctrl+Esc and Esc while quick settings is open. The handler was
      moved from inside the Start menu to the menu layer for exactly this — with the tray panel up,
      the window held focus and nothing handled a key — but the emulator wedged before it could be
      checked.
- [x] `./gradlew test lint assembleDebug` green.

## Notes

- **Why not keep the `Popup` and give it the screen.** A `Popup`'s position provider is handed the
  size of the window it was created in; there is no way to ask it about the screen. The panel would
  have to know its own window's offset, which is precisely the coupling that made the Start menu a
  separate window rather than a taller bar.
- `TrayPopoverPosition` and its tests go with the mechanism they served. The rules they encoded move
  into the host's alignment and padding, where the Start menu's already are.

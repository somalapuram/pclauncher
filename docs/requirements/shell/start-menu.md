# Shell — Start Menu

Status: **Accepted · Partially implemented** (2026-08-20)

SRS §14 Phase 5 (the menu; the command palette is [`command-palette.md`](command-palette.md)).
Derives from SRS §6.4. Depends on [`../launch/app-inventory.md`](../launch/app-inventory.md),
[`dock-taskbar.md`](dock-taskbar.md) and [`pinning.md`](pinning.md).

## Context

The dock holds what you use constantly. The Start menu is where **everything else** lives — and on
a PC "everything else" is the majority of what is installed. Without it the shell can only reach
eight apps, which is the difference between a mockup and something usable.

SRS §6.4 specifies the Windows 11 arrangement: search at the top, pinned, all apps, recommended,
a footer. This slice builds the spine of that — **all apps and search** — because those are what
make the launcher able to launch. Pinned and Recommended sections follow once there is a pins store
to feed the first and a reason to prefer the second over plain alphabetical.

**Keyboard-first, because a PC user's hands are already there.** Opening the menu focuses the
search field; typing filters; ↑/↓ moves; Enter launches; Esc closes. A Start menu that requires the
mouse for every launch is slower than the app drawer it replaces.

**It must survive a large list.** A real device has 100–300 launchable activities, and this renders
inside the HOME activity on a CPU renderer. The list is virtualised and the filter runs over
already-loaded data — never a re-query of `LauncherApps` per keystroke.

## Requirement

1. **Opens from the Start button**, springs from its origin, and closes on Esc, on a click outside,
   or on the button again. The button reads as open while it is.
2. **Shows every launchable app** from the inventory, in the inventory's own locale-aware order,
   including work-profile entries with their badge.
3. **A search field, focused on open.** Typing filters by label with a **case- and
   diacritic-insensitive substring match**, prefix matches ranked first — someone typing "cal"
   wants Calendar above "Google Calculator Sync".
4. **Keyboard navigation:** ↑/↓ move the selection, Enter launches it, Esc closes. The selection is
   visible and follows the filter.
5. **Click launches.** Right-click (long-press on touch) opens a context menu offering **Pin to
   taskbar** / **Unpin from taskbar** per `pinning.md`, plus App info.
6. **Unavailable and suspended entries are shown greyed and are not launchable**, matching the
   inventory's contract rather than hiding the user's apps.
7. **Virtualised.** The list uses a lazy layout; filtering is a pure function over the loaded
   inventory, not a re-query.
8. **Empty and loading states are designed:** "no apps yet" while the inventory fills, and "no
   matches" with the query echoed back.

## Acceptance criteria

- [x] The menu opens from the Start button and closes on Esc, on launching, and **on a click
      outside**; the button shows its open state in the accent colour.
- [x] Every launchable app appears, in the inventory's locale-aware order, work profiles included.
- [x] Search is case- and diacritic-insensitive; prefix and word-start matches rank above substring
      matches, and within a rank inventory order is kept (tests).
- [x] ↑/↓/Enter/Esc work without the mouse; the selection is clamped rather than reset when the
      filter shrinks the list under it.
- [x] Click launches; the row menu offers pin/unpin with the correct label.
- [ ] **App info is not in the row menu** — only pin/unpin.
- [x] Unavailable/suspended entries render greyed and do not launch.
- [x] The list is lazy; filtering is a pure function over the loaded inventory, not a re-query.
- [x] Loading ("Loading apps…") and no-match states are reachable and designed.
- [x] `./gradlew test` green; `./gradlew lint` clean; the menu is usable on a device.

## Notes

- **Boundary with the palette:** the Start menu is a *browser* of apps with a filter; the command
  palette (`command-palette.md`) is a *ranked* search over apps, settings, windows and files. They
  will share the matching primitives, not the UI.
- **Pinned and Recommended sections are deferred** to keep this slice to the part that makes the
  shell able to launch anything. `pinning.md` supplies the data for the first when it lands.
- **Why substring and not fuzzy here:** fuzzy matching earns its keep in the palette, where the
  user is aiming at one result. In a browsable list it mostly produces surprising ordering.
- **The row menu opens from a `⋯` affordance, not right-click.** Compose on Android has no
  secondary-click primitive, and the desktop AVD delivers a right-click as a plain tap. An explicit
  affordance works with mouse, touch and keyboard today; real right-click and long-press belong
  with the wider context-menu work.
- **The menu floats over the desktop, it does not sit in the column.** Originally it was a sibling
  of the desktop and the bar, so opening it squeezed the desktop upward. It is now a layer in an
  outer `Box`, above a full-screen dismiss layer.
- **A surface that only looks solid does not consume clicks.** Compose hit-tests only nodes that
  handle input, so clicks landing on the menu's own padding fell straight through to the dismiss
  layer and closed it. The menu root now swallows those; rows and the search field are hit-tested
  first and are unaffected. The dismiss layer carries a test tag because the failure mode here is
  z-order, which is invisible in a diff and only shows up by trying to click.
- **Ranking order was a real bug the tests caught:** plain containment was tested before the
  word-start case, so "Google Calendar Sync" ranked below an incidental match like "Vertical" for
  the query "cal". Word-start is now tested first.
- **Not in this slice:** the command palette, Recommended, the power/settings footer, drag from the
  menu, alphabet index rail, or app shortcuts.

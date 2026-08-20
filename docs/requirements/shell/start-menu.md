# Shell — Start Menu

Status: **Accepted · In progress** (2026-08-20)

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

- [ ] The menu opens and closes from the Start button, Esc, and an outside click; the button shows
      its open state.
- [ ] Every launchable app appears, in locale-aware order, work profiles included.
- [ ] Search is case- and diacritic-insensitive; prefix matches rank above substring matches (test).
- [ ] ↑/↓/Enter/Esc work without the mouse; the selection is visible and survives filtering.
- [ ] Click launches; right-click offers pin/unpin with the correct label and App info.
- [ ] Unavailable/suspended entries render greyed and do not launch.
- [ ] The list is lazy; filtering does not re-query `LauncherApps` (pure function, tested).
- [ ] Loading and no-match states are reachable and designed.
- [ ] `./gradlew test` green 3×; `./gradlew lint` clean; the menu is usable on a device.

## Notes

- **Boundary with the palette:** the Start menu is a *browser* of apps with a filter; the command
  palette (`command-palette.md`) is a *ranked* search over apps, settings, windows and files. They
  will share the matching primitives, not the UI.
- **Pinned and Recommended sections are deferred** to keep this slice to the part that makes the
  shell able to launch anything. `pinning.md` supplies the data for the first when it lands.
- **Why substring and not fuzzy here:** fuzzy matching earns its keep in the palette, where the
  user is aiming at one result. In a browsable list it mostly produces surprising ordering.
- **Not in this slice:** the command palette, Recommended, the power/settings footer, drag from the
  menu, alphabet index rail, or app shortcuts.

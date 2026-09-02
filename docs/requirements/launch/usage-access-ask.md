# Launch — asking for usage access

## Context

`recent-apps.md` shipped the Start menu's Recent row, and deliberately left this out:

> **Not in this slice:** … the optional usage-access prompt from SRS §7.1 step 4. The prompt is
> deliberately held back: two permission cards on a first run is worse than one, the local counters
> make the feature work without it, and `overlay-permission-ask.md` has just spent the user's
> attention.

That reasoning still holds; what changes is that the row now exists, so there is something concrete
to explain. Without usage access the row lists only apps launched from pclauncher itself. With it,
`SystemUsageSignals` also sees apps the user opened anywhere else — which on a device that has been
in use for a while is the difference between a row that looks empty and one that is immediately
useful (SRS §7.1 step 4, §11).

Like `SYSTEM_ALERT_WINDOW`, this is a **special** permission: `PACKAGE_USAGE_STATS` is never granted
at install, is always reported denied by the normal permission check, and can only be turned on
through a Settings screen. So the shape is the same as `overlay-permission-ask.md` — explain, then
open the screen — and the machinery built there is reused rather than rebuilt.

## Requirement

1. **At most one permission card per launch.** Not merely one on screen at a time: answering one
   must not immediately produce the next, which is the same queue of dialogs seen one at a time and
   teaches the user to dismiss cards without reading them. The usage card waits until the overlay
   question has been answered *and* the shell has been opened again.
2. **The card explains what the user gets**, in their terms: the Recent row also knowing about apps
   opened outside pclauncher. It must not claim the feature is broken without it, because it is not.
3. **Allow opens the usage-access Settings screen; Not now dismisses.** Both are remembered.
4. **Asked at most once, automatically**, and the answer survives a restart.
5. **Never shown when usage access is already granted.**
6. **A grant takes effect without a restart** — the Recent row picks up the richer source on return.
7. **The card never traps the user** (GATE 4): dismissible, the desktop reachable behind it, and an
   unreadable prompt store shows nothing rather than repeating.

## Acceptance criteria

- [ ] Fresh install: the overlay card appears and the usage card does not.
- [ ] Answering the overlay card shows no second card in that session.
- [ ] Reopening the shell then shows the usage card.
- [ ] *Not now*, then restart: neither card returns.
- [ ] *Allow*: the usage-access Settings screen opens; granting and returning makes the Recent row
      include apps launched outside pclauncher, with no restart.
- [ ] Usage access already granted: the card never appears.
- [ ] `promptToShow` returns at most one prompt for every combination of inputs.
- [ ] `./gradlew test lint assembleDebug` green; checked on device.

## Notes

- **One decision function, not two flags.** `shouldAskForOverlay` is replaced by `promptToShow`,
  which returns the single prompt to show or none. Two independent booleans can both be true at
  once, and requirement 1 is exactly the guarantee that a pair of booleans cannot make.
- **Why sequence rather than ask in context.** Asking when the user first opens the Start menu would
  be better placed, but it makes the trigger depend on a UI event in a surface that has two hosts
  (the activity and the overlay). Sequencing gets requirement 1 for free and keeps the decision pure.
- **The prompts move to their own package.** `overlay/` was the right home when the only prompt was
  about overlays; a general "which first-run card to show" decision does not belong there.
- **Stage B:** the target device can grant this by allowlist, so the card stays out of the way there
  through requirement 5, exactly as the overlay card does.

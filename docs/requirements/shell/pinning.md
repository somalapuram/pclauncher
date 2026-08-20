# Shell — Pinning

Status: **Accepted · In progress** (2026-08-20)

The `pins` store from SRS §10, and pin/unpin from every surface that lists an app. Derives from
SRS §6.3 (dock), §6.4 (Start) and §10. Depends on
[`../launch/app-inventory.md`](../launch/app-inventory.md) and
[`dock-taskbar.md`](dock-taskbar.md), which shipped with the dock showing the first eight apps in
inventory order as an explicit placeholder.

## Context

A dock the user cannot arrange is a demo, not a dock. `dock-taskbar.md` shipped one deliberately —
the first eight apps alphabetically — so the bar could be built and judged before the store
existed. This closes that gap.

Pinning is one concept reached from several places: the Start menu, the desktop, and the dock
itself. That makes the temptation to implement it three times, and the pins would then disagree.
So the store is the single source of truth, every surface calls the same two operations, and the
dock is a *view* of the store rather than an owner of it.

**What is pinned is a component, not a package.** SRS §5 and the inventory both key on
`(ComponentName, UserHandle)`, and so must a pin: pinning "Chrome" from the work profile must not
put the personal Chrome in the dock.

**Pins outlive the apps they point at.** An app can be uninstalled, disabled, or moved to a profile
that is later removed, all while its pin sits in the store. A pin whose app is gone must not
crash the dock and must not silently vanish either — the resolution rule is part of the
requirement, not an implementation detail.

## Requirement

1. **A `pins` DataStore**, holding an **ordered** list of `(component, user)` — order is what the
   user arranged, so it is data, not a sort.
2. **Two operations, everywhere:** `pin(AppKey)` and `unpin(AppKey)`, plus `isPinned(AppKey)` for
   the affordance's state. No surface writes the store directly.
3. **Pinning is idempotent.** Pinning something already pinned changes nothing — including not
   moving it to the end of the order. Unpinning something absent is a no-op, not an error.
4. **The dock renders the store**, resolved against the inventory, in stored order. When the store
   is empty the dock falls back to the inventory-order placeholder so a first run is never a bare
   bar.
5. **Unresolvable pins are kept but not drawn.** A pin whose component is not in the current
   inventory — uninstalled, or in a profile that is off — is skipped when rendering and **left in
   the store**, so turning a work profile back on restores its pins rather than losing them.
6. **Reachable from the Start menu and the desktop**, via right-click (long-press on touch), with
   the label reflecting current state: *Pin to taskbar* / *Unpin from taskbar*.
7. **Writes never block the caller.** Pinning is a suspend operation off the main thread, and a
   failed write costs the pin, never the interaction.
8. **Pure resolution.** Turning (stored pins + inventory) into the dock's list is a pure function,
   exhaustively tested — including empty store, missing components, and duplicate entries.

## Acceptance criteria

- [ ] `pins` DataStore holds an ordered list of component + user handle and survives restart.
- [ ] `pin` / `unpin` / `isPinned` are the only way any surface mutates or reads pin state.
- [ ] Pinning twice is a no-op and does not reorder; unpinning something absent is a no-op.
- [ ] The dock renders stored pins in stored order.
- [ ] An empty store falls back to the inventory-order placeholder.
- [ ] A pin whose app is missing is skipped when drawing and **retained** in the store (test).
- [ ] Right-click on a Start-menu entry and on a desktop icon offers pin/unpin with the correct
      label for current state.
- [ ] Pin writes happen off the main thread; a store failure does not propagate to the UI.
- [ ] Resolution is pure and tested: empty, missing, duplicated, reordered.
- [ ] `./gradlew test` green 3×; `./gradlew lint` clean.

## Notes

- **Why not reuse the `usage` store's shape:** usage is a map keyed by component; pins are a
  *sequence*. Storing them as a map with an index field invites the two to disagree about order.
- **Supersedes** the "pin order persists across restart" criterion left unchecked in
  `dock-taskbar.md`.
- **Not in this slice:** drag-to-reorder within the dock, drag-out-to-unpin, pinning to Start (as
  opposed to the taskbar), folders, or pinning anything that is not an app.

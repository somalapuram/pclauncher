# Shell — Dock, Start Button & Taskbar

Status: **Accepted · Partially implemented** (2026-08-20)

SRS §14 Phase 4 (the bar itself; the overlay service that lifts it above other apps is
[`overlay-service.md`](overlay-service.md)). Derives from SRS §6.3 and §6.1.
Depends on [`../launch/app-inventory.md`](../launch/app-inventory.md) and
[`../design/icon-treatment.md`](../design/icon-treatment.md).

## Context

SRS §6.3 asks for one bar carrying both heritages: a macOS dock — centred, magnifying, floating
with rounded corners — and a Windows 11 taskbar — Start button at the left, a chip per open window,
system tray at the right. One bar, because two would eat 120 dp of a 1600 px-tall screen and force
the user to learn which strip owns which behaviour.

The bar is the first thing anyone sees and the thing they touch most, so it carries most of the
shell's design budget. The current desktop has none of it — phase 1 shipped a placeholder card.

**Rendered in the HOME activity first.** SRS §9 puts the shell chrome in
`TYPE_APPLICATION_OVERLAY` windows so it survives above other apps, and that is still the plan —
but it is a separate concern from what the bar *is*. This slice builds the bar as ordinary
composables inside the desktop, where it can be seen, judged and iterated on immediately;
`overlay-service.md` then hosts the very same composables in overlay windows. Splitting it this way
keeps `main` releasable at every step (CODING-GUIDELINES §8) and gets the visual feedback loop
running now rather than after the service works.

**Software rendering shapes this more than anything else.** Dock magnification is the single most
animation-heavy thing in the shell, and on `pc_x86_64` it runs on the CPU. Magnification is
therefore a **scale and translation of already-composited bitmaps** — never a re-layout, never a
re-render of the icon — and it degrades to nothing when the renderer cannot afford it (SRS §4.3).

## Requirement

When this is done, the following must be true:

1. **One bar, three zones.** Start button at the left, dock centred, taskbar chips after a
   separator, Show Desktop at the right end. The dock stays **optically centred on the screen**, not
   on the leftover space, so it does not drift as chips appear.
2. **A Start button that carries the brand.** The pclauncher glyph on a squircle in the same
   treatment as the app icons, with hover, pressed and open states, and an accent-lit state while
   the menu is open. It is the visual anchor of the whole shell, not a generic ⊞.
3. **macOS dock magnification.** Icons scale under the pointer on a falloff curve across
   neighbours, spring-driven, with the bar growing to accommodate. Implemented as graphics-layer
   scale on cached bitmaps only. Disabled entirely in touch-first mode and on a software renderer.
4. **Running state is shape, not colour** (SRS §6.1 principle 5): a dot under running apps, a wider
   pill under the focused one.
5. **Taskbar chips per open window** — treated icon plus truncated title, filled when focused and
   outlined otherwise, scrolling horizontally past the cap in SRS §13. Click focuses;
   middle-click closes.
6. **Pointer-first, touch-tolerant.** Hover states everywhere, right-click context menus on dock
   items and chips, and a long-press equivalent for every hover-only affordance. Every target
   ≥ 44 dp including the ≤ 64 dp bar height (SRS §6.1 principle 3).
7. **Pinned apps come from the inventory and persist.** The dock's contents are `AppEntry` values
   resolved through `core:apps`; pin order survives restart. Unavailable and suspended entries are
   shown greyed rather than dropped, matching the inventory's own contract.
8. **The bar never blocks the desktop.** It renders from the inventory `Flow`, so it appears with
   the desktop and fills in — an empty dock is a valid first frame, not a spinner.
9. **Layout is pure and tested.** Magnification scale per index, dock centring, chip widths and
   overflow are pure functions of (pointer position, item count, bar width). No test needs a device.
10. **Position and auto-hide are settings** — bottom / left / right, always-visible or reveal on
    edge — with bottom and always-visible as the SRS §13 defaults. The layout code must not assume
    horizontal.

## Acceptance criteria

- [x] One bar renders Start, dock, separator, chips, Show Desktop, in that order.
- [x] The dock is centred on the screen and does not shift as chips are added (`BarLayout`, tested).
- [x] Start button has hover / pressed / open states and carries the pclauncher glyph.
- [x] Magnification follows a raised-cosine falloff across neighbours, is spring-driven, and is
      applied as a graphics-layer scale over an already-composited bitmap — no re-layout per frame.
- [x] Magnification can be switched off via `BarState.magnificationEnabled`.
- [x] Running apps show a dot; the focused app shows a wider pill.
- [x] Chips are filled when focused and outlined otherwise; the strip scrolls.
- [x] Every interactive target is ≥ 44 dp inside a 56 dp bar.
- [x] Dock contents come from `core:apps` via a pure `BarStateFactory`.
- [x] Unavailable/suspended entries render greyed, not missing.
- [x] The bar renders before the inventory completes; an empty dock is a valid frame.
- [x] Magnification curve, dock centring, chip width and overflow are pure and unit-tested.
- [ ] **Pin order persists across restart** — not implemented; the dock currently shows the first
      eight apps in inventory order. Needs the `pins` store.
- [ ] **Right-click context menus and middle-click-to-close** — not implemented.
- [ ] **Bar position / auto-hide settings** — not implemented; the layout is still horizontal-only.
- [ ] Magnification is not yet switched off automatically in touch-first mode or on a software
      renderer; the flag exists but nothing sets it.
- [x] `./gradlew test` green; `./gradlew lint` clean; the bar is visible on the desktop on a device.

## Notes

- **Depends-on / blocks:** depends on the inventory and the icon treatment. Blocked *by nothing* —
  deliberately, so the visual work can start before `overlay-service.md` exists. Blocks
  `start-menu.md` (which the Start button opens) and `system-tray.md` (which lands in the bar's
  right zone).
- **Sequencing decision:** chrome lives in the HOME activity in this slice and moves into overlay
  windows in `overlay-service.md`. The composables must therefore take no dependency on being in an
  activity — no `LocalContext` casts to `Activity`, nothing that assumes a window insets owner —
  or the move will be a rewrite.
- **Why not reuse `WindowBackend` for the chips yet:** below T2 there is no real window list
  (SRS §5.4), so this slice renders chips from our own launch bookkeeping and labels the zone as
  recent windows. `capability-tiers.md` and `freeform-launch.md` replace the source, not the UI.
- **What actually landed:** the bar, the Start button, magnification, the chip row and the greyed
  states. What did not: pinning (needs the `pins` store), context menus, the position/auto-hide
  settings, and automatic magnification disabling. Those are listed unchecked above rather than
  quietly dropped, and each needs either a store or a settings surface that does not exist yet.
- **Not in this slice:** the Start *menu* (`start-menu.md`), the system tray (`system-tray.md`),
  the overlay service (`overlay-service.md`), drag-to-reorder pins, Mission Control, live window
  thumbnails.

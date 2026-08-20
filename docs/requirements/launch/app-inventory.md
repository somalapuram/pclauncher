# Launch — App Inventory

Status: **Accepted · In progress** (2026-08-20)

SRS §14 Phase 2. The list of things the shell can launch, and what they look like. Derives from
[`../../SRS-pclauncher.md`](../../SRS-pclauncher.md) §3 (tech stack), §9 (`core:apps`), §10 (the
`usage` store) and §12 (performance).

Depends on [`../foundation/scaffolding.md`](../foundation/scaffolding.md).

## Context

Every surface in the shell is a view over one list: the dock shows pinned entries from it, the Start
menu shows all of it, the command palette searches it, the desktop puts icons from it on a grid, and
the taskbar labels windows with it. Building it once, correctly, headlessly, is what keeps those
five surfaces consistent — and what stops each of them growing its own half-right copy.

Three things make this more than "list the installed apps":

- **A package is not an identity.** The same package exists separately in the personal profile and
  the work profile, with different icons, different availability, and a different launch target.
  The identity of an entry is `(ComponentName, UserHandle)`, and getting that wrong surfaces as a
  work app launching the personal one.
- **The list changes under you.** Installs, uninstalls, updates, suspension, a profile being added
  or turned off, external storage appearing, and — easy to miss — a **locale change**, which
  changes every label. SRS §12 forbids a full rescan on a package change, so updates are deltas.
- **It is on the startup path.** SRS §12 gives a 500 ms budget to an interactive desktop (1.5 s on
  `pc_x86_64` under software rendering), and icon loading is expensive. The desktop has to render
  before the inventory finishes, so the inventory is a stream that starts empty, not a blocking
  load.

This slice is **headless**: no Compose, no UI. `core:apps` gains no dependency on any `feature:`
module. Ranking for search is *not* here — this supplies the label and the signals; the fuzzy
matching lives in [`../shell/command-palette.md`](../shell/command-palette.md).

Launching is also not here. This slice models what *can* be launched; how and where it launches is
`WindowBackend`'s job ([`../windows/capability-tiers.md`](../windows/capability-tiers.md)).

## Requirement

When this is done, the following must be true:

1. **An `AppEntry` model keyed by `(ComponentName, UserHandle)`**, carrying at least: label,
   package name, profile kind (personal / work / private), suspended, available, first-install
   time, and **whether the activity is resizable** (from `ActivityInfo.resizeMode` and fixed
   orientation). The resizable flag is captured here because SRS §5.4 needs it at launch time and
   this is the only place the `ActivityInfo` is already in hand.
2. **`LauncherApps`, not `PackageManager`.** The inventory is built from
   `LauncherApps.getActivityList` across every profile from `LauncherApps.getProfiles()`, behind an
   **injectable `AppSource` interface** so every test runs against a fake and CI needs no device.
3. **All profiles, correctly.** Work and private-space entries appear with their profile kind and
   badged icon; entries in a profile under **quiet mode** are marked unavailable rather than
   dropped, so the UI can show them greyed instead of silently losing the user's apps.
4. **Incremental updates, never a rescan.** A `LauncherApps.Callback` drives a **pure delta
   function** over the current list: package added, removed, changed/replaced,
   suspended/unsuspended, available/unavailable, plus profile added/removed and **locale change**
   (relabel + re-sort). The pure function is the thing under test; the callback is a thin adapter.
5. **A stream, not a load.** The inventory is exposed as a `Flow<AppInventory>` that emits an empty
   or partial state immediately and fills in, so the desktop renders inside the SRS §12 budget.
   Nothing blocks the main thread; the initial build runs on a background dispatcher.
6. **A two-level icon cache** — bounded in-memory LRU over a disk cache — keyed by component, user,
   density, **and package version**, so an app update does not serve a stale icon. Adaptive and
   themed/monochrome icons are preserved rather than flattened. A failed or missing icon yields a
   defined placeholder; it never throws and never blocks.
7. **Locale-aware ordering.** The default order is by label using a `Collator` for the current
   locale, not `String.compareTo` — otherwise accented and non-Latin labels sort wrongly. Ordering
   is a pure function over the list.
8. **Recency and launch counts.** Usage signals come from `UsageStatsManager` when the user has
   granted usage access, and from **our own launch counters** in the `usage` DataStore store
   (SRS §10) when they have not. Whether the permission is held is *detected*, and the ungranted
   path is a first-class behaviour, not a degraded one — the shell must rank sensibly either way.
9. **Package visibility is handled deliberately.** `QUERY_ALL_PACKAGES` is declared and justified as
   a launcher; the code must still behave correctly when a package is filtered out from a query
   rather than assuming everything installed is visible.
10. **Fully tested, headlessly.** Delta application, ordering, profile mapping, the usage-source
    decision, and cache keying are pure and exhaustively tested against a fake `AppSource`. No test
    requires a device, a real profile, or a granted permission.

## Acceptance criteria

- [ ] `AppEntry` is keyed by `(ComponentName, UserHandle)`; two entries for the same package in
      different profiles coexist and are distinguishable.
- [ ] Inventory is built via an injectable `AppSource` over `LauncherApps` + `getProfiles()`; the
      real implementation is the only thing touching the framework.
- [ ] Work and private-profile entries carry their profile kind and badged icon; quiet-mode entries
      are present and marked unavailable, not dropped.
- [ ] `resizeMode`/fixed-orientation is captured per entry (consumed later by phase 6).
- [ ] A pure delta function handles add, remove, change/replace, suspend/unsuspend,
      available/unavailable, profile add/remove, and locale change — each covered by a test.
- [ ] No code path performs a full rescan in response to a single package event.
- [ ] The inventory is a `Flow` that emits before the full list is ready; no main-thread work
      (asserted with a test dispatcher).
- [ ] Icon cache is memory-over-disk, keyed by component + user + density + package version; an app
      update invalidates its icon (test).
- [ ] A missing or failing icon returns the placeholder rather than throwing (test).
- [ ] Default ordering uses a locale `Collator`; ordering is pure and tested, including a
      non-ASCII label case.
- [ ] Usage access held → `UsageStatsManager`; not held → local counters. Both paths tested; the
      decision is detected, not assumed.
- [ ] `./gradlew test` green 3× consecutively; `./gradlew lint` clean; tests mirror `core/apps/src`.
- [ ] `core:apps` depends on no `feature:` module and pulls in no Compose.

## Notes

- **Depends-on / blocks:** depends on `foundation/scaffolding.md`. Blocks
  `shell/dock-taskbar.md`, `shell/start-menu.md`, `shell/command-palette.md`, and
  `desktop/icon-grid.md` — all four are views over this list.
- **Boundary with phase 5:** this slice supplies labels and usage signals; fuzzy matching and result
  ranking belong to the command palette's doc. Resist putting a scoring function here.
- **Boundary with phase 6:** this slice *records* whether an activity is resizable; deciding what to
  do about it is `windows/freeform-launch.md`.
- **The fallback desktop gets its app list here.** `foundation/scaffolding.md` shipped safe mode
  without one because there was no inventory yet. Wiring a minimal, cache-free listing into the
  fallback belongs to this slice — but it must not depend on the cache or the usage store, since
  those are among the things whose failure puts the user in safe mode in the first place (GATE 4).
- **Private space** (Android 15+) is a profile like any other to `getProfiles()`, but it can be
  locked and hidden. Treat "present but locked" the same as quiet mode rather than inventing a
  second mechanism.
- **Why not `PackageManager.queryIntentActivities`:** it is not profile-aware, gives no badged
  icons, and has no change callback. Every launcher that starts there ends up rewriting it.
- **Not in this slice:** launching apps, pinning, shortcuts (`getShortcuts`), app widgets, folders,
  search ranking, or any UI.

# Launch — recent apps in the Start menu

## Context

SRS §6.4 gives the Start menu a **Recommended** section — "recently used and recently installed" —
and §10 gives it a `usage` store. The ranking machinery for it was built in phase 2 and is fully
tested:

| piece | file |
|---|---|
| `PACKAGE_USAGE_STATS` declared | `core/apps/src/main/AndroidManifest.xml` |
| `UsageStatsManager` reader | `core/apps/.../UsageSources.kt` (`SystemUsageSignals`) |
| own launch counters | `core/apps/.../DataStoreUsageStore.kt` |
| source selection, recency ordering | `core/apps/.../UsageSignals.kt` |

None of it is wired. Nothing outside `core/apps`' own tests constructs `UsageSignals`, nothing
calls `recordLaunch`, and there is no provider for `UsageStore` — so the shell never records a
launch and no surface shows recency. This slice connects what exists and puts it on screen.

The question that prompted it was whether recents need the AOSP tree. They do not: a recent *apps*
list is entirely unprivileged. What needs privilege is a recent *windows* list — live thumbnails and
real task enumeration need `REAL_GET_TASKS` (SRS §5.2), which is why §5.4 says the T0/T1 surface is
labelled honestly as recent apps rather than open windows.

## Requirement

1. **The Start menu shows a Recent row** above All apps: the most recently launched apps, most
   recent first.
2. **Only apps that have actually been used appear there.** An app with no recorded use is not
   "recent", and the row must never be padded out with alphabetical filler.
3. **The row is hidden entirely when there is nothing to show** — no empty heading on a fresh
   install — and hidden while a search query is active, because the user is looking at results.
4. **Launching an app records it**, wherever it was launched from: desktop icon, dock, Start menu.
   One recording point, so no surface can be the one that forgets.
5. **A recorded launch shows up immediately** in the row, without reopening the menu.
6. **Usage access is used when granted and not needed when not.** With `PACKAGE_USAGE_STATS` the
   list also knows about apps used before pclauncher was installed; without it, our own counters
   carry it. The ungranted path is the first-class one, not a degraded one (`UsageSignals` already
   states this).
7. **The row is reachable by keyboard**, like the rest of the menu (SRS §12): arrow keys move into
   and out of it, Enter launches.
8. **Unavailable entries follow the inventory's contract** — greyed, not hidden, and not launchable
   — the same as everywhere else that lists an app.
9. **Recording never costs a launch.** A failed or slow write must not delay or block the app
   opening (GATE 4); reads must not block the main thread.

## Acceptance criteria

- [ ] Fresh install: no Recent row.
- [ ] Launch an app from the desktop, open Start: it is the first entry under Recent.
- [ ] Launch a second app: it takes first place and the first moves along.
- [ ] Launching the same app twice does not list it twice.
- [ ] Typing in the search field hides the row; clearing the query brings it back.
- [ ] Arrow keys reach the row and leave it; Enter on a recent entry launches it.
- [ ] The row survives a shell restart (the counters are persisted).
- [ ] With usage access granted, apps used outside pclauncher appear.
- [ ] `./gradlew test lint assembleDebug` green; checked on device.

## Notes

- **Named "Recent", not "Recommended".** SRS §6.4's name covers recency *and* recent installs; this
  slice implements recency only, so the heading claims only that. Recently-installed is deferred —
  `AppEntry` carries no install time today.
- **One row, five entries** — `StartColumns`, so the section is exactly one grid row and the
  keyboard arithmetic in `moveInGrid` stays untouched. Where fewer than five apps qualify the row
  is short, and the empty slots are skipped by keyboard movement rather than selectable.
- **Not in this slice:** the taskbar's recent-windows chips (SRS §5.4), Mission Control, live
  thumbnails, and the optional usage-access prompt from SRS §7.1 step 4. The prompt is deliberately
  held back: two permission cards on a first run is worse than one, the local counters make the
  feature work without it, and `overlay-permission-ask.md` has just spent the user's attention.
- **Why the recording point is the launcher, not each surface.** `AppLauncher` is the single choke
  point every launch already goes through. Recording anywhere else means one surface eventually
  forgets, and the symptom — a recents list that is quietly wrong — is one nobody reports.

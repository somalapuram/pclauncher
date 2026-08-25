# Launch — Inventory Identity and Restart

Status: **Implemented** (2026-08-24)

The inventory holds one entry per `(ComponentName, UserHandle)` however many times it is built, and
building it again replaces rather than accumulates. Amends requirement 1 and 4 of
[`app-inventory.md`](app-inventory.md), which named the key but never said it was enforced.

## Context

Making pclauncher the default home on a Pixel Tablet produced a taskbar showing Calendar, Calendar,
Camera, Camera, Chrome, Chrome, Clock, Clock — four apps, each twice, confirmed in the
accessibility tree rather than only by eye. A restart cleared it and five further attempts did not
reproduce it, which is exactly what makes it worth fixing properly: the trigger is timing, and the
defect is structural.

**The repository is application-scoped; starting it is activity-scoped.**
`AppInventoryRepository` is a `@Singleton`, and `HomeActivity.onCreate` calls `start()` on it every
time an activity instance is created. Android creates a second home activity readily — a
configuration change, a theme change, the system relaunching the home app when the default home
role moves. Nothing about that is exceptional, and the second `start()` does two wrong things:

1. It merges each profile's entries onto whatever is already there —
   `current.entries + entries` — so every app is listed once per `start()`.
2. It assigns `subscription` without closing the one already held, so the previous
   `LauncherApps.Callback` stays registered. From then on every package change is applied twice,
   and the leak is permanent for the life of the process.

**Only the first is visible, and only in one place.** The desktop hides duplicates because it
places by component id: two entries for one component land on the same cell and stack exactly. The
dock showed it because with no pins stored it falls back to `entries.take(8)`, and eight slots of a
doubled list is four apps twice.

**A key that is not enforced is a comment.** `app-inventory.md` requirement 1 calls the entry keyed
by `(ComponentName, UserHandle)`; the code never used that key to establish identity when merging.
Anything that produces the same activity twice — a second `start()`, or a platform that returns the
same profile twice from `getProfiles()` — duplicates it.

**Restarting must not freeze the list either.** The obvious fix, making `start()` a no-op once
started, trades a visible bug for an invisible one: `HomeActivity.onDestroy` calls `stop()`, so an
outgoing activity destroyed *after* an incoming one has started would leave the inventory
permanently unsubscribed and stale. A repeated `start()` has to be *correct*, not refused.

## Requirement

1. **One entry per `(ComponentName, UserHandle)`.** Merging entries into the inventory is keyed;
   an incoming entry replaces the one it matches rather than joining it.
2. **`start()` is safe to call again.** A second call rebuilds and replaces; it never accumulates
   and never leaves the list doubled.
3. **A second `start()` does not leak the change subscription.** Any subscription already held is
   closed before another is established.
4. **The list does not empty while it is being rebuilt.** Entries already known stay visible until
   their replacements arrive, so a restart is not a flash of an empty desktop.
5. **A profile returned more than once is harmless.** The platform listing the same user twice
   yields one entry per activity, not two.
6. **The merge is a pure function and tested**, including replacement order — the newer entry wins,
   so a relabelled or newly-suspended app is not shadowed by its stale copy.

## Acceptance criteria

Unit-tested in `InventoryIdentityTest`; device checks on the `Pixel_Tablet` AVD (Android 17,
API 37, 2560×1600) where the defect was first seen.

- [x] Merging a list with an entry it already contains yields one entry, and the incoming one wins.
- [x] Merging preserves entries that are not in the incoming list.
- [x] Calling `start()` twice yields exactly the entries of one build.
- [x] Calling `start()` twice closes the first change subscription — the fake counts two opened and
      one closed, and fails at two/zero against the old code.
- [x] A source whose `profiles()` returns the same user twice yields no duplicates.
- [x] Two entries for the same package in different profiles still coexist.
- [x] The dock's no-pins fallback shows eight *distinct* apps: verified after replaying the exact
      sequence that produced the duplicates (take the home role while an instance is running, then
      HOME), and held at 8/8 across four further activity-recreation cycles.
- [x] `./gradlew test lint assembleDebug` green.

**Three of these fail against the previous code** — starting twice duplicating the entries,
starting twice leaking the subscription, and a repeated profile duplicating the list — which is
what makes them a guard rather than a description.

## Notes

- **Why not make the repository start itself.** Application-scoped state started from an activity
  is the actual design smell here, and moving ownership to the application is the right eventual
  answer. It is a wider change than this defect justifies, and the requirements above make the
  current arrangement correct rather than merely lucky. Worth revisiting when the shell service
  arrives (SRS §9), which needs the inventory without an activity anyway.
- **Why the entries are not rebuilt from empty.** Publishing per profile is what keeps the desktop
  inside the SRS §12 budget; starting a rebuild from an empty list would make a restart blink.
  Keyed merging gives both.
- **Not in this slice:** removing entries that vanished while the callback was detached. The delta
  callback handles removals in the normal case, and a stale entry that no longer resolves is
  already handled as unavailable at launch time.

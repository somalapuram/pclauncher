# Shell — The Secondary Home Needs Its Own Task

Status: **Accepted · Implemented** (2026-09-24). Amends
[`secondary-display-home.md`](secondary-display-home.md) requirement 1; that doc otherwise stands.

## Context

`secondary-display-home.md` shipped a `SECONDARY_HOME` activity and was verified on the HP laptop
with an HP 527pq on HDMI-A-1. Every platform layer did what the doc said it would: SurfaceFlinger
`HWC display 1`, DisplayManager `displayId 2` with `FLAG_ALLOWS_CONTENT_MODE_SWITCH` and
`mIsEnabled=true`, SystemUI put a `StatusBar(displayId=2)` on it, WM Shell created freeform desk
roots for it, the cursor crossed to it (`InputDispatcher: No new touched window … in display 2`).
The monitor showed a status bar over black. **No home task on display 2.**

Launching the activity by hand showed why:

    am start --display 2 -c android.intent.category.SECONDARY_HOME -n …/.SecondaryHomeActivity
    ActivityTaskManager: DesktopModeLaunchParamsModifier: … task=Task{8a7e964 #9 type=home …}
    VRI[SecondaryHomeActivity]: WindowInsets changed: 1920x1200 …

Task `#9` is display 0's `HomeActivity` task; `1920x1200` is the laptop panel, not the
`2560x1440` monitor. `dumpsys activity` then listed **both** activities in `#9` on display 0.
`SecondaryHomeActivity` carries the package-default task affinity — the same one `HomeActivity`
has — so `ActivityStarter` found an existing task with a matching affinity and put the new
activity there, on the wrong display, and `--display 2` was overridden by task reuse. The
framework's own automatic start goes through the same starter and meets the same task, which is
why the monitor was empty at hotplug.

Launcher3 avoids this by giving its **primary** `Launcher` `android:taskAffinity=""`
(`AndroidManifest.xml:65`), so nothing else can be pulled into the home task. The equivalent that
leaves the primary untouched (requirement 7 of the parent doc) is a distinct affinity on the
secondary activity: nothing on display 0 matches it, so its first launch creates a new task, and
that task is created where the launch asked — on the secondary display.

## Requirement

1. **`SecondaryHomeActivity` declares its own `android:taskAffinity`**, distinct from the package
   default, so it can never be placed into `HomeActivity`'s task. `HomeActivity` is unchanged.
2. Everything else in `secondary-display-home.md` stands.

## Acceptance criteria

- [ ] The manifest carries `android:taskAffinity="com.somalapuram.pclauncher.secondary"` on
      `SecondaryHomeActivity` and nothing on `HomeActivity`.
- [ ] On the device with a second monitor: `dumpsys activity activities` shows a
      `SecondaryHomeActivity` task under `Display #2` and only `HomeActivity` under `Display #0`;
      `am start --display 2 … SecondaryHomeActivity` reports `WindowInsets` at the monitor's
      resolution, not the panel's.
- [ ] The monitor shows the pclauncher desktop with a bar, not a status bar over black.
- [ ] `./gradlew test lint assembleDebug` green.

## Notes

- Found on hardware, not in a test: a task-affinity collision cannot be unit-tested in Stage A,
  which is exactly why the parent doc's on-device criteria exist. The parent doc's box for
  "`SecondaryHomeActivity` task on that display" is what this closes.
- The framework's `DesktopModeLaunchParamsModifier` log line is the fastest tell for this class
  of bug: if `task=` names an existing `type=home` task, the launch was reused into it.
- Not changing the primary to `taskAffinity=""` on purpose: it would work, but it changes how
  every HOME intent resolves against the primary task, which is the one surface nothing here
  should touch.

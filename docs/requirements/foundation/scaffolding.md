# Foundation — Scaffolding

Status: **Accepted · Implemented** (2026-08-20)

First slice of SRS §14 Phase 1. Creates the runnable Compose shell every later feature builds on.
Derives from [`../../SRS-pclauncher.md`](../../SRS-pclauncher.md) §3 (tech stack), §9
(architecture) and §14 Phase 1.

## Context

pclauncher is the home screen for `aosp-pc-x86_64` (SRS §4), developed in **Stage A** as a
standalone APK in Android Studio (SRS §2). Before any feature exists there must be a project that
compiles from both Android Studio and the CLI, is laid out in the **feature-first modules** of
SRS §9, boots to a desktop as the **default home app**, and has a **green, deterministic test
harness** so every later change lands on a working base.

Two properties of *this particular app* shape the slice from the start, and are cheaper to build in
than to retrofit:

- **It is the home screen.** A crash on launch leaves the device with no UI (GATE 4, SRS §12). The
  HOME activity must reach a usable desktop with no permissions, no stores, and no shell service.
- **It targets a software renderer.** The design tokens must express the "cheap by default"
  translucency of SRS §4.3, so no later surface accidentally makes blur the norm.

Nothing here talks to the AOSP tree (GATE 3). `platform/privileged/` exists in this slice only as an
empty seam with a capability enum, so Stage B has somewhere to land.

## Requirement

When this is done, the following must be true:

1. **A Gradle project exists** — Kotlin DSL, a `gradle/libs.versions.toml` version catalog as the
   single place versions are declared, AGP 8.x, `compileSdk`/`targetSdk` **37**, `minSdk 31`,
   application id `com.somalapuram.pclauncher`. It builds with the **wrapper** (`./gradlew`), not a
   system Gradle.
2. **It opens and builds in Android Studio**, and the *same* build works headless from the CLI on
   JDK 17. Neither path is a special case of the other.
3. **The module skeleton of SRS §9 exists** — `app`, `core:design`, `core:data`, `core:apps`,
   `feature:desktop`, `feature:shell`, `feature:search`, `feature:windows`, `feature:settings`,
   `platform:privileged`. Each is a real module with its own `build.gradle.kts`. Only `app` depends
   on features; **no feature depends on `app`**, and that is enforced by the dependency graph, not
   by convention.
4. **Hilt is wired** at the application root, and a trivially injected dependency resolves at
   runtime — proving the graph, not just the plugin.
5. **Compose + design tokens.** `core:design` owns the theme: light/dark palettes, the accent, type
   scale, spacing, corner and hairline tokens, motion specs, and a **`SurfaceTreatment` token that
   defaults to a translucent scrim, not blur** (SRS §4.3). No feature module declares a raw colour,
   dimension, or duration.
6. **The HOME activity owns the home role.** It declares `CATEGORY_HOME` + `CATEGORY_DEFAULT`, and
   offers `RoleManager.ROLE_HOME` with a deep-link fallback when the role cannot be requested.
   Pressing HOME returns to it. It renders wallpaper + a placeholder desktop surface — no icon grid
   yet, that is `desktop/icon-grid.md`.
7. **Boot safety is real, not aspirational** (GATE 4). Startup is wrapped so that a failure in
   config, DI, or any store yields a **guarded fallback desktop** — wallpaper, a plain app list, and
   a "reset shell" action — rather than a crash. The fallback is reachable in a test by injecting a
   failing dependency.
8. **A capability seam exists.** `platform:privileged` exposes a `Capability`/`Tier` type and a
   detector returning **T0** unconditionally in this slice. Nothing else reads it yet. No hidden
   API, no `Settings.Global` write, no Shizuku dependency in this slice.
9. **Green test harness.** `./gradlew test` passes deterministically (run 3×) and `./gradlew lint`
   is clean. The theme tokens, the tier detector, and the startup/fallback decision are unit-tested;
   the HOME composable has a `createComposeRule` test. The test tree mirrors the source tree.
10. **A debug APK installs and runs as the home app** on the Android 17 x86_64 AVD (SRS §13) and
    survives being set as default home, backgrounded, and returned to via HOME.

## Acceptance criteria

- [x] `./gradlew assembleDebug` succeeds from a clean checkout on JDK 17 with only the wrapper.
- [ ] The project opens in Android Studio Quail 3 and builds/runs from the IDE. *(Studio installed
      and launched; IDE build not yet exercised — the CLI build is green.)*
- [x] All ten modules of SRS §9 exist; `app` is the only module depending on features; no cycles.
- [x] Version catalog is the single source of versions — no hardcoded version strings in any
      `build.gradle.kts` (SDK levels included).
- [~] Hilt graph resolves an injected dependency at runtime. The providers are unit-tested, and the
      graph is proven end-to-end on device — the desktop renders `Windowing tier: Basic`, which is
      only reachable through the entry point. There is no *test* that stands up the component; that
      arrives with the first feature that needs one.
- [x] `core:design` exposes theme + tokens incl. `SurfaceTreatment`, defaulting to scrim not blur;
      no raw colours/dimens/durations outside `core:design`.
- [x] HOME activity declares `CATEGORY_HOME`, requests `ROLE_HOME`, and HOME returns to it on device.
- [x] Injecting a failing dependency yields the fallback desktop, not a crash — proven by a test.
- [x] `platform:privileged` returns tier T0; no hidden-API, `Settings.Global`, or Shizuku usage.
- [x] `./gradlew test` green 3× consecutively (25 tests); `./gradlew lint` clean; tests mirror source.
- [~] Debug APK set as default home and HOME returns to the desktop — verified on
      `pclauncher_desktop_api34`, **not** on the android-37.0 AVD (see Notes).
- [x] No `INTERNET` permission in the merged manifest.

## Notes

- **Depends-on / blocks:** blocks everything. `launch/app-inventory.md` and
  `windows/capability-tiers.md` build directly on this shell. No behaviour beyond boot, theme, and
  the home role belongs here — the dock, taskbar, and overlay service are `shell/overlay-service.md`.
- **Why the modules exist now and stay empty:** SRS §9 calls the Stage A/B seam the whole point of
  the architecture. Creating the boundaries before there is code to put in them is what stops
  feature code from reaching for a hidden API later.
- **JDK:** the CLI build runs on the system JDK 17; Android Studio ships its own JBR 25. Both must
  work — pin the Gradle toolchain rather than relying on whichever JDK happens to launch it.
- **The android-37.0 emulator image cannot run this app — or any third-party app.** `am start`
  returns *"Activity class does not exist"* for **every** component in the package, including
  `androidx.compose.ui.tooling.PreviewActivity`, while `dumpsys package` shows the activities
  correctly registered and preinstalled apps launch fine. The package dump shows `ceDataInode=0`,
  so credential-encrypted storage was never prepared for the app. The identical APK runs on
  `system-images;android-34;android-desktop`, so this is the image, not the code. Until it is
  resolved, **`pclauncher_desktop_api34` is the working run target** — which is no great loss, since
  it is also the only image that gives real desktop-windowing behaviour.
- **AGP 9 has built-in Kotlin support.** Applying `org.jetbrains.kotlin.android` is a hard error.
  Kotlin is pinned to 2.3.21 rather than the newer 2.4.10 because KSP — which Hilt requires — has
  not shipped a 2.4.x build.
- **Not in this slice:** overlay windows, dock, taskbar, Start, palette, inventory, widgets,
  freeform launching, Shizuku. Each has its own doc.

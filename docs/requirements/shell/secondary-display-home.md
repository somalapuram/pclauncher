# Shell — A Desktop on Every Connected Display

Status: **Accepted · Implemented** (2026-09-24)

A second monitor gets a desktop of its own — wallpaper, icons, bar and Start menu — rather than
a blank surface or a mirror of the first. SRS §1 (a PC), §7.3 "Move to display", §7.5 geometry
memory "per app, per display", §10 `window_geometry` keyed per display. Stage A: nothing here
needs the AOSP tree, a platform signature or a device build; the framework side is already on.

## Context

**The platform already does extended mode; the launcher is the only thing that does not.** This
was established by reading the actual code on `aosp-pc-x86_64` `d631143`, not assumed:

- **Kernel.** i915, xe, amdgpu and nouveau are all built in with `DRM_DISPLAY_DP_HELPER`. Nothing
  limits CRTCs or connectors. Nothing to change.
- **drm_hwcomposer.** `ResourceManager::UpdateFrontendDisplays()` walks *every* connector and
  binds each connected one; `DrmHwc::BindDisplay()` hands the second one `++last_display_handle_`
  and raises a hotplug event. The primary is always reported `INTERNAL`
  (`HwcDisplay::GetDisplayType`, "otherwise SF will be unhappy"), any further one `EXTERNAL`.
  Nothing to change.
- **DisplayManager.** `enable_display_content_mode_management` is **ENABLED** in `trunk_staging`
  and in the built image (`aconfig dump` on `all_aconfig_declarations.pb`). With it, an external
  display gets `FLAG_ALLOWS_CONTENT_MODE_SWITCH` (`LocalDisplayAdapter.java:843`) and
  `canHostTasks = !MIRROR_BUILT_IN_DISPLAY`, whose Secure default is `0` — **extended, not
  mirrored, by default.** Nothing to change.
- **SystemUI.** A newly connected external display is disabled pending consent
  (`ExternalDisplayPolicy.shouldAutoEnable`), *unless* SystemUI decides otherwise.
  `ConnectingDisplayViewModel.handleNewPendingDisplay` computes
  `isInExtendedMode = isDesktopModeSupportedOnDisplay(DEFAULT_DISPLAY)`, which is **true** on this
  device (`config_isDesktopModeSupported`, `config_canInternalDisplayHostDesktops` in the
  product overlay) and takes the `enableForDesktop()` + toast branch. **No dialog, no sysprop.**
  Nothing to change.
- **WindowManager.** `DisplayContent.updateContentMode()` turns system decorations on when
  `canHostTasks()`; `DesktopDisplayEventHandler.onDisplayAdded` calls
  `updateExternalDisplayWindowingMode`, which sets **FREEFORM** on any external display where
  desktop mode is supported — so the second monitor is a freeform desktop at hotplug without a
  `display_settings.xml` entry. `isEligibleForDesktopMode()` then passes. Nothing to change.

**Then WindowManager asks for a home activity on the new display, and there is none.**
`RootWindowContainer.canStartHomeOnDisplayArea` refuses the primary home on a secondary display
when its `launchMode` is `singleTask` or `singleInstance` — ours is `singleTask`. It falls back to
`resolveSecondaryHomeActivity`, which looks for `CATEGORY_SECONDARY_HOME` **in the primary home's
package** — pclauncher declares none — and then to `config_secondaryHomePackage`, which is
`com.android.launcher3`, and **pclauncher's `Android.bp` `overrides` Launcher3 out of the image.**
Result: the second monitor comes up as an extended, freeform, decorated desktop with **no home on
it** — wallpaper and nothing else. This is exactly how Launcher3 solves it upstream: a separate
`SecondaryDisplayLauncher` activity, `launchMode="singleTop"`, category `SECONDARY_HOME`
(`AndroidManifest-common.xml:163-175`).

**And the bar would be on the wrong screen even if home appeared.** `ShellOverlayService` builds
one `ComposeOverlayWindow(this, …)`, whose `WindowManager` comes from the service's own context,
i.e. `DEFAULT_DISPLAY`. `HomeActivity` then hides its own bar whenever the overlay reports up
(`chromeHostFor(hasPermission, overlayRunning)`), and `isChromeUp` is one global flow. A home on
display 2 would see `true`, hide its bar, and get no overlay on its display either: a desktop with
no bar. The existing `ChromeHost` decision is the right seam; it lacks one input — *which display*.

**Per-display overlay windows are deliberately not in this slice.** They would need
`createWindowContext(display, TYPE_APPLICATION_OVERLAY, …)` per display, a
`DisplayManager.DisplayListener` in the service, a per-display bar map, and per-display focus
discipline on top of `overlay-service.md`'s. The activity already knows how to host the chrome
itself (`ChromeHost.HomeActivity` is today's behaviour without the permission), which puts a full
bar on display 2 with no new window type and no new lifecycle. The cost is the one
`overlay-service.md` records — the bar can sit behind an app window on that display — and it is
the right trade for a first slice.

## Requirement

1. **Every connected display that the platform enables for desktop gets pclauncher as its home.**
   pclauncher declares an activity with category `android.intent.category.SECONDARY_HOME` and
   `launchMode="singleTop"`, in the same package, so `resolveSecondaryHomeActivity` finds it in the
   first place it looks. The primary `HomeActivity` keeps `singleTask`.
2. **The secondary home is the same desktop, not a cut-down one.** Same wallpaper tone logic, same
   icon grid, same dock, Start menu and tray. It is the existing `HomeActivity` content on another
   display; the only differences are the two below.
3. **The bar is hosted by the activity on any display that is not the default.**
   `chromeHostFor` gains an `onDefaultDisplay: Boolean` input and returns `ChromeHost.HomeActivity`
   whenever it is `false`, regardless of permission or the overlay's state. The decision stays pure
   and tested.
4. **A non-default home never starts, stops or toggles the overlay service.** It is display 0's.
   `ShellOverlayService.start()` is called only from a home on `DEFAULT_DISPLAY`; the
   `onOverlayStartToggle` path is a no-op elsewhere, since the activity hosts its own Start.
5. **Which display an activity is on is read at runtime, not configured.** `Activity.getDisplay()`
   against `Display.DEFAULT_DISPLAY` (API 30+, `minSdk` is 31). No port numbers, no
   `display_settings.xml` entries, no per-board anything — the same rule the platform port lives by.
6. **Removing the display removes its desktop cleanly.** WindowManager destroys the task; the
   activity must not hold anything that outlives it or reaches back to display 0 on the way out
   (`onDestroy` must not stop the overlay service).
7. **The primary display is unchanged.** With one display connected, behaviour is byte-for-byte
   today's: overlay-hosted bar when permitted, activity-hosted otherwise, `singleTask` home.
8. **Nothing in Stage A requires Stage B.** The manifest entry, the class and the decision change
   build and test in Android Studio; the device merely stops needing Launcher3 as a fallback.

## Acceptance criteria

- [ ] `AndroidManifest.xml` declares `SecondaryHomeActivity` with `MAIN` + `SECONDARY_HOME` +
      `DEFAULT`, `launchMode="singleTop"`, `exported="true"`, the same `theme`, `configChanges`
      and `resizeableActivity` as `HomeActivity`. The `main`→`aosp` merge is clean: the two
      manifests differ only in the `package` attribute.
- [ ] `SecondaryHomeActivity` is a subclass of `HomeActivity` that adds nothing but its
      declaration; the per-display behaviour lives in `HomeActivity` keyed on the real display.
- [ ] `chromeHostFor(hasPermission, overlayRunning, onDefaultDisplay)`: the three existing
      `ChromeHostTest` cases still pass with `onDefaultDisplay = true`; a new case proves
      `onDefaultDisplay = false` yields `ChromeHost.HomeActivity` even with
      `hasPermission = true, overlayRunning = true`.
- [ ] `ShellOverlayService.start(...)` is reachable only when `display?.displayId ==
      Display.DEFAULT_DISPLAY`; a unit test on the extracted predicate covers both values.
- [ ] `HomeActivity.onDestroy` does not call `ShellOverlayService.stop` (it does not today; the
      test pins it so a future "tidy up" cannot break display 0 by closing display 1).
- [ ] On the device (Stage B verification, not a Stage A gate): plugging a second monitor into
      the workstation's NVIDIA card logs `drmhwc: Attaching pipeline 'DP-2' to the display #1`,
      `pc-select-egl` is unchanged, SystemUI shows the "connected display" toast and no dialog,
      `dumpsys display` reports the new display `canHostTasks=true`, and the monitor shows the
      pclauncher desktop with a bar, not wallpaper alone. `dumpsys activity activities` shows a
      `SecondaryHomeActivity` task on that display and the `HomeActivity` task still on display 0.
- [ ] Unplugging it: the task is gone, display 0's overlay bar is still up, no crash, no
      `ShellOverlayService` restart in logcat.
- [ ] `./gradlew test lint assembleDebug` green.

## Notes

- **Why not change `HomeActivity` to `singleTop` and add `SECONDARY_HOME` to it?** One class,
  one declaration, and it would work — `canStartHomeOnDisplayArea` only rejects
  `singleTask`/`singleInstance`. But `singleTask` is what keeps the primary home a single, reused
  instance on display 0 across the many `HOME` intents the system fires, and Launcher3 keeps it
  for the same reason while using `singleTop` only for the secondary activity. Mirroring the
  proven shape costs one empty subclass and risks nothing on the display everyone uses.
- **Why the activity hosts the bar rather than a second overlay.** Recorded in Context. A
  follow-up slice, `shell/overlay-per-display.md`, can move the bar into a
  `createWindowContext(display, TYPE_APPLICATION_OVERLAY, null)` window per display once this
  one has shown that a second desktop is stable; `ChromeHost` will then need the display *and*
  that display's overlay state, which is a small extension of the same pure function.
- **The consent dialog is not a concern here, and the reason is worth keeping.** On a device
  whose *internal* display supports desktop mode, SystemUI auto-enables external displays for
  desktop and shows a toast; the dialog exists for phones. Do not add
  `persist.sys.display.enable_on_connect.external` to the product: it is a userdebug-only
  `system_prop`, it is unnecessary here, and it would hide the day the overlay config regresses.
- **`window_geometry` per display (SRS §10)** is already keyed per display in the data model;
  nothing in this slice writes to it. Apps opened from the secondary desktop land on that display
  because that is where the launching activity is, which is the behaviour SRS §7.3 wants before
  "Move to display" exists.
- **Not in this slice:** per-display overlay windows; "Move to display" in the window menu; a
  per-display wallpaper; remembering which display an app last used; DP-MST daisy-chains (they
  are just more connectors to the kernel and need nothing here).
- **Platform facts this relies on, for the `aosp-pc-x86_64` context:** with
  `enable_display_content_mode_management` on, an external display defaults to *extended*; the
  windowing mode is set to FREEFORM at hotplug by `DesktopDisplayEventHandler`; and the
  `config_secondaryHomePackage` fallback is Launcher3, which this product overrides away. All
  three are the reason the second monitor was blank, and none of them needs a platform change.

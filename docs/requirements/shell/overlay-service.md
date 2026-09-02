# Shell — The Chrome Lives in an Overlay Window

Status: **Accepted · In progress** (2026-08-24)

The bar, and everything anchored to it, render into a `TYPE_APPLICATION_OVERLAY` window owned by a
foreground service, so they stay above freeform app windows. SRS §14 phase 4; implements the hosting
model SRS §9 describes. Amends [`dock-taskbar.md`](dock-taskbar.md), which built the bar inside the
home activity.

## Context

Open an app in a freeform window and the shell disappears behind it. The bar, the Start menu and the
tray are drawn by the home activity, and the home activity is a window like any other — it renders
*behind* the desktop windows by design (`config_showHomeBehindDesktop`, SRS §4.1). That is right for
the desktop and wallpaper; it is wrong for a taskbar, whose entire purpose is to be reachable while
you are using something else.

**This does not need AOSP.** `TYPE_APPLICATION_OVERLAY` requires `SYSTEM_ALERT_WINDOW`, a special
permission any ordinary app may hold once the user grants it in Settings. SRS §5.1 already lists
floating the chrome among the things the shell can do **unprivileged**. On `aosp-pc-x86_64` the only
difference is that a platform app can be pre-granted, so the prompt disappears — the same code runs
in both worlds, as with the power actions.

**Compose in a raw window needs its owners attached.** A `ComposeView` outside an activity crashes
on attach unless `setViewTreeLifecycleOwner`, `setViewTreeViewModelStoreOwner` and
`setViewTreeSavedStateRegistryOwner` are all set. That is boilerplate to be written once and never
repeated per surface (SRS §9).

**The shell must not eat keystrokes.** An overlay that is focusable steals every key from the app
the user is actually typing into. The window is `FLAG_NOT_FOCUSABLE` at rest and becomes focusable
only while a menu is open, reverting the moment it closes.

**Two bars would be worse than none.** While the overlay is up the home activity must not draw its
own copy, and while it is not, it must — the desktop cannot be left with no bar at all if the
permission is refused or the service is killed (GATE 4).

**Dragging between the dock and the desktop will stop working.** They become separate windows, and a
Compose drag cannot cross a window boundary. Dock-internal drags are unaffected; dragging a dock
icon onto the desktop, and a desktop icon onto the bar to pin it, both need Android's cross-window
drag-and-drop and are out of scope here. This is a real regression against
[`pinning.md`](pinning.md) and is called out rather than discovered later — pinning by context menu,
which is the same store operation, continues to work.

## Requirement

1. **A foreground service owns the chrome window**, restartable, with a low-importance notification.
2. **The bar, Start menu and tray render in that window** and appear above freeform app windows.
3. **The window is not focusable at rest** and becomes focusable only while the Start menu or a
   popover is open, reverting immediately after.
4. **Exactly one bar is visible**: the home activity draws its own only while the overlay is not
   running.
5. **Without the permission the shell still works** — the chrome stays in the home activity, as
   today, and nothing crashes or nags.
6. **The permission is asked for on first run**, once, with a plain explanation of what it buys
   (SRS §7.1 step 3). A refusal is remembered and respected.
7. **The service rebuilds its window after being killed.**
8. **The Compose owners are attached in one place** and not repeated per surface.
9. **The shell's state is shared, not duplicated** — the same inventory, pins and layout feed both
   hosts, so they cannot disagree.

## Acceptance criteria

- [ ] With the permission granted, opening an app in a freeform window leaves the bar visible above
      it.
- [ ] The Start menu and tray popover open above that window too.
- [ ] Typing into a focused app is unaffected while the shell is idle.
- [ ] Exactly one bar is on screen at all times.
- [ ] With the permission refused, the shell behaves as it does today.
- [ ] Killing the service brings the window back.
- [ ] Pin state stays consistent between the overlay's dock and the desktop.
- [ ] `./gradlew test lint assembleDebug` green; checked on device in both permission states.

## Notes

- **Why a foreground service rather than the activity holding the window.** An overlay owned by the
  home activity dies with it, and the home activity is restarted routinely. The bar has to outlive
  it.
- **Not in this slice:** the menu bar along the top (SRS §6.2), cross-window drag and drop, or
  auto-hide.

# Software Requirements Specification — "pclauncher"

The desktop shell for **Android 17 on a bare-metal x86_64 PC**, built in Jetpack Compose.

> **Audience:** Claude Code. This document is the single source of truth for *what* to build.
> **Instruction to Claude Code:** build phase by phase (§14). Where a detail is unspecified, take
> the default in §13 rather than asking. Keep the code idiomatic Kotlin + Compose. No code is
> written before a requirement doc exists for the slice (`docs/requirements/`, GATE 1).

---

## 1. Product Summary

**pclauncher** is the home screen for a PC running Android. It replaces **Launcher3QuickStep** as
the default launcher of the [`aosp-pc-x86_64`](../../aosp-pc-x86_64/README.md) build — Android 17
on real x86_64 hardware with a keyboard, a mouse, and a 2560×1600 display.

Launcher3 is a phone launcher wearing a tablet costume. On a PC it is the wrong product: a page of
icons, a dock built for thumbs, and a recents carousel. pclauncher is what that machine actually
needs — a **wallpapered desktop** with icons and widgets, a **global menu bar** across the top, a
**dock and taskbar** across the bottom, a **Start menu**, and a **Spotlight-style command palette**.
Apps live in resizable freeform windows; you move between them by clicking the dock, clicking the
taskbar, Alt+Tab, or typing.

The visual language is a deliberate hybrid: the *shape and motion* come from macOS (thin translucent
menu bar, centred magnifying dock, Spotlight, Mission Control); the *organisation* comes from
Windows 11 (Start button, searchable pinned/all-apps menu, taskbar window list, system tray, snap
layouts). It must feel like neither a phone launcher nor a copy of either desktop.

**What this is not.** pclauncher does not emulate or run desktop software, and it does not
reimplement Android's window manager. Android 17's desktop windowing already places, decorates,
drags, resizes, and snaps windows on this device (§4). pclauncher is the **shell**: it owns the
desktop, decides what launches and where, and is how you get back.

---

## 2. Two Stages — build order for the whole project

The end state is a platform app inside an AOSP tree. Getting there through AOSP builds from day one
would be intolerable: a full build is hours, the device renders in software, and the UI is the part
that needs the most iteration. So the work splits in two, and **this SRS is written so Stage A never
blocks on Stage B**.

### Stage A — the app, in Android Studio (now)

A standalone, installable APK developed in **Android Studio** against a normal SDK, run on an
x86_64 emulator and any stock Android 12+ device. All UI, all interaction, all state, all logic.
Everything that needs platform privilege sits behind an interface with a working unprivileged
fallback (§5), so the whole shell is usable and demoable without touching AOSP.

**Nothing in Stage A may require the AOSP tree, a platform signature, or a device build.**

### Stage B — integration into `aosp-pc-x86_64` (later, its own requirement docs)

The same source, built in-tree as the product's home app: an `Android.bp`, platform signing, a
privileged-permissions allowlist, `PRODUCT_PACKAGES` in `device/pcx86/pc_x86_64/device.mk`, and
removal of `Launcher3QuickStep`. Stage B is deliberately deferred and explicitly out of scope for
every phase before §14 phase 10 — but Stage A's architecture must not make it hard (§5.2, §9).

---

## 3. Tech Stack (fixed — do not substitute)

| Concern | Choice |
|---|---|
| Language | Kotlin (latest stable 2.x), JDK 17 |
| UI | **Jetpack Compose**. Material 3 as a base only, restyled per §6. `androidx.compose:compose-bom` |
| IDE | **Android Studio** is the primary development environment (Stage A). The build must *also* work from the CLI via the Gradle wrapper — CI and Stage B need it |
| Build | Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`), AGP 8.x |
| SDK | `compileSdk`/`targetSdk` = **37** (Android 17, `platforms;android-37.0`) — the same platform level the `pc_x86_64` build ships, so Stage A compiles against the Stage B target from day one. `minSdk 31` |
| Architecture | Feature-first Gradle modules, unidirectional data flow, Compose state hoisting |
| DI | Hilt |
| Async | Coroutines + `Flow`. Nothing blocking on the main thread |
| Local state | DataStore (Proto) for settings, layout, pins, window geometry |
| App inventory | `LauncherApps` + `UserManager` (work-profile aware), **not** raw `PackageManager` scans |
| Widgets | `AppWidgetHost` / `AppWidgetHostView` hosted in Compose via `AndroidView` |
| Files | Storage Access Framework + `MediaStore`. **No** `MANAGE_EXTERNAL_STORAGE` |
| Privilege (Stage A dev only) | **Shizuku** (`dev.rikka.shizuku:api`), optional — a stand-in on stock devices for what Stage B gets natively |
| Testing | JUnit + Turbine for logic; `createComposeRule` for UI; Robolectric for framework-adjacent units |

**No network.** v1 declares no `INTERNET` permission. No accounts, no telemetry, no analytics, no
ads. A later feature that needs the network must argue for it in its own requirement doc.

---

## 4. Target Platform — `aosp-pc-x86_64`

Read `~/amar/aosp-pc-x86_64/README.md` and `android_17/device/pcx86/pc_x86_64/` before designing
anything windowing-related. The relevant facts, as of this writing:

| | |
|---|---|
| Platform | AOSP `android17-release` (**API 37**), product `pc_x86_64`, `PRODUCT_CHARACTERISTICS := tablet` |
| Hardware | Bare-metal Intel Meteor Lake, eDP-1 @ 60 Hz, 2560×1600, keyboard + mouse |
| Current home app | `Launcher3QuickStep` (via `handheld_system_ext.mk`) — **pclauncher replaces this** |
| Rendering | ⚠️ **Software only** — SwiftShader + ANGLE. No Mesa yet. The UI is measurably sluggish |
| Density | `ro.sf.lcd_density=240`, chosen *only* to keep the panel under `sw600dp` so Launcher3 does not take its taskbar path and crash-loop |
| Lock screen | Disabled (`ro.lockscreen.disable.default=true`) |
| SELinux | Permissive; AVB disabled; static partitions |
| adb over TCP | Currently refused, though `adbd` listens — Stage B deployment must account for this |

### 4.1 Desktop windowing is already on

The device target enables it deliberately, and this is the single most important thing to design
around — **the platform does the window management, pclauncher does not**:

- `config_isDesktopModeSupported`, `config_canInternalDisplayHostDesktops`,
  `config_isDesktopModeDevOptionSupported`, `config_enterDesktopByDefaultOnFreeformDisplay` — all
  `true` in `device/pcx86/pc_x86_64/overlay/`.
- `android.software.freeform_window_management` declared in `/vendor/etc/permissions/`.
- `display_settings.xml` puts the built-in panel in `windowingMode="5"` (`WINDOWING_MODE_FREEFORM`),
  which is what makes the device *enter* desktop mode rather than merely support it.
- `large_screen_common.mk` supplies window extensions.

So on the target device **WM Shell already draws caption bars and handles drag, resize, and snap.**
pclauncher must **complement** that, never duplicate or fight it.

- `config_showHomeBehindDesktop` is `true` — the home app renders *behind* the desktop windows.
  That is exactly the surface a desktop needs, and it is already ours.
- **Open decision (Stage B, own requirement doc):** whether pclauncher's dock/taskbar replaces WM
  Shell's desktop taskbar or coexists with it. Two bars is not shippable. Default assumption:
  suppress the Shell taskbar by device overlay once pclauncher's is proven. Do not design Stage A
  around either answer — the shell chrome must be independently positionable and toggleable.

### 4.2 What pclauncher unblocks on the device

The 240 dpi density hack exists solely because Launcher3's large-screen taskbar path NPEs on this
device. pclauncher has no such path. Once it is the home app, the device can move to a real
large-screen density and the workaround can be retired — this is a **stated goal of Stage B**, and
pclauncher must therefore be correct at `sw800dp`+ and at 160 dpi, not merely at the current 240.

### 4.3 Software rendering is a hard design constraint

Everything renders on the CPU through SwiftShader until Mesa lands. This directly constrains §6:

- **Backdrop blur is opt-in, never the default.** `RenderEffect`/`BlurEffect` on a full-width menu
  bar and dock is a per-frame full-screen CPU blur. Default to a **cheap translucent scrim** with a
  hairline border, which reads almost identically at these sizes. Blur is a Settings toggle that is
  **off** on a software renderer and detected, not assumed.
- Minimise overdraw: no stacked translucent layers, no large alpha-composited shadows. Prefer
  hairlines and flat surfaces to elevation.
- Animate transforms and alpha only — never layout, never shape reallocation, per frame.
- Every animation must degrade: a `reducedMotion`/`softwareRenderer` mode shortens or drops dock
  magnification, Mission Control transitions, and menu springs.
- Budgets in §12 are measured **on the target device**, not on a fast phone.

---

## 5. Platform Reality & Capability Tiers

**A third-party app cannot draw, move, or resize another app's window on stock Android.** On the
target device the platform does it for us (§4.1); on a Stage A development device it may not. The
same code must work in both worlds, so every window operation goes through one abstraction.

### 5.1 What the shell can do unprivileged

- **Choose launch bounds** — `ActivityOptions.setLaunchBounds(Rect)`, public API since API 24. When
  freeform is active, launching with bounds produces a freeform window there. *This is the primary
  lever:* placement, cascade, tiling, and snap are all expressed as bounds at launch time.
- **Float its own chrome above everything** — menu bar, dock, and taskbar render into
  `TYPE_APPLICATION_OVERLAY` windows owned by a foreground service (`SYSTEM_ALERT_WINDOW`).
- **Be the home app** — `CATEGORY_HOME` + `RoleManager.ROLE_HOME`, owning the desktop and HOME key.
- **Read what is installed and recently used** — `LauncherApps`, plus `UsageStatsManager`.

### 5.2 What needs privilege

Enabling freeform where it is off; `setLaunchWindowingMode(WINDOWING_MODE_FREEFORM)` (hidden API);
the real running-task list (`REAL_GET_TASKS`); moving or resizing an already-open window; task
thumbnails. On the target device these come from being a **platform app**. In Stage A they come
from **Shizuku**, or not at all.

### 5.3 The tiers

Tier is **detected at runtime**, never assumed, and shown in Settings in plain language: what is
unavailable, and why. The shell must run and never dead-end at any tier.

| Tier | Where | Windowing behaviour |
|---|---|---|
| **T0 — Basic** | Stock device/emulator without freeform | Apps launch fullscreen or into split-screen. Full shell otherwise: desktop, dock, taskbar, Start, palette, tray, widgets. |
| **T1 — Freeform** | Freeform available (dev options, or a large-screen Android 16+ device) | Apps launch into freeform windows at bounds we choose; the system decorates them. Snap and tiling act at launch/relaunch. |
| **T2 — Privileged** | Shizuku connected (Stage A), **or the platform app on `pc_x86_64` (Stage B — the real target)** | Everything in T1, plus live move/resize, real window list, task thumbnails, drag-to-snap on existing windows, freeform enablement. |

**Design rule:** one `WindowBackend` interface, three implementations. No feature calls a hidden
API, writes `Settings.Global`, or talks to Shizuku directly. Stage B adds a fourth *provider* behind
the same interface and changes nothing above it — that is the whole point of the abstraction.

### 5.4 Constraints to design around, not fight

- **Global hotkeys do not exist.** Key events reach pclauncher only while one of its windows has
  focus, and Meta is often swallowed. Mitigation: an on-screen Start button, a focusable dock hot
  zone, and `Ctrl+Esc` as the documented fallback. **Never** ship an accessibility service purely to
  grab keys. (Stage B may revisit this with platform privilege — its own doc.)
- **Overlays cannot reserve screen space.** A freeform window can sit under the dock. All launch
  bounds are computed inside a **work area** = display bounds − menu bar − dock − system insets.
- **Android apps expose no menus.** The macOS-style menu bar carries *window and system* menus, never
  a fabricated reconstruction of the app's own menus (§6.2).
- **Not every app is resizable.** Detect `resizeableActivity=false` and fixed orientation from
  `ActivityInfo`; mark those apps and launch them fullscreen rather than into a broken window.
- **`getRunningTasks` returns one entry** below T2. The T0/T1 taskbar is built from our own launch
  bookkeeping plus `UsageStatsManager`, and is honestly labelled a *recent windows* list.

---

## 6. UI Design Language — "Mac shape, Windows organisation"

### 6.1 Principles

1. **Chrome is thin and quiet.** Menu bar ≤ 28 dp, dock ≤ 64 dp at rest. The user's apps are the
   content; the shell is furniture.
2. **Layered and translucent — cheaply.** Translucent scrims with hairline borders over the
   wallpaper. Blur only where the renderer can afford it (§4.3).
3. **Pointer-first, touch-tolerant.** Hover states, right-click menus, drag targets — with every hit
   target ≥ 44 dp and a long-press equivalent for every hover affordance.
4. **Motion is macOS.** Spring-based, short (150–250 ms), spatial: the Start menu grows from the
   Start button, the palette drops from the top, dock icons magnify under the pointer. All of it
   degradable (§4.3).
5. **One accent, honest state.** A single user-selectable accent. Running / focused / needs-attention
   are distinguished by shape, not colour alone.
6. **Light and dark both first-class**, following the system theme with a manual override.

### 6.2 Menu bar (top) — macOS

Full width, fixed to the top edge, translucent, always on top, `FLAG_NOT_FOCUSABLE` until a menu
opens.

- **Left:** the pclauncher glyph (About, Settings, Sleep, Restart shell), then the **focused
  window's app name in semibold**, then launcher-owned menus:
  - **Window** — Minimise, Maximise, Close, snap layouts, Move to display
  - **View** — Show Desktop, Mission Control, show/hide desktop icons, wallpaper
  - **Help** — shortcut cheat sheet, capability-tier explainer
- **Right — the system tray** (Windows organisation, mac styling): notifications, volume, Wi-Fi,
  Bluetooth, battery, then clock + date. Each opens a small popover. One grouped control, not
  scattered icons.

### 6.3 Dock + taskbar (bottom) — one bar, both heritages

A single translucent bar, floating with a margin and rounded corners (mac), laid out left-to-right
(Windows):

1. **Start button** (⊞) at the left of the bar.
2. **Dock** — centred pinned apps. Magnification on hover with a spring curve, bounce on launch, a
   **dot** under running apps, drag to reorder, drag out to unpin, right-click menu (Open, New
   Window, Pin/Unpin, App info, Close all).
3. **Separator**, then the **taskbar window list** — one chip per open window (icon + truncated
   title); the focused chip is filled, others outlined. Hover shows a preview card with the title,
   app, and a snap-layout picker — a live thumbnail only at T2. Click to focus, middle-click to
   close.
4. **Right end:** Show Desktop hot corner.

Always visible by default; auto-hide with edge reveal is a setting. Position (bottom / left / right)
is a setting; bottom is the default.

### 6.4 Start menu — Windows 11

Springs open from the Start button, translucent, keyboard-driven:

- **Search field** at the top, focused on open, typing straight into fuzzy app search.
- **Pinned** — a grid, drag to rearrange, right-click to unpin.
- **All apps** — alphabetical with a fast index rail, a work-profile section, and a filter.
- **Recommended** — recently used and recently installed.
- **Footer** — device name, Settings, Power (sleep/lock, restart shell).

Esc closes; ↑/↓/Enter navigate and launch; typing anywhere filters.

### 6.5 Command palette — Spotlight

A centred translucent card, invoked from the dock search affordance, the Start search field, or
`Ctrl+Space` / `Meta+Space` while a launcher window is focused (§5.4).

- Fuzzy-matches apps, settings pages, open windows, shortcuts, and pinned files.
- Ranks by match quality then usage recency; the top hit is shown large with a preview line.
- Enter launches the top hit; ↑/↓ moves; Tab completes; Esc dismisses.
- Extensible via a `PaletteProvider` interface, so new result kinds arrive without touching the UI.

### 6.6 Desktop

Wallpaper via `WallpaperManager` (live wallpapers supported), rendered *behind* the desktop windows
(§4.1), with:

- **Icons** on a snap grid: drag to arrange, double-click to open (single tap on touch), rubber-band
  multi-select, right-click menu, folders.
- **Widgets** — Android app widgets on the same grid, freely placed and resized within each
  widget's min/max cells.
- **Right-click on empty desktop** — New Folder, Change Wallpaper, Display Settings, Sort Icons, Add
  Widget, Show Desktop Icons.
- **Touch-first mode** when no pointer is present: single-tap opens, long-press replaces hover, the
  dock does not magnify.

### 6.7 Mission Control

`F3`, a dock button, or a three-finger swipe: dims the desktop and lays every open window out as
cards. Live thumbnails at T2; large icon + title + app name below it. Click a card to focus, ✕ to
close, drag onto a snap zone to place.

---

## 7. Core Flows

### 7.1 First run
1. **Welcome** — what pclauncher is, what it needs, and the capability tier it detected, in honest
   language.
2. **Set as default home** via `RoleManager.ROLE_HOME` (fallback: deep-link to the setting). On the
   target device this is already true by construction.
3. **Grant "Display over other apps"** — explained as "so the dock and menu bar stay visible while
   you use apps". Without it the shell appears only on the desktop, and the app says so plainly
   once rather than nagging.
4. **Optional: usage access** — "so recent apps are ordered sensibly".
5. **Optional: Shizuku** (Stage A only) — detected, never required. One screen explaining exactly
   what T2 adds, then continue at the detected tier.
6. Wallpaper + accent + light/dark → the desktop.

### 7.2 Launching an app
1. User activates an app (dock, Start, desktop icon, palette).
2. `LaunchPolicy` resolves a windowing mode for the current tier and a bounds rect from (a) the
   app's remembered geometry, else (b) the default new-window rect, cascaded so it does not land
   exactly on an existing window — clamped to the work area (§5.4).
3. `WindowBackend` launches with those options. The dock icon bounces; a taskbar chip appears.
4. A non-resizable app launches fullscreen instead, with a one-time inline note on its chip saying
   why.
5. Failures (app disabled, uninstalled mid-flight, permission revoked) surface a short actionable
   message — never a stack trace, never silence.

### 7.3 Working with windows
- **Focus** — click a taskbar chip, dock icon, or Mission Control card → `moveTaskToFront`.
- **Snap** — from the taskbar hover card, the Window menu, or `Meta+←/→/↑/↓`. Layouts: half L/R,
  quarters, thirds, two-thirds + third, maximise. At T2, dragging to a screen edge shows the
  snap-layout flyout; at T0/T1 snapping applies at launch/relaunch and the UI says so.
- **Close / minimise** — from the chip, the Window menu, or Mission Control.
- **Geometry memory** — per app, per display: last size and position, reused on next launch.

### 7.4 Returning to the desktop
HOME focuses the shell. `Meta+D` or the Show Desktop corner reveals the desktop. The shell is always
one action away from anywhere.

### 7.5 Settings
A windowed Settings surface inside the launcher: **Appearance** (wallpaper, accent, theme, dock
position/size/auto-hide, icon size, grid, blur toggle), **Windows** (default size, cascade, snap
behaviour, geometry memory), **Start & Search** (pins, recommended, palette providers), **System**
(tier status, permissions, Shizuku, restart shell), **About**.

---

## 8. Keyboard & Pointer

Shortcuts apply while a pclauncher window has focus (§5.4). All are listed in the Help cheat sheet
and are **rebindable** in Settings.

| Action | Default |
|---|---|
| Start menu | `Meta` (when delivered) / `Ctrl+Esc` |
| Command palette | `Meta+Space` / `Ctrl+Space` |
| Show desktop | `Meta+D` |
| Mission Control | `F3` |
| Snap focused window | `Meta+←/→/↑/↓` |
| Close focused window | `Meta+W` (`Alt+F4` accepted) |
| Cycle windows | `Alt+Tab` — the platform may own this in desktop mode; do not fight it |
| Settings | `Meta+,` |

Pointer: left-click activates; right-click opens a context menu everywhere it makes sense;
middle-click closes taskbar chips; scroll pans the desktop grid; drag-and-drop moves icons and
widgets; `Ctrl`+scroll changes desktop icon size.

---

## 9. Architecture

Feature-first Gradle modules. Nothing depends on `app` but itself.

```
app/                     application, DI graph, HOME activity, overlay service wiring
core/design/             theme, tokens, translucency, dock & window primitives, motion specs
core/data/               DataStore schemas: settings, pins, layout, window geometry
core/apps/               LauncherApps inventory, icons, work profiles, usage/recency
feature/desktop/         wallpaper, icon grid, folders, marquee, widget host
feature/shell/           menu bar, dock, taskbar, Start menu, tray popovers  (overlay windows)
feature/search/          command palette + PaletteProvider implementations
feature/windows/         WindowBackend (T0/T1/T2), LaunchPolicy, snap geometry, Mission Control
feature/settings/        settings UI
platform/privileged/     capability detection, Shizuku provider, hidden-API shims
                         (Stage B adds a platform provider here and nothing else changes)
```

**Two rendering hosts, one design system:**

- The **HOME activity** renders the desktop — wallpaper, icons, widgets — in Compose, behind the
  desktop windows (§4.1).
- The **shell chrome** — menu bar, dock/taskbar, Start, palette, tray — is rendered by a
  **foreground service** into `TYPE_APPLICATION_OVERLAY` windows so it survives above other apps.
  - Each overlay hosts a `ComposeView`. The service **must** attach `setViewTreeLifecycleOwner`,
    `setViewTreeViewModelStoreOwner`, and `setViewTreeSavedStateRegistryOwner`, or Compose crashes
    on attach. Wrap this once in a `ComposeOverlayWindow` helper; never repeat it per surface.
  - Overlays are `FLAG_NOT_FOCUSABLE` at rest and become focusable *only* while a menu, Start, or
    the palette is open, reverting immediately. The shell must never eat the user's keystrokes.
  - The service is bound, restartable, foreground, with a low-importance notification, and it
    rebuilds its windows after being killed.

**State:** one `ShellState` flow (tier, focused window, open windows, pins, chrome visibility) is
the single source of truth for every surface. No surface owns state another surface reads.

**Stage B seam:** the only Stage-B-aware code is `platform/privileged/` and the build files. If a
Stage B change would touch `feature/` or `core/`, the abstraction is wrong — fix the abstraction.

---

## 10. Data Model (DataStore Proto)

| Store | Contents |
|---|---|
| `settings` | theme, accent, wallpaper ref, dock position/size/auto-hide, icon size, grid density, blur, touch-first override, rebound shortcuts |
| `pins` | ordered dock pins and Start pins (component + user handle) |
| `desktop_layout` | icon and folder grid positions; widget id, cell rect, provider |
| `window_geometry` | per (component, user, display) last bounds + windowing mode |
| `usage` | our own launch counts and recency, used when usage access is not granted |

Every store is versioned with an explicit migration path. A corrupt store resets **that store alone**
to defaults — never the whole app, never a crash loop on boot.

---

## 11. Permissions

| Permission | Why | When |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | dock/menu bar over apps | first run, explained |
| `PACKAGE_USAGE_STATS` | recency ordering | first run, optional |
| `BIND_APPWIDGET` / `ACTION_APPWIDGET_BIND` | host widgets | when adding the first widget |
| `QUERY_ALL_PACKAGES` | app inventory | manifest; justified as a launcher |
| `FOREGROUND_SERVICE` (+ `SPECIAL_USE`) | shell service | manifest |
| `POST_NOTIFICATIONS` | shell service notification | first run |
| Shizuku | T2 in Stage A | never required; offered once |
| **`INTERNET`** | — | **not declared** |

Stage B replaces the Shizuku row with a privileged-permission allowlist entry; the runtime prompts
above become no-ops on the target device. Every runtime prompt is preceded by a plain-language
screen saying what breaks without it. A denial is remembered and respected: degrade, state the
consequence once, never re-prompt automatically.

---

## 12. Non-Functional Requirements

- **Boot safety is the top priority.** This app *is* the home screen — a crash on launch bricks the
  device's UI. The HOME activity must reach a usable desktop even if every store is corrupt, the
  shell service is dead, and no permission is granted. A guarded fallback desktop (wallpaper + app
  list + "reset shell") exists and is tested.
- **Cold start to interactive desktop ≤ 500 ms** on a normal device, and **≤ 1.5 s on `pc_x86_64`
  under software rendering**. Wallpaper and grid render before the inventory finishes loading —
  skeletons, never a blank screen.
- **60 fps on a hardware renderer; no dropped frames beyond the software-rendering floor on
  `pc_x86_64`.** Dock magnification, Start open, palette open, and Mission Control are traced on the
  target device, and the degradation path (§4.3) is what makes them acceptable there.
- **Inventory loads off the main thread**, is cached, and updates incrementally from
  `LauncherApps.Callback` — never a full rescan on a package change.
- **Icons cached** in memory and on disk, keyed by component + user + density.
- **Idle cost near zero.** No polling; tray data comes from broadcasts and callbacks. Shell service
  idle CPU ≈ 0%, flat memory across a day, verified by a soak test.
- **Correct at every density and size.** Must be right at `sw800dp` and 160 dpi, not only at the
  current 240 dpi workaround (§4.2).
- **Accessibility:** full TalkBack labels on every shell control, focus order matching visual order,
  keyboard-only operation of the entire shell, text contrast ≥ 4.5:1 on translucent surfaces in both
  themes.
- **Tests (GATE 2):** every non-UI unit tested. `WindowBackend`, `LaunchPolicy`, snap geometry,
  fuzzy ranking, and grid placement are pure and exhaustively tested. Tier behaviour is tested with
  fake backends at T0/T1/T2 — CI never needs a privileged device.

---

## 13. Defaults for Unspecified Items (use these; do not block)

- **App id:** `com.somalapuram.pclauncher`. **Name:** "pclauncher" (placeholder, renameable).
- **Language:** English only; all strings in resources, ready for localisation.
- **Dock:** bottom, always visible, 56 dp, centred, magnification on, blur off.
- **New window:** 60% × 70% of the work area, centred, cascading 32 dp per window.
- **Snap layouts:** halves, quarters, and 1⁄3 + 2⁄3 columns.
- **Desktop grid:** 96 dp cells, icons top-left, sorted by name on first run.
- **Theme:** follow system; accent from wallpaper with a manual override.
- **Max tracked windows:** 24; the taskbar scrolls beyond that.
- **Stage A test targets:** an x86_64 AVD on `system-images;android-37.0;google_apis;x86_64` at
  2560×1600 / 240 dpi with freeform enabled in developer options — same platform level and panel as
  `pc_x86_64`. Secondary: the `android-desktop` AVD (`system-images;android-34;android-desktop;x86_64`),
  which boots a real desktop-windowing environment and is the closest emulator analogue to the
  target's WM Shell behaviour.
- **Distribution:** Stage A — debug APK from Android Studio / the wrapper. Stage B — built in-tree.
  No Play Store.

---

## 14. Build Phases (implement in order; each phase must run)

| # | Phase | Scope |
|---|---|---|
| 1 | **Foundation** | Gradle + version catalog, module skeleton, Hilt, Compose + design tokens, HOME activity owning `ROLE_HOME`, green test harness, APK from Android Studio *and* the CLI wrapper |
| 2 | **App inventory** | `LauncherApps` inventory, icon cache, work profiles, incremental updates, usage/recency — headless and fully tested |
| 3 | **Capability tiers** | `WindowBackend` interface + T0 impl, runtime tier detection, the Settings "System" page telling the user their tier honestly |
| 4 | **Shell chrome** | Overlay foreground service, `ComposeOverlayWindow`, menu bar + dock with magnification and running dots, taskbar chips, Show Desktop |
| 5 | **Start + palette** | Start menu (pinned / all apps / recommended / search), command palette with `PaletteProvider`, fuzzy ranking, keyboard navigation |
| 6 | **Freeform windowing** | T1 backend (launch bounds, work area, cascade, geometry memory), snap layouts, non-resizable app handling, the Window menu |
| 7 | **Desktop** | Wallpaper, icon grid, drag/marquee/folders, context menus, widget host, SAF file entries |
| 8 | **Tray + Mission Control** | Clock, battery, volume, Wi-Fi, Bluetooth, notification popovers; window overview |
| 9 | **Privileged tier** | Shizuku provider, real window list, live move/resize, drag-to-snap, thumbnails — all behind T2 detection |
| 10 | **Stage B — AOSP integration** | `Android.bp`, platform signing, privapp-permissions, `PRODUCT_PACKAGES` in `device/pcx86/pc_x86_64/device.mk`, drop `Launcher3QuickStep`, resolve the taskbar-coexistence decision (§4.1), retire the density workaround (§4.2), tune for software rendering on real hardware |
| 11 | **Polish** | Motion pass, accessibility pass, soak + startup budgets on the target device, boot-safety fallback, first-run flow, settings completeness |

**Definition of done per phase (1–9):** builds from Android Studio *and* the CLI; installs and runs
as the home app on an Android 12+ device or the x86_64 emulator; the happy path is demonstrated;
every state (loading, empty, error, denied permission, wrong tier) is designed and reachable;
`./gradlew test` and `lint` are green; **no phase leaves a device without a working home screen.**

---

## 15. Out of Scope (v1)

Running desktop or Linux software; x86 app emulation; writing a window manager (the platform has
one); multi-device sync; cloud accounts; a plugin API or theming engine; multi-user beyond work
profiles; TV / Wear / Auto form factors; a full file *manager* beyond SAF-backed shortcuts; a
built-in terminal; Play Store distribution.

Explicitly **deferred, not excluded:** everything in Stage B (§2) before phase 10 — AOSP tree
changes, platform signing, SELinux policy, device makefile edits, and the Mesa/graphics work that
`aosp-pc-x86_64` tracks separately.

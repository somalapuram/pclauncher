# Requirements

Every change starts here. **No code is written before a requirement doc for it exists**
(CODING-GUIDELINES §7 / CLAUDE.md GATE 1).

The product spec is [`../SRS-pclauncher.md`](../SRS-pclauncher.md). That SRS is the *source of
truth for what*; the docs below are the **agreed, buildable slices** derived from it — each small
enough to implement, test, and ship on its own, in the order of SRS §14.

## Process

1. A chat instruction (or an SRS line) is *raw input*, not the contract — turn it into a doc first.
2. Each doc states **Context · Requirement · Acceptance criteria · Notes**.
3. Confirm the doc, *then* implement. Code and commits reference the doc by path.
4. **Append-only:** to change a requirement, write a new doc that supersedes the old one
   (mark the old `Superseded by <path>`). The path is the id — name for *what it is*.

## Stage rule (SRS §2)

Docs for phases 1–9 are **Stage A**: a standalone APK in Android Studio. A Stage A doc may not
require the AOSP tree, a platform signature, or a device build. Anything that does belongs in a
`system/` doc under Stage B and waits for phase 10.

## Index — by module

*(No slices written yet. Add a line here as each is sliced.)*

### foundation
- [`foundation/scaffolding.md`](foundation/scaffolding.md) — Gradle + version catalog, module skeleton, Hilt, Compose design
  tokens, HOME activity owning `ROLE_HOME`, green test harness. *Status: Accepted · Implemented.*

### launch
- [`launch/app-inventory.md`](launch/app-inventory.md) — `LauncherApps` inventory keyed by
  (component, user), icon cache, work/private profiles, incremental deltas, usage/recency.
  *Status: Accepted · Implemented.*

### windows
- `windows/capability-tiers.md` — `WindowBackend` interface + T0, runtime tier detection, the
  Settings System page. *Status: Planned.*
- `windows/freeform-launch.md` — T1 backend: launch bounds, work area, cascade, geometry memory,
  snap layouts, non-resizable apps. *Status: Planned.*
- `windows/mission-control.md` — window overview. *Status: Planned.*

### design
- [`design/icon-treatment.md`](design/icon-treatment.md) — squircle tiles with gloss, rim light and
  shadow, baked once into the icon cache; dark-glass and soft-clay palettes by theme.
  *Status: Accepted · Implemented.*

### shell
- `shell/overlay-service.md` — foreground service + `ComposeOverlayWindow`, focus discipline,
  restart recovery. *Status: Planned.*
- `shell/menu-bar.md` — top bar, launcher-owned Window/View/Help menus. *Status: Planned.*
- [`shell/dock-taskbar.md`](shell/dock-taskbar.md) — one bar: Start button, magnifying dock,
  taskbar window chips, Show Desktop. Rendered in the HOME activity first; the overlay service
  hosts the same composables later. *Status: Accepted · Partially implemented.*
- [`shell/start-menu.md`](shell/start-menu.md) — all apps + search, keyboard-first, right-click to
  pin. *Status: Accepted · Partially implemented.*
- [`shell/pinning.md`](shell/pinning.md) — the `pins` store and pin/unpin from every surface that
  lists an app. *Status: Accepted · Implemented.*
- `shell/command-palette.md` — Spotlight palette + `PaletteProvider`. *Status: Planned.*
- `shell/system-tray.md` — clock, battery, volume, Wi-Fi, Bluetooth, notification popovers.
  *Status: Planned.*

### desktop
- `desktop/icon-grid.md` — wallpaper, icon grid, drag, marquee, folders, context menus.
  *Status: Planned.*
- `desktop/widget-host.md` — `AppWidgetHost` inside Compose. *Status: Planned.*

### system
- `system/privileged-provider.md` — Shizuku provider behind the T2 backend. *Status: Planned.*
- `system/aosp-integration.md` — **Stage B.** `Android.bp`, platform signing, privapp-permissions,
  `PRODUCT_PACKAGES` in `device/pcx86/pc_x86_64/device.mk`, dropping `Launcher3QuickStep`, the
  taskbar-coexistence decision, retiring the density workaround. *Status: Planned (blocked until
  phase 10).*

## Build phases (SRS §14) → docs

| Phase | Scope | Requirement docs |
|---|---|---|
| 1 | Foundation | `foundation/scaffolding.md` |
| 2 | App inventory | `launch/app-inventory.md` |
| 3 | Capability tiers | `windows/capability-tiers.md` |
| 4 | Shell chrome | `shell/overlay-service.md`, `shell/menu-bar.md`, `shell/dock-taskbar.md` |
| 5 | Start + palette | `shell/start-menu.md`, `shell/command-palette.md` |
| 6 | Freeform windowing | `windows/freeform-launch.md` |
| 7 | Desktop | `desktop/icon-grid.md`, `desktop/widget-host.md` |
| 8 | Tray + Mission Control | `shell/system-tray.md`, `windows/mission-control.md` |
| 9 | Privileged tier | `system/privileged-provider.md` |
| 10 | Stage B — AOSP | `system/aosp-integration.md` |
| 11 | Polish | *(sliced when reached)* |

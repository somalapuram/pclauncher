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
- [`launch/inventory-identity.md`](launch/inventory-identity.md) — one entry per key, and a build
  that replaces rather than accumulates. *Status: Accepted · Implemented.*
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
- [`design/visual-pass.md`](design/visual-pass.md) — labels without boxes, panels with depth, and a
  Start menu that breathes. *Status: Accepted · Implemented.*
- [`design/wallpaper-chrome.md`](design/wallpaper-chrome.md) — chrome takes its light/dark polarity
  from the wallpaper, not the system theme. *Status: Accepted · Implemented.*
- [`design/dynamic-color.md`](design/dynamic-color.md) — adopt Android's Material You colours
  instead of a hardcoded palette, with a manual override. *Status: Accepted · Implemented.*
- [`design/hover-feedback.md`](design/hover-feedback.md) — desktop icons, widgets and Start menu
  entries answer the pointer. *Status: Implemented (hover unverified).*
- [`design/icon-gloss.md`](design/icon-gloss.md) — a glaze over the glyph, an underside, and a
  light style that is actually glossy. *Status: Accepted · Implemented.*
- [`design/icon-treatment.md`](design/icon-treatment.md) — squircle tiles with gloss, rim light and
  shadow, baked once into the icon cache; dark-glass and soft-clay palettes by theme.
  *Status: Accepted · Implemented.*

### shell
- `shell/overlay-service.md` — foreground service + `ComposeOverlayWindow`, focus discipline,
  restart recovery. *Status: Planned.*
- `shell/menu-bar.md` — top bar, launcher-owned Window/View/Help menus. *Status: Planned.*
- [`shell/bar-alignment.md`](shell/bar-alignment.md) — the dock on the bar's centre line, and an
  Android app-grid Start mark. *Status: Accepted · Implemented.*
- [`shell/pointer-follow-through.md`](shell/pointer-follow-through.md) — magnification lets go when
  the pointer leaves, and the drag ghost sits under it. *Status: Implemented (hover unverified).*
- [`shell/bar-outline-order.md`](shell/bar-outline-order.md) — the bar's edge draws behind icons
  that rise through it. *Status: Implemented (overlap unverified).*
- [`shell/dock-magnification-fixes.md`](shell/dock-magnification-fixes.md) — hover magnifies the
  icon under the pointer and leaves the bar’s height alone. *Status: Implemented (hover unverified).*
- [`shell/shell-insets.md`](shell/shell-insets.md) — the on-screen keyboard does not move the
  desktop or the bar. *Status: Accepted · Implemented.*
- [`shell/context-menu.md`](shell/context-menu.md) — menus that disturb nothing and match the
  shell's surfaces. *Status: Accepted · Implemented.*
- [`shell/dock-taskbar.md`](shell/dock-taskbar.md) — one bar: Start button, magnifying dock,
  taskbar window chips, Show Desktop. Rendered in the HOME activity first; the overlay service
  hosts the same composables later. *Status: Accepted · Partially implemented.*
- [`shell/start-selection.md`](shell/start-selection.md) — no keyboard caret until the keyboard is
  used. *Status: Accepted · Implemented.*
- [`shell/start-power.md`](shell/start-power.md) — the Start menu footer, with device power gated
  on privilege the AOSP branch grants. *Status: Accepted · Implemented.*
- [`shell/start-menu.md`](shell/start-menu.md) — all apps + search, keyboard-first, right-click to
  pin. *Status: Accepted · Partially implemented (outside-click dismiss done; App info pending).*
- [`shell/grid-layouts.md`](shell/grid-layouts.md) — Start as a fixed-column grid; the desktop
  filling column-major. *Status: Accepted · Implemented.*
- [`shell/direct-manipulation.md`](shell/direct-manipulation.md) — right-click and long-press
  context menus, and dragging apps between the desktop and the taskbar.
  *Status: Accepted · Implemented.*
- [`shell/pinning.md`](shell/pinning.md) — the `pins` store and pin/unpin from every surface that
  lists an app. *Status: Accepted · Implemented.*
- `shell/command-palette.md` — Spotlight palette + `PaletteProvider`. *Status: Planned.*
- [`shell/tray-popover-placement.md`](shell/tray-popover-placement.md) — the popover aligned to the
  bar, and a tray control that presses in its own shape. *Status: Accepted · Implemented.*
- [`shell/quick-settings-surface.md`](shell/quick-settings-surface.md) — the popover as a glossy
  surface with state-carrying tiles. *Status: Accepted · Implemented.*
- [`shell/tray-controls.md`](shell/tray-controls.md) — drawn glyphs for the tray, a volume
  control, and one quick-settings popover behind them. *Status: Accepted · Implemented.*
- [`shell/system-tray.md`](shell/system-tray.md) — clock, battery, Wi-Fi, Bluetooth in the bar's
  right zone, all push-driven. *Status: Accepted · Implemented.*

### desktop
- [`desktop/icon-size.md`](desktop/icon-size.md) — icons drawn large enough to fill their cells,
  and the reserved-margin fraction that made them small. *Status: Accepted · Implemented.*
- [`desktop/placement-timing.md`](desktop/placement-timing.md) — no auto-placement before the grid
  is measured. *Status: Accepted · Implemented.*
- [`desktop/drag-origin.md`](desktop/drag-origin.md) — a drag reports the pointer's position, not
  the grid's corner. *Status: Accepted · Implemented.*
- [`desktop/icon-grid.md`](desktop/icon-grid.md) — persisted icon cells, drag-to-arrange, and the
  desktop’s own context menu (wallpaper, add widget). *Status: Accepted · Implemented.*
- [`desktop/widget-resize.md`](desktop/widget-resize.md) — long-press a widget to resize it by
  dragging, within the provider's permitted axes. *Status: Accepted · Implemented.*
- [`desktop/widget-drag.md`](desktop/widget-drag.md) — drag a widget to arrange it on the grid,
  snapping its corner and refusing a drop that would overlap. *Status: Accepted · Implemented.*
- [`desktop/widget-removal.md`](desktop/widget-removal.md) — a context menu on a widget, carrying
  Remove widget, and the id release that goes with it. *Status: Accepted · Implemented.*
- [`desktop/widget-alignment.md`](desktop/widget-alignment.md) — a widget's edges land on the grid
  rather than inside it. *Status: Accepted · Implemented.*
- [`desktop/widget-sizing.md`](desktop/widget-sizing.md) — a widget is told the size it is drawn
  at, not only when resized. *Status: Accepted · Implemented.*
- [`desktop/widget-chrome.md`](desktop/widget-chrome.md) — a hosted widget draws itself, without a
  box of ours around it. *Status: Accepted · Implemented.*
- [`desktop/widget-host.md`](desktop/widget-host.md) — `AppWidgetHost` inside Compose, binding and
  id lifecycle. *Status: Accepted · Implemented (removal pending).*

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

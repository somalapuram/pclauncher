# Shell — System Tray

Status: **Accepted · Implemented** (2026-08-20)

Clock, battery, Wi-Fi and Bluetooth in the bar's right zone. SRS §14 Phase 8; derives from SRS §6.2
(the tray is Windows organisation in mac styling) and §12 (idle cost near zero). Depends on
[`dock-taskbar.md`](dock-taskbar.md), which left that zone holding only the Show Desktop handle.

## Context

The bar currently ends in a Show Desktop grip. Every desktop puts status there instead, and the
reason is not decoration: the time and the battery are the two things a person glances at without
meaning to, and a shell that makes you open something to see them fails at being furniture.

**Everything here is push, not poll.** SRS §12 requires idle CPU at ~0%, and a tray is precisely
the thing that tempts a one-second timer. Android broadcasts every value needed: `ACTION_TIME_TICK`
fires each minute, `ACTION_BATTERY_CHANGED` is sticky so the current level is available immediately
and then on change, and connectivity has a callback. A polling loop here would be a permanent tax
on a device that already renders in software.

**Two of the four are readable without asking for anything.** Time and battery need no permission.
Connectivity needs `ACCESS_NETWORK_STATE`, which is normal and granted at install. Bluetooth is the
awkward one: `BluetoothAdapter.isEnabled()` has required `BLUETOOTH_CONNECT` at runtime since
API 31, and prompting the user for a *permission* so we can draw a *glyph* is a bad trade. The
on/off state is also in `Settings.Global`, readable without any permission, and that is what a tray
indicator actually needs.

**It is a status area, not a control panel.** This slice *shows* state. Popovers that let the user
toggle Wi-Fi or change volume are their own work, and pretending otherwise leads to a tray that is
half-interactive and inconsistent about it.

## Requirement

1. **A tray in the bar's right zone**, left of Show Desktop: Bluetooth, Wi-Fi, battery, then the
   clock — one grouped control, not scattered icons (SRS §6.2).
2. **The clock shows time in the user's format**, 12- or 24-hour per the system setting, and updates
   on `ACTION_TIME_TICK` — never on a timer.
3. **Battery shows level and charging state**, from the sticky `ACTION_BATTERY_CHANGED`, so the
   first frame is already correct rather than empty until the first change.
4. **Wi-Fi shows connected / disconnected**, driven by a connectivity callback.
5. **Bluetooth shows on / off**, read from the system setting so no runtime permission is required.
6. **All four are one observable state object** fed by registered receivers, not four independent
   subscriptions each surface manages.
7. **Nothing polls.** No timers, no loops, no repeated queries. Receivers are registered while the
   tray is composed and unregistered when it leaves.
8. **The tray degrades rather than disappears.** A value that cannot be read shows a neutral
   unknown state; it never blanks the tray or crashes the bar (GATE 4).
9. **Formatting is pure and tested** — battery percentage, time format selection, and state→glyph
   mapping are functions, not inline expressions.

## Acceptance criteria

- [x] Bluetooth, Wi-Fi, battery and clock render in the bar's right zone in that order.
- [x] The clock honours the system 12/24-hour setting (`DateFormat.getTimeFormat`).
- [x] The clock updates on `ACTION_TIME_TICK`; no timer or polling loop exists anywhere in the file.
- [x] Battery is correct on the first frame — the sticky broadcast returned by `registerReceiver`
      seeds the state rather than waiting for a change.
- [x] Wi-Fi reflects connectivity via `registerDefaultNetworkCallback`.
- [x] Bluetooth reflects on/off from `Settings.Global`, with no `BLUETOOTH_CONNECT` request.
- [x] One `TrayState` feeds all four indicators.
- [x] Receivers and the network callback unregister in `awaitClose`.
- [x] An unreadable value renders as `Unknown`, distinct from `Off`.
- [x] Percentage, charging, glyph mapping and the spoken description are pure and unit-tested,
      including a non-100 battery scale.
- [x] `./gradlew test` green (218 project-wide); `./gradlew lint` clean; verified on device —
      the tray shows `BT WiFi 100% 6:48 PM`.

## Notes

- **Why `Settings.Global` for Bluetooth:** asking for `BLUETOOTH_CONNECT` — a permission whose
  prompt says the app wants to "find, connect to, and determine the relative position of nearby
  devices" — to render an on/off glyph is not a trade worth making. The setting says exactly what
  the indicator needs and costs nothing.
- **Signal strength is deliberately absent.** Wi-Fi RSSI needs location permission on modern
  Android. Connected/not is what a tray glyph conveys anyway.
- **Off and unknown both draw nothing.** A tray is glanced at, and an indicator that appears only
  when it means something is faster to read than one permanently present in three states. The
  distinction still exists in the model and in the spoken description — it is the *glyph* that is
  omitted, not the information.
- **Not in this slice:** tray popovers, volume, notifications, toggling anything, a date tooltip,
  or per-indicator settings.

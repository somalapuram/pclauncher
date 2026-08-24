# Shell — Tray Icons and Controls

Status: **Implemented** (2026-08-24)

Real glyphs for Bluetooth, Wi-Fi and battery, a volume control beside them, and a quick-settings
popover that makes them do something. Extends [`system-tray.md`](system-tray.md), which delivered
the same four values as text and deliberately left them read-only — that doc's "It is a status
area, not a control panel" is superseded here. Depends on it for the push-not-poll state pipeline.

## Context

The tray reads `BT WiFi 100% 8:45 AM`. Two of those are words standing in for glyphs, and none of
them do anything when clicked. On a desktop the tray is the one place a person expects to *change*
these things — clicking the speaker to turn it down is muscle memory, not a feature request. As it
is, the tray looks like a debug readout and behaves like one.

**Sound is missing entirely,** which is the one of the four a user touches most often.

**Only one of them can actually be toggled by an app.** This is the constraint the whole slice has
to be honest about:

| | What an unprivileged app can do |
|---|---|
| **Volume** | Genuinely controllable. `AudioManager.setStreamVolume` on `STREAM_MUSIC` needs no permission, so a slider in our own popover is real, not a shortcut to somewhere else. |
| **Wi-Fi** | Cannot be toggled — `setWifiEnabled` has been privileged since API 29. `Settings.Panel.ACTION_WIFI` opens the platform's own panel, which lists networks and toggles the radio. That panel *is* the Windows flyout equivalent, and it is public API. |
| **Bluetooth** | Cannot be toggled directly either, but `ACTION_REQUEST_ENABLE` asks the system to turn it on with one confirmation. Off → ask to enable; on → open Bluetooth settings. |
| **Battery** | Read-only by nature. Clicking opens the platform's battery screen. |

Pretending otherwise would mean a switch that silently fails, which is worse than a control that
plainly hands off. Every hand-off goes to a real system surface, so the click always accomplishes
what the user meant — it is just not always us that finishes it.

**One popover, not four.** SRS §6.2 asks for "one grouped control, not scattered icons", and
Windows 11 agrees with it: clicking the network, volume or battery icon opens the same Quick
Settings flyout. Four separate popovers would mean four anchors, four dismiss rules, and a tray
that behaves differently depending on which pixel was hit.

**The glyphs are drawn, not imported.** Pulling in `material-icons-extended` costs a dependency and
several thousand vectors to use four of them, and the icons that matter here are *stateful* — Wi-Fi
has signal levels, battery has a fill and a bolt, volume has muted through loud. Drawing them means
the state is in the geometry rather than in a table of look-alike assets, and it matches how the app
icons are already treated. It also keeps them crisp under software rendering (SRS §4.3): flat
paths, no bitmaps, no per-frame blur.

**Nothing new may poll.** Volume changed elsewhere arrives through a `ContentObserver`, the same
push discipline the rest of the tray already follows.

## Requirement

1. **Bluetooth, Wi-Fi, battery and volume draw as glyphs**, not words, in that order, left of the
   clock.
2. **The glyphs carry their state.** Wi-Fi shows signal as filled arcs; battery shows a fill
   proportional to charge plus a bolt while charging; volume shows muted / low / medium / high;
   Bluetooth shows on or off.
3. **An unknown value draws as the "off" or empty form**, never a blank gap that shifts the row.
4. **A volume indicator is added to the tray**, reflecting the music stream.
5. **Clicking any of the four opens one quick-settings popover**, anchored to the tray.
6. **The popover carries a working volume slider.** Dragging it changes the device volume
   immediately, and volume changed elsewhere moves it.
7. **The popover's Wi-Fi row opens the system Wi-Fi panel.**
8. **The popover's Bluetooth row asks the system to enable Bluetooth when it is off**, and opens
   Bluetooth settings when it is on.
9. **The popover's battery row opens the system battery screen**, and shows level and charging
   state as text.
10. **Every hand-off is guarded.** A device with no activity to handle an intent shows nothing
    happening rather than crashing the shell (GATE 4).
11. **The popover closes on an outside click and on Escape**, like the Start menu.
12. **Nothing polls.** Volume state arrives by observer; the existing values keep their receivers.
13. **All the arithmetic and routing is pure and tested**: glyph level from a value, battery fill,
    volume index from a slider fraction and back, and which action an indicator resolves to.

## Acceptance criteria

Verified on the `pclauncher_desktop_api34` emulator.

- [x] The tray draws four glyphs and a clock; no `BT`/`WiFi` text remains.
- [x] Wi-Fi renders distinct connected and disconnected forms (arcs filled vs. faint).
- [x] Battery fill tracks the percentage, and a bolt appears while charging.
- [x] Volume renders muted, low, medium and high forms — the muted glyph appeared at level 0 and
      the waves returned as the level rose.
- [x] Clicking each of the four opens the same popover.
- [x] The slider changes device volume: dragging it took `STREAM_MUSIC` from 5 to 14 of 15.
- [x] Changing volume with the hardware keys moves the slider without reopening the popover — six
      key presses left the device at 11/15 and the handle at 73%.
- [x] The Wi-Fi row launches `Settings.Panel.ACTION_WIFI` (`SettingsPanelActivity`).
- [x] The Bluetooth row launches `ACTION_BLUETOOTH_SETTINGS` (`ConnectedDeviceDashboardActivity`)
      in both states in Stage A — see below.
- [x] The battery row launches `ACTION_POWER_USAGE_SUMMARY` (`PowerUsageSummaryActivity`), and
      still does after visiting the other two.
- [x] An intent with no handler is a no-op, not a crash.
- [x] Outside click and Back both close the popover.
- [x] No timer or polling loop is introduced — volume arrives by `ContentObserver`.
- [x] Unit tests cover signal level → bars, battery percent → fill, charging, volume value → glyph,
      slider fraction → stream index and back (including a full round trip over every level),
      rounding at both ends, a zero and a negative maximum, and the action each indicator resolves
      to in every state (`TrayControlsTest`, 28 cases).
- [x] Row wiring covered by `QuickSettingsTest` (7 cases), which fails if a row's click, the
      slider's callback, or the panel's close is disconnected.
- [x] `./gradlew test lint assembleDebug` green.

**Requirement 8 lands differently than written.** `ACTION_REQUEST_ENABLE` has required
`BLUETOOTH_CONNECT` since API 31, and without it the intent fails *silently* — the click looks like
it worked and nothing happens, which was exactly the behaviour on device. Stage A does not hold that
permission, so the Bluetooth row now resolves to Bluetooth settings whenever the permission is
absent, in either radio state. The one-tap enable path is kept and tested, and lights up wherever
the permission is held.

## Notes

- **Why the volume slider is ours and the rest are hand-offs:** it is the only one of the four the
  platform lets an app change. Making the others look like local switches would be a lie the first
  tap exposes.
- **`STREAM_MUSIC`, not the ringer.** The music stream is what "volume" means on a desktop, and it
  is the stream that needs no Do Not Disturb access to change. Changing it can still throw under a
  DND policy, so the write is guarded and the slider reports what the device actually did rather
  than what it was asked to do.
- **Bluetooth state still comes from `Settings.Global`**, as `system-tray.md` established — the
  point of that decision was to avoid asking for `BLUETOOTH_CONNECT` to draw a glyph, and adding a
  control does not change it. Enabling goes through the system's own consent dialog, which needs no
  permission from us.
- **A hand-off has to clear the task it lands in.** With `FLAG_ACTIVITY_NEW_TASK` alone, the second
  hand-off merely brought the existing Settings task forward at whatever page it had been left on —
  asking for Bluetooth after visiting Battery showed Battery. `FLAG_ACTIVITY_CLEAR_TASK` makes the
  requested screen the one that appears, every time.
- **Not in this slice:** notifications, a calendar flyout on the clock, per-app volume, output
  device switching, an airplane-mode tile, brightness, or a Wi-Fi network list of our own.

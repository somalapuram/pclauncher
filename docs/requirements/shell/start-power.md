# Shell — Power in the Start Menu

Status: **Implemented** (2026-08-24)

A footer on the Start menu: Settings, Restart shell, Restart device, Power off. Two of them work
today; two are shown disabled with the reason, and light up on the target device without the UI
changing. Completes the footer SRS §6.4 specifies and applies the tier discipline of SRS §5.3.

## Context

The Start menu has no footer. SRS §6.4 asks for "device name, Settings, Power (sleep/lock, restart
shell)", and none of it exists.

**Only some of it can work here.** Android gives a third-party app no way to reboot or shut down the
device, deliberately: `PowerManager.reboot` requires `REBOOT` and shutdown requires `SHUTDOWN`, both
`signature|privileged`. There is no public intent either. Restarting *our own shell* and opening
Settings need nothing at all.

| | Stage A | Stage B (`aosp-pc-x86_64`) |
|---|---|---|
| Settings | works | works |
| Restart shell | works | works |
| Restart device | needs `REBOOT` | granted as a platform app |
| Power off | needs `SHUTDOWN` | granted as a platform app |

**Showing them disabled is the point.** The alternative — hiding what we cannot do — makes the shell
look less capable than the machine it ships on, and gives the Stage B work nowhere to land. A
disabled control with the reason attached is honest, and SRS §5.3 already requires exactly this of
capability tiers: detected at runtime, never assumed, and *explained in plain language*.

**Detected, not compiled in.** The same APK must behave correctly in both worlds, so availability is
a runtime permission check rather than a build flag. Nothing in the UI knows which stage it is in;
it asks whether the permission is held. The AOSP branch grants it through the privileged-permission
allowlist and the buttons become live with no code change — which is the whole point of the seam
SRS §5.2 describes.

**"Restart shell" means our process, not the device.** It is the recovery action for a shell that
has got into a bad state, and it is the one power-ish thing that has always been possible.

## Requirement

1. **The Start menu has a footer** carrying Settings, Restart shell, Restart device and Power off.
2. **Settings opens the system settings.**
3. **Restart shell restarts pclauncher** and returns to a working desktop.
4. **Restart device and Power off are enabled only when the corresponding permission is held**, and
   perform the real action when it is.
5. **When not held they are visibly disabled and say why** — a short, plain reason, not a silent
   no-op and not a crash.
6. **Availability is decided at runtime** from the permissions actually granted, with no build-time
   switch and nothing Stage-B-specific outside the permission declaration.
7. **The decision is a pure function** of which privileges are held, so both worlds are testable
   without a device.
8. **A failed or refused action costs the action, not the shell** (GATE 4) — including Restart
   shell, which must never leave the device without a home screen.

## Acceptance criteria

Checked on the `Pixel_Tablet` AVD, where neither privilege is held — so the disabled path is the one
exercised, and the enabled path is covered by tests.

- [x] The footer renders the actions and, where there is one, the device name. On this emulator
      every source returns the build identifier `sdk_gphone16k_x86_64`, so the slot is empty —
      see below.
- [x] Settings opens the system settings (`SettingsHomepageActivity`).
- [x] Restart shell restarts the launcher: the process id changed and the desktop came back on its
      own, with Settings still in the back stack.
- [x] Without the privileges, Restart device and Power off are disabled and carry the reason.
- [x] Clicking a disabled action does nothing: the menu stayed open, no activity changed, nothing
      crashed.
- [ ] With the privileges held, both are enabled and route to the real calls. **Not verifiable
      here** — it needs the platform-signed build. The routing is unit-tested for every combination
      of the two privileges, and the performer re-checks availability before calling.
- [x] The availability decision is unit-tested across all four combinations (`PowerActionsTest`).
- [x] The permissions are declared in the manifest, with lint's `ProtectedPermissions` suppressed at
      the declaration rather than baselined — lint is right that an ordinary app cannot hold them,
      and that is the design.
- [x] `./gradlew test lint assembleDebug` green.

**Restart shell had to ask for itself back before exiting.** The first implementation called
`finishAffinity()` and exited, which killed the process and left whatever was behind the launcher in
front — the desktop did not return until the user pressed HOME. A home screen that does not come
back is exactly what GATE 4 forbids. Starting the home activity before exiting makes the system
relaunch it, which was confirmed with another app in the stack.

**The device name is shown only when it is a name.** SRS §6.4 asks for it, and on real hardware
`Settings.Global.DEVICE_NAME` holds what the user set — the name Bluetooth and Nearby show. An
emulator or an un-named build returns a build identifier instead, and `sdk_gphone16k_x86_64` in a
footer reads as debug text someone forgot to remove. The slot now shows a name or nothing, and the
model only stands in when no name has been set. On `aosp-pc-x86_64` it will appear as soon as the
device is given a name.

## Notes

- **Why declare permissions that cannot be granted here.** A `signature|privileged` permission
  declared by an ordinary APK is simply not granted — it costs nothing and it is what lets the same
  binary become capable when it is signed and allowlisted. Declaring it is also where the Stage B
  requirement is written down.
- **Restart shell exits the process rather than recreating the activity.** Recreating rebuilds the
  UI but keeps every singleton — including the inventory repository — so the states worth recovering
  from would survive the recovery. The home activity is relaunched by the system by definition.
- **Not in this slice:** sleep or lock (both need device-admin or an accessibility service, and
  SRS §5.4 declines to ship an accessibility service for this kind of thing), a confirmation dialog
  before a device restart, or a power action in the command palette.

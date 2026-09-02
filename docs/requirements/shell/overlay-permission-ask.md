# Shell — asking for "Display over other apps" on first run

## Context

The chrome now renders into `TYPE_APPLICATION_OVERLAY` windows so it stays above app windows
(`overlay-service.md`, `overlay-window-split.md`). That needs `SYSTEM_ALERT_WINDOW`, and so far the
only way it has ever been granted on the test device is by hand:

```
adb shell appops set com.somalapuram.pclauncher SYSTEM_ALERT_WINDOW allow
```

A user has no such lever. Without the permission the shell silently falls back to drawing its bar
inside the home activity, which means the bar vanishes behind the first app they open and nothing
tells them why. SRS §7.1 step 3 and §11 both call for the ask; it was deferred while the overlay
itself was being proven, and this is that slice.

`SYSTEM_ALERT_WINDOW` is a **special** permission: it is not granted at install and cannot be
requested through the runtime permission dialog. The only way to get it is to send the user to a
Settings screen and let them turn it on there. So "asking" here means explaining first, then opening
that screen — there is no system prompt to lean on.

## Requirement

1. **Explain before sending the user anywhere.** On first run, when the permission is not held, the
   desktop shows a plain-language card: what the permission does *for them* ("the taskbar and Start
   menu stay visible while you use apps") and what happens without it ("they show only on the
   desktop"). No permission jargon, no API names.
2. **Two ways out, both honest.** *Allow* opens the system screen scoped to this app; *Not now*
   dismisses. Both are a decision, and both are remembered.
3. **Asked at most once, automatically.** The answer survives a restart. Whichever way it went, the
   card does not come back on its own.
4. **A refusal is respected.** The consequence is stated in the card the user is dismissing, so it
   has been said once; the shell then degrades quietly and never nags. Granting it later from the
   system Settings screen still works.
5. **The ask never traps the user** (GATE 4). It is dismissible, the desktop is reachable, and a
   prompt store that cannot be read shows no card — a store that cannot be read cannot be written,
   so a card shown from one would come back on every launch, which requirement 3 forbids.
6. **A grant takes effect without a restart.** Returning from the Settings screen with the
   permission on moves the chrome into the overlay there and then.
7. **Never shown when the permission is already held** — including on the target device, where
   Stage B pre-grants it and this card must never appear.

## Acceptance criteria

- [ ] Fresh install without the permission: the card appears once on the desktop.
- [ ] *Not now*, then restart the shell: the card does not reappear.
- [ ] *Allow*: the system overlay screen for this app opens. Granting and returning puts the bar in
      the overlay with no restart, and the card does not reappear.
- [ ] Permission already granted: the card never appears.
- [ ] An unreadable prompt store shows no card and does not crash.
- [ ] `shouldAskForOverlay` is false whenever the permission is held, whatever the store says.
- [ ] `./gradlew test lint assembleDebug` green; both paths checked on device.

## Notes

- **Why not a runtime permission request.** `SYSTEM_ALERT_WINDOW` is not requestable that way;
  `ACTION_MANAGE_OVERLAY_PERMISSION` is the only route, and it leaves the app. That is exactly why
  the explanation has to come first — the user arrives at a Settings toggle already knowing what it
  is for.
- **Where the answer lives:** a new `prompts` store, alongside `pins` and `desktop_layout`. It holds
  which one-time prompts have been shown, not the permission state — the permission itself is always
  read live from the platform, never cached (`overlay-service.md`).
- **Observed on the test device (Android 17, Pixel Tablet):** `ACTION_MANAGE_OVERLAY_PERMISSION`
  carrying a `package:` URI still lands on Settings' *list* of apps rather than this app's own
  screen — Settings' SPA rewrite ignores the scoping. pclauncher is one row away and its state is
  shown there, so this is left alone rather than worked around; the button says "Open settings"
  and that is what it does.
- **Stage B:** the target device grants this by allowlist, so requirement 7 is what keeps this out
  of the way there. No AOSP work is needed for this slice (GATE 3).

# Design — the chrome must not open light and turn dark

## Context

Reported on the device: *"when the start menu pressed, there is white theam then dar theam, looks
like flicker"*.

`rememberWallpaperTone()` returns `WallpaperTone()` — nothing known — from its first composition,
and only reads the real value inside a `DisposableEffect`, which runs *after* the first frame is
composed. `chromeIsDark` falls through an unknown tone to `isSystemInDarkTheme()`
(`wallpaper-chrome.md`). On the test device that is `Night mode: no` over a dark wallpaper, so the
first frame is drawn **light** and the second **dark**.

This was always true, but it used to happen once, at launch, where it read as part of starting up.
`overlay-window-split.md` made the Start menu a window of its own, created on every press — so every
new window is a new composition, and the flash now happens each time the user opens the menu. The
split did not introduce the bug; it made an existing one visible.

The fix is not to guess better. A tone that is *unknown* is a real state — a wallpaper may genuinely
report nothing — and the fallback for it is right. The bug is that "unknown" is reported for the
first frame of every window when the answer is available synchronously.

## Requirement

1. **The chrome's first drawn frame carries its final tone.** No window opens in one polarity and
   settles into another.
2. **The tone is read before the first composition returns**, not from an effect that runs after it.
3. **A window opened later starts from what is already known**, without repeating the platform call
   on every Start press.
4. **A wallpaper change still flips the chrome live**, with no restart — the existing behaviour must
   survive the fix.
5. **A wallpaper that cannot be read still costs the tint and nothing else** (GATE 4). Unknown
   remains a legitimate answer that falls through to the system setting.

## Acceptance criteria

- [x] Opening the Start menu shows no light frame before the dark one. Measured with a probe on the
      theme composition: before, `dark=false` then `dark=true` **401 ms** later; after, a single
      `dark=true` on the first press and on the second.
- [x] The same for the bar and the desktop at launch — two compositions, both `dark=true`.
- [x] Changing the wallpaper still re-tints the chrome without a restart. Verified live: the
      wallpaper was changed through the system picker with the shell running (pid unchanged across
      it), and `OnColorsChangedListener` fired with `which=3` and re-read the tone in-process.
- [x] With the wallpaper unreadable, the shell still draws and follows the system setting — the
      `runCatching` fallback is unchanged and `WallpaperTone()` remains a legitimate answer.
- [x] `./gradlew test lint assembleDebug` green; checked on device.

## Notes

- **Why a process-level cache and not just a synchronous read.** The synchronous read alone fixes
  the polarity, but it puts a binder call to `WallpaperManager` on the composition of every window
  the shell opens — which is every Start press. Remembering the last resolved value makes the second
  and later windows free, and the listener keeps it honest.
- The cache is written and read on the main thread only, which is where both composition and the
  colours listener run.

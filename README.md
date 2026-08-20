# pclauncher

A desktop-class launcher for Android, built in Jetpack Compose — mac shape, Windows organisation.

Wallpapered desktop with icons and widgets, a global menu bar, a magnifying dock fused with a
taskbar window list, a Windows 11-style Start menu, a Spotlight-style command palette, and apps in
resizable freeform windows.

Its destination is [`aosp-pc-x86_64`](https://github.com/somalapuram/aosp-pc-x86_64) — Android 17 on
bare-metal x86_64 — where it replaces `Launcher3QuickStep` as the home app. Until then it is
developed as a standalone APK in Android Studio, runnable on any Android 12+ device.

## Docs

- [`docs/SRS-pclauncher.md`](docs/SRS-pclauncher.md) — the spec. Start here.
- [`docs/requirements/`](docs/requirements/README.md) — buildable slices; no code lands without one.
- [`docs/CODING-GUIDELINES.md`](docs/CODING-GUIDELINES.md) — the engineering standard.

## Status

Requirements only. No code yet — SRS §14 phase 1 is next.

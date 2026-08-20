# Project rules for Claude

pclauncher — a desktop-class Android launcher in Jetpack Compose, destined to replace
`Launcher3QuickStep` as the home app of the `aosp-pc-x86_64` build.

Auto-loaded by Claude Code. Follow `docs/CODING-GUIDELINES.md` — the operating standard (pulled in
below). The rules below are **hard gates**: apply them on every task, without being reminded.

## GATE 1 — No code without a requirement doc

Before writing or changing **any** code, a requirement doc must exist at
`docs/requirements/<module>/<feature>.md`. If the request has none, **write it first**
(Context · Requirement · Acceptance criteria · Notes), confirm it, *then* implement. A chat
instruction is the trigger to write the requirement, not to start coding.

## GATE 2 — A test for every function

No untested logic. Tests mirror the source tree, are deterministic, and the suite is green before a
change is "done".

## GATE 3 — Stage A does not touch AOSP

Until SRS §14 phase 10, this is a **standalone APK developed in Android Studio**. No task may
require the AOSP tree at `~/amar/aosp-pc-x86_64`, a platform signature, or a device build.
Anything needing platform privilege goes behind an interface in `platform/privileged/` with a
working unprivileged fallback. See SRS §2 and §5.3.

## GATE 4 — Never ship a home screen that can crash on boot

This app *is* the home screen. A crash on launch leaves the device with no UI. The HOME activity
must reach a usable desktop with every store corrupt, the shell service dead, and no permission
granted. That fallback path is tested, not assumed. See SRS §12.

## The spec

@docs/SRS-pclauncher.md

## The standard (read in full)

@docs/CODING-GUIDELINES.md

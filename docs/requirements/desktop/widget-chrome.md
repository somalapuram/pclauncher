# Desktop — A Widget Is Not a Box

Status: **Implemented** (2026-08-24)

A hosted widget draws itself, and nothing of ours around it. Amends
[`widget-host.md`](widget-host.md).

## Context

Every widget on the desktop sits inside a translucent scrim with a hairline border. That frame was
written as the *placeholder* — the thing to show when a provider fails to inflate and there is
nothing else to draw — but it is applied to the container unconditionally, so a perfectly good
widget gets it too.

The result is a box around every widget, permanently. Widgets are designed by their authors to sit
on a wallpaper: they bring their own background, their own corners, their own padding. Framing them
adds a second, competing edge and makes the desktop look like a form.

**The placeholder still needs its frame.** "Widget unavailable" floating on the wallpaper with no
ground under it reads as a rendering fault rather than a message. The chrome is right for that case
and wrong for every other.

## Requirement

1. **A hosted widget draws no background, border or corner of ours.** The provider's view fills the
   cell.
2. **The unavailable placeholder keeps its frame**, so the message has ground under it.
3. **The cell's bounds are unchanged** either way — removing the chrome must not move or resize the
   widget.
4. **Resize handles are unaffected**: the resize frame is drawn in resize mode and is a separate
   thing from this chrome.

## Acceptance criteria

- [x] A hosted widget shows no frame around it — checked on device with Chrome Dino, which now
      shows only its own white card and rounded corners.
- [ ] A widget whose provider fails to inflate still shows a framed "Widget unavailable". **Not
      re-checked on device:** it needs a provider that fails to inflate, which none here does. The
      branch is covered by `WidgetChromeTest`.
- [x] The widget occupies the same rectangle with and without the chrome — the frame was a
      decoration on the same Box, and only the decoration changed.
- [x] Long-pressing a widget still shows the resize frame, which is drawn separately.
- [x] The decision is unit-tested as a function of whether there is a view.
- [x] `./gradlew test lint assembleDebug` green.

## Notes

- **Not in this slice:** a per-widget setting to re-enable a background, or padding around widgets
  that ship without their own.

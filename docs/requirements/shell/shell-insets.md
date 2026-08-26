# Shell — The Keyboard Must Not Move the Shell

Status: **Implemented** (2026-08-24)

The desktop, the bar and the Start menu stay where they are when the on-screen keyboard appears.

## Context

Opening the Start menu focuses its search field (`start-menu.md` requires it), which on a device
with no hardware keyboard raises the IME. The shell's root uses `safeDrawingPadding()`, and safe
drawing *includes* the IME inset — so the whole shell is squeezed into the space above the keyboard:
the desktop grid compresses, the bar rises off the bottom edge, and the Start menu ends up floating
in the middle of the screen next to a bar that is no longer at the bottom.

It reads as "the Start menu is not aligned", but nothing about the menu's own placement is wrong.
Everything under it moved.

**A home screen does not get out of the keyboard's way.** It *is* the screen. The bar belongs to the
bottom edge whatever else is on screen, and the desktop's grid positions are the user's — reflowing
them because a text field took focus loses the arrangement's whole point. The manifest already asks
for `adjustPan` rather than `adjustResize`, but a Compose inset applied in the layout overrides that
intent from inside.

**The menu is the thing that should adapt, not the shell.** If a keyboard ever covers the Start
menu, that is the menu's problem to solve, and it can solve it without the desktop moving.

## Requirement

1. **The shell's root padding ignores the IME.** System bars and display cutouts still inset it;
   the keyboard does not.
2. **The bar stays on the bottom edge** while the keyboard is up.
3. **Desktop icon and widget positions do not change** when the keyboard appears or disappears.
4. **The window is not panned or resized by the system either** — the manifest asks for nothing to
   move.
5. **The Start menu still opens with its search field focused**; this changes what the keyboard does
   to the shell, not whether typing works.

## Acceptance criteria

- [x] With the Start menu open, the bar's bottom edge is where it is with the menu closed.
- [x] The desktop's icons are at the same positions with the menu open and closed.
- [x] The Start menu's own position is unchanged, and its left edge lines up with the bar's.
- [x] Typing in the search field still filters — the field focuses when tapped.
- [x] `./gradlew test lint assembleDebug` green; checked on device.

**Requirement 5 is not met as written, deliberately.** The menu no longer focuses its search field
on open. Focusing raises the on-screen keyboard over the bottom half of the display — which is
exactly where the Start menu is — so opening the menu covered the menu. Detecting a hardware
keyboard did not avoid it: the emulator reports one, reports it as not hidden, and Android raises
the IME regardless. `showKeyboardOnFocus = false` is ignored by this `BasicTextField` overload, and
hiding the keyboard immediately after requesting focus loses the race.

SRS §6.4's intent is that *keystrokes* reach search, which is a key-routing problem rather than a
focus one — it belongs with SRS §8's shortcut work, where a key event can be delivered to the field
without placing a caret in it. `shouldFocusSearchOnOpen()` exists and returns false so the decision
is visible at the call site and has somewhere to be reversed.

## Notes

- **Why not `adjustNothing` alone.** The manifest flag governs what the *system* does to the window;
  `safeDrawingPadding` is what the *app* does to its own layout. Both have to agree, and the app's
  was the one actually moving things.
- **Not in this slice:** the Start menu avoiding the keyboard itself, or a hardware-keyboard
  detection path that skips focusing the field (SRS §5.4 territory).

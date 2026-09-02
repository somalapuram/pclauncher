# Input — keyboard shortcuts the shell can actually receive

## Context

SRS §8 lists eight shortcuts. None are implemented: there is no key handling anywhere outside the
Start menu's own arrow keys, which themselves only began working when the menu was given a focus
target (`recent-apps.md` requirement 7).

Two facts decide what this slice can contain.

**Key events reach pclauncher only while one of its windows has focus** (SRS §5.4). The shell has no
global hotkeys and must not grow an accessibility service to fake them. The bar sits in a
`FLAG_NOT_FOCUSABLE` overlay precisely so it never eats the user's typing, so it receives nothing at
rest. That leaves two windows that can hear a key: the **desktop**, when it is in front, and an open
**menu**.

**Most of the actions do not exist yet.** The command palette is phase 5, Mission Control is phase 8,
and snapping and closing a window need a `WindowBackend` that is not built. A shortcut bound to a
no-op is worse than an absent one: it teaches the user the key does not work, and they stop trying.

So this slice binds only the actions that exist *and* can be heard, and writes down why each of the
rest is absent — so the list is a plan rather than an omission.

## Requirement

1. **`Ctrl+Esc` toggles the Start menu**, from the desktop and from the open menu itself. It reaches
   the menu whichever host is drawing it — the home activity or the overlay window.
2. **`Meta+,` or `Ctrl+,` opens Settings**, matching the Start menu's own Settings button. Both,
   because Meta chords are frequently swallowed before they reach an app (SRS §5.4) — measured on
   the test device, where `Meta+,` never arrives — and a shortcut the platform eats is a shortcut
   that does not exist.
3. **`Esc` closes an open menu** without launching anything.
4. **Typing filters.** With the menu open, a printable character appends to the search query and
   `Backspace` removes one — SRS §6.4's "typing anywhere filters", which `SearchFocus.kt` deferred
   to exactly this work. The caret is still never placed in the field, because that is what raises
   the on-screen keyboard over the menu.
5. **A shortcut never fires an action that does not exist.** Every binding drives something the user
   can already do by pointer.
6. **The mapping is pure and tested** — key plus modifiers to action — rather than a `when` block
   inside a key handler, because that is where a chord silently overlaps another.
7. **Nothing the shortcuts add may cost the desktop its normal input.** Icons, drag, and the context
   menu behave exactly as before (GATE 4).

## Acceptance criteria

- [ ] `Ctrl+Esc` on the desktop opens the Start menu; again closes it.
- [ ] `Ctrl+Esc` works with the chrome in the overlay as well as in the activity.
- [ ] `Ctrl+,` opens the system Settings app.
- [ ] `Meta+,` does the same where the platform delivers it.
- [ ] `Esc` closes the menu and launches nothing.
- [ ] With the menu open, typing narrows the list and `Backspace` widens it again.
- [ ] Arrow keys and Enter still work as they did.
- [ ] Clicking and dragging desktop icons is unaffected.
- [ ] `./gradlew test lint assembleDebug` green; checked on device.

## Notes — what is deliberately not bound, and why

| SRS §8 | Why not now |
|---|---|
| `Meta` alone → Start | Needs key-up tracking to tell a modifier press from a chord, and the platform frequently swallows it. `Ctrl+Esc` is the documented fallback and is unambiguous. |
| `Meta+Space` → palette | The command palette is phase 5. Nothing to open. |
| `Meta+D` → show desktop | Only useful from *inside* an app, which is exactly where the shell hears no keys. `onShowDesktop` is also still an empty lambda. |
| `F3` → Mission Control | Phase 8. Nothing to open. |
| `Meta+←/→/↑/↓` → snap | Needs `WindowBackend` and a focused-window concept; neither is built. |
| `Meta+W` → close window | Same. |
| `Alt+Tab` → cycle windows | The platform owns this in desktop mode and SRS §8 says not to fight it. On `pc_x86_64` WM Shell already handles it. |

- **Rebindable** (SRS §8) is not in this slice: there is no settings surface to rebind them in, and a
  mapping that is pure is the thing that makes rebinding a later change rather than a rewrite.

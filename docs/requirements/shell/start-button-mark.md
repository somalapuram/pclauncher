# Shell — The Start Mark Points at Its Menu

Status: **Implemented** (2026-08-24)

A rounded triangle that points down at rest and turns to point up while the Start menu is open.
Supersedes the app-grid mark in [`bar-alignment.md`](bar-alignment.md).

## Context

The Start button carries a three-by-three grid of dots. It says "apps", which is true, but it says
nothing about what pressing it does or about the state it is in — the button looks identical whether
the menu is open or shut, and only its fill colour distinguishes them.

**A triangle can say both things at once.** Pointing down at rest it reads as "there is something
here"; turning to point up as the menu rises, it becomes the menu's own indicator, and turning back
closes the loop. One mark, two states, and the rotation *is* the explanation — no legend needed.

**The turn has to be animated or it is just a different picture.** An instant flip reads as the icon
being swapped rather than the same object moving, which loses the connection to the menu it is
describing. SRS §6.1 principle 4 asks for spring-based motion of 150–250 ms, degradable on a
software renderer.

**Rounded, because everything else in the shell is.** A bare geometric triangle would be the one
hard-cornered thing among squircle icons, rounded panels and a rounded bar.

## Requirement

1. **The Start mark is a triangle with rounded corners.**
2. **It points down when the menu is closed.**
3. **It points up while the menu is open.**
4. **The change between them is animated**, spring-based and short, in both directions.
5. **The button's behaviour is unchanged** — same target size, hover and pressed states, accessible
   name, and the same click that opens and closes the menu.
6. **The rotation follows the menu's state**, not the click, so a menu closed by clicking elsewhere
   turns the mark back too.

## Acceptance criteria

- [x] At rest the mark is a downward rounded triangle.
- [x] Opening the menu turns it to point up.
- [ ] The turn is animated rather than instant. **Not captured:** a screen capture takes about two
      seconds here and the spring is a fifth of that, so only the end states were photographed. It
      is `animateFloatAsState` on a spring, the same spec the rest of the shell's motion uses.
- [x] Closing the menu turns it back down.
- [x] Dismissing by clicking away turns it back too — the resting mark is pixel-identical to before
      the menu was opened, which is the property that matters: the mark follows the menu's state,
      not the click.
- [x] The button still reports itself as "Start" and still toggles the menu.
- [x] `./gradlew test lint assembleDebug` green.

## Notes

- **Why rotate rather than swap two glyphs.** Rotating one mark is what makes it read as the same
  object turning; two glyphs cross-fading is a substitution, and the eye reads it as such.
- **Not in this slice:** a mark that follows the bar to a left or right screen edge, or an animation
  tied to the menu's own open progress rather than its state.

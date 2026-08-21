# Shell — Grid Layouts

Status: **Accepted · Implemented** (2026-08-20)

Turns the Start menu's app list into a grid, and makes the desktop fill **column-major** — top to
bottom, then across — instead of row-major. Derives from SRS §6.4 (Start is a grid of pinned apps)
and §6.6 (desktop icons). Supersedes the list layout in
[`start-menu.md`](start-menu.md) and the row-major grid in
[`direct-manipulation.md`](direct-manipulation.md)'s desktop surface.

## Context

Two layout decisions that were expedient rather than right.

**The Start menu is a list.** SRS §6.4 asks for a Windows 11 arrangement, and Windows 11's Start is a
*grid* of pinned apps. A list shows about eleven apps in the height available; a five-column grid
shows thirty-five in the same space. On a machine with two hundred apps installed that is the
difference between scrolling to find something and seeing it. The list was the fastest way to get
"all apps" working; this is the shape it should have had.

**The desktop fills row-major**, which is what `LazyVerticalGrid` does by default: left to right,
wrap, repeat. Every desktop that has ever existed fills the *other* way — top to bottom down the
first column, then across to the next. It is not arbitrary: a desktop's free space is on the right,
because that is where windows are least likely to be, and column-major keeps icons packed against
the left edge as they accumulate. Row-major spreads a handful of icons across the entire top of the
screen, which is exactly what the current build does and exactly why it looks wrong.

**Keyboard navigation stops being one-dimensional.** In a list, ↑/↓ are the whole story. In a grid,
↑/↓ move by a row — which means by *columns* items — and ←/→ move by one. Getting this wrong is
subtle and constant, so the index arithmetic is a pure function rather than something inlined into
a key handler.

## Requirement

1. **The Start menu lays out apps as a grid**, icon above label, with a **fixed** column count so
   keyboard navigation has exact arithmetic. Adaptive columns would make the step size depend on
   the measured width.
2. **Grid keyboard navigation:** ←/→ move by one, ↑/↓ move by one row, Enter launches, Esc closes.
   Movement is clamped at the edges — it must not wrap from the end of one row to the start of the
   next, which loses the user's place.
3. **The selection survives filtering**, clamped into range as results shrink, exactly as it does
   today.
4. **The desktop fills column-major:** top to bottom, then across, overflowing to the right.
5. **Everything already true stays true** — treated icons, greyed unavailable entries, launch on
   click, the context-menu gestures, and drag-to-pin all work unchanged on both surfaces.
6. **The navigation arithmetic is pure and tested** at every edge: first cell, last cell, a partial
   final row, single column, single item, and an empty grid.

## Acceptance criteria

- [x] Start renders a fixed 5-column grid of icon-above-label cells.
- [x] ←/→ move one cell; ↑/↓ move one row; all four clamp rather than wrap (15 tests).
- [x] Enter launches the selected cell; Esc closes; typing still filters.
- [x] Selection is clamped into range when a filter shrinks the results.
- [x] The desktop fills top-to-bottom then across, overflowing right — verified on device.
- [x] Icon treatment, greyed entries, launching, context menus and drag-to-pin all still work.
- [x] Navigation arithmetic is pure and tested: first, last, partial last row, single column,
      single item, empty grid, zero columns, out-of-range selection.
- [x] `./gradlew test` green (209 project-wide); `./gradlew lint` clean; both layouts on device.

## Notes

- **Why a fixed column count in Start and an adaptive one on the desktop:** Start is a bounded
  popover whose width we choose, so a fixed count is both possible and necessary for the keyboard
  arithmetic. The desktop is whatever size the display is, and its icons are not keyboard-navigated
  yet, so adapting to the available height is the better trade there.
- **`LazyHorizontalGrid` is what makes the desktop column-major.** It fills each column top to
  bottom before moving right, which is the behaviour wanted here — not a horizontal *list*.
- **The Start menu had to become opaque.** At 96% the desktop's icons read straight through the
  panel and looked like rendering ghosts — a "Contacts" label showing behind the Calendar cell,
  because Contacts sits at that spot on the desktop. Translucency reads as depth on thin chrome
  over wallpaper; on a dense panel over a grid of bright icons it just makes the panel hard to
  read. The bar stays translucent; the menu does not.
- **Not in this slice:** free icon placement, drag-to-arrange on the desktop, folders, the alphabet
  index rail, Start's pinned/recommended sections, or keyboard navigation of desktop icons.

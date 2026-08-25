# Shell — Quick Settings, as a Surface

Status: **Implemented** (2026-08-24)

The quick-settings popover restyled to match the shell it belongs to: a glossy surface, controls
that look like controls, and state you can read without reading. Extends
[`tray-controls.md`](tray-controls.md), which established what the panel *does* and left it as
three text rows. Shares the sheen introduced by [`../design/icon-gloss.md`](../design/icon-gloss.md).

## Context

The panel works — the slider drives `STREAM_MUSIC`, and each row lands on the right system screen.
It just looks like a settings list that wandered onto a desktop: a flat white card, three rows of
label-and-value text, and a stock slider. Everything around it now has dimension. The tiles are
glossy, the bar has a sheen, and the one surface a user opens *deliberately* is the flattest thing
on screen.

**A row is the wrong shape for a toggle.** "Wi-Fi ......... Connected" is a table cell. Windows 11
puts these in tiles precisely because a tile can carry its own state — filled when on, plain when
off — which is legible at a glance and needs no value column at all. We already draw glyphs for all
three; a tile is the frame they were designed for.

**On and off should differ in more than a word.** SRS §6.1 asks for honest state distinguished by
shape rather than colour alone, and today the only difference between a connected and a
disconnected radio is the text on the right. A filled tile plus a lit glyph says it twice.

**The slider is the one true control here** — the only one of the four the platform lets us change
(`tray-controls.md`). It should look like the most substantial thing in the panel rather than the
default Material track, which reads as thinner and less deliberate than the tiles beside it.

**Battery is not a toggle and must not pretend to be.** It opens a screen and reports a level. Its
tile carries the same shape but never the "on" fill, or the panel starts lying about what is
switchable — the exact failure `tray-controls.md` refused for the radios.

**The surface itself has to sit above a wallpaper.** A flat white card over an arbitrary wallpaper
either glares or disappears. The bar solved this with a scrim plus a hairline plus a sheen, and the
panel should use the same treatment so the shell reads as one material.

## Requirement

1. **The panel is a glossy surface** — a vertical sheen, a hairline border and rounded corners, the
   same treatment the bar uses rather than a flat fill.
2. **Wi-Fi, Bluetooth and Battery are tiles**, laid out together, each carrying its glyph, its name
   and its state.
3. **A tile that is "on" is filled with the accent** and draws its glyph against that fill; one that
   is off or unknown stays plain.
4. **Battery never takes the on-fill.** It is informational, and a filled battery tile would claim a
   switch that does not exist.
5. **An unknown state reads as off rather than on** — never claim a radio is connected because we
   could not tell.
6. **The volume control is the panel's primary control**: a full-width row with the glyph, a
   substantial track, and the level shown as a percentage.
7. **Every tile keeps its existing action** — the routing from `tray-controls.md` is unchanged, and
   the same accessible names remain so the wiring stays under test.
8. **Cost stays flat.** The sheen is one gradient per surface, tiles are solid fills; no blur, no
   stacked translucency (SRS §4.3).
9. **The tile's fill decision is a pure function**, tested for every state including battery.

## Acceptance criteria

Verified on the `Pixel_Tablet` AVD (Android 17, 2560×1600 @ 320 dpi), light theme.

- [x] The panel draws a sheen and a hairline rather than a flat fill.
- [x] Wi-Fi, Bluetooth and Battery render as tiles with glyph, name and state.
- [x] A connected radio's tile is accent-filled with its glyph and text drawn against the fill; the
      battery tile stays plain.
- [x] An unknown radio's tile is not filled (`tileIsOn`).
- [x] The battery tile is never filled, at any level or charging state (`tileIsOn`).
- [x] The volume row shows the level as a percentage and updates live — eight hardware volume-ups
      moved the device to 7/15 and the row to 47% without reopening the panel.
- [x] Clicking each tile still produces the specified action: Wi-Fi launched
      `SettingsPanelActivity`, Bluetooth `ConnectedDeviceDashboardActivity`, Battery
      `PowerUsageSummaryActivity`. The slider drove `STREAM_MUSIC` 8 → 14 → 1.
- [x] The existing `QuickSettingsTest` wiring assertions pass unchanged — the restyle did not move
      the seams they hold.
- [x] The fill decision is unit-tested across on / off / unknown / battery / volume.
- [x] `./gradlew test lint assembleDebug` green.
- [x] **Fixed after review:** the press indication drew as a square. Compose clips indication to the
      node's bounds, and the tile's rounded shape lived only in its background — so the ripple on a
      rounded tile was a rectangle. The tile is now clipped to its shape before the click modifier.

## Notes

- **Why not a 2×2 grid.** Three tiles and one slider is what this panel holds; a grid would leave a
  hole or invite a filler tile. A row of three reads as a set without inventing a fourth control.
- **Why the percentage rather than a tooltip.** The slider is dragged with the pointer and the value
  matters while dragging; a number that is always present costs one text node and removes the need
  for a hover affordance the touch path would not have.
- **The slider needed its own track and thumb.** With the tiles restyled, the stock Material
  control was visibly the least finished thing in the panel — and it is the *only* one of the four
  that changes anything locally. The `thumb`/`track` slots are still marked experimental; opting in
  locally was the better trade against shipping the panel's one real control looking provisional.
- **Not in this slice:** notifications, a brightness slider, media controls, output-device
  switching, or editing which tiles appear.

# Shell — Context Menus & Drag-and-Drop

Status: **Accepted · Implemented** (2026-08-20)

Right-click and long-press context menus, and dragging apps between the desktop and the taskbar.
Derives from SRS §6.1 (principle 3, pointer-first/touch-tolerant) and §8 (pointer). Supersedes the
"right-click context menus" criterion left unchecked in [`dock-taskbar.md`](dock-taskbar.md) and the
"row menu is an explicit affordance" note in [`start-menu.md`](start-menu.md). Depends on
[`pinning.md`](pinning.md).

## Context

Pin/unpin currently hangs off a `⋯` button on every row and every desktop icon. It works, but it is
the wrong shape twice over: it puts a permanent piece of UI on every item to hold an action almost
nobody uses, and it is not what anyone with a mouse reaches for. On a PC the answer is right-click;
on touch it is long-press. Both should reach the same menu.

Dragging is the other half. Pinning by opening a menu and choosing an item is the *fallback* path;
dragging an icon onto the taskbar is the one people actually expect, and dragging one off it is how
they expect to remove it. The two directions are the same operation as the menu items, so they must
go through the same store operations rather than growing their own.

**Right-click exists on Android and Compose exposes it.** A mouse's secondary button arrives as a
pointer event with `buttons.isSecondaryPressed`; it is not a gesture Android hides. What Compose
lacks is a ready-made `onRightClick` modifier, so this needs a small custom gesture — not a
workaround, just the missing convenience.

**Long-press and drag compete for the same gesture**, which is the real design problem here. Press
and hold on touch could mean either. The resolution is the one every launcher uses: hold *and
release* opens the menu; hold *and move* becomes a drag. And a mouse should not have to wait — with
a pointer, movement past the touch slop starts a drag immediately, because waiting out a long-press
timer on a desktop feels broken.

**Drag has to survive the move to overlay windows.** Today the desktop grid and the bar are in one
composition, so a self-managed drag works. When `overlay-service.md` moves the bar into a
`TYPE_APPLICATION_OVERLAY` window, source and target are in *different windows*, and a
composition-local drag stops crossing between them. This slice therefore keeps the drag payload a
plain app id and the drop decision a pure function of coordinates, so the transport underneath can
be swapped for platform drag-and-drop without touching the interactions above it.

## Requirement

1. **A shared gesture modifier** used by every surface that lists an app — desktop grid, Start menu
   row, dock icon — so the three cannot drift apart.
2. **Right-click opens the context menu.** Detected from the pointer's secondary button, on press,
   without waiting for release.
3. **Long-press opens the same menu** on touch, when the finger does not move.
4. **Drag beats long-press.** Moving past the touch slop during a long-press starts a drag and
   suppresses the menu. With a mouse, movement past slop starts the drag **immediately**, with no
   long-press wait.
5. **Dragging must not fight the grid.** A drag gesture on a scrollable surface must not steal
   vertical scrolling on touch.
6. **Desktop → taskbar pins.** Dropping an app onto the bar pins it, through the same `pin`
   operation the menu uses.
7. **Taskbar → desktop unpins.** Dragging a dock icon off the bar and releasing over the desktop
   unpins it.
8. **The drop target is a pure function** of the pointer position and the bar's bounds, returning
   dock / desktop / none — so every case is testable without a device.
9. **A drag is visible.** The dragged icon follows the pointer, and the surface that would receive
   it is highlighted, so a drop is never a guess. Releasing outside any target is a no-op, and the
   ghost disappears.
10. **The `⋯` affordance stays.** Right-click and long-press are additions, not replacements: the
    explicit button is the only route that works for keyboard-only users and screen readers.

## Acceptance criteria

- [x] One gesture modifier (`appItemGestures`) is used by the desktop grid, Start menu rows, and
      dock icons.
- [~] Right-click opens the context menu on press. **Implemented but not verified end-to-end** —
      `adb shell input` cannot synthesise a secondary button and `input motionevent` errors on this
      image, so this path has only been exercised by reading `buttons.isSecondaryPressed`. Needs a
      physical mouse to confirm.
- [x] Long-press without movement opens the same menu (test + device).
- [x] Long-press followed by movement starts a drag and no menu appears (test).
- [x] With a mouse, movement past slop starts a drag with no long-press delay.
- [x] Touch movement *before* the long-press timer is left to the scrollable ancestor, so the grid
      and the Start list still scroll (test + device).
- [x] Dropping a desktop icon on the bar pins it — verified on device against the `pins` store.
- [x] Dropping a dock icon on the desktop unpins it — verified on device against the `pins` store.
- [x] The drop-target function is pure and tested: over the bar, over the desktop, outside both, on
      both boundaries, and with a zero-height and an inverted bar rect.
- [x] A drag ghost follows the pointer and the bar highlights while it is the candidate target.
- [x] Releasing over no target changes nothing and clears the ghost (test).
- [x] The `⋯` affordance still opens the menu.
- [x] `./gradlew test` green (186 project-wide); `./gradlew lint` clean.

## Notes

- **Depends-on / blocks:** depends on `pinning.md` for the operations. Blocks nothing, but
  `overlay-service.md` will have to revisit the transport (see below).
- **Why self-managed drag rather than `dragAndDropSource`/`dragAndDropTarget`:** the platform APIs
  carry a `ClipData` and are built for crossing process and window boundaries — which is exactly
  what will be needed *later*, and exactly what is not needed now. A self-managed drag has no
  serialisation, no MIME types, and can render its own ghost with the treated icon. The seam is the
  pure drop-target function; swapping transports later should not touch any interaction.
- **Known limit:** while the bar lives inside the HOME activity, a drag cannot leave the app. Once
  the bar is an overlay window this needs `DRAG_FLAG_GLOBAL` and real platform drag-and-drop, and
  that is unproven for a `TYPE_APPLICATION_OVERLAY` target — it should be prototyped before
  `overlay-service.md` commits to a design.
- **`positionChange()` returns zero once a change is consumed.** Consuming before reading the delta
  turned every drag into a stationary one: the ghost never moved and every drop landed back where it
  started. Read the delta first, then consume. This cost a debugging round on device and is the kind
  of thing that looks correct in review.
- **The whole gesture has to stay inside one `awaitPointerEventScope`.** Leaving and re-entering it
  between the down and the timer drops the events in between, so the drag never began. The timer
  uses the scope's own `withTimeoutOrNull`, not the coroutines one.
- **The travel that crosses slop is replayed into the first `onDrag`.** Slop detection consumes that
  movement, and without replaying it a drag starts a couple of hundred pixels behind the finger.
- **`adb shell input swipe` cannot express "hold, then move"** — it interpolates from the first
  frame, so every synthetic swipe reads as a scroll. The three touch outcomes are told apart in a
  Compose test using `advanceEventTime`, which is the only way to control the timing.
- **Not in this slice:** reordering pins by dragging *within* the dock, dragging to a specific dock
  position, dragging into folders, dragging files, drag between displays, or a full context menu
  beyond pin/unpin and App info.

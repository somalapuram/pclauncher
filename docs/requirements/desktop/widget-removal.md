# Desktop — Widget Removal

Status: **Implemented** (2026-08-24)

A context menu on a widget, carrying **Remove widget**. Closes the "removal pending" gap left open
by [`widget-host.md`](widget-host.md). Amends requirement 3 of
[`widget-resize.md`](widget-resize.md) — see *Requirement 2* below — and depends on
[`../shell/direct-manipulation.md`](../shell/direct-manipulation.md) for the gesture vocabulary.

## Context

A widget can be added and resized but never removed. The only way out of a mistaken add is to clear
the launcher's data, which takes the whole desktop arrangement with it. That is not a gap in
polish — it makes adding a widget a decision the user cannot walk back, so the sensible response to
the Add widget menu is to avoid it.

**Removing a widget is two deletions, not one.** The placement leaves our layout store, and the
widget id must be handed back to the framework with `AppWidgetHost.deleteAppWidgetId`. Dropping
only the placement leaks the id: the host keeps the binding alive, the provider keeps being asked
for updates for a widget nobody can see, and the id is never reused. The cached host view and
cached resize permission have to go with it, or a later widget allocated the same id inherits them.

**There is nowhere to put the option yet.** The desktop's own long-press menu deliberately does
*not* open when the press lands on a widget — that press means "resize me", and letting both fire
was a race. So a widget has no menu at all, and the press that would summon one is already spoken
for.

**Which is the real problem with the current gesture.** Long-press on a widget goes straight into
resize mode: one gesture, one hidden outcome, and no way to discover that anything else is
possible. Every other surface in the shell answers a long press with a menu — the desktop, an icon,
a dock item. The widget is the exception, and the exception is why removal has no home. Making the
widget answer with a menu costs one tap on the rare occasion someone resizes, and buys a place for
every widget action that will ever exist.

**A removed widget must not strand the resize frame.** If the removed widget is the one in resize
mode, the frame has to go with it rather than hanging over an empty cell.

## Requirement

1. **Long-press (or right-click) on a widget opens that widget's context menu**, positioned at the
   widget.
2. **The menu offers Resize and Remove widget.** *Resize* enters the resize mode specified by
   `widget-resize.md`; this supersedes that doc's requirement 3, where the long press entered
   resize mode directly. Everything else in `widget-resize.md` is unchanged.
3. **Resize is offered only when the provider permits it.** A `RESIZE_NONE` widget shows a menu
   with Remove alone — it must still be removable.
4. **Remove widget deletes the placement** from the layout store and the cell becomes free.
5. **Remove widget releases the widget id** via `AppWidgetHost.deleteAppWidgetId`, and drops the
   cached host view and cached resize permission for that id.
6. **Removing the widget being resized leaves resize mode**, taking the frame with it.
7. **The desktop's own menu still does not open over a widget** — the existing guard stands, so
   exactly one menu answers any press.
8. **Removal survives restart**: the widget does not come back.
9. **A failed store write costs the removal, not the desktop** — the same `runCatching` treatment
   every other layout write gets (GATE 4).

## Acceptance criteria

Verified on the `pclauncher_desktop_api34` emulator with the Google Search widget.

- [x] Long-press on a widget opens a menu at the widget rather than entering resize mode.
- [x] The menu shows Resize and Remove widget for a resizable provider.
- [ ] A `RESIZE_NONE` widget's menu shows Remove widget and no Resize — the code path is the same
      `permission.isResizable` check the resize handles use and which was verified on device for
      Chrome Dino, but the menu itself was not re-checked against a `RESIZE_NONE` provider.
- [x] Resize from the menu enters resize mode and dragging still works as `widget-resize.md` says.
- [x] Remove widget clears the placement — the layout store went from `widget:6|9|4|5|1` to empty.
- [x] `deleteAppWidgetId` is called: `dumpsys appwidget` entries for the package dropped from 6 to
      5 on removal and did not come back on restart.
- [x] Removing the widget currently in resize mode dismisses the frame (the grid clears
      `resizingWidget` before reporting the removal).
- [x] The desktop's own menu does not open when the press lands on a widget.
- [x] The removed widget is still gone after a force-stop and relaunch.
- [x] `DesktopLayout.without` and the store's remove are unit-tested, including removing an id that
      is not there (`WidgetDragTest`).
- [x] `./gradlew test lint assembleDebug` green.

## Notes

- **Why the id must be released and not merely forgotten:** `deleteAppWidgetId` is what tells
  `AppWidgetService` to drop the binding. Without it the provider stays bound to a live id for a
  widget with no view, which is a background cost the user cannot see or stop.
- **The desktop's guard was deciding against the first frame.** `appItemGestures` creates its
  pointer handler once and keeps the lambdas it was given, so the check for "did this press land on
  a widget" was reading the empty layout and the one-row grid of the very first composition — and
  the desktop menu opened on top of every widget. The values it reads now come through
  `rememberUpdatedState`. This is the third time a captured-once lambda has produced a bug that is
  invisible in the signature.
- **Undo is not in this slice.** Removing a widget is one menu item away and re-adding it is two,
  so the cheaper answer is to make Add easy rather than to build an undo stack for a rare action.
- **Not in this slice:** removing a widget by dragging it off the desktop, a confirmation prompt,
  reconfiguring a widget, or a Remove item for desktop *icons* (an icon is not removable — it is
  the app list).

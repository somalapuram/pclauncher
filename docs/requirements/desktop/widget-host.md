# Desktop — Widget Host

Status: **Accepted · In progress** (2026-08-20)

Android app widgets on the desktop, added from its context menu. SRS §14 Phase 7; derives from
SRS §6.6 and §11 (`BIND_APPWIDGET`). Depends on [`icon-grid.md`](icon-grid.md) for the cells they
occupy and the menu they are added from.

## Context

Widgets are the one thing on an Android desktop that a launcher cannot fake. They are other apps'
views, hosted inside ours, and the hosting protocol is specific: allocate an id, bind it to a
provider, ask the host for a view, and put that view on screen. Get the order wrong and you get a
blank rectangle with no error.

**Binding is a permission dance, and it is the part that goes wrong.** `BIND_APPWIDGET` cannot be
granted to a normal app at install; `bindAppWidgetIdIfAllowed` succeeds only for a default launcher
that already holds it, and otherwise returns false and needs the system's bind dialog. Both paths
must work — pclauncher is frequently *not* the default home during Stage A development, and a
widget host that only works when it is would go untested until Stage B.

**Some widgets need configuring before they exist.** A provider with a configure activity must have
it run after binding and before display; skipping it yields a widget that renders nothing and looks
like our bug.

**Ids are a resource that leaks.** An allocated id that is never bound, or a widget removed without
deleting its id, stays allocated in the host forever. Every failure path has to release its id, or
a user who cancels the picker ten times has ten orphans.

## Requirement

1. **An `AppWidgetHost` owned by the desktop**, listening while the desktop is visible and stopped
   when it is not.
2. **Add Widget lists installed providers** with their label and preview, from
   `AppWidgetManager.getInstalledProviders()`.
3. **Binding handles both paths:** try `bindAppWidgetIdIfAllowed`, and fall back to the system bind
   dialog when it refuses.
4. **A provider with a configure activity is configured** after binding and before it is shown.
5. **Every failure path releases its id** — cancelled picker, refused bind, cancelled configure,
   removed widget. No orphans.
6. **Widgets occupy desktop cells** like icons, sized from the provider's minimum cell span, and
   their placement persists.
7. **A widget can be removed** from its context menu, which deletes the id.
8. **The desktop survives a widget that misbehaves.** A provider that fails to inflate leaves a
   placeholder, not a broken desktop (GATE 4).
9. **The id lifecycle is pure where it can be** — span-to-cells arithmetic and the bind-outcome
   decision are functions, tested without a device.

## Acceptance criteria

- [ ] An `AppWidgetHost` starts and stops with the desktop's visibility.
- [ ] Add Widget lists installed providers with label and preview.
- [ ] `bindAppWidgetIdIfAllowed` is tried first; the system bind dialog is used when it refuses.
- [ ] A provider with a configure activity is configured before display.
- [ ] Cancelling the picker, the bind, or the configure releases the allocated id (test).
- [ ] Removing a widget deletes its id.
- [ ] Widgets occupy cells sized from the provider's minimum span, and placement persists.
- [ ] A provider that fails to inflate yields a placeholder, not a crash.
- [ ] Span arithmetic and bind-outcome logic are pure and unit-tested.
- [ ] `./gradlew test` green 3×; `./gradlew lint` clean; a real widget verified on device.

## Notes

- **Hosted through `AndroidView`.** `AppWidgetHostView` is a `View`; Compose hosts it rather than
  reimplementing it. Glance is for *providing* widgets and is not relevant to hosting them.
- **Not in this slice:** resizing widgets by drag, widget stacks, reconfiguring an existing widget,
  or previewing before adding.

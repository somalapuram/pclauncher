# Desktop — Widget Host

Status: **Accepted · Partially implemented** (2026-08-20)

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

- [x] An `AppWidgetHost` starts with the desktop and stops in `onDestroy`.
- [x] Add Widget lists installed providers with label, preview and cell size — 29 shown on device.
- [x] `bindAppWidgetIdIfAllowed` is tried first; the system bind dialog is used when it refuses.
      Verified on device: `AllowBindAppWidgetActivity` appears, because pclauncher is not the
      default home — the consent path is the common one in Stage A, exactly as predicted.
- [x] A provider with a configure activity is configured before display (flow implemented).
- [x] Cancelling the picker, bind, or configure releases the allocated id.
- [x] Widgets occupy cells sized from the provider's minimum span, and placement persists —
      verified: `widget:3|2|3` survives restart.
- [x] Bind-outcome, span arithmetic and the id-release rule are pure and unit-tested.
- [ ] **The provider's view does not inflate.** `getAppWidgetInfo` returns null for a bound id, so
      every widget falls back to the placeholder. Diagnosed no further — see Notes.
- [ ] Removing a widget from its context menu is not implemented.
- [x] A provider that fails to inflate yields a placeholder, not a crash — which is what is on
      screen today.
- [x] `./gradlew test` green (246 project-wide); `./gradlew lint` clean.

## Notes

- **Hosted through `AndroidView`.** `AppWidgetHostView` is a `View`; Compose hosts it rather than
  reimplementing it. Glance is for *providing* widgets and is not relevant to hosting them.
- **What actually works, and what does not.** The whole flow up to display is verified on device:
  picker, the system bind dialog, id allocation, placement at a free cell, persistence across
  restart. What does not work is the last step — `AppWidgetManager.getAppWidgetInfo` returns null
  for the bound id, so `createView` cannot build a view and the placeholder shows instead. The
  guard doing its job is why the desktop still looks correct rather than blank.
- **A widget must not choose its cell from the store alone.** The first build did, and the widget
  landed on cell (0,0) directly on top of an auto-placed icon — precisely the failure this doc
  warned about. Auto-placement lived in the UI and never reached the store, so the store believed
  every cell was free. Auto-placement is now computed once, above both, and the free cell is chosen
  against that effective layout.
- **Host views are cached per id.** `createView` allocates a real `View` and registers it with the
  host, so calling it per recomposition churns views and detaches the one on screen.
- **Not in this slice:** resizing widgets by drag, widget stacks, reconfiguring an existing widget,
  or previewing before adding.

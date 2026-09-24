package com.somalapuram.pclauncher.overlay

import android.content.Context
import android.provider.Settings

/**
 * Whether the shell may float its chrome over other windows.
 *
 * `SYSTEM_ALERT_WINDOW` is a *special* permission: not granted at install and not requestable
 * through the runtime dialog, only through a Settings screen the user has to visit. So it is
 * detected, never assumed — the shell runs either way and only its hosting changes
 * (overlay-service.md, SRS §5.1).
 */
fun canDrawOverlay(context: Context): Boolean =
    runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)

/**
 * Where the chrome should be drawn.
 *
 * Exactly one of these is true at a time, which is the point: two bars on screen would be worse
 * than the one that hides behind app windows.
 */
enum class ChromeHost { Overlay, HomeActivity }

/**
 * Which host draws the bar.
 *
 * The overlay only wins when it can actually be shown *and* is running. A permission that was
 * granted but a service that died leaves the chrome in the activity rather than nowhere (GATE 4).
 *
 * And only on the default display. The overlay window belongs to `ShellOverlayService`, whose
 * `WindowManager` comes from the service's own context, i.e. display 0 — there is exactly one of
 * it and it is not on any other screen. `isChromeUp` is one global flow, so a home on a second
 * monitor would otherwise see "up", hide its own bar, and have no overlay on its display either:
 * a desktop with no bar at all. A home that is not on the default display therefore always keeps
 * the chrome, whatever the permission or the service says (secondary-display-home.md).
 *
 * [onDefaultDisplay] defaults to `true` so that every existing caller, and any future one that
 * does not think about displays, gets today's display-0 behaviour rather than a silent change.
 */
fun chromeHostFor(
    hasPermission: Boolean,
    overlayRunning: Boolean,
    onDefaultDisplay: Boolean = true,
): ChromeHost =
    if (onDefaultDisplay && hasPermission && overlayRunning) ChromeHost.Overlay
    else ChromeHost.HomeActivity

package com.somalapuram.pclauncher.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
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

/** The Settings screen that grants it, scoped to this app rather than the whole list. */
fun overlayPermissionIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

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
 */
fun chromeHostFor(hasPermission: Boolean, overlayRunning: Boolean): ChromeHost =
    if (hasPermission && overlayRunning) ChromeHost.Overlay else ChromeHost.HomeActivity

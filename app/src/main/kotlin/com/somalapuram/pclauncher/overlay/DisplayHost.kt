package com.somalapuram.pclauncher.overlay

import android.view.Display

/**
 * Whether a home activity attached to [displayId] is the one on the built-in, default display.
 *
 * Read at runtime from the activity's real display, never configured: no port numbers, no
 * `display_settings.xml` entries, no per-board anything — the same rule the platform port lives
 * by. The platform decides which display is default; this only reports it
 * (secondary-display-home.md requirement 5).
 *
 * `null` means the activity has no display yet. That cannot happen after `attach`, which is
 * before `onCreate`, so it is a defensive branch — and the safe answer for it is *default*:
 * treating display 0 as secondary would put a second bar on the one screen everyone uses,
 * while treating a secondary display as default merely delays its bar until the next
 * recomposition. Prefer the mistake that is invisible.
 */
fun isDefaultDisplay(displayId: Int?): Boolean =
    displayId == null || displayId == Display.DEFAULT_DISPLAY

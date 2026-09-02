package com.somalapuram.pclauncher.overlay

import android.view.WindowManager

/**
 * The size and focus a chrome window is created with, and keeps.
 *
 * A value rather than two arguments at the call site, because the whole point is that these are
 * decided once and never changed afterwards: every `updateViewLayout` that alters height or
 * focusability re-lays-out the window and re-creates its surface, which reads as the bar blinking
 * (overlay-window-split.md). Naming the two shapes makes it visible that there are exactly two.
 */
data class OverlayWindowSpec(
    val height: Int,
    val focusable: Boolean,
)

/**
 * The bar: as tall as its content and no taller, and never focusable.
 *
 * Non-focusable at rest is not a detail — a focusable overlay takes every keystroke from the app
 * the user is actually typing into (SRS §9).
 */
val BarWindowSpec = OverlayWindowSpec(
    height = WindowManager.LayoutParams.WRAP_CONTENT,
    focusable = false,
)

/**
 * A menu: the full screen, and focusable.
 *
 * Full-screen because the click-catcher behind the menu has to cover the screen for a click
 * anywhere else to dismiss it. Focusable because a menu that cannot take keys cannot be typed into
 * or dismissed with Esc — and this window only exists while the menu is open, so the keystrokes it
 * takes are the ones meant for it.
 */
val MenuWindowSpec = OverlayWindowSpec(
    height = WindowManager.LayoutParams.MATCH_PARENT,
    focusable = true,
)

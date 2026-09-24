package com.somalapuram.pclauncher

/**
 * The desktop on any display that is not the default one.
 *
 * WindowManager will not place a `singleTask` home on a secondary display
 * (`RootWindowContainer.canStartHomeOnDisplayArea`: "Can't launch home on secondary displays if
 * it requested to be single instance"), and looks instead for `CATEGORY_SECONDARY_HOME` in the
 * primary home's package. This class exists to be that declaration — `singleTop`, so one instance
 * per display — and adds nothing else on purpose: what differs on a second monitor is decided in
 * [HomeActivity] from the display it is actually on, not from which class was launched, so the
 * two can never drift apart. Same shape as Launcher3's `SecondaryDisplayLauncher`
 * (secondary-display-home.md).
 */
class SecondaryHomeActivity : HomeActivity()

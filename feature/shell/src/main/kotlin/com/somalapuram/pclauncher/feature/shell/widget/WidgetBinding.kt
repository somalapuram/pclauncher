package com.somalapuram.pclauncher.feature.shell.widget

/** What has to happen next to get a widget on screen. */
sealed interface BindOutcome {
    /** Bound already — go straight to configure-or-show. */
    data object Bound : BindOutcome

    /** The system must ask the user. */
    data object NeedsUserConsent : BindOutcome

    /** Nothing worked; release the id. */
    data object Failed : BindOutcome
}

/**
 * Decide what to do with a bind attempt.
 *
 * `bindAppWidgetIdIfAllowed` succeeds only for a launcher that already holds `BIND_APPWIDGET`,
 * which pclauncher usually is *not* during Stage A. Both paths have to work or the widget host
 * would go untested until Stage B (widget-host.md).
 */
fun bindOutcomeFor(allowedDirectly: Boolean, canAskUser: Boolean): BindOutcome = when {
    allowedDirectly -> BindOutcome.Bound
    canAskUser -> BindOutcome.NeedsUserConsent
    else -> BindOutcome.Failed
}

/**
 * How many cells a widget needs.
 *
 * Providers report minimum size in dp; the desktop thinks in cells. Rounding *up* matters: a
 * widget given less room than it asked for clips its own content and looks like our bug.
 */
fun cellsFor(minSizeDp: Int, cellSizeDp: Int): Int {
    if (cellSizeDp <= 0) return 1
    if (minSizeDp <= 0) return 1
    return ((minSizeDp + cellSizeDp - 1) / cellSizeDp).coerceAtLeast(1)
}

/**
 * Whether an allocated id must be released.
 *
 * Every failure path leaks otherwise — a user who cancels the picker ten times leaves ten orphans
 * in the host, and nothing ever collects them (widget-host.md requirement 5).
 */
fun shouldReleaseId(bound: Boolean, configured: Boolean, cancelled: Boolean): Boolean =
    cancelled || !bound || !configured

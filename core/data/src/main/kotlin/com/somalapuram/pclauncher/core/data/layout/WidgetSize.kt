package com.somalapuram.pclauncher.core.data.layout

/**
 * The size, in dp, a placement occupies.
 *
 * A property of the placement rather than of an event. Deriving it from "the user just resized" is
 * what left widgets un-sized when they were merely *placed* — or re-created after a restart, which
 * is every widget on every boot (widget-sizing.md).
 */
data class WidgetSizeDp(val width: Int, val height: Int)

/** Span × cell. Guards against a zero or negative cell, which would report a nonsense size. */
fun widgetSizeDp(span: DesktopSpan, cellWidthDp: Int, cellHeightDp: Int): WidgetSizeDp =
    WidgetSizeDp(
        width = (span.columns * cellWidthDp).coerceAtLeast(0),
        height = (span.rows * cellHeightDp).coerceAtLeast(0),
    )

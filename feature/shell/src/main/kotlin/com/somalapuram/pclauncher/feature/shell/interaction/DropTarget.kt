package com.somalapuram.pclauncher.feature.shell.interaction

/** Where a drag would land if released now. */
enum class DropTarget {
    /** The taskbar — dropping here pins. */
    Dock,

    /** The desktop — dropping a dock icon here unpins. */
    Desktop,

    /** Nowhere useful; releasing does nothing. */
    None,
}

/**
 * Which surface the pointer is over.
 *
 * Pure and coordinate-only, deliberately: this is the seam that lets the drag *transport* change
 * later. When the bar moves into an overlay window the mechanics of carrying a payload between
 * windows will be different, but "is the pointer over the bar" stays exactly this function
 * (direct-manipulation.md requirement 8).
 */
fun dropTargetFor(
    pointerY: Float,
    barTopY: Float,
    barBottomY: Float,
    desktopTopY: Float = 0f,
): DropTarget = when {
    // A zero-height or inverted bar can never be a target; without this an invalid rect would
    // swallow every drop at that coordinate.
    barBottomY <= barTopY -> if (pointerY >= desktopTopY) DropTarget.Desktop else DropTarget.None

    // The bar wins where the two touch: it is the smaller, more deliberate target, and the desktop
    // is forgiving of a near-miss in a way the bar is not.
    pointerY in barTopY..barBottomY -> DropTarget.Dock

    pointerY in desktopTopY..<barTopY -> DropTarget.Desktop

    else -> DropTarget.None
}

/**
 * Does dropping [item] on [target] change anything?
 *
 * Dropping a pinned app back on the bar, or an unpinned one on the desktop, is a no-op — and saying
 * so here keeps the highlight honest: a target that would do nothing is not highlighted.
 */
fun dropChangesAnything(target: DropTarget, isPinned: Boolean): Boolean = when (target) {
    DropTarget.Dock -> !isPinned
    DropTarget.Desktop -> isPinned
    DropTarget.None -> false
}

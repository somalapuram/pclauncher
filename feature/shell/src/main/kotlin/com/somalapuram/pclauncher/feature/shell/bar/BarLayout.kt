package com.somalapuram.pclauncher.feature.shell.bar

/**
 * Where the bar's three zones sit.
 *
 * The dock is centred **on the screen**, not on the space left over between the Start button and
 * the chips. Centring on the leftover space makes the dock drift left as windows open, which is
 * both ugly and a moving target for muscle memory.
 */
object BarLayout {

    /**
     * Left edge of the dock, in bar coordinates.
     *
     * Clamped so that when the dock is too wide to be centred without overlapping a neighbouring
     * zone, it gives way rather than colliding — a squeezed dock is recoverable, an overlapping
     * one is not.
     */
    fun dockStartX(
        barWidth: Float,
        dockWidth: Float,
        leadingZoneWidth: Float,
        trailingZoneWidth: Float,
        gap: Float,
    ): Float {
        val centred = (barWidth - dockWidth) / 2f
        val minX = leadingZoneWidth + gap
        val maxX = barWidth - trailingZoneWidth - gap - dockWidth
        if (maxX < minX) return minX
        return centred.coerceIn(minX, maxX)
    }

    /** Width of a dock holding [itemCount] items. Zero items is a valid, zero-width dock. */
    fun dockWidth(itemCount: Int, itemPitch: Float, padding: Float): Float =
        if (itemCount == 0) 0f else itemCount * itemPitch + padding * 2f

    /**
     * How many window chips fit before the strip has to scroll.
     *
     * Returns at least one whenever there is any room at all: showing zero chips and a scrollbar
     * is worse than showing one and scrolling.
     */
    fun visibleChipCount(availableWidth: Float, chipWidth: Float, gap: Float): Int {
        if (availableWidth <= 0f || chipWidth <= 0f) return 0
        val perChip = chipWidth + gap
        val count = ((availableWidth + gap) / perChip).toInt()
        return count.coerceAtLeast(if (availableWidth >= chipWidth) 1 else 0)
    }

    /**
     * Chip width, shrinking toward [minWidth] as more windows open before falling back to
     * scrolling — the same bargain Windows strikes, and it keeps titles readable for longer than a
     * fixed width would.
     */
    fun chipWidthFor(
        availableWidth: Float,
        chipCount: Int,
        preferredWidth: Float,
        minWidth: Float,
        gap: Float,
    ): Float {
        if (chipCount <= 0) return preferredWidth
        val fair = (availableWidth - gap * (chipCount - 1)) / chipCount
        return fair.coerceIn(minWidth, preferredWidth)
    }
}

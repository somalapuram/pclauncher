package com.somalapuram.pclauncher.feature.shell.bar

import kotlin.math.abs
import kotlin.math.cos

/**
 * macOS dock magnification (SRS §6.3).
 *
 * Pure arithmetic on purpose. This runs for every dock item on every pointer move, and on
 * `pc_x86_64` that happens on a CPU renderer — so the result must be a scale factor applied to an
 * already-composited bitmap via a graphics layer, never anything that triggers re-layout or
 * re-rasterises the icon.
 */
object DockMagnification {

    /** Peak scale directly under the pointer. */
    const val MAX_SCALE = 1.62f

    /** How many neighbours either side are affected. Wider reads as mushy; narrower as jumpy. */
    const val FALLOFF_ITEMS = 2.6f

    /**
     * Scale for the item at [index] when the pointer is at [pointerIndex] in item-space
     * (fractional, so the peak glides between items rather than snapping to one).
     *
     * A raised cosine rather than a linear ramp: linear puts a visible crease at the edge of the
     * falloff, and the eye reads the crease as a bug.
     */
    fun scaleAt(
        index: Int,
        pointerIndex: Float?,
        maxScale: Float = MAX_SCALE,
        falloff: Float = FALLOFF_ITEMS,
    ): Float {
        if (pointerIndex == null) return 1f

        val distance = abs(index - pointerIndex)
        if (distance >= falloff) return 1f

        // cos ramps 1 -> 0 over the falloff with zero gradient at both ends, so the curve meets
        // the un-magnified neighbours smoothly.
        val t = distance / falloff
        val bell = (1f + cos(t * Math.PI.toFloat())) / 2f
        return 1f + (maxScale - 1f) * bell
    }

    /**
     * Where the pointer sits in item-space, or null when it is outside the dock.
     *
     * Returned as a fraction so magnification tracks the pointer continuously; snapping to the
     * nearest index makes the dock lurch.
     */
    fun pointerIndexFor(
        pointerX: Float?,
        dockStartX: Float,
        itemPitch: Float,
        itemCount: Int,
    ): Float? {
        if (pointerX == null || itemCount == 0 || itemPitch <= 0f) return null

        val index = (pointerX - dockStartX) / itemPitch - 0.5f
        // A little tolerance past each end so the outermost icon still magnifies as the pointer
        // approaches it from outside the dock.
        val tolerance = 1f
        if (index < -tolerance || index > itemCount - 1 + tolerance) return null
        return index
    }

    /**
     * Extra height the bar needs so a magnified icon is not clipped.
     *
     * The bar grows rather than the icon overflowing: an icon drawn outside the bar has no
     * background behind it and reads as a rendering glitch over the wallpaper.
     */
    fun barHeightFor(baseHeight: Float, iconSize: Float, maxScale: Float = MAX_SCALE): Float =
        baseHeight + iconSize * (maxScale - 1f) * GROWTH_SHARE

    /** The bar takes part of the growth; the rest lifts the icon above the bar's centre line. */
    private const val GROWTH_SHARE = 0.55f
}

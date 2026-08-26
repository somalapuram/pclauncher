package com.somalapuram.pclauncher.core.design

/**
 * What hovering and pressing do to a surface, in one place.
 *
 * Three surfaces implementing "a bit bigger, a bit lighter" separately is how they end up with
 * three different bits. The numbers live here and each surface picks the ones that apply to it: an
 * icon takes the scale and the wash, a widget takes only the outline, a menu row only the wash.
 */
object PcHover {

    /** Enough to notice at a glance, not enough to shove a neighbouring cell's artwork. */
    const val HoveredScale = 1.08f

    /** Pressing reads as pushing *in*, so it goes the other way from hover. */
    const val PressedScale = 0.96f

    const val HoveredWash = 0.12f
    const val PressedWash = 0.18f
    const val HoveredOutline = 0.55f

    /**
     * The scale a pointer-aware surface should draw at.
     *
     * Pressed outranks hovered because a press is always accompanied by a hover, and the press is
     * the more specific thing the user is doing.
     */
    fun scaleFor(hovered: Boolean, pressed: Boolean = false): Float = when {
        pressed -> PressedScale
        hovered -> HoveredScale
        else -> 1f
    }

    /** How strong the background wash behind a hovered target is. Zero means draw nothing. */
    fun washFor(hovered: Boolean, pressed: Boolean = false): Float = when {
        pressed -> PressedWash
        hovered -> HoveredWash
        else -> 0f
    }

    /**
     * How visible a hovered outline is.
     *
     * For surfaces that must not change size — a hosted widget is someone else's rendered UI, and
     * scaling it resamples their pixels into a blur at the sizes widgets occupy.
     */
    fun outlineFor(hovered: Boolean): Float = if (hovered) HoveredOutline else 0f
}

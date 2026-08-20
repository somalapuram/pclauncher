package com.somalapuram.pclauncher.core.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/**
 * The design tokens every shell surface draws from (SRS §6.1).
 *
 * No feature module declares a raw colour, dimension, or duration — they come from here, so the
 * shell reads as one system and a change lands in one place.
 */
object PcSpacing {
    val Hairline = 1.dp
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
}

object PcSize {
    /** SRS §6.1: chrome is thin and quiet. */
    val MenuBarHeight = 28.dp
    val DockHeightAtRest = 56.dp
    val DockHeightMax = 64.dp
    val DockIcon = 40.dp

    /** SRS §6.1 principle 3 — pointer-first, but never below a touchable target. */
    val MinTouchTarget = 44.dp

    /** SRS §13: the desktop grid. */
    val DesktopGridCell = 96.dp
}

object PcCorners {
    val Surface = 12.dp
    val Dock = 16.dp
    val Popover = 10.dp
}

/**
 * Motion is macOS: spring-based, short, spatial (SRS §6.1 principle 4).
 *
 * Every spec here must survive being switched off — on a software renderer the shell drops to
 * [Instant] rather than animating a full-screen surface per frame (SRS §4.3).
 */
object PcMotion {
    val Surface = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    val DockMagnify = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    val Instant = spring<Float>(stiffness = Spring.StiffnessHigh)
}

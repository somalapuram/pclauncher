package com.somalapuram.pclauncher.feature.shell.bar

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How big the grid of boxes inside the Start button is drawn (start-glyph-size.md).
 *
 * Pure, like the fill decision beside it, so the sizes can be pinned by a test rather than read off
 * a screenshot.
 */
fun startGlyphSize(isOpen: Boolean): Dp = if (isOpen) StartGlyphOpen else StartGlyphAtRest

/**
 * The glyph at rest — 30% larger than the 20 dp it was drawn at.
 *
 * At 20 dp it was 42% of its 48 dp tile: a small mark centred in a large empty square. Now that the
 * tile is a glossy object (start-button-gloss.md) rather than an outline, the undersized glyph is
 * what stands out.
 */
val StartGlyphAtRest: Dp = 26.dp

/**
 * And larger again while the menu is up.
 *
 * Size is a second channel for the same fact the accent fill already carries. That redundancy is
 * the point: SRS §6.1 principle 5 asks that state be distinguished by more than colour alone, and
 * an accent derived from the wallpaper is not guaranteed to contrast with the bar behind it.
 *
 * Still 9 dp clear of the tile's edge a side, so it never reads as clipped.
 */
val StartGlyphOpen: Dp = 30.dp

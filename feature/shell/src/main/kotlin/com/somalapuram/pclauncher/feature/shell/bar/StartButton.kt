package com.somalapuram.pclauncher.feature.shell.bar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcMotion
import com.somalapuram.pclauncher.core.design.PcSize
import com.somalapuram.pclauncher.core.design.surfaceSheen

/**
 * The Start button — the visual anchor of the whole shell, so it carries the brand rather than a
 * generic ⊞ (requirement 2).
 *
 * Its tile uses the same squircle silhouette as the app icons, so the bar reads as one set.
 */
@Composable
fun StartButton(
    isOpen: Boolean,
    onClick: () -> Unit,
    glyph: ImageVector,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = LocalPcColors.current
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    // The state machine lives in `startTileFill` so it can be tested without a composition; here
    // it is only resolved against the theme (start-button-gloss.md).
    val fill = startTileFill(isOpen = isOpen, pressed = pressed, hovered = hovered)
    val base = when (fill.role) {
        StartTileRole.Accent -> colors.accent
        StartTileRole.Surface -> colors.onSurface
    }
    val background by animateColorAsState(base.copy(alpha = fill.alpha), label = "start-bg")
    val gloss = startTileGloss(fill.role)
    // Animated as a Float because `PcMotion.DockMagnify` is a Float spring — the same one the
    // press-scale above already rides, rather than a second curve for the same gesture.
    val pressScrim by animateFloatAsState(
        targetValue = pressScrimAlpha(pressed),
        animationSpec = PcMotion.DockMagnify,
        label = "start-press-scrim",
    )
    val glyphSize by animateFloatAsState(
        targetValue = startGlyphSize(isOpen).value,
        animationSpec = PcMotion.DockMagnify,
        label = "start-glyph-size",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = PcMotion.DockMagnify,
        label = "start-scale",
    )

    Box(
        modifier = modifier
            // ≥ 44 dp even inside a ≤ 64 dp bar (SRS §6.1 principle 3).
            .size(PcSize.MinTouchTarget)
            .clickable(
                interactionSource = interactionSource,
                // No ripple: the shell's surfaces state-change instead (SRS §6.1).
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = "Start" },
        contentAlignment = Alignment.Center,
    ) {
        // Allocated once rather than per recomposition: this is drawn every frame the pointer
        // moves along the bar.
        val tile = remember { RoundedCornerShape(SquircleCornerPercent) }

        Box(
            modifier = Modifier
                .size(PcSize.DockIcon)
                .scale(scale)
                // A sheen rather than a flat fill, so the button reads as glass like the icons it
                // sits beside. One gradient over a fill that was happening anyway is what a
                // software renderer can afford (SRS §4.3).
                .background(
                    brush = run {
                        // Alpha from the shell's sheen, colour from the role's profile. A
                        // translucent tile only needs the alpha; an opaque accent has no alpha
                        // headroom and must shade its colour instead (start-button-gloss.md).
                        val stops = surfaceSheen(background.alpha, lift = StartSheenLift)
                            .zip(listOf(gloss.top, gloss.middle, gloss.bottom))
                            .map { (a, mix) -> background.copy(alpha = a).shadedBy(mix) }
                        // Placed, not evenly spaced: the midpoint sits below the glyph so the
                        // shaded underside does not run through it.
                        Brush.verticalGradient(
                            0f to stops[0],
                            TileGlossMidStop to stops[1],
                            1f to stops[2],
                        )
                    },
                    shape = tile,
                )
                // Over the fill but under the glyph. `IconStyle.glaze` records why a highlight
                // has to sit above the surface it is lighting; the glyph still has to stay
                // legible (requirement 5), and at 20 dp in a 48 dp tile the band is almost
                // entirely clear of it anyway.
                .drawBehind {
                    val corner = CornerRadius(size.minDimension * SquircleCornerPercent / 100f)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = startSpecularAlpha(pressed)),
                            StartSpecularStop to Color.Transparent,
                        ),
                        cornerRadius = corner,
                    )
                    // Over the finished tile, so the press dims whatever the gloss produced
                    // rather than being tuned against it (start-press-dim.md). Still under the
                    // glyph, which keeps the label legible while the surface goes dark.
                    if (pressScrim > 0f) {
                        drawRoundRect(
                            color = Color.Black.copy(alpha = pressScrim),
                            cornerRadius = corner,
                        )
                    }
                }
                .border(
                    width = 1.dp,
                    color = if (isOpen) colors.accent else colors.hairline,
                    shape = tile,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = glyph,
                contentDescription = null,
                tint = if (isOpen) colors.onAccent else colors.onSurface,
                modifier = Modifier.size(glyphSize.dp),
            )
        }
    }
}

/**
 * Corner radius as a percentage, chosen to approximate the icon squircle.
 *
 * Compose has no superellipse shape, and building a custom `Shape` per surface would cost a path
 * allocation on every recomposition. The icon tiles are true squircles because they are baked once
 * into a bitmap; live chrome uses this approximation instead, which is indistinguishable at 40 dp
 * and free.
 */
const val SquircleCornerPercent = 30

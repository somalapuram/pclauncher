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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcMotion
import com.somalapuram.pclauncher.core.design.RoundedTriangleShape
import com.somalapuram.pclauncher.core.design.PcSize

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
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = LocalPcColors.current
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    // Open outranks hover: while the menu is up the button must read as the thing that owns it,
    // even after the pointer moves into the menu.
    val target = when {
        isOpen -> colors.accent
        pressed -> colors.accent.copy(alpha = 0.45f)
        hovered -> colors.onSurface.copy(alpha = 0.14f)
        else -> Color.Transparent
    }
    val background by animateColorAsState(target, label = "start-bg")

    // Follows the menu's state rather than the click, so a menu dismissed by clicking elsewhere
    // turns the mark back too (start-button-mark.md).
    val markRotation by animateFloatAsState(
        targetValue = if (isOpen) 180f else 0f,
        animationSpec = PcMotion.Surface,
        label = "start-mark-turn",
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
        // A tile like every other item in the bar, carrying a mark that turns. The bar reads as a
        // row of equals, which a bare triangle among squircles did not (start-button-mark.md).
        Box(
            modifier = Modifier
                .size(PcSize.DockIcon)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(background, RoundedCornerShape(SquircleCornerPercent))
                .border(
                    width = 1.dp,
                    color = if (isOpen) colors.accent else colors.hairline,
                    shape = RoundedCornerShape(SquircleCornerPercent),
                ),
            contentAlignment = Alignment.Center,
        ) {
            // The mark is the shape itself rather than a vector, so its corner rounding is a value
            // rather than a hand-drawn path, and it is the same one the tests pin.
            Box(
                modifier = Modifier
                    .size(MarkSize)
                    // Rotated, not swapped: turning one mark reads as the same object moving,
                    // where cross-fading two glyphs reads as a substitution.
                    .graphicsLayer { rotationZ = markRotation }
                    .background(
                        color = if (isOpen) colors.onAccent else colors.onSurface,
                        shape = MarkShape,
                    ),
            )
        }
    }
}

/**
 * The mark inside the tile.
 *
 * A triangle covers about half the area of the square bounding it, so it is drawn larger than a
 * glyph would be to carry the same optical weight.
 */
private val MarkSize = 18.dp

/** Rounded enough to sit among squircles without being a bare geometric triangle. */
private val MarkShape = RoundedTriangleShape(corner = 4.dp)

/**
 * Corner radius as a percentage, chosen to approximate the icon squircle.
 *
 * Compose has no superellipse shape, and building a custom `Shape` per surface would cost a path
 * allocation on every recomposition. The icon tiles are true squircles because they are baked once
 * into a bitmap; live chrome uses this approximation instead, which is indistinguishable at 40 dp
 * and free.
 */
const val SquircleCornerPercent = 30

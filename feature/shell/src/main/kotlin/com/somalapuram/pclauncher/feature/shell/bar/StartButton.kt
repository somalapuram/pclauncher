package com.somalapuram.pclauncher.feature.shell.bar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcMotion
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
    glyph: ImageVector,
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
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = PcMotion.DockMagnify,
        label = "start-scale",
    )

    Box(
        modifier = modifier
            // ≥ 44 dp even inside a ≤ 64 dp bar (SRS §6.1 principle 3).
            .size(PcSize.MinTouchTarget)
            .semantics { contentDescription = "Start" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(PcSize.DockIcon)
                .scale(scale)
                .background(background, RoundedCornerShape(SquircleCornerPercent))
                .border(
                    width = 1.dp,
                    color = if (isOpen) colors.accent else colors.hairline,
                    shape = RoundedCornerShape(SquircleCornerPercent),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = glyph,
                contentDescription = null,
                tint = if (isOpen) colors.onAccent else colors.onSurface,
                modifier = Modifier.size(20.dp),
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

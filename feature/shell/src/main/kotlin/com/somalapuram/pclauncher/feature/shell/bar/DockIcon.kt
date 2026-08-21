package com.somalapuram.pclauncher.feature.shell.bar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.somalapuram.pclauncher.core.design.PcSize
import com.somalapuram.pclauncher.feature.shell.interaction.appItemGestures

/**
 * One dock icon, magnified.
 *
 * [scale] is applied through a **graphics layer** on an already-composited bitmap — no re-layout,
 * no re-rasterising of the icon (SRS §4.3). That is what makes magnification affordable on a CPU
 * renderer, and it is why the icon treatment bakes its gloss and shadow into the bitmap rather
 * than drawing them live.
 */
@Composable
fun DockIcon(
    item: DockItem,
    scale: Float,
    painter: Painter?,
    modifier: Modifier = Modifier,
    iconSize: Dp = PcSize.DockIcon,
    onClick: () -> Unit = {},
    onContextMenu: () -> Unit = {},
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    val colors = LocalPcColors.current
    // Root coordinates, because the drop test compares against the bar's own bounds.
    var originInRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(PcSize.MinTouchTarget)
            .onGloballyPositioned { originInRoot = it.positionInRoot() }
            .appItemGestures(
                key = item.id,
                onClick = onClick,
                onContextMenu = { _ -> onContextMenu() },
                onDragStart = { local -> onDragStart(originInRoot + local) },
                onDrag = onDrag,
                onDragEnd = onDragEnd,
            )
            .semantics {
                contentDescription = buildString {
                    append(item.label)
                    if (item.isRunning) append(", running")
                    if (!item.isAvailable) append(", unavailable")
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    // Grow upward from the bar, the way a dock does — scaling about the centre
                    // would push the icon through the bottom edge.
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.85f)
                }
                // Unavailable entries are shown greyed rather than dropped, matching the
                // inventory's contract (requirement 7).
                .alpha(if (item.isAvailable) 1f else 0.4f),
            contentAlignment = Alignment.Center,
        ) {
            if (painter != null) {
                Image(painter = painter, contentDescription = null, modifier = Modifier.size(iconSize))
            } else {
                // A tile-shaped placeholder, so the dock does not reflow when icons arrive.
                Box(
                    Modifier
                        .size(iconSize)
                        .background(colors.onSurfaceMuted.copy(alpha = 0.18f), RoundedCornerShape(SquircleCornerPercent)),
                )
            }
        }

        // Running state is shape, not colour (SRS §6.1 principle 5): a dot when running, a wider
        // pill when focused, so the two are distinguishable without relying on hue.
        if (item.isRunning) {
            Box(
                Modifier
                    .size(width = if (item.isFocused) 14.dp else 4.dp, height = 3.dp)
                    .background(colors.accent, RoundedCornerShape(50)),
            )
        } else {
            Spacer(Modifier.size(3.dp))
        }
    }
}

/** Wrap a platform drawable's bitmap for Compose without re-decoding it every recomposition. */
fun bitmapPainterFor(drawable: android.graphics.drawable.Drawable?): Painter? {
    val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return null
    return BitmapPainter(bitmap.asImageBitmap())
}

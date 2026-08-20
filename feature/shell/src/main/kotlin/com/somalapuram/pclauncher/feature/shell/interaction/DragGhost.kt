package com.somalapuram.pclauncher.feature.shell.interaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.feature.shell.bar.bitmapPainterFor
import kotlin.math.roundToInt

/**
 * The icon that follows the pointer during a drag.
 *
 * A drag with no ghost is a guess — the user has to infer that anything is happening at all
 * (direct-manipulation.md requirement 9). Placed with `offset` and drawn from the already-treated
 * bitmap, so following the pointer costs a layer translation and nothing else.
 */
@Composable
fun DragGhost(
    drag: DragState,
    iconFor: (com.somalapuram.pclauncher.core.apps.AppEntry) -> android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
) {
    val entry = drag.entry ?: return
    val painter = bitmapPainterFor(iconFor(entry)) ?: return
    val halfPx = with(androidx.compose.ui.platform.LocalDensity.current) { (size / 2).toPx() }

    Box(
        modifier = modifier
            .offset { IntOffset((drag.position.x - halfPx).roundToInt(), (drag.position.y - halfPx).roundToInt()) }
            .size(size)
            // Slightly transparent and lifted, so it reads as being carried rather than dropped.
            .alpha(0.85f)
            .graphicsLayer { scaleX = 1.1f; scaleY = 1.1f },
    ) {
        Image(painter = painter, contentDescription = null, modifier = Modifier.size(size))
    }
}

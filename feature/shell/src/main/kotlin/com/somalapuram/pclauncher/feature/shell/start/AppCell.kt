package com.somalapuram.pclauncher.feature.shell.start

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcHover
import com.somalapuram.pclauncher.core.design.PcMenu
import com.somalapuram.pclauncher.core.design.PcMotion
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.feature.shell.bar.bitmapPainterFor
import com.somalapuram.pclauncher.feature.shell.interaction.appItemGestures

@Composable
internal fun AppCell(
    entry: AppEntry,
    isSelected: Boolean,
    isPinned: Boolean,
    painter: android.graphics.drawable.Drawable?,
    onLaunch: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val colors = LocalPcColors.current
    var menuOpen by remember { mutableStateOf(false) }

    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()

    // Hover and keyboard selection are different states and must not look the same: the caret can
    // be on one row while the pointer rests on another, and the user needs to see which is which.
    // Selection is the stronger of the two, being where Enter will act.
    val wash by animateFloatAsState(
        targetValue = when {
            isSelected -> SelectedWash
            menuOpen -> PcHover.PressedWash
            else -> PcHover.washFor(hovered)
        },
        animationSpec = PcMotion.DockMagnify,
        label = "start-entry-wash",
    )
    val scale by animateFloatAsState(
        targetValue = PcHover.scaleFor(hovered && !isSelected),
        animationSpec = PcMotion.DockMagnify,
        label = "start-entry-scale",
    )

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .hoverable(interactions)
                .appItemGestures(
                    key = entry.key,
                    enabled = entry.isLaunchable,
                    onClick = onLaunch,
                    onContextMenu = { _ -> menuOpen = true },
                )
                // After the gesture: a transform applies to everything inside it, and scaling the
                // gesture node scales the coordinates it reports.
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(
                    colors.onSurface.copy(alpha = wash),
                    RoundedCornerShape(PcCorners.Popover),
                )
                .padding(vertical = PcSpacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
        ) {
            // Unavailable and suspended entries are greyed rather than hidden — the inventory's
            // contract, carried through to every surface that lists an app.
            Box(modifier = Modifier.alpha(if (entry.isLaunchable) 1f else 0.4f)) {
                bitmapPainterFor(painter)?.let {
                    Image(painter = it, contentDescription = null, modifier = Modifier.size(44.dp))
                } ?: Box(Modifier.size(44.dp))
            }

            Text(
                text = entry.label,
                color = if (entry.isLaunchable) colors.onSurface else colors.onSurfaceMuted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }

        if (isPinned) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(6.dp)
                    .background(colors.accent, RoundedCornerShape(50)),
            )
        }

        PcMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(if (isPinned) "Unpin from taskbar" else "Pin to taskbar") },
                onClick = { onTogglePin(); menuOpen = false },
            )
        }
    }
}

/** The keyboard caret's row. Stronger than a hover, because Enter acts here. */
internal const val SelectedWash = 0.14f

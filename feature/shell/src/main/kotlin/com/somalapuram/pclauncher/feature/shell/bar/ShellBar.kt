package com.somalapuram.pclauncher.feature.shell.bar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.LocalSurfaceTreatment
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcMotion
import com.somalapuram.pclauncher.core.design.PcSize
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.core.design.SurfaceTreatment

/**
 * The one bar (SRS §6.3): Start at the left, dock centred, window chips after a separator,
 * Show Desktop at the right.
 *
 * Two bars would eat 120 dp of a 1600 px screen and force the user to learn which strip owns which
 * behaviour, so both heritages share one.
 *
 * Takes no dependency on being hosted in an activity — no `Activity` casts, no assumptions about
 * who owns window insets — because `overlay-service.md` will host these very same composables in
 * a `TYPE_APPLICATION_OVERLAY` window.
 */
@Composable
fun ShellBar(
    state: BarState,
    startGlyph: ImageVector,
    isStartOpen: Boolean = false,
    onStartClick: () -> Unit,
    onDockItemClick: (DockItem) -> Unit,
    onWindowFocus: (WindowChip) -> Unit,
    onWindowClose: (WindowChip) -> Unit,
    onShowDesktop: () -> Unit,
    modifier: Modifier = Modifier,
    /** Reports the bar's vertical extent in root coordinates, for the drop test. */
    onBoundsChanged: (Float, Float) -> Unit = { _, _ -> },
    /** True while a drag would land here — the bar lights up so a drop is never a guess. */
    isDropTarget: Boolean = false,
    onItemContextMenu: (DockItem) -> Unit = {},
    onItemDragStart: (DockItem, androidx.compose.ui.geometry.Offset) -> Unit = { _, _ -> },
    onItemDrag: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onItemDragEnd: () -> Unit = {},
) {
    val colors = LocalPcColors.current
    val density = LocalDensity.current

    var pointerX by remember { mutableStateOf<Float?>(null) }
    var dockOriginX by remember { mutableStateOf(0f) }

    val itemPitchPx = with(density) { PcSize.MinTouchTarget.toPx() }
    val pointerIndex = if (state.magnificationEnabled) {
        DockMagnification.pointerIndexFor(
            pointerX = pointerX,
            dockStartX = dockOriginX,
            itemPitch = itemPitchPx,
            itemCount = state.dockItems.size,
        )
    } else {
        null
    }

    val alpha = when (val treatment = LocalSurfaceTreatment.current) {
        is SurfaceTreatment.Scrim -> treatment.alpha
        is SurfaceTreatment.Blur -> treatment.scrimAlpha
    }

    // The bar grows so a magnified icon is contained; an icon drawn outside it would float over
    // the wallpaper with no background and read as a glitch.
    val extraHeight by animateFloatAsState(
        targetValue = if (pointerIndex != null) MagnifiedExtraHeight else 0f,
        animationSpec = PcMotion.DockMagnify,
        label = "bar-height",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PcSize.DockHeightAtRest)
            .height(PcSize.DockHeightAtRest + extraHeight.dp)
            .onGloballyPositioned {
                val top = it.positionInRoot().y
                onBoundsChanged(top, top + it.size.height)
            }
            .background(colors.scrim.copy(alpha = alpha), RoundedCornerShape(PcCorners.Dock))
            .border(
                width = if (isDropTarget) 2.dp else 1.dp,
                color = if (isDropTarget) colors.accent else colors.hairline,
                shape = RoundedCornerShape(PcCorners.Dock),
            )
            .padding(horizontal = PcSpacing.Small)
            .pointerInput(state.magnificationEnabled) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        pointerX = event.changes.lastOrNull()?.position?.x
                    }
                }
            },
        verticalAlignment = Alignment.Bottom,
    ) {
        StartButton(
            isOpen = isStartOpen || state.isStartOpen,
            onClick = onStartClick,
            glyph = startGlyph,
            modifier = Modifier.align(Alignment.CenterVertically),
        )

        Spacer(Modifier.width(PcSpacing.Small))

        // The dock is centred on the screen, not on the leftover space, so it does not drift left
        // as windows open (BarLayout.dockStartX).
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Row(
                modifier = Modifier.onSizeChanged { /* origin captured below via position */ },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                state.dockItems.forEachIndexed { index, item ->
                    DockIcon(
                        item = item,
                        scale = DockMagnification.scaleAt(index, pointerIndex),
                        painter = bitmapPainterFor(item.icon),
                        onClick = { onDockItemClick(item) },
                        onContextMenu = { onItemContextMenu(item) },
                        onDragStart = { at -> onItemDragStart(item, at) },
                        onDrag = onItemDrag,
                        onDragEnd = onItemDragEnd,
                    )
                }
            }
        }

        if (state.windows.isNotEmpty()) {
            Spacer(Modifier.width(PcSpacing.Small))
            Box(
                Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(colors.hairline)
                    .align(Alignment.CenterVertically),
            )
            Spacer(Modifier.width(PcSpacing.Small))

            WindowChipRow(
                windows = state.windows,
                onFocus = onWindowFocus,
                onClose = onWindowClose,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .align(Alignment.CenterVertically),
            )
        }

        Spacer(Modifier.width(PcSpacing.Small))
        ShowDesktopHandle(onClick = onShowDesktop, modifier = Modifier.align(Alignment.CenterVertically))
    }
}

@Composable
private fun ShowDesktopHandle(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalPcColors.current
    Box(
        modifier = modifier
            .width(6.dp)
            .height(28.dp)
            .background(colors.hairline, RoundedCornerShape(50))
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    )
}

private const val MagnifiedExtraHeight = 14f

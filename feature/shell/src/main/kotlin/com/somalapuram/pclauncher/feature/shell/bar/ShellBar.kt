package com.somalapuram.pclauncher.feature.shell.bar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import com.somalapuram.pclauncher.core.design.surfaceSheen
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
import com.somalapuram.pclauncher.feature.shell.tray.SystemTray
import com.somalapuram.pclauncher.feature.shell.tray.TrayState

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
    tray: TrayState = TrayState(),
    onTrayAction: (com.somalapuram.pclauncher.feature.shell.tray.TrayAction) -> Unit = {},
) {
    val colors = LocalPcColors.current
    val density = LocalDensity.current

    var pointerX by remember { mutableStateOf<Float?>(null) }
    // Both measured in root coordinates so they can be compared. The previous version declared
    // this, read it, and never assigned it — so the pointer was mapped against a dock starting at
    // x = 0 while the real one is centred in a 2560 px bar.
    var dockOriginInRoot by remember { mutableStateOf(0f) }
    var barContentOriginInRoot by remember { mutableStateOf(0f) }

    // The width a dock item actually lays out at, not the touch minimum it used to assume: an item
    // sizes to the larger of the two, and the icons are now bigger than the minimum.
    val itemPitchPx = with(density) { maxOf(PcSize.MinTouchTarget, PcSize.DockIcon).toPx() }
    val pointerIndex = if (state.magnificationEnabled) {
        DockMagnification.pointerIndexFor(
            // Lifted into root space: the pointer arrives local to the bar's padded content box.
            pointerX = pointerX?.let { it + barContentOriginInRoot },
            dockStartX = dockOriginInRoot,
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Fixed. Growing the bar to contain a magnified icon moved the whole piece of
            // furniture — and everything measuring against its top edge — whenever a pointer
            // crossed it. A dock's icons rise above its background; the background stays put.
            .height(PcSize.DockHeightAtRest)
            .onGloballyPositioned {
                val top = it.positionInRoot().y
                onBoundsChanged(top, top + it.size.height)
            }
            // A sheen rather than a flat fill: the bar frames glossy icons, and one linear
            // gradient is the affordable way to give it a surface of its own.
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    surfaceSheen(alpha).map { colors.scrim.copy(alpha = it) },
                ),
                shape = RoundedCornerShape(PcCorners.Dock),
            )
            .border(
                width = if (isDropTarget) 2.dp else 1.dp,
                color = if (isDropTarget) colors.accent else colors.hairline,
                shape = RoundedCornerShape(PcCorners.Dock),
            )
            .padding(horizontal = PcSpacing.Small)
            // Captured at the same level the pointer is read at, so the two agree about where
            // zero is.
            .onGloballyPositioned { barContentOriginInRoot = it.positionInRoot().x }
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
            // `weight` sizes the width; without an explicit alignment this box still takes the
            // Row's `Alignment.Bottom` and its content-sized height rides the bar's lower edge.
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
            // Centred like everything else in the bar. Bottom alignment used to be here for
            // magnification's sake, but magnification is a `graphicsLayer` transform — a draw-time
            // effect that is independent of where layout rests the icon (bar-alignment.md).
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.onGloballyPositioned {
                    dockOriginInRoot = it.positionInRoot().x
                },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
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

        Spacer(Modifier.width(PcSpacing.Medium))
        SystemTray(
            state = tray,
            onAction = onTrayAction,
            modifier = Modifier.align(Alignment.CenterVertically),
        )

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


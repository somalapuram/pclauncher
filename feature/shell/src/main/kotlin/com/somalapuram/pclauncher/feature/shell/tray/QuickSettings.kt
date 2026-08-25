package com.somalapuram.pclauncher.feature.shell.tray

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.core.design.surfaceSheen

/**
 * The quick-settings popover behind every tray indicator.
 *
 * One panel rather than one per icon: SRS §6.2 asks for a grouped control, and Windows 11 agrees —
 * clicking network, volume or battery there opens the same flyout.
 *
 * Only the slider acts locally. Every other tile hands off to a system surface, because the
 * platform does not let an app toggle the radios (tray-controls.md) — and a switch that silently
 * fails is worse than a control that plainly takes you somewhere it works.
 */
@Composable
fun QuickSettingsPanel(
    state: TrayState,
    onAction: (TrayAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPcColors.current
    val shape = RoundedCornerShape(PcCorners.Popover)

    Column(
        modifier = modifier
            .width(360.dp)
            // The same treatment as the bar, so the shell reads as one material rather than a flat
            // card that happens to float over the wallpaper.
            .background(
                brush = Brush.verticalGradient(
                    surfaceSheen(1f, lift = 0.06f).map { colors.surface.copy(alpha = it) },
                ),
                shape = shape,
            )
            .border(1.dp, colors.hairline, shape)
            .padding(PcSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(PcSpacing.Medium),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpacing.Small)) {
            QuickTile(
                label = "Wi-Fi",
                value = state.wifi.label(connected = "Connected", off = "Off"),
                on = tileIsOn(TrayIndicator.Wifi, state),
                onClick = { onAction(TrayAction.OpenWifiPanel) },
                modifier = Modifier.weight(1f),
            ) { tint -> WifiGlyph(bars = wifiBars(state.wifi), color = tint) }

            QuickTile(
                label = "Bluetooth",
                value = state.bluetooth.label(connected = "On", off = "Off"),
                on = tileIsOn(TrayIndicator.Bluetooth, state),
                onClick = { onAction(bluetoothAction(state.bluetooth)) },
                modifier = Modifier.weight(1f),
            ) { tint -> BluetoothGlyph(on = state.bluetooth == ConnectionState.On, color = tint) }

            QuickTile(
                label = "Battery",
                value = state.battery.label(),
                // Never filled: it opens a screen and reports a level, and a filled tile would
                // advertise a switch that does not exist.
                on = tileIsOn(TrayIndicator.Battery, state),
                onClick = { onAction(TrayAction.OpenBatterySettings) },
                modifier = Modifier.weight(1f),
            ) { tint ->
                BatteryGlyph(
                    fill = batteryFill(state.battery),
                    charging = (state.battery as? BatteryState.Known)?.charging == true,
                    color = tint,
                )
            }
        }

        VolumeRow(state.volume) { onAction(it) }
    }
}

/**
 * The volume slider — the panel's one real control.
 *
 * Driven from [VolumeState] rather than from its own remembered position, so a change made with the
 * hardware keys moves it, and a write the system refuses springs the handle back to what the device
 * actually did rather than leaving a lie on screen.
 */
// The thumb/track slots are still marked experimental. Opted in deliberately and locally: the
// alternative is the stock control, and the panel's one real control looking unfinished is a worse
// trade than an annotation that may be renamed.
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VolumeRow(volume: VolumeState, onAction: (TrayAction) -> Unit) {
    val colors = LocalPcColors.current
    // Read outside the draw scope: inside `Canvas`, `colors` would still resolve but the intent is
    // clearer with the palette pulled out, and the lit stop is derived once rather than per frame.
    val trackBase = colors.onSurface.copy(alpha = 0.16f)
    val accentColor = colors.accent
    val accentLit = colors.accent.copy(alpha = 0.72f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                colors.onSurface.copy(alpha = 0.06f),
                RoundedCornerShape(PcCorners.Popover),
            )
            .padding(horizontal = PcSpacing.Small, vertical = PcSpacing.ExtraSmall),
        horizontalArrangement = Arrangement.spacedBy(PcSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VolumeGlyphIcon(glyph = volumeGlyph(volume), color = colors.onSurface)
        Slider(
            value = fractionOf(volume),
            onValueChange = { onAction(volumeAction(it, volume.max)) },
            enabled = volume.isKnown,
            modifier = Modifier.weight(1f),
            // The stock track and thumb read as thinner and less deliberate than the tiles above
            // them, on the one control in this panel that actually changes something.
            thumb = {
                Box(
                    Modifier
                        .size(18.dp)
                        .background(colors.accent, CircleShape)
                        .border(2.dp, colors.onAccent, CircleShape),
                )
            },
            track = {
                Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                    val radius = size.height / 2f
                    val filled = size.width * fractionOf(volume)
                    drawRoundRect(
                        color = trackBase,
                        cornerRadius = CornerRadius(radius),
                    )
                    if (filled > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(accentLit, accentColor)),
                            size = Size(filled.coerceAtLeast(size.height), size.height),
                            cornerRadius = CornerRadius(radius),
                        )
                    }
                }
            },
        )
        // Always present rather than on hover: the value matters while dragging, and the touch path
        // has no hover to reveal it with.
        Text(
            text = "${volumePercent(volume)}%",
            color = colors.onSurfaceMuted,
            fontSize = 12.sp,
            modifier = Modifier.width(40.dp),
        )
    }
}

/**
 * One control, as a tile.
 *
 * A filled tile says "on" in shape as well as in words (SRS §6.1: state is never carried by colour
 * alone). The glyph is handed the tint it must draw against, so an accent-filled tile does not end
 * up with a glyph that vanishes into it.
 */
@Composable
private fun QuickTile(
    label: String,
    value: String,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: @Composable (Color) -> Unit,
) {
    val colors = LocalPcColors.current
    val shape = RoundedCornerShape(PcCorners.Popover)
    val tint = if (on) colors.onAccent else colors.onSurface

    Column(
        modifier = modifier
            .background(
                brush = if (on) {
                    Brush.verticalGradient(listOf(colors.accent, colors.accent.copy(alpha = 0.82f)))
                } else {
                    Brush.verticalGradient(
                        listOf(
                            colors.onSurface.copy(alpha = 0.10f),
                            colors.onSurface.copy(alpha = 0.04f),
                        ),
                    )
                },
                shape = shape,
            )
            .border(1.dp, if (on) Color.Transparent else colors.hairline, shape)
            .clickable(onClick = onClick)
            .padding(vertical = PcSpacing.Small, horizontal = PcSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
    ) {
        glyph(tint)
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = if (on) colors.onAccent.copy(alpha = 0.82f) else colors.onSurfaceMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** On/off in the words a tile needs, keeping "unknown" honest rather than calling it off. */
fun ConnectionState.label(connected: String, off: String): String = when (this) {
    ConnectionState.On -> connected
    ConnectionState.Off -> off
    ConnectionState.Unknown -> "Unknown"
}

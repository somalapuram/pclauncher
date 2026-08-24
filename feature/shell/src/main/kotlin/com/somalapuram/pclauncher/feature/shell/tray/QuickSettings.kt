package com.somalapuram.pclauncher.feature.shell.tray

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcSpacing

/**
 * The quick-settings popover behind every tray indicator.
 *
 * One panel rather than one per icon: SRS §6.2 asks for a grouped control, and Windows 11 agrees —
 * clicking network, volume or battery there opens the same flyout. Four popovers would mean four
 * anchors and four dismiss rules for what the user reads as one thing.
 *
 * Only the slider acts locally. Every other row hands off to a system surface, because the platform
 * does not let an app toggle the radios (tray-controls.md) — and a switch that silently fails is
 * worse than a row that plainly takes you somewhere it works.
 */
@Composable
fun QuickSettingsPanel(
    state: TrayState,
    onAction: (TrayAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPcColors.current

    Column(
        modifier = modifier
            .width(280.dp)
            .background(colors.surface, RoundedCornerShape(PcCorners.Popover))
            .border(1.dp, colors.hairline, RoundedCornerShape(PcCorners.Popover))
            .padding(PcSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(PcSpacing.Small),
    ) {
        VolumeRow(state.volume) { onAction(it) }

        QuickRow(
            label = "Wi-Fi",
            value = state.wifi.label(connected = "Connected", off = "Off"),
            onClick = { onAction(TrayAction.OpenWifiPanel) },
        ) { WifiGlyph(bars = wifiBars(state.wifi), color = colors.onSurface) }

        QuickRow(
            label = "Bluetooth",
            value = state.bluetooth.label(connected = "On", off = "Off"),
            onClick = { onAction(bluetoothAction(state.bluetooth)) },
        ) { BluetoothGlyph(on = state.bluetooth == ConnectionState.On, color = colors.onSurface) }

        QuickRow(
            label = "Battery",
            value = state.battery.label(),
            onClick = { onAction(TrayAction.OpenBatterySettings) },
        ) {
            BatteryGlyph(
                fill = batteryFill(state.battery),
                charging = (state.battery as? BatteryState.Known)?.charging == true,
                color = colors.onSurface,
            )
        }
    }
}

/**
 * The volume slider.
 *
 * Driven from [VolumeState] rather than from its own remembered position, so a change made with the
 * hardware keys moves it — and so a write the system refuses springs the handle back to what the
 * device actually did rather than leaving a lie on screen.
 */
@Composable
private fun VolumeRow(volume: VolumeState, onAction: (TrayAction) -> Unit) {
    val colors = LocalPcColors.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = PcSpacing.ExtraSmall),
        horizontalArrangement = Arrangement.spacedBy(PcSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VolumeGlyphIcon(glyph = volumeGlyph(volume), color = colors.onSurface)
        Slider(
            value = fractionOf(volume),
            onValueChange = { onAction(volumeAction(it, volume.max)) },
            enabled = volume.isKnown,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
            ),
        )
    }
}

@Composable
private fun QuickRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    val colors = LocalPcColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = PcSpacing.Small),
        horizontalArrangement = Arrangement.spacedBy(PcSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        glyph()
        Text(text = label, color = colors.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = colors.onSurfaceMuted, fontSize = 12.sp)
    }
}

/** On/off in the words a row needs, keeping "unknown" honest rather than calling it off. */
fun ConnectionState.label(connected: String, off: String): String = when (this) {
    ConnectionState.On -> connected
    ConnectionState.Off -> off
    ConnectionState.Unknown -> "Unknown"
}

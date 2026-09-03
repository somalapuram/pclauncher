package com.somalapuram.pclauncher.feature.shell.tray

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Popup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcSpacing

/**
 * The tray: Bluetooth, Wi-Fi, battery, volume, clock (SRS §6.2).
 *
 * The four indicators are one click target, not four. They open the same quick-settings panel, so
 * there is one anchor and one dismiss rule for what a user reads as a single control
 * (tray-controls.md requirement 5).
 *
 * The panel itself is **not** drawn here. It is the host's, drawn in the same full-screen surface
 * that draws the Start menu and placed by the same alignment — because a panel anchored inside the
 * bar cannot be placed above it once the bar is a window of its own (tray-popover-host.md).
 */
@Composable
fun SystemTray(
    state: TrayState,
    modifier: Modifier = Modifier,
    isOpen: Boolean = false,
    onToggle: () -> Unit = {},
) {
    val colors = LocalPcColors.current
    val open = isOpen

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                // Clipped above the click, not merely painted below it: indication is bounded by
                // the node, so a rounded background alone leaves the press drawing as a rectangle.
                .clip(RoundedCornerShape(PcCorners.Popover))
                .clickable { onToggle() }
                .background(
                    if (open) colors.onSurface.copy(alpha = 0.10f)
                    else androidx.compose.ui.graphics.Color.Transparent,
                    RoundedCornerShape(PcCorners.Popover),
                )
                .padding(horizontal = PcSpacing.Small, vertical = PcSpacing.ExtraSmall)
                .semantics { contentDescription = state.describe() },
            horizontalArrangement = Arrangement.spacedBy(PcSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BluetoothGlyph(on = state.bluetooth == ConnectionState.On, color = colors.onSurfaceMuted)
            WifiGlyph(bars = wifiBars(state.wifi), color = colors.onSurfaceMuted)
            BatteryGlyph(
                fill = batteryFill(state.battery),
                charging = (state.battery as? BatteryState.Known)?.charging == true,
                color = colors.onSurfaceMuted,
            )
            VolumeGlyphIcon(glyph = volumeGlyph(state.volume), color = colors.onSurfaceMuted)
            Text(text = state.timeText, color = colors.onSurface, fontSize = 13.sp)
        }

    }
}

/**
 * From the anchor's top up to the panel's bottom.
 *
 * Covers the bar's upper half above the vertically-centred tray, plus the bar's own outer margin
 * and a little air — so the panel clears the bar instead of touching it.
 */
private val TrayPopoverGap = 32.dp

/** Battery as text. Charging is a prefix rather than a colour, so it survives being read in mono. */
fun BatteryState.label(): String = when (this) {
    is BatteryState.Known -> if (charging) "⚡$percent%" else "$percent%"
    BatteryState.Unknown -> "--%"
}

/** One spoken description for the whole tray — five separate announcements would be noise. */
fun TrayState.describe(): String = buildString {
    append(timeText)
    append(", battery ").append(battery.label())
    append(", wi-fi ").append(wifi.name.lowercase())
    append(", bluetooth ").append(bluetooth.name.lowercase())
    if (volume.isKnown) append(", volume ").append(volumeGlyph(volume).name.lowercase())
}

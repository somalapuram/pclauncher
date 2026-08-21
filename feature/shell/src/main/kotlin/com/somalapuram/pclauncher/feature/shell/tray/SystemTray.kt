package com.somalapuram.pclauncher.feature.shell.tray

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcSpacing

/**
 * The tray: Bluetooth, Wi-Fi, battery, clock (SRS §6.2).
 *
 * A status area, not a control panel — it shows state and nothing here toggles anything. A tray
 * that is half-interactive is worse than one that is honestly read-only.
 */
@Composable
fun SystemTray(state: TrayState, modifier: Modifier = Modifier) {
    val colors = LocalPcColors.current

    Row(
        modifier = modifier.semantics { contentDescription = state.describe() },
        horizontalArrangement = Arrangement.spacedBy(PcSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Off and unknown both draw nothing: a tray is glanced at, and an indicator that is only
        // ever present when it means something is faster to read than one that is always there in
        // three states.
        Text(
            text = connectionGlyph(state.bluetooth, on = "BT", off = "", unknown = ""),
            color = colors.onSurfaceMuted,
            fontSize = 11.sp,
        )
        Text(
            text = connectionGlyph(state.wifi, on = "WiFi", off = "", unknown = ""),
            color = colors.onSurfaceMuted,
            fontSize = 11.sp,
        )
        Text(
            text = state.battery.label(),
            color = colors.onSurfaceMuted,
            fontSize = 11.sp,
        )
        Text(
            text = state.timeText,
            color = colors.onSurface,
            fontSize = 13.sp,
        )
    }
}

/** Battery as text. Charging is a prefix rather than a colour, so it survives being read in mono. */
fun BatteryState.label(): String = when (this) {
    is BatteryState.Known -> if (charging) "⚡$percent%" else "$percent%"
    BatteryState.Unknown -> "--%"
}

/** One spoken description for the whole tray — four separate announcements would be noise. */
fun TrayState.describe(): String = buildString {
    append(timeText)
    append(", battery ").append(battery.label())
    append(", wi-fi ").append(wifi.name.lowercase())
    append(", bluetooth ").append(bluetooth.name.lowercase())
}

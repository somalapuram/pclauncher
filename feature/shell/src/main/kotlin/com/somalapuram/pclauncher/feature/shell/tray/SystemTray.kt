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
 * The four indicators are one click target, not four. They open the same quick-settings popover, so
 * there is one anchor and one dismiss rule for what a user reads as a single control
 * (tray-controls.md requirement 5).
 */
@Composable
fun SystemTray(
    state: TrayState,
    modifier: Modifier = Modifier,
    onAction: (TrayAction) -> Unit = {},
) {
    val colors = LocalPcColors.current
    var open by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable { open = !open }
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

        if (open) {
            // Anchored above the bar and right-aligned to the tray, the way every desktop puts it.
            Popup(
                alignment = Alignment.BottomEnd,
                offset = androidx.compose.ui.unit.IntOffset(0, -TrayPopupGap),
                onDismissRequest = { open = false },
                properties = androidx.compose.ui.window.PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true,
                ),
            ) {
                QuickSettingsPanel(
                    state = state,
                    onAction = { action ->
                        onAction(action)
                        // A hand-off leaves for another screen, so the panel has done its job. The
                        // slider is the exception: closing it on every drag step would make the
                        // control unusable.
                        if (action !is TrayAction.SetVolume) open = false
                    },
                )
            }
        }
    }
}

/** Enough that the panel clears the bar rather than growing out of it. */
private const val TrayPopupGap = 64

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

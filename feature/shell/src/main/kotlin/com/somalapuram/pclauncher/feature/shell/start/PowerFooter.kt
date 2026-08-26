package com.somalapuram.pclauncher.feature.shell.start

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcSpacing

/**
 * The Start menu's footer (SRS §6.4).
 *
 * Two of its four controls cannot work off the target device, and they are shown anyway — disabled,
 * with the reason. Hiding them would make the shell look less capable than the machine it ships on
 * and leave the Stage B work nowhere to land; SRS §5.3 asks for what is unavailable *and why*.
 */
@Composable
fun PowerFooter(
    deviceName: String,
    privileges: PowerPrivileges,
    onAction: (PowerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPcColors.current

    // The device name gets its own line. On one row with four controls it was squeezed to nothing
    // by the two that carry a reason underneath them.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PcSpacing.Small, vertical = PcSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
    ) {
        if (deviceName.isNotBlank()) {
            Text(text = deviceName, color = colors.onSurfaceMuted, fontSize = 11.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PowerActionOrder.forEach { action ->
                PowerControl(
                    action = action,
                    enabled = isAvailable(action, privileges),
                    reason = unavailableReason(action, privileges),
                    onClick = { onAction(action) },
                    // Equal shares, so a long reason cannot crowd out its neighbours.
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PowerControl(
    action: PowerAction,
    enabled: Boolean,
    reason: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPcColors.current
    val shape = RoundedCornerShape(PcCorners.Popover)
    val label = labelFor(action)

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.onSurface.copy(alpha = if (enabled) 0.08f else 0.03f), shape)
            // A disabled control is not clickable at all rather than clickable-and-ignored: a
            // press that visibly happens and then does nothing reads as a fault.
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = PcSpacing.Small, vertical = PcSpacing.ExtraSmall)
            .semantics {
                contentDescription = if (reason == null) label else "$label, unavailable: $reason"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = if (enabled) colors.onSurface else colors.onSurfaceMuted.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = if (enabled) FontWeight.Medium else FontWeight.Normal,
        )
        if (reason != null) {
            Text(text = reason, color = colors.onSurfaceMuted.copy(alpha = 0.6f), fontSize = 10.sp)
        }
    }
}

package com.somalapuram.pclauncher.feature.shell.start

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcGlyphs
import com.somalapuram.pclauncher.core.design.PcSpacing

/**
 * The Start menu's footer (SRS §6.4): the device, and two buttons.
 *
 * Buttons rather than a row of labelled controls. Four text buttons carrying their own
 * unavailability notes wrapped to three lines each and crowded out the device name — and a footer
 * is furniture, not a form.
 *
 * The power actions are grouped behind one control, the way Windows 11 does it. That is also what
 * makes the disabled ones explicable: a menu row has space for a reason, where an icon does not.
 */
@Composable
fun PowerFooter(
    deviceName: String,
    privileges: PowerPrivileges,
    onAction: (PowerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPcColors.current
    var powerMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PcSpacing.Small, vertical = PcSpacing.Small),
        horizontalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = deviceName,
            color = colors.onSurfaceMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )

        FooterButton(
            glyph = PcGlyphs.Settings,
            label = labelFor(PowerAction.OpenSettings),
            onClick = { onAction(PowerAction.OpenSettings) },
        )

        Box {
            FooterButton(
                glyph = PcGlyphs.Power,
                label = "Power",
                onClick = { powerMenuOpen = true },
            )

            DropdownMenu(
                expanded = powerMenuOpen,
                onDismissRequest = { powerMenuOpen = false },
            ) {
                PowerMenuActions.forEach { action ->
                    val reason = unavailableReason(action, privileges)
                    DropdownMenuItem(
                        enabled = reason == null,
                        text = {
                            Column {
                                Text(labelFor(action), fontSize = 13.sp)
                                // The reason lives here because a menu row has room for it and an
                                // icon does not.
                                if (reason != null) {
                                    Text(
                                        text = reason,
                                        color = colors.onSurfaceMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        },
                        onClick = { powerMenuOpen = false; onAction(action) },
                    )
                }
            }
        }
    }
}

/** What sits behind the power button. Settings is a button of its own and is not repeated here. */
private val PowerMenuActions = PowerActionOrder.filter { it != PowerAction.OpenSettings }

@Composable
private fun FooterButton(
    glyph: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalPcColors.current

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.onSurface.copy(alpha = 0.08f), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = glyph,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

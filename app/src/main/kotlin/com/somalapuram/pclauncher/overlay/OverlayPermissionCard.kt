package com.somalapuram.pclauncher.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners

/**
 * The one-time explanation for "Display over other apps".
 *
 * Written for someone who has never heard of `SYSTEM_ALERT_WINDOW`: it says what they get and what
 * they lose, in that order, and never names the permission the way the platform does. The Settings
 * screen they land on says "Display over other apps", so that phrase — and only that phrase —
 * appears here too, or the toggle they arrive at looks like a different thing entirely.
 *
 * Dismissible by design. The shell works without this permission; the card must never be a wall in
 * front of the desktop (GATE 4).
 */
@Composable
fun OverlayPermissionCard(
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
) {
    val colors = LocalPcColors.current

    Dialog(onDismissRequest = onNotNow) {
        Surface(
            shape = RoundedCornerShape(PcCorners.Surface),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.hairline),
            tonalElevation = 0.dp,
            shadowElevation = CardElevation,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = CardWidth)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Keep the taskbar on top",
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Turn on \"Display over other apps\" and the taskbar, Start menu and " +
                        "system tray stay visible while you use apps.\n\n" +
                        "Without it they show only on the desktop, and anything you open covers " +
                        "them. You can change this later in Settings.",
                    color = colors.onSurfaceMuted,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onNotNow) {
                        Text("Not now", color = colors.onSurfaceMuted)
                    }
                    TextButton(onClick = onAllow) {
                        Text("Open settings", color = colors.accent, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private val CardWidth = 420.dp

/** The same lift the shell's other floating surfaces carry. */
private val CardElevation = 12.dp

package com.somalapuram.pclauncher.prompts

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
 * A one-time explanation for a permission the user has to grant in Settings.
 *
 * Written for someone who has never heard of the permission's real name: it says what they get and
 * what they lose, in that order. The one platform phrase that does appear is the label of the
 * toggle they will land on — without it, the screen they arrive at looks like a different thing
 * entirely.
 *
 * Dismissible by design. The shell works without any of these; a card must never be a wall in front
 * of the desktop (GATE 4).
 */
@Composable
fun PermissionCard(
    title: String,
    body: String,
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
                    text = title,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = body, color = colors.onSurfaceMuted)
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

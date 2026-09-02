package com.somalapuram.pclauncher.feature.shell.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcSpacing
import androidx.compose.material3.Text

/**
 * The apps the user came back to most recently (recent-apps.md).
 *
 * Drawn in the same cells as All apps rather than a denser strip of its own: the two rows sit
 * directly above one another, and two sizes of the same thing a few pixels apart reads as a
 * rendering fault rather than a distinction.
 *
 * Laid out as a fixed number of equal columns, so a short row leaves its slots empty instead of
 * spreading four apps across the panel — which would put them at different x positions from the
 * grid beneath and make the alignment look broken.
 */
@Composable
fun RecentRow(
    entries: List<AppEntry>,
    columns: Int,
    selectedIndex: Int?,
    isPinned: (AppEntry) -> Boolean,
    iconFor: (AppEntry) -> android.graphics.drawable.Drawable?,
    onLaunch: (AppEntry) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    val colors = LocalPcColors.current

    Text(
        text = "Recent",
        color = colors.onSurfaceMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(top = PcSpacing.Medium, bottom = PcSpacing.Small),
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PcSpacing.ExtraSmall),
    ) {
        for (column in 0 until columns) {
            val entry = entries.getOrNull(column)
            Box(modifier = Modifier.weight(1f)) {
                if (entry != null) {
                    AppCell(
                        entry = entry,
                        isSelected = selectedIndex == column,
                        isPinned = isPinned(entry),
                        painter = iconFor(entry),
                        onLaunch = { onLaunch(entry) },
                        onTogglePin = { onTogglePin(entry) },
                    )
                }
            }
        }
    }
}

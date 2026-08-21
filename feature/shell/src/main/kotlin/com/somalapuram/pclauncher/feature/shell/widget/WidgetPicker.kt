package com.somalapuram.pclauncher.feature.shell.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.feature.shell.bar.bitmapPainterFor

/** One provider, as the picker shows it. Kept free of framework types so the list is testable. */
data class WidgetChoice(
    val id: String,
    val label: String,
    val preview: android.graphics.drawable.Drawable?,
    val columns: Int,
    val rows: Int,
)

/**
 * Choose a widget to add.
 *
 * Deliberately a plain list of label plus preview: the interesting part of adding a widget is the
 * binding that follows, and a gallery with live previews would be a lot of surface for a choice
 * made once.
 */
@Composable
fun WidgetPicker(
    choices: List<WidgetChoice>,
    onPick: (WidgetChoice) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPcColors.current

    Column(
        modifier = modifier
            .width(420.dp)
            .heightIn(max = 520.dp)
            .background(colors.surface, RoundedCornerShape(PcCorners.Surface))
            .border(1.dp, colors.hairline, RoundedCornerShape(PcCorners.Surface))
            .clickable(interactionSource = null, indication = null) { }
            .padding(PcSpacing.Medium),
    ) {
        Text("Add widget", color = colors.onSurface, fontSize = 15.sp)
        Text(
            text = if (choices.isEmpty()) "No widgets installed" else "${choices.size} available",
            color = colors.onSurfaceMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = PcSpacing.Small),
        )

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(choices, key = { it.id }) { choice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(choice) }
                        .padding(PcSpacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PcSpacing.Medium),
                ) {
                    bitmapPainterFor(choice.preview)?.let {
                        Image(painter = it, contentDescription = null, modifier = Modifier.size(40.dp))
                    } ?: Box(Modifier.size(40.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = choice.label,
                            color = colors.onSurface,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${choice.columns} × ${choice.rows}",
                            color = colors.onSurfaceMuted,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

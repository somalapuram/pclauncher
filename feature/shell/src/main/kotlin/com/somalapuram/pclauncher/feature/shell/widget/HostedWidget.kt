package com.somalapuram.pclauncher.feature.shell.widget

import android.appwidget.AppWidgetHostView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcCorners

/**
 * Another app's widget, on our desktop.
 *
 * `AppWidgetHostView` is a `View`, so Compose hosts it through `AndroidView` rather than
 * reimplementing it. (Glance is for *providing* widgets; it has nothing to do with hosting them.)
 *
 * A null view means the provider failed to inflate. That leaves a placeholder rather than an empty
 * gap, because a widget that silently vanishes reads as the launcher losing it (GATE 4).
 */
@Composable
fun HostedWidget(
    view: AppWidgetHostView?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPcColors.current

    Box(
        modifier = modifier
            .background(colors.scrim.copy(alpha = 0.25f), RoundedCornerShape(PcCorners.Surface))
            .border(1.dp, colors.hairline, RoundedCornerShape(PcCorners.Surface)),
        contentAlignment = Alignment.Center,
    ) {
        if (view == null) {
            Text(
                text = "Widget unavailable",
                color = colors.onSurfaceMuted,
                fontSize = 11.sp,
            )
        } else {
            AndroidView(
                factory = { view },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

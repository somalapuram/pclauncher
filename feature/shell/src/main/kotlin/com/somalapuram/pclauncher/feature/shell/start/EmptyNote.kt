package com.somalapuram.pclauncher.feature.shell.start

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.PcSpacing

/** What the menu says when it has nothing to list: still loading, or nothing matched. */
@Composable
internal fun EmptyNote(text: String) {
    Text(
        text = text,
        color = LocalPcColors.current.onSurfaceMuted,
        fontSize = 13.sp,
        modifier = Modifier.padding(PcSpacing.Large),
    )
}

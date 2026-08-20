package com.somalapuram.pclauncher.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Shell glyphs, drawn in code.
 *
 * Vector assets would mean an XML round-trip and a resource id per glyph for shapes this small;
 * building them here keeps them next to the tokens they are drawn with, and lets them be tinted
 * per state without a second asset.
 */
object PcGlyphs {

    /**
     * The Start glyph: four panes, with the top-left one detached.
     *
     * A nod to the Windows four-pane Start without copying it — the offset pane is what makes it
     * pclauncher's mark rather than a clone, and it reads at 20 dp, which a more detailed logo
     * would not.
     */
    val Start: ImageVector by lazy {
        ImageVector.Builder(
            name = "pc_start",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Detached pane, lifted and rotated slightly by being drawn a shade smaller.
            path(fill = SolidColor(Color.White)) {
                moveTo(3.2f, 4.6f)
                lineTo(9.6f, 3.2f)
                lineTo(9.6f, 10.4f)
                lineTo(3.2f, 10.4f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(12.4f, 3.2f)
                lineTo(20.8f, 3.2f)
                lineTo(20.8f, 10.4f)
                lineTo(12.4f, 10.4f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(3.2f, 13.2f)
                lineTo(9.6f, 13.2f)
                lineTo(9.6f, 20.4f)
                lineTo(3.2f, 20.4f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(12.4f, 13.2f)
                lineTo(20.8f, 13.2f)
                lineTo(20.8f, 20.8f)
                lineTo(12.4f, 19.4f)
                close()
            }
        }.build()
    }
}

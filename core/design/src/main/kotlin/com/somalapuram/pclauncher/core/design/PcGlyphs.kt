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
     * The Start glyph: a three-by-three grid of dots.
     *
     * Was four panes with one detached, described here as "a nod to the Windows four-pane Start
     * without copying it". On a device that *is* Android, running Android apps, the nod reads as
     * the wrong operating system, and SRS §1 asks for something that feels like a copy of neither
     * desktop. The Windows *organisation* — a Start button at the left opening a searchable menu
     * (§6.4) — is unchanged; only the mark is.
     *
     * A dot grid is the app-drawer mark Android launchers have used for a decade, and it survives
     * being drawn at 20 dp, which a robot silhouette or anything finer would not.
     */
    val Start: ImageVector by lazy {
        ImageVector.Builder(
            name = "pc_start",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            val centres = listOf(6f, 12f, 18f)
            val radius = 2.15f
            centres.forEach { cy ->
                centres.forEach { cx ->
                    path(fill = SolidColor(Color.White)) {
                        // Two half-arcs rather than a circle primitive: the vector builder has no
                        // circle, and four quadratics drift visibly at this size.
                        moveTo(cx - radius, cy)
                        arcToRelative(radius, radius, 0f, true, true, radius * 2f, 0f)
                        arcToRelative(radius, radius, 0f, true, true, -radius * 2f, 0f)
                        close()
                    }
                }
            }
        }.build()
    }
}

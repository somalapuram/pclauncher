package com.somalapuram.pclauncher.feature.shell.bar

import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.core.design.PcSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The size of the grid of boxes inside the Start button (start-glyph-size.md).
 *
 * Pinned by a test because "30% larger" is a claim about a number, and a number that drifts back
 * down is invisible in a diff.
 */
class StartGlyphSizeTest {

    /** What the glyph was drawn at before this slice. */
    private val original = 20.dp

    @Test
    fun `at rest the glyph is 30 percent larger than it was`() {
        assertEquals((original.value * 1.3f), startGlyphSize(isOpen = false).value, 0.01f)
    }

    @Test
    fun `open is larger still`() {
        assertTrue(
            "open must grow beyond rest",
            startGlyphSize(isOpen = true) > startGlyphSize(isOpen = false),
        )
    }

    @Test
    fun `open and rest differ by size alone`() {
        // The point of the second channel: distinguishable without reference to the accent colour,
        // which is derived from the wallpaper and not guaranteed to contrast (SRS 6.1 principle 5).
        assertTrue(startGlyphSize(isOpen = true) != startGlyphSize(isOpen = false))
    }

    @Test
    fun `the glyph stays inside its tile with margin at both sizes`() {
        // A glyph reaching the border reads as clipped. The tile is PcSize.DockIcon across.
        for (open in listOf(false, true)) {
            val margin = (PcSize.DockIcon - startGlyphSize(open)) / 2f
            assertTrue("glyph has no margin when open=$open", margin > 4.dp)
        }
    }

    @Test
    fun `the growth is a visible step, not a rounding difference`() {
        val grew = startGlyphSize(isOpen = true) - startGlyphSize(isOpen = false)
        assertTrue("open grows by only $grew", grew >= 3.dp)
    }

    @Test
    fun `both sizes are positive`() {
        assertTrue(startGlyphSize(isOpen = false) > 0.dp)
        assertTrue(startGlyphSize(isOpen = true) > 0.dp)
    }
}

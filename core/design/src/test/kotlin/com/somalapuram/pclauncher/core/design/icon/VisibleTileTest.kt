package com.somalapuram.pclauncher.core.design.icon

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import com.somalapuram.pclauncher.core.design.PcSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * How much of a drawn icon box the visible tile actually occupies.
 *
 * The compositor reserves room outside the tile for the drop shadow and the outer glow, so a
 * treated icon never fills its own bitmap. That reservation is invisible at the call site: an icon
 * drawn into a 52 dp box was showing a ~27 dp tile, and nothing in the codebase said so. The
 * consequence was a desktop of small icons adrift in large cells.
 *
 * `PcSize.VisibleTileFraction` is what the sizing decisions are built on, so growing the glow
 * without noticing must fail here rather than quietly shrinking every icon on screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisibleTileTest {

    private val size = 192

    /** The extent of solidly-drawn pixels — the tile itself, not its blurred shadow or halo. */
    private fun tileFractionOf(style: IconStyle): Float {
        val bitmap: Bitmap = IconCompositor(style)
            .composite(ColorDrawable(AndroidColor.rgb(120, 120, 120)), size)

        val middle = size / 2
        val solid = (0 until size).filter { x ->
            AndroidColor.alpha(bitmap.getPixel(x, middle)) > 200
        }
        return (solid.max() - solid.min() + 1).toFloat() / size
    }

    @Test
    fun `no style draws a smaller tile than the sizing assumes`() {
        // The constant is the worst case, so every style must meet or beat it. A style that
        // reserved more room than this would leave its icons small at the sizes chosen from it.
        listOf(IconStyle.SoftClay, IconStyle.DarkGlass).forEach { style ->
            val measured = tileFractionOf(style)
            assertTrue(
                "${'$'}{style.id} draws ${'$'}measured of its box, under the assumed ${'$'}{PcSize.VisibleTileFraction}",
                measured >= PcSize.VisibleTileFraction - 0.01f,
            )
        }
    }

    @Test
    fun `the worst case is the one the constant describes`() {
        // Not merely a floor nobody meets: clay is the style it was taken from, so if clay drifts
        // upward the constant is stale and the icons could have been larger all along.
        assertEquals(PcSize.VisibleTileFraction, tileFractionOf(IconStyle.SoftClay), 0.02f)
    }

    @Test
    fun `a desktop icon's visible tile is a reasonable share of its cell`() {
        val tile = PcSize.DesktopIcon.value * PcSize.VisibleTileFraction
        val share = tile / PcSize.DesktopGridCell.value

        // It was 0.28 before this: an icon adrift in mostly empty space.
        assertTrue("the desktop icon covers only $share of its cell", share > 0.36f)
    }

    @Test
    fun `a dock icon fits inside the bar at rest`() {
        // The bar is the constraint. An icon at or above the bar's resting height has nowhere to
        // grow into on hover and clips against the edge instead of magnifying.
        assertTrue(
            "the dock icon does not fit the bar at rest",
            PcSize.DockIcon < PcSize.DockHeightAtRest,
        )
        assertTrue(
            "the dock tile is too small to read",
            PcSize.DockIcon.value * PcSize.VisibleTileFraction > 22f,
        )
    }
}

package com.somalapuram.pclauncher.feature.shell.bar

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.core.design.PcGlyphs
import com.somalapuram.pclauncher.core.design.PcSize
import com.somalapuram.pclauncher.core.design.PcTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * That the bar sits on one line.
 *
 * The dock used to be bottom-aligned while the Start button, chips, tray and Show Desktop handle
 * were centred — so the Start button's centre floated above the row of icons immediately beside it.
 * It is the first thing the eye lands on, and it was invisible to every test because nothing
 * compared the two.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp")
class BarAlignmentTest {

    @get:Rule
    val compose = createComposeRule()

    private fun bar(running: Boolean = false) {
        compose.setContent {
            PcTheme {
                ShellBar(
                    state = BarState(
                        dockItems = listOf(
                            DockItem(id = "a", label = "Alpha", icon = null, isRunning = running),
                            DockItem(id = "b", label = "Beta", icon = null),
                        ),
                    ),
                    startGlyph = PcGlyphs.Start,
                    onStartClick = {},
                    onDockItemClick = {},
                    onWindowFocus = {},
                    onWindowClose = {},
                    onShowDesktop = {},
                )
            }
        }
        compose.waitForIdle()
    }

    private fun centreY(description: String): Float {
        val bounds = compose.onNodeWithContentDescription(description).getUnclippedBoundsInRoot()
        return (bounds.top + bounds.bottom).value / 2f
    }

    @Test
    fun `the start button and the dock icons share a centre line`() {
        bar()

        val start = centreY("Start")
        val dock = centreY("Alpha")

        assertTrue(
            "Start centres at $start but the dock at $dock — the bar is not on one line",
            abs(start - dock) < 1f,
        )
    }

    @Test
    fun `a running app does not shift its own icon off that line`() {
        // The running indicator used to sit in the layout flow beneath the icon, so launching an
        // app nudged its icon upward.
        bar(running = true)

        assertTrue(abs(centreY("Start") - centreY("Alpha, running")) < 1f)
    }

    @Test
    fun `the tray stays on the same line as the dock`() {
        bar()

        val tray = compose.onNodeWithContentDescription(", battery --%, wi-fi unknown, bluetooth unknown")
            .getUnclippedBoundsInRoot()
        val trayCentre = (tray.top + tray.bottom).value / 2f

        assertTrue(abs(trayCentre - centreY("Alpha")) < 1f)
    }

    @Test
    fun `the bar rests at its resting height`() {
        // It used to grow 14 dp under the pointer, moving the whole bar -- and everything measured
        // against its top edge -- whenever a pointer crossed it. A dock's icons rise above its
        // background; the background stays put.
        bar()

        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val barHeight = root.bottom - root.top

        assertTrue(
            "the bar measures ${'$'}barHeight, taller than its resting height",
            barHeight <= PcSize.DockHeightAtRest,
        )
    }
}
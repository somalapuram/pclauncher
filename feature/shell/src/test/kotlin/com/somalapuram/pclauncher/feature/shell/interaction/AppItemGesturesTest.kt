package com.somalapuram.pclauncher.feature.shell.interaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The gesture contract, verified with controlled event timing.
 *
 * `adb shell input swipe` cannot express "hold, then move" — it interpolates from the first frame,
 * so every synthetic swipe looks like a scroll. Compose's test input can advance event time
 * explicitly, which is the only way to tell the three touch outcomes apart.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp")
class AppItemGesturesTest {

    @get:Rule
    val compose = createComposeRule()

    private class Recorder {
        var clicks = 0
        var menus = 0
        var dragStarts = 0
        var dragEnds = 0
        var dragged = Offset.Zero
    }

    private fun content(rec: Recorder) {
        compose.setContent {
            Box(
                Modifier
                    .testTag("item")
                    .size(120.dp)
                    .appItemGestures(
                        key = "item",
                        onClick = { rec.clicks++ },
                        onContextMenu = { rec.menus++ },
                        onDragStart = { rec.dragStarts++ },
                        onDrag = { rec.dragged += it },
                        onDragEnd = { rec.dragEnds++ },
                    ),
            )
        }
    }

    @Test
    fun `a quick tap is a click`() {
        val rec = Recorder()
        content(rec)

        compose.onNodeWithTag("item").performTouchInput { down(center); up() }

        assertEquals(1, rec.clicks)
        assertEquals(0, rec.menus)
        assertEquals(0, rec.dragStarts)
    }

    @Test
    fun `moving before the long-press timer is a scroll, not a drag`() {
        // The grid and the Start list are scrollable. Claiming this movement would leave the user
        // unable to reach anything past the first screen.
        val rec = Recorder()
        content(rec)

        compose.onNodeWithTag("item").performTouchInput {
            down(center)
            advanceEventTime(40)
            moveTo(center + Offset(0f, 300f))
            up()
        }

        assertEquals("must not drag", 0, rec.dragStarts)
        assertEquals("must not open a menu", 0, rec.menus)
        assertEquals("must not count as a click", 0, rec.clicks)
    }

    @Test
    fun `holding then releasing opens the context menu`() {
        val rec = Recorder()
        content(rec)

        compose.onNodeWithTag("item").performTouchInput {
            down(center)
            advanceEventTime(800)
            up()
        }

        assertEquals(1, rec.menus)
        assertEquals(0, rec.dragStarts)
        assertEquals(0, rec.clicks)
    }

    @Test
    fun `holding then moving starts a drag and suppresses the menu`() {
        val rec = Recorder()
        content(rec)

        compose.onNodeWithTag("item").performTouchInput {
            down(center)
            advanceEventTime(800)
            moveTo(center + Offset(0f, 200f))
            advanceEventTime(16)
            moveTo(center + Offset(0f, 300f))
            up()
        }

        assertEquals("drag should start once", 1, rec.dragStarts)
        assertEquals("drag should end once", 1, rec.dragEnds)
        assertEquals("no menu while dragging", 0, rec.menus)
        assertTrue("drag deltas must be reported, got ${rec.dragged}", rec.dragged.getDistance() > 0f)
    }

    @Test
    fun `drag deltas are non-zero`() {
        // Regression: consuming a change before reading positionChange() reports zero movement, so
        // the ghost never moves and every drop lands back where it started.
        val rec = Recorder()
        content(rec)

        compose.onNodeWithTag("item").performTouchInput {
            down(center)
            advanceEventTime(800)
            moveTo(center + Offset(0f, 250f))
            up()
        }

        assertTrue("expected real movement, got ${rec.dragged}", rec.dragged.y > 100f)
    }

    @Test
    fun `a disabled item reports nothing`() {
        val rec = Recorder()
        compose.setContent {
            Box(
                Modifier
                    .testTag("item")
                    .size(120.dp)
                    .appItemGestures(
                        key = "item",
                        enabled = false,
                        onClick = { rec.clicks++ },
                        onContextMenu = { rec.menus++ },
                    ),
            )
        }

        compose.onNodeWithTag("item").performTouchInput { down(center); up() }

        assertEquals(0, rec.clicks)
        assertFalse(rec.menus > 0)
    }
}

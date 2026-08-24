package com.somalapuram.pclauncher.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.core.data.layout.DesktopCell
import com.somalapuram.pclauncher.core.data.layout.DesktopPlacement
import com.somalapuram.pclauncher.core.data.layout.DesktopSpan
import com.somalapuram.pclauncher.core.data.layout.ResizePermission
import com.somalapuram.pclauncher.core.design.PcTheme
import com.somalapuram.pclauncher.feature.shell.desktop.DesktopWidget
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * That a widget drag is measured from where the widget is *now*.
 *
 * `pointerInput` keeps the lambdas it was created with until one of its keys changes. The first
 * implementation keyed only on whether the gesture was enabled, so the block held the placement
 * from the very first composition: the widget moved once and every drag after that was measured
 * from the cell it no longer occupied. Nothing about that is visible in the signature — the
 * parameter is passed, read, and stale — so it takes a second drag to catch it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp")
class WidgetDragWiringTest {

    @get:Rule
    val compose = createComposeRule()

    private val cell = 96f
    private val row = 104f

    /**
     * Renders one widget that follows its own moves, and returns the cells it was moved to.
     *
     * Each drag names where it grabs the widget, because after a move the widget is no longer
     * where it was — pressing the old spot would land on bare desktop and prove nothing.
     */
    private fun dragTwice(
        first: Pair<Offset, Offset>,
        second: Pair<Offset, Offset>,
    ): List<DesktopCell> {
        val moves = mutableListOf<DesktopCell>()

        compose.setContent {
            var placement by remember {
                mutableStateOf(DesktopPlacement("widget:1", DesktopCell(0, 0), DesktopSpan(2, 1)))
            }
            PcTheme {
                DesktopWidget(
                    placement = placement,
                    widgetId = 1,
                    view = null,
                    permission = ResizePermission(horizontal = true, vertical = true),
                    isResizing = false,
                    cellWidth = 96.dp,
                    cellHeight = 104.dp,
                    cellWidthPx = cell,
                    cellHeightPx = row,
                    columnsAvailable = 12,
                    rowsAvailable = 7,
                    onOpenResize = {},
                    onRemove = {},
                    onMove = { landed ->
                        moves += landed
                        // What the real grid does: the store writes, and the placement comes back.
                        placement = placement.copy(cell = landed)
                    },
                    onResizeDrag = { _, _ -> },
                    onResizeStart = {},
                    onResizeEnd = {},
                )
            }
        }

        compose.onRoot().performTouchInput { dragFrom(first.first, first.second) }
        compose.waitForIdle()
        compose.onRoot().performTouchInput { dragFrom(second.first, second.second) }
        compose.waitForIdle()

        return moves
    }

    /** Press inside the widget, move in steps big enough to pass the slop, release. */
    private fun androidx.compose.ui.test.TouchInjectionScope.dragFrom(start: Offset, delta: Offset) {
        down(start)
        moveTo(start + delta / 2f)
        moveTo(start + delta)
        up()
    }

    @Test
    fun `each drag starts from the cell the previous one landed on`() {
        val moves = dragTwice(
            // Grab the widget at (0,0), drag it two columns right.
            first = Offset(30f, 30f) to Offset(2 * cell, 0f),
            // Now grab it where it landed, and take it one further column and one row down.
            second = Offset(2 * cell + 30f, 30f) to Offset(cell, row),
        )

        assertEquals(
            "the second drag was measured from a stale placement",
            listOf(DesktopCell(2, 0), DesktopCell(3, 1)),
            moves,
        )
    }

    @Test
    fun `a drag too small to cross a cell boundary moves nothing`() {
        val moves = dragTwice(
            first = Offset(30f, 30f) to Offset(10f, 10f),
            second = Offset(30f, 30f) to Offset(10f, 10f),
        )
        assertEquals(emptyList<DesktopCell>(), moves)
    }
}

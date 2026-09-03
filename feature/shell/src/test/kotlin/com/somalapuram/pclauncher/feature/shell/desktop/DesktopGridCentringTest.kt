package com.somalapuram.pclauncher.feature.shell.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.apps.AppKey
import com.somalapuram.pclauncher.core.apps.ProfileKind
import com.somalapuram.pclauncher.core.data.layout.DesktopLayout
import com.somalapuram.pclauncher.core.data.layout.withAutoPlacement
import com.somalapuram.pclauncher.core.design.PcSize
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.core.design.PcTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * That the rows sit centred in the height the grid is given (grid-bounds.md).
 *
 * The remainder used to fall entirely below the last row, so the grid read as pushed against the
 * top edge. Nothing compared the two gaps, which is why it survived — the same blind spot that let
 * the bar's own misalignment through (`bar/BarAlignmentTest`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp")
class DesktopGridCentringTest {

    @get:Rule
    val compose = createComposeRule()

    /** 104 dp cells: 500 dp holds four whole rows and leaves 84 dp over. */
    private val gridHeight = 500.dp

    private fun grid(count: Int) {
        val entries = (0 until count).map { entry("app$it") }
        val ids = entries.map { it.key.component.flattenToShortString() }
        compose.setContent {
            PcTheme {
                Box(modifier = Modifier.fillMaxWidth().height(gridHeight)) {
                    DesktopAppGrid(
                        entries = entries,
                        // Four rows, matching what the height above can hold.
                        layout = withAutoPlacement(DesktopLayout(), ids, rowsPerColumn = 4),
                        isPinned = { false },
                        onLaunch = {},
                        onTogglePin = {},
                        iconFor = { null },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    /**
     * Where a label sits inside its own cell: 8 dp top padding, the 72 dp icon, 4 dp of spacing.
     *
     * Measured from the tokens rather than guessed, because the labels are *not* centred in their
     * cells — comparing label edges instead of cell edges reads a 126/27 dp split as a centring
     * bug when the cells either side of them are exactly symmetric.
     */
    private val labelOffsetInCell = PcSpacing.Small + PcSize.DesktopIcon + PcSpacing.ExtraSmall

    /** Four 104 dp rows fit in 500 dp less 2x16 dp padding, leaving 52 dp to split. */
    private val expectedOffset = 26.dp

    @Test
    fun `the space above the first cell equals the space below the last`() {
        grid(count = 4)

        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val firstLabel = compose.onNodeWithText("app0").getUnclippedBoundsInRoot()

        val firstCellTop = firstLabel.top - labelOffsetInCell
        val lastCellBottom = firstCellTop + (DesktopCellHeight * 4)

        val above = (firstCellTop - root.top).value
        val below = (root.bottom - lastCellBottom).value

        assertTrue(
            "cells are not centred: ${above}dp above, ${below}dp below",
            abs(above - below) < 1f,
        )
    }

    @Test
    fun `the first cell starts a padding plus half the leftover down`() {
        // The exact arithmetic, not just symmetry: half of the 52 dp remainder is 26 dp, and the
        // grid's own 16 dp padding sits above that. With the remainder left at the bottom, as it
        // was, this would be 16 dp.
        grid(count = 4)

        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val firstLabel = compose.onNodeWithText("app0").getUnclippedBoundsInRoot()
        val firstCellTop = firstLabel.top - labelOffsetInCell

        assertEquals(
            (PcSpacing.Large + expectedOffset).value,
            (firstCellTop - root.top).value,
            0.5f,
        )
    }

    @Test
    fun `an empty grid still lays out`() {
        // GATE 4: no icons is a valid desktop, not a crash.
        grid(count = 0)
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        assertEquals(true, (root.bottom - root.top).value > 0f)
    }
}

private fun entry(label: String) = AppEntry(
    key = AppKey(
        android.content.ComponentName("com.example.$label", "com.example.$label.Main"),
        android.os.UserHandle.getUserHandleForUid(0),
    ),
    label = label,
    packageName = "com.example.$label",
    profile = ProfileKind.Personal,
)

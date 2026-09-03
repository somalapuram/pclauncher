package com.somalapuram.pclauncher.feature.shell.tray

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithContentDescription
import com.somalapuram.pclauncher.core.design.PcTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * That the tray's rows reach the thing which can act on them.
 *
 * The routing itself is pure and covered elsewhere; what this guards is the wiring — a row whose
 * `onClick` is accepted and never called compiles perfectly and does nothing, which is exactly the
 * bug the Start button and the widget resolver both shipped with once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp")
class QuickSettingsTest {

    @get:Rule
    val compose = createComposeRule()

    private val state = TrayState(
        timeText = "9:41",
        battery = BatteryState.Known(80, charging = false),
        wifi = ConnectionState.On,
        bluetooth = ConnectionState.Off,
        volume = VolumeState(level = 5, max = 10),
    )

    private fun panel(): MutableList<TrayAction> {
        val actions = mutableListOf<TrayAction>()
        compose.setContent { PcTheme { QuickSettingsPanel(state = state, onAction = { actions += it }) } }
        return actions
    }

    @Test
    fun `the wifi row opens the system panel`() {
        val actions = panel()
        compose.onNodeWithText("Wi-Fi").performClick()
        assertEquals(listOf(TrayAction.OpenWifiPanel), actions)
    }

    @Test
    fun `the bluetooth row offers to enable a radio that is off`() {
        val actions = panel()
        compose.onNodeWithText("Bluetooth").performClick()
        assertEquals(listOf(TrayAction.EnableBluetooth), actions)
    }

    @Test
    fun `the battery row opens the battery screen`() {
        val actions = panel()
        compose.onNodeWithText("Battery").performClick()
        assertEquals(listOf(TrayAction.OpenBatterySettings), actions)
    }

    @Test
    fun `the rows show what each value currently is`() {
        panel()
        compose.onNodeWithText("Connected").assertIsDisplayed()
        compose.onNodeWithText("Off").assertIsDisplayed()
        compose.onNodeWithText("80%").assertIsDisplayed()
    }

    @Test
    fun `dragging the slider asks for a volume, not a hand-off`() {
        val actions = panel()

        compose.onNode(androidx.compose.ui.test.hasSetTextAction().not() and
            androidx.compose.ui.test.hasProgressBarRangeInfo(
                androidx.compose.ui.semantics.ProgressBarRangeInfo(0.5f, 0f..1f, 0),
            ))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }

        assertEquals(listOf(TrayAction.SetVolume(10)), actions)
    }

    /**
     * The tray asks; it no longer opens. The panel is drawn by the host in its full-screen surface,
     * because one anchored inside the bar cannot be placed above it once the bar is its own window
     * (tray-popover-host.md).
     */
    @Test
    fun `clicking the tray asks the host to toggle the panel`() {
        var toggles = 0
        compose.setContent { PcTheme { SystemTray(state = state, onToggle = { toggles++ }) } }

        compose.onNodeWithContentDescription(state.describe()).performClick()

        assertEquals(1, toggles)
    }

    @Test
    fun `the tray draws itself pressed while the panel is open`() {
        compose.setContent { PcTheme { SystemTray(state = state, isOpen = true) } }

        compose.onNodeWithContentDescription(state.describe()).assertIsDisplayed()
    }

    @Test
    fun `a hand-off closes the panel and a volume change does not`() {
        assertTrue(dismissesPanel(TrayAction.OpenBatterySettings))
        assertTrue(dismissesPanel(TrayAction.OpenWifiPanel))
        assertTrue(dismissesPanel(TrayAction.OpenBluetoothSettings))
        assertTrue(dismissesPanel(TrayAction.EnableBluetooth))
        // The exception, and the reason this is a named rule rather than a condition.
        assertFalse(dismissesPanel(TrayAction.SetVolume(10)))
    }
}

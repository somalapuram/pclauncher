package com.somalapuram.pclauncher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import com.somalapuram.pclauncher.core.design.PcTheme
import com.somalapuram.pclauncher.platform.privileged.Tier
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Start menu's dismiss layer.
 *
 * Worth a test specifically because the failure mode is **z-order**: put the scrim under the
 * desktop and every click silently misses it. That looks fine in a diff and only shows up by
 * trying to click, which is what this does.
 */
@RunWith(RobolectricTestRunner::class)
// A PC-sized window on purpose. Robolectric's default is 320dp wide — narrower than the menu
// itself — so there would be no "outside" to click and the test would exercise nothing.
@Config(sdk = [34], qualifiers = "w1280dp-h800dp")
class StartMenuDismissTest {

    @get:Rule
    val compose = createComposeRule()

    private fun content() {
        compose.setContent {
            PcTheme {
                HomeScreen(
                    outcome = StartupOutcome.Ready(DesktopEnvironment(Tier.Basic)),
                    inventory = MutableStateFlow(
                        com.somalapuram.pclauncher.core.apps.AppInventory(isComplete = true),
                    ),
                    isDefaultHome = true,
                    onSetDefaultHome = {},
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun `the dismiss layer is absent until the menu opens`() {
        content()
        compose.onNodeWithTag(StartScrimTag).assertDoesNotExist()
    }

    @Test
    fun `opening Start shows a reachable dismiss layer`() {
        content()
        compose.onNodeWithContentDescriptionSafe("Start").performClick()
        compose.onNodeWithTag(StartScrimTag).assertIsDisplayed()
    }

    @Test
    fun `clicking away from the menu closes it`() {
        content()
        compose.onNodeWithContentDescriptionSafe("Start").performClick()
        compose.onNodeWithTag(StartScrimTag).assertIsDisplayed()

        // Clicked near the top-right, well clear of the menu. The node's centre would land on the
        // menu, which sits above the scrim — so a plain performClick would prove nothing.
        compose.onNodeWithTag(StartScrimTag).performTouchInput {
            click(Offset(width * 0.9f, height * 0.1f))
        }

        compose.onNodeWithTag(StartScrimTag).assertDoesNotExist()
    }

    @Test
    fun `clicking the menu itself does not close it`() {
        // The other half of the contract: the scrim must not swallow interaction with the menu.
        content()
        compose.onNodeWithContentDescriptionSafe("Start").performClick()

        compose.onNodeWithTag(StartScrimTag).performTouchInput {
            click(Offset(width * 0.1f, height * 0.9f))
        }

        compose.onNodeWithTag(StartScrimTag).assertIsDisplayed()
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithContentDescriptionSafe(
    label: String,
) = onNode(androidx.compose.ui.test.hasContentDescription(label))

package com.somalapuram.pclauncher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.somalapuram.pclauncher.core.design.PcTheme
import com.somalapuram.pclauncher.platform.privileged.Tier
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a ready desktop shows the tier`() {
        compose.setContent {
            PcTheme {
                HomeScreen(
                    outcome = StartupOutcome.Ready(DesktopEnvironment(Tier.Basic)),
                    isDefaultHome = true,
                    onSetDefaultHome = {},
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("pclauncher").assertIsDisplayed()
        compose.onNodeWithText("Windowing tier: Basic").assertIsDisplayed()
    }

    @Test
    fun `the set-as-home action appears only when we are not the home app`() {
        compose.setContent {
            PcTheme {
                HomeScreen(
                    outcome = StartupOutcome.Ready(DesktopEnvironment(Tier.Basic)),
                    isDefaultHome = false,
                    onSetDefaultHome = {},
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("Set as default home").assertIsDisplayed()
    }

    @Test
    fun `a startup failure renders the safe-mode desktop, not a crash`() {
        var retried = false

        compose.setContent {
            PcTheme {
                HomeScreen(
                    outcome = StartupOutcome.Fallback(FallbackReason.StartupFailed, RuntimeException()),
                    isDefaultHome = true,
                    onSetDefaultHome = {},
                    onRetry = { retried = true },
                )
            }
        }

        compose.onNodeWithText("pclauncher started in safe mode").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        assertTrue("the fallback must offer a way out", retried)
    }
}

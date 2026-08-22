package com.somalapuram.pclauncher

import androidx.compose.ui.test.junit4.createComposeRule
import com.somalapuram.pclauncher.core.apps.AppInventory
import com.somalapuram.pclauncher.core.data.layout.DesktopCell
import com.somalapuram.pclauncher.core.data.layout.DesktopLayout
import com.somalapuram.pclauncher.core.data.layout.DesktopPlacement
import com.somalapuram.pclauncher.core.data.layout.widgetPlacementId
import com.somalapuram.pclauncher.core.design.PcTheme
import com.somalapuram.pclauncher.platform.privileged.Tier
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * That a placed widget actually reaches the thing which can render it.
 *
 * This exists because of a bug that compiled perfectly: `Desktop` accepted a `widgetViewFor`
 * parameter and never passed it on, so the grid silently used its `{ null }` default and every
 * widget drew the "unavailable" placeholder. A dropped parameter is invisible in review — the
 * signature is right, the call site is right, and nothing warns — so the only way to catch it is
 * to render and check the resolver was asked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp")
class WidgetWiringTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a placed widget asks for its view`() {
        val asked = mutableListOf<Int>()
        val layout = DesktopLayout(
            listOf(DesktopPlacement(widgetPlacementId(42), DesktopCell(2, 1))),
        )

        compose.setContent {
            PcTheme {
                HomeScreen(
                    outcome = StartupOutcome.Ready(DesktopEnvironment(Tier.Basic)),
                    inventory = MutableStateFlow(
                        AppInventory(entries = listOf(testEntry("Files")), isComplete = true),
                    ),
                    desktopLayout = MutableStateFlow(layout),
                    isDefaultHome = true,
                    onSetDefaultHome = {},
                    onRetry = {},
                    widgetViewFor = { id -> asked += id; null },
                )
            }
        }
        compose.waitForIdle()

        assertTrue("the widget's view was never requested, so nothing could render it", 42 in asked)
    }

    @Test
    fun `a desktop with no widgets asks for nothing`() {
        val asked = mutableListOf<Int>()

        compose.setContent {
            PcTheme {
                HomeScreen(
                    outcome = StartupOutcome.Ready(DesktopEnvironment(Tier.Basic)),
                    inventory = MutableStateFlow(
                        AppInventory(entries = listOf(testEntry("Files")), isComplete = true),
                    ),
                    isDefaultHome = true,
                    onSetDefaultHome = {},
                    onRetry = {},
                    widgetViewFor = { id -> asked += id; null },
                )
            }
        }
        compose.waitForIdle()

        assertTrue("app icons must not be mistaken for widgets, got $asked", asked.isEmpty())
    }
}

private fun testEntry(label: String) = com.somalapuram.pclauncher.core.apps.AppEntry(
    key = com.somalapuram.pclauncher.core.apps.AppKey(
        android.content.ComponentName("com.example.$label", "com.example.$label.Main"),
        android.os.UserHandle.getUserHandleForUid(0),
    ),
    label = label,
    packageName = "com.example.$label",
    profile = com.somalapuram.pclauncher.core.apps.ProfileKind.Personal,
)

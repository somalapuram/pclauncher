package com.somalapuram.pclauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.os.UserManager
import androidx.lifecycle.lifecycleScope
import com.somalapuram.pclauncher.desktop.AppLauncher
import com.somalapuram.pclauncher.desktop.ShellController
import android.graphics.drawable.Drawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.design.PcTheme
import com.somalapuram.pclauncher.desktop.IconResolver
import com.somalapuram.pclauncher.di.HomeEntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * The desktop. This is the home screen (`CATEGORY_HOME`), so it renders behind everything else and
 * must never fail to come up.
 *
 * Note what is *not* here: no `@AndroidEntryPoint`, and no `@Inject lateinit`. Field injection
 * happens before `onCreate` runs, so a broken graph would crash the home screen before any guard
 * could catch it. Resolving the entry point inside [runCatching] instead means a dependency failure
 * lands on the fallback desktop rather than on the user (GATE 4, SRS §12).
 */
class HomeActivity : ComponentActivity() {

    private var shell: ShellController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val outcome = resolveStartup(runCatching { loadEnvironment() })
        // Built inside the same guard as everything else on this path: no controller means an
        // empty dock and desktop, not a dead home screen (GATE 4).
        shell = runCatching { buildController() }.getOrNull()?.also { it.start() }

        // Only paid for when we are actually in safe mode, and guarded again on the way in:
        // the listing is a best effort, not a second thing that can strand the user.
        val safeModeApps = if (outcome is StartupOutcome.Fallback) {
            runCatching { entryPoint().safeModeApps().list() }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        // The inventory now belongs to the ViewModel, which also owns pin state so the dock, the
        // Start menu and the desktop cannot disagree about what is pinned.
        val launcher = AppLauncher(applicationContext)

        setContent {
            val dark = isSystemInDarkTheme()
            // Rebuilt when the theme flips, so the dock reloads icons baked for the new palette
            // rather than showing dark-glass tiles on a light desktop.
            val iconFor = remember(dark) { iconResolverFor(dark) }

            PcTheme(darkTheme = dark) {
                HomeScreen(
                    inventory = shell?.inventory ?: EmptyInventory,
                    pins = shell?.pins ?: EmptyPins,
                    iconFor = iconFor,
                    onLaunchApp = { launcher.launch(it) },
                    onTogglePin = { entry -> shell?.togglePin(entry) },
                    outcome = outcome,
                    isDefaultHome = HomeRole.isDefault(this),
                    onSetDefaultHome = { startActivity(HomeRole.requestIntent(this)) },
                    onRetry = { recreate() },
                    safeModeApps = safeModeApps,
                )
            }
        }
    }

    private fun buildController(): ShellController {
        val entryPoint = entryPoint()
        val userSerial = runCatching {
            getSystemService(UserManager::class.java)
                .getSerialNumberForUser(android.os.Process.myUserHandle())
        }.getOrDefault(0L)

        return ShellController(
            repository = entryPoint.appInventoryRepository(),
            pinStore = entryPoint.pinStore(),
            scope = lifecycleScope,
            userSerial = userSerial,
        )
    }

    override fun onDestroy() {
        shell?.stop()
        super.onDestroy()
    }

    /**
     * The dock's icon lookup, or a lookup that yields nothing.
     *
     * Guarded like everything else on this path: no icon cache costs the dock its artwork, not the
     * user their desktop (GATE 4).
     */
    private fun iconResolverFor(darkTheme: Boolean): (AppEntry) -> Drawable? {
        val cache = runCatching { entryPoint().iconCache() }.getOrNull() ?: return { null }
        val resolver = IconResolver(applicationContext, cache, darkTheme = darkTheme)
        return { entry -> resolver.iconFor(entry) }
    }

    private fun loadEnvironment(): DesktopEnvironment = entryPoint().desktopEnvironmentSource().load()

    private fun entryPoint(): HomeEntryPoint =
        EntryPointAccessors.fromApplication(applicationContext, HomeEntryPoint::class.java)
}

/** Stand-ins for a shell that could not be built, so the desktop still renders. */
private val EmptyInventory = kotlinx.coroutines.flow.MutableStateFlow(
    com.somalapuram.pclauncher.core.apps.AppInventory(),
)
private val EmptyPins = kotlinx.coroutines.flow.MutableStateFlow(
    com.somalapuram.pclauncher.core.data.pins.Pins(),
)

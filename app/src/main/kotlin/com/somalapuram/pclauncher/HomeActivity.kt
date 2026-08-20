package com.somalapuram.pclauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.somalapuram.pclauncher.core.design.PcTheme
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val outcome = resolveStartup(runCatching { loadEnvironment() })

        setContent {
            PcTheme {
                HomeScreen(
                    outcome = outcome,
                    isDefaultHome = HomeRole.isDefault(this),
                    onSetDefaultHome = { startActivity(HomeRole.requestIntent(this)) },
                    onRetry = { recreate() },
                )
            }
        }
    }

    private fun loadEnvironment(): DesktopEnvironment =
        EntryPointAccessors
            .fromApplication(applicationContext, HomeEntryPoint::class.java)
            .desktopEnvironmentSource()
            .load()
}

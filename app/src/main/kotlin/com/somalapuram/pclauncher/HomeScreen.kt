package com.somalapuram.pclauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.apps.AppInventory
import com.somalapuram.pclauncher.core.design.PcGlyphs
import com.somalapuram.pclauncher.desktop.BarStateFactory
import com.somalapuram.pclauncher.feature.shell.bar.ShellBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.somalapuram.pclauncher.core.design.LocalPcColors
import com.somalapuram.pclauncher.core.design.LocalSurfaceTreatment
import com.somalapuram.pclauncher.core.design.PcCorners
import com.somalapuram.pclauncher.core.design.PcSpacing
import com.somalapuram.pclauncher.core.design.PcTheme
import com.somalapuram.pclauncher.core.design.SurfaceTreatment

/**
 * The desktop surface.
 *
 * The window is transparent and shows the wallpaper (`android:windowShowWallpaper`), which is also
 * how the home app renders *behind* desktop windows on the target device (SRS §4.1).
 */
@Composable
fun HomeScreen(
    outcome: StartupOutcome,
    inventory: StateFlow<AppInventory> = MutableStateFlow(AppInventory()),
    iconFor: (AppEntry) -> android.graphics.drawable.Drawable? = { null },
    isDefaultHome: Boolean,
    onSetDefaultHome: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    safeModeApps: List<AppEntry> = emptyList(),
) {
    val apps by inventory.collectAsState()

    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (outcome) {
                is StartupOutcome.Ready -> Desktop(
                    environment = outcome.environment,
                    isDefaultHome = isDefaultHome,
                    onSetDefaultHome = onSetDefaultHome,
                )

                is StartupOutcome.Fallback -> FallbackDesktop(
                    reason = outcome.reason,
                    onRetry = onRetry,
                    onSetDefaultHome = onSetDefaultHome,
                    apps = safeModeApps,
                )
            }
        }

        // The bar renders from the inventory Flow, so it appears with the desktop and fills in —
        // an empty dock is a valid first frame, never a spinner (dock-taskbar.md requirement 8).
        // Safe mode gets no bar: it must not depend on the inventory or the icon cache.
        if (outcome is StartupOutcome.Ready) {
            ShellBar(
                state = BarStateFactory.from(apps, iconFor = iconFor),
                startGlyph = PcGlyphs.Start,
                onStartClick = {},
                onDockItemClick = {},
                onWindowFocus = {},
                onWindowClose = {},
                onShowDesktop = {},
                modifier = Modifier.padding(
                    horizontal = PcSpacing.Large,
                    vertical = PcSpacing.Small,
                ),
            )
        }
    }
}

@Composable
private fun Desktop(
    environment: DesktopEnvironment,
    isDefaultHome: Boolean,
    onSetDefaultHome: () -> Unit,
) {
    // Placeholder only. The icon grid, folders, and widgets arrive with
    // docs/requirements/desktop/icon-grid.md; the dock and taskbar with shell/overlay-service.md.
    ShellCard {
        Text(
            text = "pclauncher",
            color = LocalPcColors.current.onSurface,
            fontSize = 22.sp,
        )
        Text(
            text = "Windowing tier: ${environment.tier}",
            color = LocalPcColors.current.onSurfaceMuted,
            fontSize = 13.sp,
        )
        if (!isDefaultHome) {
            TextButton(onClick = onSetDefaultHome) {
                Text("Set as default home", color = LocalPcColors.current.accent)
            }
        }
    }
}

/**
 * The guarded desktop (GATE 4). Reached when startup failed — it must not depend on anything that
 * could have been what failed, so it takes no injected state at all.
 */
@Composable
private fun FallbackDesktop(
    reason: FallbackReason,
    onRetry: () -> Unit,
    onSetDefaultHome: () -> Unit,
    apps: List<AppEntry> = emptyList(),
) {
    ShellCard {
        Text(
            text = "pclauncher started in safe mode",
            color = LocalPcColors.current.onSurface,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = when (reason) {
                FallbackReason.StartupFailed ->
                    "The shell could not load its settings, so the desktop is running with defaults. " +
                        "Your apps are unaffected."
            },
            color = LocalPcColors.current.onSurfaceMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text("Try again", color = LocalPcColors.current.accent)
        }
        TextButton(onClick = onSetDefaultHome) {
            Text("Home settings", color = LocalPcColors.current.onSurfaceMuted)
        }

        // Labels only, no icons: safe mode must not touch the icon cache, which is one of the
        // things whose failure lands the user here (GATE 4).
        if (apps.isNotEmpty()) {
            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                items(apps, key = { it.key.component.flattenToShortString() }) { app ->
                    Text(
                        text = app.label,
                        color = LocalPcColors.current.onSurface,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = PcSpacing.ExtraSmall),
                    )
                }
            }
        }
    }
}

/** A shell surface, drawn with whatever treatment the renderer can afford (SRS §4.3). */
@Composable
private fun ShellCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalPcColors.current
    val alpha = when (val treatment = LocalSurfaceTreatment.current) {
        is SurfaceTreatment.Scrim -> treatment.alpha
        is SurfaceTreatment.Blur -> treatment.scrimAlpha
    }

    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .background(colors.scrim.copy(alpha = alpha), RoundedCornerShape(PcCorners.Surface))
            .border(PcSpacing.Hairline, colors.hairline, RoundedCornerShape(PcCorners.Surface))
            .padding(PcSpacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PcSpacing.Small),
        content = { content() },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF202430, widthDp = 1280, heightDp = 800)
@Composable
private fun DesktopPreview() {
    PcTheme(darkTheme = true) {
        Box(Modifier.fillMaxSize().background(Color(0xFF202430))) {
            HomeScreen(
                outcome = StartupOutcome.Ready(
                    DesktopEnvironment(com.somalapuram.pclauncher.platform.privileged.Tier.Basic),
                ),
                isDefaultHome = false,
                onSetDefaultHome = {},
                onRetry = {},
            )
        }
    }
}

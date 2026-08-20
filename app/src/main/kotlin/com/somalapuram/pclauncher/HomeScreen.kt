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
    isDefaultHome: Boolean,
    onSetDefaultHome: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
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

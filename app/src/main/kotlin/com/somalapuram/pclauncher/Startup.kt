package com.somalapuram.pclauncher

import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.platform.privileged.Tier

/**
 * What the desktop needs in order to draw itself. Deliberately tiny in this slice — the app
 * inventory, pins, and layout join it in later phases.
 */
data class DesktopEnvironment(val tier: Tier)

/** Loads [DesktopEnvironment]. Injected, so a failing load can be exercised in a test. */
fun interface DesktopEnvironmentSource {
    fun load(): DesktopEnvironment
}

/**
 * The app list shown by the **fallback** desktop.
 *
 * Deliberately its own seam rather than the normal inventory: safe mode is reached *because*
 * something failed, so this must not depend on the icon cache, the usage store, or anything else
 * that could have been the thing that broke (GATE 4, requirement doc Notes). It returns labels
 * only — no icons — and returning an empty list is an acceptable answer.
 */
fun interface SafeModeApps {
    fun list(): List<AppEntry>

    companion object {
        /** What safe mode falls back to when even the bare listing fails. */
        val Empty = SafeModeApps { emptyList() }
    }
}

/** Why the shell fell back. Shown to the user — so each value has to mean something to them. */
enum class FallbackReason {
    /** Dependency injection, config, or a store failed on the way up. */
    StartupFailed,
}

sealed interface StartupOutcome {
    data class Ready(val environment: DesktopEnvironment) : StartupOutcome
    data class Fallback(val reason: FallbackReason, val cause: Throwable?) : StartupOutcome
}

/**
 * Turn a possibly-failed environment load into something renderable — **always** something
 * renderable.
 *
 * GATE 4 / SRS §12: this app *is* the home screen, so a crash on launch leaves the device with no
 * UI at all. Every failure path has to end at a usable desktop, which is why this is a total
 * function over [Result] rather than a rethrow.
 */
fun resolveStartup(result: Result<DesktopEnvironment>): StartupOutcome =
    result.fold(
        onSuccess = { StartupOutcome.Ready(it) },
        onFailure = { StartupOutcome.Fallback(FallbackReason.StartupFailed, it) },
    )

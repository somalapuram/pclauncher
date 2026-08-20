package com.somalapuram.pclauncher

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

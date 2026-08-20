package com.somalapuram.pclauncher

import com.somalapuram.pclauncher.platform.privileged.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupTest {

    @Test
    fun `a successful load is ready`() {
        val environment = DesktopEnvironment(Tier.Basic)
        val outcome = resolveStartup(Result.success(environment))
        assertEquals(StartupOutcome.Ready(environment), outcome)
    }

    @Test
    fun `a failed load falls back instead of throwing`() {
        // GATE 4: this app is the home screen. There is no failure that may reach the user as a
        // crash, so resolveStartup is total over Result.
        val boom = IllegalStateException("store unreadable")
        val outcome = resolveStartup(Result.failure(boom))

        assertTrue(outcome is StartupOutcome.Fallback)
        outcome as StartupOutcome.Fallback
        assertEquals(FallbackReason.StartupFailed, outcome.reason)
        assertSame(boom, outcome.cause)
    }

    @Test
    fun `a source that throws is caught, not propagated`() {
        val exploding = DesktopEnvironmentSource { error("dependency graph failed") }
        val outcome = resolveStartup(runCatching { exploding.load() })
        assertTrue(outcome is StartupOutcome.Fallback)
    }

    @Test
    fun `an Error is caught too, not just an Exception`() {
        // runCatching catches Throwable; a missing class or method at startup surfaces as an
        // Error, and that must land on the fallback desktop as well.
        val exploding = DesktopEnvironmentSource { throw NoSuchMethodError("hidden API moved") }
        val outcome = resolveStartup(runCatching { exploding.load() })
        assertTrue(outcome is StartupOutcome.Fallback)
    }
}

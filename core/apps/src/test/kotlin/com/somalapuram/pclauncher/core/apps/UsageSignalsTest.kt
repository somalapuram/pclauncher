package com.somalapuram.pclauncher.core.apps

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsageSignalsTest {

    @Test
    fun `usage access chooses the system source`() {
        assertEquals(UsageSource.SystemUsageStats, usageSourceFor(hasUsageAccess = true))
    }

    @Test
    fun `without usage access we fall back to our own counters`() {
        // SRS §11: this is a first-class path, not a degraded one. Ranking must still work.
        assertEquals(UsageSource.LocalCounters, usageSourceFor(hasUsageAccess = false))
    }

    @Test
    fun `recency orders most recently launched first`() {
        val mail = entry("Mail")
        val files = entry("Files")
        val clock = entry("Clock")
        val signals = mapOf(
            mail.key to UsageSignal(mail.key, lastLaunchedAtMillis = 100, launchCount = 1),
            files.key to UsageSignal(files.key, lastLaunchedAtMillis = 300, launchCount = 1),
            clock.key to UsageSignal(clock.key, lastLaunchedAtMillis = 200, launchCount = 9),
        )

        val ranked = byRecency(listOf(mail, files, clock), signals)
        assertEquals(listOf("Files", "Clock", "Mail"), ranked.map { it.label })
    }

    @Test
    fun `launch count breaks a recency tie`() {
        val a = entry("Alpha")
        val b = entry("Beta")
        val signals = mapOf(
            a.key to UsageSignal(a.key, lastLaunchedAtMillis = 100, launchCount = 2),
            b.key to UsageSignal(b.key, lastLaunchedAtMillis = 100, launchCount = 7),
        )

        assertEquals(listOf("Beta", "Alpha"), byRecency(listOf(a, b), signals).map { it.label })
    }

    @Test
    fun `with no signals at all the order is stable and alphabetical`() {
        // First run: nothing has ever been launched. "Recommended" must not be random.
        val entries = listOf(entry("Zebra"), entry("Apple"), entry("Mango"))
        val ranked = byRecency(entries, emptyMap())
        assertEquals(listOf("Apple", "Mango", "Zebra"), ranked.map { it.label })
    }

    @Test
    fun `limit truncates`() {
        val entries = listOf(entry("A"), entry("B"), entry("C"))
        assertEquals(2, byRecency(entries, emptyMap(), limit = 2).size)
    }
}

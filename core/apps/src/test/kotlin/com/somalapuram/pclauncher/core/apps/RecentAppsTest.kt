package com.somalapuram.pclauncher.core.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What belongs in the Start menu's Recent row (recent-apps.md). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentAppsTest {

    private fun signal(entry: AppEntry, at: Long, count: Int = 1) =
        entry.key to UsageSignal(entry.key, lastLaunchedAtMillis = at, launchCount = count)

    @Test
    fun `most recently launched comes first`() {
        val mail = entry("Mail")
        val files = entry("Files")
        val clock = entry("Clock")

        val recent = recentlyUsed(
            entries = listOf(mail, files, clock),
            signals = mapOf(signal(mail, 100), signal(files, 300), signal(clock, 200)),
            limit = 5,
        )

        assertEquals(listOf("Files", "Clock", "Mail"), recent.map { it.label })
    }

    /**
     * The difference from `byRecency`, and the reason this function exists: a row of apps the user
     * has never opened is not a list of recent apps.
     */
    @Test
    fun `an app with no recorded use is not recent`() {
        val used = entry("Used")
        val never = entry("Never")

        val recent = recentlyUsed(listOf(used, never), mapOf(signal(used, 100)), limit = 5)

        assertEquals(listOf("Used"), recent.map { it.label })
    }

    /** A key recorded without a timestamp is "never", not "at the epoch". */
    @Test
    fun `a zero timestamp does not count as recent`() {
        val ghost = entry("Ghost")
        assertTrue(recentlyUsed(listOf(ghost), mapOf(signal(ghost, 0)), limit = 5).isEmpty())
    }

    @Test
    fun `nothing recorded means no row at all`() {
        assertTrue(recentlyUsed(listOf(entry("Alpha"), entry("Beta")), emptyMap(), 5).isEmpty())
    }

    @Test
    fun `the row is capped at the limit`() {
        val entries = (1..9).map { entry("App$it") }
        val signals = entries.mapIndexed { i, e -> signal(e, at = (i + 1) * 100L) }.toMap()

        val recent = recentlyUsed(entries, signals, limit = 5)

        assertEquals(5, recent.size)
        assertEquals("App9", recent.first().label)
    }

    @Test
    fun `a limit of zero yields nothing`() {
        val a = entry("Alpha")
        assertTrue(recentlyUsed(listOf(a), mapOf(signal(a, 100)), limit = 0).isEmpty())
    }

    /**
     * The system source buckets its timestamps, so ties are ordinary rather than exotic. Without a
     * tie-break the row reshuffles between reads, which reads as the menu being broken.
     */
    @Test
    fun `launch count breaks a timestamp tie`() {
        val a = entry("Alpha")
        val b = entry("Beta")

        val recent = recentlyUsed(
            listOf(a, b),
            mapOf(signal(a, 100, count = 2), signal(b, 100, count = 7)),
            limit = 5,
        )

        assertEquals(listOf("Beta", "Alpha"), recent.map { it.label })
    }

    @Test
    fun `the label breaks a full tie so the order is stable`() {
        val a = entry("Alpha")
        val b = entry("Beta")
        val signals = mapOf(signal(a, 100, count = 1), signal(b, 100, count = 1))

        assertEquals(
            recentlyUsed(listOf(a, b), signals, 5).map { it.label },
            recentlyUsed(listOf(b, a), signals, 5).map { it.label },
        )
        assertEquals(listOf("Alpha", "Beta"), recentlyUsed(listOf(b, a), signals, 5).map { it.label })
    }

    /** A signal for an app that is no longer installed must not conjure a row entry. */
    @Test
    fun `a signal without an entry is ignored`() {
        val gone = entry("Gone")
        val here = entry("Here")

        val recent = recentlyUsed(listOf(here), mapOf(signal(gone, 900), signal(here, 100)), 5)

        assertEquals(listOf("Here"), recent.map { it.label })
    }

    /**
     * The inventory's contract: unavailable entries are greyed by the surface that draws them, not
     * filtered out here. Dropping them would make an app vanish from Recent the moment its SD card
     * was unmounted, and reappear later, with no explanation either way.
     */
    @Test
    fun `an unavailable entry still counts as recent`() {
        val offline = entry("Offline", available = false)
        assertEquals(listOf("Offline"), recentlyUsed(listOf(offline), mapOf(signal(offline, 5)), 5).map { it.label })
    }
}

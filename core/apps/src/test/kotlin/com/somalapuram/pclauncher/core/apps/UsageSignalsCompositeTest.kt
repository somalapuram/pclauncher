package com.somalapuram.pclauncher.core.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsageSignalsCompositeTest {

    private val mail = entry("Mail")
    private val files = entry("Files")

    private fun signalsOf(vararg pairs: Pair<AppEntry, Long>): Map<AppKey, UsageSignal> =
        pairs.associate { (e, at) -> e.key to UsageSignal(e.key, at, launchCount = 1) }

    @Test
    fun `with usage access the system source is used`() {
        val signals = UsageSignals(
            hasUsageAccess = { true },
            system = UsageSignalSource { signalsOf(mail to 500L) },
            local = UsageSignalSource { signalsOf(files to 900L) },
        ).signals()

        assertEquals(setOf(mail.key), signals.keys)
    }

    @Test
    fun `without usage access local counters are used`() {
        val signals = UsageSignals(
            hasUsageAccess = { false },
            system = UsageSignalSource { signalsOf(mail to 500L) },
            local = UsageSignalSource { signalsOf(files to 900L) },
        ).signals()

        assertEquals(setOf(files.key), signals.keys)
    }

    @Test
    fun `an empty system result falls through to local rather than showing nothing`() {
        // Usage access revoked mid-session, or simply no history yet: queryUsageStats returns an
        // empty list rather than throwing. Reporting that as "no recent apps" would be a lie.
        val signals = UsageSignals(
            hasUsageAccess = { true },
            system = UsageSignalSource { emptyMap() },
            local = UsageSignalSource { signalsOf(files to 900L) },
        ).signals()

        assertEquals(setOf(files.key), signals.keys)
    }

    @Test
    fun `access is re-checked on every call, not captured at construction`() {
        var granted = true
        val signals = UsageSignals(
            hasUsageAccess = { granted },
            system = UsageSignalSource { signalsOf(mail to 500L) },
            local = UsageSignalSource { signalsOf(files to 900L) },
        )

        assertEquals(setOf(mail.key), signals.signals().keys)
        granted = false
        assertEquals(setOf(files.key), signals.signals().keys)
    }

    @Test
    fun `the no-op store reads and writes nothing`() {
        // What safe mode gets: it must not touch the usage store at all.
        assertTrue(UsageStore.None.signals().isEmpty())
    }
}

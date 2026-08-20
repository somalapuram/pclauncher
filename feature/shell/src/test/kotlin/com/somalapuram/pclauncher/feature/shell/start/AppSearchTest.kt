package com.somalapuram.pclauncher.feature.shell.start

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppSearchTest {

    private val apps = listOf(
        app("Calendar"),
        app("Calculator"),
        app("Google Calendar Sync"),
        app("Camera"),
        app("Café"),
        app("Files"),
    )

    @Test
    fun `an empty query returns everything in order`() {
        assertEquals(apps, AppSearch.filter(apps, ""))
        assertEquals(apps, AppSearch.filter(apps, "   "))
    }

    @Test
    fun `matching is case-insensitive`() {
        assertEquals(
            AppSearch.filter(apps, "CALENDAR").map { it.label },
            AppSearch.filter(apps, "calendar").map { it.label },
        )
    }

    @Test
    fun `diacritics are ignored both ways`() {
        // Someone on a US keyboard should not have to produce an accent to find an app that has one.
        assertTrue(AppSearch.filter(apps, "cafe").any { it.label == "Café" })
        assertTrue(AppSearch.filter(apps, "café").any { it.label == "Café" })
    }

    @Test
    fun `prefix matches rank above substring matches`() {
        // Typing "cal" should surface Calendar before an app that merely contains "cal".
        val results = AppSearch.filter(listOf(app("Vertical Sync"), app("Calendar")), "cal")
        assertEquals("Calendar", results.first().label)
    }

    @Test
    fun `a word-start match counts as a prefix`() {
        // "cal" should find "Google Calendar Sync" strongly, not bury it behind incidental matches.
        val results = AppSearch.filter(listOf(app("Vertical"), app("Google Calendar Sync")), "cal")
        assertEquals("Google Calendar Sync", results.first().label)
    }

    @Test
    fun `non-matches are excluded`() {
        assertTrue(AppSearch.filter(apps, "zzzz").isEmpty())
    }

    @Test
    fun `within a rank, inventory order is kept`() {
        // All five are prefix-rank: four begin with "ca", and "Google Calendar Sync" qualifies on
        // its second word. Ranking must not reshuffle them beyond that.
        val results = AppSearch.filter(apps, "ca").map { it.label }
        assertEquals(
            listOf("Calendar", "Calculator", "Google Calendar Sync", "Camera", "Café"),
            results,
        )
    }

    @Test
    fun `normalize strips case and accents`() {
        assertEquals("ahnlich", AppSearch.normalize("Ähnlich"))
        assertEquals("cafe", AppSearch.normalize("CAFÉ"))
    }

    @Test
    fun `searching an empty list is empty`() {
        assertTrue(AppSearch.filter(emptyList(), "anything").isEmpty())
    }
}

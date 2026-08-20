package com.somalapuram.pclauncher.core.apps

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SortingTest {

    @Test
    fun `sorts case-insensitively`() {
        val sorted = sortedByLabel(listOf(entry("zebra"), entry("Apple"), entry("banana")))
        assertEquals(listOf("Apple", "banana", "zebra"), sorted.map { it.label })
    }

    @Test
    fun `accented labels sort next to their base letter, not after Z`() {
        // The reason this uses a Collator: String.compareTo orders by UTF-16 code unit, which puts
        // "Ähnlich" after "Zulu" and looks broken to anyone outside en-US.
        val sorted = sortedByLabel(
            listOf(entry("Zulu"), entry("Ähnlich"), entry("Apfel")),
            Locale.GERMAN,
        )
        assertEquals(listOf("Ähnlich", "Apfel", "Zulu"), sorted.map { it.label })
    }

    @Test
    fun `identical labels in different profiles keep a stable order`() {
        // The same app in personal and work profiles collides on label; profile breaks the tie so
        // the list does not shuffle between rebuilds.
        val personal = entry("Mail", user = TestUsers.personal, profile = ProfileKind.Personal)
        val work = entry("Mail", user = TestUsers.work, profile = ProfileKind.Work)

        val once = sortedByLabel(listOf(work, personal))
        val twice = sortedByLabel(listOf(personal, work))

        assertEquals(once.map { it.key }, twice.map { it.key })
        assertEquals(ProfileKind.Personal, once.first().profile)
    }

    @Test
    fun `sorting an empty list is empty`() {
        assertEquals(emptyList<AppEntry>(), sortedByLabel(emptyList()))
    }
}

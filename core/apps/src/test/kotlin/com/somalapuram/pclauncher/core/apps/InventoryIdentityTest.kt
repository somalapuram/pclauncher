package com.somalapuram.pclauncher.core.apps

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * That the inventory holds one entry per key however many times it is built.
 *
 * The repository is a `@Singleton` and `HomeActivity.onCreate` starts it, so a second home activity
 * — a configuration change, or the system relaunching the home app when the default home role moves
 * — used to append the whole list again. On a Pixel Tablet that showed as a taskbar of four apps
 * listed twice each. The desktop hid it, because two entries for one component land on the same
 * cell and stack exactly (inventory-identity.md).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InventoryIdentityTest {

    private fun source() = FakeAppSource(
        mapOf(
            TestUsers.personal to listOf(entry("Files"), entry("Browser")),
            TestUsers.work to listOf(entry("Mail", user = TestUsers.work, profile = ProfileKind.Work)),
        ),
    )

    // --- the merge itself ---------------------------------------------------------------------

    @Test
    fun `merging an entry the list already holds keeps one of it`() {
        val existing = listOf(entry("Files"), entry("Browser"))

        val merged = mergedByKey(existing, listOf(entry("Files")))

        assertEquals(2, merged.size)
        assertEquals(1, merged.count { it.label == "Files" })
    }

    @Test
    fun `the incoming entry wins so a stale copy cannot shadow it`() {
        val stale = entry("Files")
        val renamed = stale.copy(label = "Files (new)")

        val merged = mergedByKey(listOf(stale), listOf(renamed))

        assertEquals(listOf("Files (new)"), merged.map { it.label })
    }

    @Test
    fun `entries not in the incoming list survive the merge`() {
        val merged = mergedByKey(listOf(entry("Files"), entry("Browser")), listOf(entry("Mail")))
        assertEquals(setOf("Files", "Browser", "Mail"), merged.map { it.label }.toSet())
    }

    @Test
    fun `merging nothing changes nothing`() {
        val existing = listOf(entry("Files"))
        assertEquals(existing, mergedByKey(existing, emptyList()))
    }

    @Test
    fun `the same package in two profiles is two entries, not one`() {
        // The key is the pair. Collapsing on the component alone would lose the work copy —
        // app-inventory.md requirement 1 must not regress in the course of fixing duplication.
        val personal = entry("Mail")
        val work = entry("Mail", user = TestUsers.work, profile = ProfileKind.Work)

        assertEquals(2, mergedByKey(listOf(personal), listOf(work)).size)
    }

    // --- building twice -----------------------------------------------------------------------

    @Test
    fun `starting twice leaves the entries of one build`() = runTest {
        val repo = AppInventoryRepository(source(), StandardTestDispatcher(testScheduler))

        repo.start(TestScope(testScheduler))
        advanceUntilIdle()
        val once = repo.inventory.value.entries.map { it.label }

        repo.start(TestScope(testScheduler))
        advanceUntilIdle()

        assertEquals(once, repo.inventory.value.entries.map { it.label })
    }

    @Test
    fun `starting twice does not leave the first subscription registered`() = runTest {
        val fake = source()
        val repo = AppInventoryRepository(fake, StandardTestDispatcher(testScheduler))

        repo.start(TestScope(testScheduler))
        advanceUntilIdle()
        repo.start(TestScope(testScheduler))
        advanceUntilIdle()

        // A leak is the gap: two opened, one closed, and every package change applied twice for
        // the life of the process.
        assertEquals(2, fake.subscriptionsOpened)
        assertEquals(1, fake.subscriptionsClosed)
    }

    @Test
    fun `a platform that lists the same profile twice still yields one entry per app`() = runTest {
        val fake = FakeAppSource(
            mapOf(TestUsers.personal to listOf(entry("Files"), entry("Browser"))),
            profileOrder = listOf(TestUsers.personal, TestUsers.personal),
        )
        val repo = AppInventoryRepository(fake, StandardTestDispatcher(testScheduler))

        repo.start(TestScope(testScheduler))
        advanceUntilIdle()

        assertEquals(2, repo.inventory.value.size)
    }

    @Test
    fun `a rebuild never empties the list on the way through`() = runTest {
        val repo = AppInventoryRepository(source(), StandardTestDispatcher(testScheduler))
        repo.start(TestScope(testScheduler))
        advanceUntilIdle()

        val sizes = mutableListOf<Int>()
        repo.start(TestScope(testScheduler))
        repeat(6) { advanceUntilIdle(); sizes += repo.inventory.value.size }

        assertEquals("the desktop blinked empty during a rebuild", emptyList<Int>(), sizes.filter { it == 0 })
    }
}

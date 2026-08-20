package com.somalapuram.pclauncher.core.apps

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppInventoryRepositoryTest {

    private fun source() = FakeAppSource(
        mapOf(
            TestUsers.personal to listOf(entry("Files"), entry("Browser")),
            TestUsers.work to listOf(entry("Mail", user = TestUsers.work, profile = ProfileKind.Work)),
        ),
    )

    @Test
    fun `the first emission is empty and incomplete so the desktop can render`() = runTest {
        // SRS §12: the desktop must paint inside its budget rather than block on the inventory.
        val repo = AppInventoryRepository(source(), StandardTestDispatcher(testScheduler))

        assertEquals(0, repo.inventory.value.size)
        assertFalse(repo.inventory.value.isComplete)
    }

    @Test
    fun `every profile is loaded and the list is marked complete`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repo = AppInventoryRepository(source(), dispatcher)

        repo.start(TestScope(testScheduler))
        advanceUntilIdle()

        assertEquals(3, repo.inventory.value.size)
        assertTrue(repo.inventory.value.isComplete)
        assertEquals(listOf("Browser", "Files", "Mail"), repo.inventory.value.entries.map { it.label })
    }

    @Test
    fun `changes arrive as deltas after start`() {
        val scheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val fake = source()
        val repo = AppInventoryRepository(fake, dispatcher)
        val scope = TestScope(scheduler)

        repo.start(scope)
        scheduler.advanceUntilIdle()

        assertTrue("the repository must be observing once built", fake.observing)

        fake.push(AppChange.PackageRemoved("com.example.files", TestUsers.personal))
        assertEquals(2, repo.inventory.value.size)
    }

    @Test
    fun `stopping unregisters the observer`() {
        val scheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
        val fake = source()
        val repo = AppInventoryRepository(fake, StandardTestDispatcher(scheduler))

        repo.start(TestScope(scheduler))
        scheduler.advanceUntilIdle()
        repo.stop()

        assertFalse(fake.observing)
    }

    @Test
    fun `a profile that returns nothing does not hold up the others`() {
        // A locked private space or a quiet work profile is a normal state, not a failure.
        val scheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
        val fake = FakeAppSource(
            mapOf(
                TestUsers.work to emptyList(),
                TestUsers.personal to listOf(entry("Files")),
            ),
        )
        val repo = AppInventoryRepository(fake, StandardTestDispatcher(scheduler))

        repo.start(TestScope(scheduler))
        scheduler.advanceUntilIdle()

        assertEquals(1, repo.inventory.value.size)
        assertTrue(repo.inventory.value.isComplete)
    }
}

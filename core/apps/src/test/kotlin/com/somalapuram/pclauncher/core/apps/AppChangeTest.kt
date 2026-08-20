package com.somalapuram.pclauncher.core.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppChangeTest {

    private val calendar = entry("Calendar")
    private val files = entry("Files")
    private val browser = entry("Browser")
    private val start = inventoryOf(calendar, files, browser)

    @Test
    fun `an install adds the entry in sorted position`() {
        val added = entry("Clock")
        val result = applyChange(
            start,
            AppChange.PackageUpserted(added.packageName, TestUsers.personal, listOf(added)),
        )

        assertEquals(listOf("Browser", "Calendar", "Clock", "Files"), result.entries.map { it.label })
    }

    @Test
    fun `an update replaces the package's entries rather than duplicating them`() {
        val renamed = calendar.copy(label = "Agenda", versionCode = 2L)
        val result = applyChange(
            start,
            AppChange.PackageUpserted(calendar.packageName, TestUsers.personal, listOf(renamed)),
        )

        assertEquals(3, result.size)
        assertEquals(listOf("Agenda", "Browser", "Files"), result.entries.map { it.label })
    }

    @Test
    fun `an update that drops an activity drops it from the inventory`() {
        // A package can lose a launcher activity across versions; patching in place would leave a
        // ghost entry pointing at a component that no longer exists.
        val twoActivities = inventoryOf(
            calendar,
            calendar.copy(
                key = AppKey(
                    android.content.ComponentName(calendar.packageName, "${calendar.packageName}.Second"),
                    TestUsers.personal,
                ),
                label = "Calendar Widgets",
            ),
        )
        val result = applyChange(
            twoActivities,
            AppChange.PackageUpserted(calendar.packageName, TestUsers.personal, listOf(calendar)),
        )

        assertEquals(1, result.size)
    }

    @Test
    fun `an uninstall removes only that package in that profile`() {
        val workCalendar = calendar.copy(
            key = AppKey(calendar.key.component, TestUsers.work),
            profile = ProfileKind.Work,
        )
        val withWork = inventoryOf(calendar, files, workCalendar)

        val result = applyChange(
            withWork,
            AppChange.PackageRemoved(calendar.packageName, TestUsers.personal),
        )

        // The work copy of the same package must survive.
        assertNull(result.entryFor(calendar.key))
        assertNotNull(result.entryFor(workCalendar.key))
    }

    @Test
    fun `suspension marks entries rather than removing them`() {
        val result = applyChange(
            start,
            AppChange.PackagesSuspended(setOf(files.packageName), TestUsers.personal, suspended = true),
        )

        val suspended = result.entryFor(files.key)!!
        assertTrue(suspended.isSuspended)
        assertFalse("a suspended app is still listed, just not launchable", suspended.isLaunchable)
        assertEquals(3, result.size)
    }

    @Test
    fun `unavailable entries stay in the list`() {
        // Quiet mode and detached storage must never make the user's apps silently vanish.
        val result = applyChange(
            start,
            AppChange.PackagesAvailability(
                setOf(files.packageName),
                TestUsers.personal,
                available = false,
            ),
        )

        val unavailable = result.entryFor(files.key)!!
        assertFalse(unavailable.isAvailable)
        assertFalse(unavailable.isLaunchable)
        assertEquals(3, result.size)
    }

    @Test
    fun `adding a profile brings its entries and replaces any stale ones`() {
        val workFiles = entry("Files", user = TestUsers.work, profile = ProfileKind.Work)
        val result = applyChange(
            start,
            AppChange.ProfileAdded(TestUsers.work, listOf(workFiles)),
        )

        assertEquals(4, result.size)
        assertEquals(2, result.entries.count { it.label == "Files" })
    }

    @Test
    fun `removing a profile takes everything in it and nothing else`() {
        val workFiles = entry("Files", user = TestUsers.work, profile = ProfileKind.Work)
        val withWork = applyChange(start, AppChange.ProfileAdded(TestUsers.work, listOf(workFiles)))

        val result = applyChange(withWork, AppChange.ProfileRemoved(TestUsers.work))

        assertEquals(3, result.size)
        assertTrue(result.entries.none { it.key.user == TestUsers.work })
    }

    @Test
    fun `a locale change relabels and re-sorts`() {
        // The change that is easy to forget: every label is different and so is the order.
        val german = listOf(entry("Zebra"), entry("Ähnlich"), entry("Apfel"))
        val result = applyChange(
            start,
            AppChange.LocaleChanged(german, Locale.GERMAN),
            locale = Locale.ENGLISH,
        )

        assertEquals(listOf("Ähnlich", "Apfel", "Zebra"), result.entries.map { it.label })
    }

    @Test
    fun `applying a change to an empty inventory works`() {
        val result = applyChange(
            AppInventory(),
            AppChange.PackageUpserted(calendar.packageName, TestUsers.personal, listOf(calendar)),
        )
        assertEquals(1, result.size)
    }

    @Test
    fun `removing something that is not there is a no-op, not a crash`() {
        val result = applyChange(start, AppChange.PackageRemoved("com.example.ghost", TestUsers.personal))
        assertEquals(3, result.size)
    }
}

package com.somalapuram.pclauncher.core.apps

import android.app.AppOpsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The contract of [usageAccessGranted]. Modes are written as the framework constants rather than
 * their numeric values so a reader can check them against `AppOpsManager` without a lookup table.
 */
class UsageAccessTest {

    @Test
    fun `allowed op grants access`() {
        assertTrue(usageAccessGranted(AppOpsManager.MODE_ALLOWED) { false })
    }

    @Test
    fun `allowed op does not consult the permission`() {
        var asked = false
        usageAccessGranted(AppOpsManager.MODE_ALLOWED) { asked = true; false }
        assertFalse("MODE_ALLOWED is the whole answer; asking again is a wasted binder call", asked)
    }

    @Test
    fun `default op falls back to the permission`() {
        assertTrue(usageAccessGranted(AppOpsManager.MODE_DEFAULT) { true })
        assertFalse(usageAccessGranted(AppOpsManager.MODE_DEFAULT) { false })
    }

    @Test
    fun `default op asks the permission exactly once`() {
        var asks = 0
        usageAccessGranted(AppOpsManager.MODE_DEFAULT) { asks++; true }
        assertEquals(1, asks)
    }

    @Test
    fun `an explicit denial is not overridden by the permission`() {
        // The user turned the toggle off in Settings. A signature grant must not undo that:
        // MODE_IGNORED is an opinion, unlike MODE_DEFAULT.
        assertFalse(usageAccessGranted(AppOpsManager.MODE_IGNORED) { true })
        assertFalse(usageAccessGranted(AppOpsManager.MODE_ERRORED) { true })
    }

    @Test
    fun `unknown modes deny`() {
        assertFalse(usageAccessGranted(AppOpsManager.MODE_FOREGROUND) { true })
        assertFalse(usageAccessGranted(Int.MIN_VALUE) { true })
        assertFalse(usageAccessGranted(Int.MAX_VALUE) { true })
    }

    @Test
    fun `the ungranted default is what a stock device reports`() {
        // Nothing granted the permission and the user never opened the toggle: the case that must
        // keep behaving exactly as before this rule existed.
        assertFalse(usageAccessGranted(AppOpsManager.MODE_DEFAULT) { false })
    }
}

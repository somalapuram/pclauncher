package com.somalapuram.pclauncher

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.provider.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeRoleTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `asking for the home role always yields a usable intent`() {
        // Whichever branch is taken, the button must never be dead (SRS §6.1: no dead ends).
        val intent = HomeRole.requestIntent(context)
        assertNotNull(intent.action)
    }

    @Test
    fun `it is either a role request or the home settings screen`() {
        // RoleManager's request action is not public API, so match on shape rather than on a
        // constant we would have to reach for through reflection.
        val action = HomeRole.requestIntent(context).action
        assertTrue(
            "expected a role request or the home settings screen, got $action",
            action == Settings.ACTION_HOME_SETTINGS || action?.contains("REQUEST_ROLE") == true,
        )
    }

    @Test
    fun `isDefault resolves HOME and compares packages`() {
        // In Robolectric this app's manifest is the only CATEGORY_HOME handler, so it *is* the
        // resolved home app. That makes this a real check of the lookup: a wrong intent or a
        // missing HOME category here would report false.
        assertTrue(HomeRole.isDefault(context))
    }

    @Test
    fun `only our own package counts as default`() {
        assertTrue(HomeRole.isDefault("com.somalapuram.pclauncher", "com.somalapuram.pclauncher"))
        assertFalse(HomeRole.isDefault("com.android.launcher3", "com.somalapuram.pclauncher"))
    }

    @Test
    fun `nobody holding the role does not read as us`() {
        // A false positive here would hide the set-as-home button and strand the user.
        assertFalse(HomeRole.isDefault(null, "com.somalapuram.pclauncher"))
    }
}

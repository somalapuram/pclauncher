package com.somalapuram.pclauncher.overlay

import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The flags the chrome's window carries.
 *
 * An overlay that is focusable steals every keystroke from the app the user is actually typing
 * into, so the shell is not focusable at rest and only becomes so while a menu is open. Getting
 * this backwards is not visible on screen — the shell looks right and the user's typing goes
 * missing — which is why it is pinned here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OverlayWindowFlagsTest {

    private fun has(flags: Int, flag: Int) = flags and flag != 0

    @Test
    fun `at rest the window takes no key events`() {
        assertEquals(true, has(windowFlags(focusable = false), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE))
    }

    @Test
    fun `a menu makes it focusable`() {
        assertEquals(false, has(windowFlags(focusable = true), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE))
    }

    @Test
    fun `the two states really differ`() {
        assertNotEquals(windowFlags(focusable = false), windowFlags(focusable = true))
    }

    @Test
    fun `the window lays out beyond its bounds in both states`() {
        // The bar floats with a margin and its menus open past its edges; without this they are
        // clipped to the window rather than the screen.
        listOf(true, false).forEach { focusable ->
            val flags = windowFlags(focusable)
            assertEquals(true, has(flags, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS))
            assertEquals(true, has(flags, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN))
        }
    }

    @Test
    fun `the overlay type is the one an unprivileged app may use`() {
        // TYPE_SYSTEM_ALERT and friends have been unavailable to ordinary apps since API 26.
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, overlayType())
    }
}

package com.somalapuram.pclauncher.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Which host draws the bar.
 *
 * Exactly one at a time. Two bars would be worse than the one that hides behind app windows, and
 * none would be worse still — a permission that was granted but a service that has died must leave
 * the chrome in the activity rather than nowhere (GATE 4).
 */
class ChromeHostTest {

    @Test
    fun `the overlay draws it only when it can and does`() {
        assertEquals(
            ChromeHost.Overlay,
            chromeHostFor(hasPermission = true, overlayRunning = true),
        )
    }

    @Test
    fun `without the permission the activity keeps it`() {
        assertEquals(
            ChromeHost.HomeActivity,
            chromeHostFor(hasPermission = false, overlayRunning = true),
        )
    }

    @Test
    fun `a dead service falls back rather than leaving no bar`() {
        assertEquals(
            ChromeHost.HomeActivity,
            chromeHostFor(hasPermission = true, overlayRunning = false),
        )
    }

    @Test
    fun `neither means the activity, which is today's behaviour`() {
        assertEquals(
            ChromeHost.HomeActivity,
            chromeHostFor(hasPermission = false, overlayRunning = false),
        )
    }
}

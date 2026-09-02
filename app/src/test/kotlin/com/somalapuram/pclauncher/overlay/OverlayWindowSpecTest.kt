package com.somalapuram.pclauncher.overlay

import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The two window shapes the chrome uses, and why they have to be two (overlay-window-split.md). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OverlayWindowSpecTest {

    @Test
    fun `bar is never focusable`() {
        assertFalse(BarWindowSpec.focusable)
    }

    @Test
    fun `bar is only as tall as its content`() {
        assertEquals(WindowManager.LayoutParams.WRAP_CONTENT, BarWindowSpec.height)
    }

    @Test
    fun `menu covers the screen so a click anywhere else dismisses it`() {
        assertEquals(WindowManager.LayoutParams.MATCH_PARENT, MenuWindowSpec.height)
    }

    @Test
    fun `menu takes keys`() {
        assertTrue(MenuWindowSpec.focusable)
    }

    /**
     * The reason for two windows at all. If one window could serve both it would have to change
     * size and focus as the menu opens, and each of those re-creates the window's surface.
     */
    @Test
    fun `the two shapes differ in both height and focus`() {
        assertNotEquals(BarWindowSpec.height, MenuWindowSpec.height)
        assertNotEquals(BarWindowSpec.focusable, MenuWindowSpec.focusable)
    }

    @Test
    fun `each shape maps to different window flags`() {
        assertNotEquals(
            windowFlags(BarWindowSpec.focusable),
            windowFlags(MenuWindowSpec.focusable),
        )
    }
}

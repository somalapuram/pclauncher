package com.somalapuram.pclauncher.feature.shell.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * That a hosted widget is not put in a box.
 *
 * The scrim and hairline were written for the *placeholder* — the thing to show when a provider
 * fails to inflate — but were applied to the container unconditionally, so every working widget got
 * a frame too. Widgets are designed to sit on a wallpaper and bring their own background and
 * corners; ours became a second, competing edge around each one.
 */
class WidgetChromeTest {

    @Test
    fun `a widget that inflated draws no frame of ours`() {
        assertFalse(needsChrome(hasView = true))
    }

    @Test
    fun `the placeholder keeps its frame`() {
        // "Widget unavailable" on bare wallpaper reads as a rendering fault, not a message.
        assertTrue(needsChrome(hasView = false))
    }
}

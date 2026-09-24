package com.somalapuram.pclauncher.overlay

import android.view.Display
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which display a home is on decides who hosts its bar (secondary-display-home.md).
 *
 * The predicate is pure so the decision can be pinned here rather than discovered on a second
 * monitor. `Display.DEFAULT_DISPLAY` is a compile-time constant, so this runs on the JVM with no
 * framework.
 */
class DisplayHostTest {

    @Test
    fun `the default display is the default display`() {
        assertTrue(isDefaultDisplay(Display.DEFAULT_DISPLAY))
    }

    @Test
    fun `any other display is not`() {
        for (id in listOf(1, 2, 7, 4614472054165788160L.toInt())) {
            assertFalse("displayId=$id", isDefaultDisplay(id))
        }
    }

    @Test
    fun `no display yet counts as default, the invisible mistake`() {
        // Treating display 0 as secondary would draw a second bar on the main screen; treating a
        // secondary as default only delays its bar until the next recomposition.
        assertTrue(isDefaultDisplay(null))
    }
}

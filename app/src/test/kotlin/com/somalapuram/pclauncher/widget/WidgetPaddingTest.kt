package com.somalapuram.pclauncher.widget

import android.appwidget.AppWidgetHostView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * That a hosted widget fills the cells it was placed on.
 *
 * `AppWidgetHostView` insets what it hosts by its own default padding — spacing intended for a home
 * screen that does none of its own. Ours is a grid, so the cells *are* the spacing, and the host's
 * padding lands on top of a layout that already accounts for it: a 5×4 widget drew 912 × 784 inside
 * a 960 × 832 rectangle, centred and short of the grid lines on every side.
 *
 * Asserted against a real host view rather than in prose, because the fault is symmetric and
 * therefore survives a visual check — the widget just looks deliberately small.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetPaddingTest {

    private fun hostView() =
        AppWidgetHostView(ApplicationProvider.getApplicationContext())

    @Test
    fun `the host's padding is taken off`() {
        val view = hostView().apply { setPadding(24, 24, 24, 24) }

        fillItsCell(view)

        assertEquals(0, view.paddingLeft)
        assertEquals(0, view.paddingTop)
        assertEquals(0, view.paddingRight)
        assertEquals(0, view.paddingBottom)
    }

    @Test
    fun `asymmetric padding is taken off too`() {
        // Defaults differ per edge on some platform versions.
        val view = hostView().apply { setPadding(8, 16, 24, 32) }

        fillItsCell(view)

        assertEquals(0, view.paddingLeft + view.paddingTop + view.paddingRight + view.paddingBottom)
    }

    @Test
    fun `a view already flush is left alone`() {
        val view = hostView()

        fillItsCell(view)

        assertEquals(0, view.paddingLeft)
    }
}

package com.somalapuram.pclauncher.core.data.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The size a placement occupies, which is what the provider has to be told.
 *
 * Until it is told, a provider lays out for its own default and the host centres that inside
 * whatever bounds it was given — a 4×3 widget rendering as a narrow pill in a large empty box.
 */
class WidgetSizeTest {

    @Test
    fun `a span becomes the dp it covers`() {
        assertEquals(WidgetSizeDp(384, 312), widgetSizeDp(DesktopSpan(4, 3), 96, 104))
    }

    @Test
    fun `a single cell is one cell`() {
        assertEquals(WidgetSizeDp(96, 104), widgetSizeDp(DesktopSpan.Single, 96, 104))
    }

    @Test
    fun `a different cell size scales it`() {
        // The grid pitch is a token and has already changed once.
        assertEquals(WidgetSizeDp(240, 260), widgetSizeDp(DesktopSpan(2, 2), 120, 130))
    }

    @Test
    fun `a nonsense cell size reports nothing rather than a negative`() {
        assertEquals(WidgetSizeDp(0, 0), widgetSizeDp(DesktopSpan(3, 3), 0, 0))
        assertEquals(WidgetSizeDp(0, 0), widgetSizeDp(DesktopSpan(3, 3), -8, -8))
    }
}

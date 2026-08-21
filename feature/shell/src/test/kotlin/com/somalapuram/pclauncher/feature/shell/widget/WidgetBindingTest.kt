package com.somalapuram.pclauncher.feature.shell.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBindingTest {

    @Test
    fun `a launcher holding the permission binds directly`() {
        assertEquals(BindOutcome.Bound, bindOutcomeFor(allowedDirectly = true, canAskUser = true))
    }

    @Test
    fun `otherwise the system asks the user`() {
        // pclauncher usually is not the default home during Stage A, so this is the common path,
        // not the exception.
        assertEquals(
            BindOutcome.NeedsUserConsent,
            bindOutcomeFor(allowedDirectly = false, canAskUser = true),
        )
    }

    @Test
    fun `with no way to ask, binding fails`() {
        assertEquals(BindOutcome.Failed, bindOutcomeFor(allowedDirectly = false, canAskUser = false))
    }

    @Test
    fun `a widget gets at least one cell`() {
        assertEquals(1, cellsFor(minSizeDp = 40, cellSizeDp = 96))
        assertEquals(1, cellsFor(minSizeDp = 96, cellSizeDp = 96))
    }

    @Test
    fun `cells round up, never down`() {
        // A widget given less room than it asked for clips its own content and looks like our bug.
        assertEquals(2, cellsFor(minSizeDp = 97, cellSizeDp = 96))
        assertEquals(3, cellsFor(minSizeDp = 200, cellSizeDp = 96))
    }

    @Test
    fun `a provider reporting nothing still gets a cell`() {
        assertEquals(1, cellsFor(minSizeDp = 0, cellSizeDp = 96))
        assertEquals(1, cellsFor(minSizeDp = -10, cellSizeDp = 96))
    }

    @Test
    fun `a degenerate cell size does not divide by zero`() {
        assertEquals(1, cellsFor(minSizeDp = 200, cellSizeDp = 0))
    }

    @Test
    fun `a completed widget keeps its id`() {
        assertFalse(shouldReleaseId(bound = true, configured = true, cancelled = false))
    }

    @Test
    fun `every failure path releases the id`() {
        // A user who cancels the picker ten times would otherwise leave ten orphans that nothing
        // ever collects.
        assertTrue(shouldReleaseId(bound = false, configured = false, cancelled = false))
        assertTrue(shouldReleaseId(bound = true, configured = false, cancelled = false))
        assertTrue(shouldReleaseId(bound = true, configured = true, cancelled = true))
    }
}

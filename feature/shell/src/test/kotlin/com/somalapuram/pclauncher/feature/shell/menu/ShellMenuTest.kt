package com.somalapuram.pclauncher.feature.shell.menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** One menu at a time, by construction (tray-popover-host.md requirement 5). */
class ShellMenuTest {

    @Test
    fun `nothing is open to begin with`() {
        assertFalse(ShellMenu.None.isOpen)
    }

    @Test
    fun `toggling from nothing opens that menu`() {
        assertEquals(ShellMenu.Start, ShellMenu.None.toggled(ShellMenu.Start))
        assertEquals(ShellMenu.QuickSettings, ShellMenu.None.toggled(ShellMenu.QuickSettings))
    }

    @Test
    fun `toggling the open menu closes it`() {
        assertEquals(ShellMenu.None, ShellMenu.Start.toggled(ShellMenu.Start))
        assertEquals(ShellMenu.None, ShellMenu.QuickSettings.toggled(ShellMenu.QuickSettings))
    }

    /** The reason this is one value: opening one menu must close the other, not sit over it. */
    @Test
    fun `opening one menu replaces the other`() {
        assertEquals(ShellMenu.QuickSettings, ShellMenu.Start.toggled(ShellMenu.QuickSettings))
        assertEquals(ShellMenu.Start, ShellMenu.QuickSettings.toggled(ShellMenu.Start))
    }

    @Test
    fun `every menu but None reports itself open`() {
        for (menu in ShellMenu.entries) {
            assertEquals(menu != ShellMenu.None, menu.isOpen)
        }
    }

    @Test
    fun `two menus are never open together`() {
        // There is one slot, so the invariant is the type's rather than a rule to be maintained.
        for (from in ShellMenu.entries) {
            for (to in ShellMenu.entries.filter { it != ShellMenu.None }) {
                assertTrue(from.toggled(to) == ShellMenu.None || from.toggled(to) == to)
            }
        }
    }
}

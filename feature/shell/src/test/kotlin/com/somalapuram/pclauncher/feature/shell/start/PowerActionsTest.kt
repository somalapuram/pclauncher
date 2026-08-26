package com.somalapuram.pclauncher.feature.shell.start

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the Start menu's footer can do, in both worlds.
 *
 * The same APK runs unprivileged here and as a platform app on `aosp-pc-x86_64`, so availability is
 * a runtime question about permissions rather than a build-time one — and both answers have to be
 * testable without either device.
 */
class PowerActionsTest {

    private val unprivileged = PowerPrivileges()
    private val platform = PowerPrivileges(canReboot = true, canShutDown = true)

    @Test
    fun `settings and restarting our own shell never need anything`() {
        listOf(unprivileged, platform).forEach { privileges ->
            assertTrue(isAvailable(PowerAction.OpenSettings, privileges))
            assertTrue(isAvailable(PowerAction.RestartShell, privileges))
        }
    }

    @Test
    fun `restarting the device needs reboot and nothing else`() {
        assertFalse(isAvailable(PowerAction.RestartDevice, unprivileged))
        assertTrue(isAvailable(PowerAction.RestartDevice, PowerPrivileges(canReboot = true)))
        assertFalse(isAvailable(PowerAction.RestartDevice, PowerPrivileges(canShutDown = true)))
    }

    @Test
    fun `powering off needs shutdown and nothing else`() {
        assertFalse(isAvailable(PowerAction.PowerOff, unprivileged))
        assertTrue(isAvailable(PowerAction.PowerOff, PowerPrivileges(canShutDown = true)))
        assertFalse(isAvailable(PowerAction.PowerOff, PowerPrivileges(canReboot = true)))
    }

    @Test
    fun `the two privileges are independent`() {
        // A platform build could allowlist one and not the other; the footer must not assume.
        val rebootOnly = PowerPrivileges(canReboot = true, canShutDown = false)

        assertTrue(isAvailable(PowerAction.RestartDevice, rebootOnly))
        assertFalse(isAvailable(PowerAction.PowerOff, rebootOnly))
    }

    @Test
    fun `an unavailable action explains itself`() {
        // A greyed control with no reason reads as a bug rather than a boundary (SRS §5.3).
        assertNotNull(unavailableReason(PowerAction.PowerOff, unprivileged))
        assertNotNull(unavailableReason(PowerAction.RestartDevice, unprivileged))
    }

    @Test
    fun `an available action has nothing to explain`() {
        assertNull(unavailableReason(PowerAction.RestartShell, unprivileged))
        assertNull(unavailableReason(PowerAction.PowerOff, platform))
    }

    @Test
    fun `every action is labelled and ordered`() {
        assertEquals(PowerAction.entries.size, PowerActionOrder.size)
        assertEquals(PowerAction.entries.toSet(), PowerActionOrder.toSet())
        PowerActionOrder.forEach { assertTrue(labelFor(it).isNotBlank()) }
    }

    @Test
    fun `settings comes first and powering off last`() {
        assertEquals(PowerAction.OpenSettings, PowerActionOrder.first())
        assertEquals(PowerAction.PowerOff, PowerActionOrder.last())
    }
}

package com.somalapuram.pclauncher.feature.shell.start

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the Start menu's footer calls this machine.
 *
 * SRS §6.4 asks for the device name, and on real hardware that is a name the user chose. On an
 * emulator every source returns `sdk_gphone16k_x86_64`, which in a footer reads as debug text
 * someone forgot to remove.
 */
class DeviceNameTest {

    @Test
    fun `the user's own name for the device wins`() {
        assertEquals(
            "Amar's Tablet",
            displayableDeviceName(deviceName = "Amar's Tablet", model = "Pixel Tablet"),
        )
    }

    @Test
    fun `the model stands in when nothing has been set`() {
        assertEquals("Pixel Tablet", displayableDeviceName(deviceName = null, model = "Pixel Tablet"))
        assertEquals("Pixel Tablet", displayableDeviceName(deviceName = "  ", model = "Pixel Tablet"))
    }

    @Test
    fun `a build identifier is shown as nothing at all`() {
        assertNull(displayableDeviceName("sdk_gphone16k_x86_64", "sdk_gphone16k_x86_64"))
        assertNull(displayableDeviceName(null, "generic_x86_64"))
        assertNull(displayableDeviceName(null, "aosp_pc_x86_64"))
    }

    @Test
    fun `a set name is kept even when the model is an identifier`() {
        // The exact case on the target device once someone has named it.
        assertEquals("Workshop PC", displayableDeviceName("Workshop PC", "aosp_pc_x86_64"))
    }

    @Test
    fun `nothing at all is nothing`() {
        assertNull(displayableDeviceName(null, null))
        assertNull(displayableDeviceName("", ""))
    }

    @Test
    fun `ordinary names are not mistaken for identifiers`() {
        listOf("Pixel Tablet", "Amar's Tablet", "Galaxy Tab S9", "Surface Pro").forEach {
            assertFalse(it, looksLikeBuildIdentifier(it))
        }
    }

    @Test
    fun `the markers match the shapes emulators actually produce`() {
        listOf("sdk_gphone64_arm64", "generic_x86", "emulator64_x86_64").forEach {
            assertTrue(it, looksLikeBuildIdentifier(it))
        }
    }
}

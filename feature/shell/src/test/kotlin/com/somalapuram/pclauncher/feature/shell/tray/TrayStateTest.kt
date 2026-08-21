package com.somalapuram.pclauncher.feature.shell.tray

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrayStateTest {

    @Test
    fun `percentage is computed against the reported scale`() {
        // scale is not always 100; assuming it is quietly misreports on any device that says
        // otherwise.
        assertEquals(50, batteryPercent(level = 50, scale = 100))
        assertEquals(50, batteryPercent(level = 5, scale = 10))
        assertEquals(25, batteryPercent(level = 1000, scale = 4000))
    }

    @Test
    fun `an unreadable level yields no percentage`() {
        assertNull(batteryPercent(level = -1, scale = 100))
        assertNull(batteryPercent(level = 50, scale = 0))
        assertNull(batteryPercent(level = 50, scale = -1))
    }

    @Test
    fun `percentage is clamped to a sane range`() {
        assertEquals(100, batteryPercent(level = 200, scale = 100))
        assertEquals(0, batteryPercent(level = 0, scale = 100))
    }

    @Test
    fun `charging covers every way of being plugged in`() {
        assertTrue(isCharging(BATTERY_STATUS_CHARGING, plugged = 0))
        assertTrue(isCharging(BATTERY_STATUS_FULL, plugged = 0))
        assertTrue(isCharging(status = -1, plugged = 1))
    }

    @Test
    fun `not plugged and not charging is not charging`() {
        assertTrue(!isCharging(status = 3, plugged = 0))
    }

    @Test
    fun `unknown is distinct from off`() {
        // "off" and "we could not tell" are different things, and only one is actionable.
        assertEquals("?", connectionGlyph(ConnectionState.Unknown, "on", "off", "?"))
        assertEquals("off", connectionGlyph(ConnectionState.Off, "on", "off", "?"))
        assertEquals("on", connectionGlyph(ConnectionState.On, "on", "off", "?"))
    }

    @Test
    fun `battery label marks charging`() {
        assertEquals("⚡80%", BatteryState.Known(80, charging = true).label())
        assertEquals("80%", BatteryState.Known(80, charging = false).label())
        assertEquals("--%", BatteryState.Unknown.label())
    }

    @Test
    fun `the tray reads as one description, not four`() {
        val text = TrayState(
            timeText = "09:41",
            battery = BatteryState.Known(80, false),
            wifi = ConnectionState.On,
            bluetooth = ConnectionState.Off,
        ).describe()

        assertTrue(text.startsWith("09:41"))
        assertTrue(text.contains("battery 80%"))
        assertTrue(text.contains("wi-fi on"))
        assertTrue(text.contains("bluetooth off"))
    }

    @Test
    fun `a default tray is entirely unknown, not falsely off`() {
        val state = TrayState()
        assertEquals(ConnectionState.Unknown, state.wifi)
        assertEquals(ConnectionState.Unknown, state.bluetooth)
        assertEquals(BatteryState.Unknown, state.battery)
    }
}

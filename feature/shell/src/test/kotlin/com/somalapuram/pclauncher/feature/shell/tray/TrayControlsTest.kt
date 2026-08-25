package com.somalapuram.pclauncher.feature.shell.tray

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The tray's arithmetic and routing: what a value draws as, and what a row does. */
class TrayControlsTest {

    // --- volume glyph -------------------------------------------------------------------------

    @Test
    fun `silence is muted whatever the maximum`() {
        assertEquals(VolumeGlyph.Muted, volumeGlyph(VolumeState(level = 0, max = 15)))
        assertEquals(VolumeGlyph.Muted, volumeGlyph(VolumeState(level = 0, max = 1)))
    }

    @Test
    fun `a volume that cannot be read draws as muted rather than as full`() {
        // Guessing loud would be the one wrong answer a user notices immediately.
        assertEquals(VolumeGlyph.Muted, volumeGlyph(VolumeState(level = 7, max = 0)))
    }

    @Test
    fun `a negative level is muted, not a negative fill`() {
        assertEquals(VolumeGlyph.Muted, volumeGlyph(VolumeState(level = -3, max = 15)))
    }

    @Test
    fun `the three audible bands each get their own glyph`() {
        assertEquals(VolumeGlyph.Low, volumeGlyph(VolumeState(level = 2, max = 15)))
        assertEquals(VolumeGlyph.Medium, volumeGlyph(VolumeState(level = 8, max = 15)))
        assertEquals(VolumeGlyph.High, volumeGlyph(VolumeState(level = 15, max = 15)))
    }

    @Test
    fun `full volume is high`() {
        assertEquals(VolumeGlyph.High, volumeGlyph(VolumeState(level = 1, max = 1)))
    }

    // --- slider position ----------------------------------------------------------------------

    @Test
    fun `the handle sits at the level's share of the maximum`() {
        assertEquals(0.5f, fractionOf(VolumeState(level = 5, max = 10)), 0.001f)
        assertEquals(1f, fractionOf(VolumeState(level = 10, max = 10)), 0.001f)
        assertEquals(0f, fractionOf(VolumeState(level = 0, max = 10)), 0.001f)
    }

    @Test
    fun `an unknown volume parks the handle at silent`() {
        assertEquals(0f, fractionOf(VolumeState(level = 5, max = 0)), 0.001f)
    }

    @Test
    fun `a level above the maximum still reads as full rather than past it`() {
        assertEquals(1f, fractionOf(VolumeState(level = 99, max = 10)), 0.001f)
    }

    @Test
    fun `the top of the slider reaches the top of the range`() {
        // Truncating here leaves the handle against the end with the volume one step short.
        assertEquals(15, streamIndexFor(fraction = 1f, max = 15))
        assertEquals(15, streamIndexFor(fraction = 0.98f, max = 15))
    }

    @Test
    fun `the bottom of the slider is silence`() {
        assertEquals(0, streamIndexFor(fraction = 0f, max = 15))
    }

    @Test
    fun `a slider position rounds to the nearest step`() {
        assertEquals(8, streamIndexFor(fraction = 0.5f, max = 15))
        assertEquals(7, streamIndexFor(fraction = 0.49f, max = 15))
    }

    @Test
    fun `a position outside the track is clamped, not extrapolated`() {
        assertEquals(0, streamIndexFor(fraction = -2f, max = 15))
        assertEquals(15, streamIndexFor(fraction = 4f, max = 15))
    }

    @Test
    fun `a device with no music stream yields no index`() {
        assertEquals(0, streamIndexFor(fraction = 0.7f, max = 0))
        assertEquals(0, streamIndexFor(fraction = 0.7f, max = -1))
    }

    @Test
    fun `a position round-trips through the level it selects`() {
        val max = 15
        (0..max).forEach { level ->
            val fraction = fractionOf(VolumeState(level, max))
            assertEquals(level, streamIndexFor(fraction, max))
        }
    }

    @Test
    fun `known means there is a stream to change`() {
        assertTrue(VolumeState(level = 0, max = 15).isKnown)
        assertFalse(VolumeState(level = 0, max = 0).isKnown)
    }

    // --- battery ------------------------------------------------------------------------------

    @Test
    fun `the battery fill tracks the percentage`() {
        assertEquals(0f, batteryFill(BatteryState.Known(0, charging = false)), 0.001f)
        assertEquals(0.42f, batteryFill(BatteryState.Known(42, charging = false)), 0.001f)
        assertEquals(1f, batteryFill(BatteryState.Known(100, charging = true)), 0.001f)
    }

    @Test
    fun `an unknown battery draws empty rather than guessing`() {
        assertEquals(0f, batteryFill(BatteryState.Unknown), 0.001f)
    }

    @Test
    fun `a percentage outside the range cannot overflow the glyph`() {
        assertEquals(1f, batteryFill(BatteryState.Known(140, charging = false)), 0.001f)
        assertEquals(0f, batteryFill(BatteryState.Known(-5, charging = false)), 0.001f)
    }

    // --- wi-fi --------------------------------------------------------------------------------

    @Test
    fun `wifi fills its arcs when connected and none otherwise`() {
        assertEquals(3, wifiBars(ConnectionState.On))
        assertEquals(0, wifiBars(ConnectionState.Off))
        assertEquals(0, wifiBars(ConnectionState.Unknown))
    }

    // --- routing ------------------------------------------------------------------------------

    @Test
    fun `bluetooth that is on opens its settings`() {
        assertEquals(TrayAction.OpenBluetoothSettings, bluetoothAction(ConnectionState.On))
    }

    @Test
    fun `bluetooth that is off offers to turn it on`() {
        // Burying the one thing the user wanted behind a settings screen is the wrong answer here.
        assertEquals(TrayAction.EnableBluetooth, bluetoothAction(ConnectionState.Off))
    }

    @Test
    fun `bluetooth we cannot read still offers the system's own dialog`() {
        assertEquals(TrayAction.EnableBluetooth, bluetoothAction(ConnectionState.Unknown))
    }

    @Test
    fun `enabling bluetooth without the permission goes where the toggle exists`() {
        // ACTION_REQUEST_ENABLE fails silently without BLUETOOTH_CONNECT, and a click that looks
        // like it worked while nothing happens is the worst of the available answers.
        assertEquals(TrayAction.OpenBluetoothSettings, bluetoothEnableAction(hasConnectPermission = false))
    }

    @Test
    fun `enabling bluetooth with the permission uses the system's own dialog`() {
        assertEquals(TrayAction.EnableBluetooth, bluetoothEnableAction(hasConnectPermission = true))
    }

    @Test
    fun `a slider drag becomes a volume the performer can apply`() {
        assertEquals(TrayAction.SetVolume(8), volumeAction(fraction = 0.5f, max = 15))
        assertEquals(TrayAction.SetVolume(0), volumeAction(fraction = 0.5f, max = 0))
    }

    // --- labels -------------------------------------------------------------------------------

    @Test
    fun `a state we could not read says so instead of claiming off`() {
        assertEquals("Unknown", ConnectionState.Unknown.label(connected = "On", off = "Off"))
        assertEquals("On", ConnectionState.On.label(connected = "On", off = "Off"))
        assertEquals("Off", ConnectionState.Off.label(connected = "On", off = "Off"))
    }

    @Test
    fun `the tray describes every indicator it knows about`() {
        val described = TrayState(
            timeText = "9:41",
            battery = BatteryState.Known(80, charging = true),
            wifi = ConnectionState.On,
            bluetooth = ConnectionState.Off,
            volume = VolumeState(level = 15, max = 15),
        ).describe()

        assertTrue(described.contains("9:41"))
        assertTrue(described.contains("⚡80%"))
        assertTrue(described.contains("wi-fi on"))
        assertTrue(described.contains("bluetooth off"))
        assertTrue(described.contains("volume high"))
    }

    @Test
    fun `an unreadable volume is left out of the description rather than announced as silent`() {
        val described = TrayState(volume = VolumeState(max = 0)).describe()
        assertFalse(described.contains("volume"))
    }

    // --- tile fill ----------------------------------------------------------------------------

    private fun state(
        wifi: ConnectionState = ConnectionState.Unknown,
        bluetooth: ConnectionState = ConnectionState.Unknown,
        battery: BatteryState = BatteryState.Unknown,
        volume: VolumeState = VolumeState(),
    ) = TrayState(battery = battery, wifi = wifi, bluetooth = bluetooth, volume = volume)

    @Test
    fun `a connected radio fills its tile`() {
        assertTrue(tileIsOn(TrayIndicator.Wifi, state(wifi = ConnectionState.On)))
        assertTrue(tileIsOn(TrayIndicator.Bluetooth, state(bluetooth = ConnectionState.On)))
    }

    @Test
    fun `a disconnected radio does not`() {
        assertFalse(tileIsOn(TrayIndicator.Wifi, state(wifi = ConnectionState.Off)))
        assertFalse(tileIsOn(TrayIndicator.Bluetooth, state(bluetooth = ConnectionState.Off)))
    }

    @Test
    fun `a state we could not read never claims to be on`() {
        assertFalse(tileIsOn(TrayIndicator.Wifi, state(wifi = ConnectionState.Unknown)))
        assertFalse(tileIsOn(TrayIndicator.Bluetooth, state(bluetooth = ConnectionState.Unknown)))
    }

    @Test
    fun `battery is never filled, however full or charging it is`() {
        // A filled tile advertises a switch, and battery has none.
        listOf(
            BatteryState.Unknown,
            BatteryState.Known(0, charging = false),
            BatteryState.Known(100, charging = true),
        ).forEach { assertFalse(tileIsOn(TrayIndicator.Battery, state(battery = it))) }
    }

    @Test
    fun `volume counts as on only when it is audible`() {
        assertTrue(tileIsOn(TrayIndicator.Volume, state(volume = VolumeState(1, 15))))
        assertFalse(tileIsOn(TrayIndicator.Volume, state(volume = VolumeState(0, 15))))
        assertFalse(tileIsOn(TrayIndicator.Volume, state(volume = VolumeState(9, 0))))
    }

    @Test
    fun `the percentage rounds to whole numbers across the range`() {
        assertEquals(0, volumePercent(VolumeState(0, 15)))
        assertEquals(100, volumePercent(VolumeState(15, 15)))
        assertEquals(53, volumePercent(VolumeState(8, 15)))
        assertEquals(0, volumePercent(VolumeState(8, 0)))
    }
}
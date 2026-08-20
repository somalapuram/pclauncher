package com.somalapuram.pclauncher.core.data.pins

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinsTest {

    private val chrome = Pin("com.android.chrome/.Main", 0L)
    private val files = Pin("com.android.files/.Main", 0L)
    private val workChrome = Pin("com.android.chrome/.Main", 10L)

    @Test
    fun `pinning appends in order`() {
        val pins = Pins().plus(chrome).plus(files)
        assertEquals(listOf(chrome, files), pins.items)
    }

    @Test
    fun `pinning twice does not duplicate`() {
        assertEquals(1, Pins().plus(chrome).plus(chrome).size)
    }

    @Test
    fun `pinning something already pinned does not reorder it`() {
        // A user who double-clicks Pin should not find their dock rearranged.
        val pins = Pins().plus(chrome).plus(files).plus(chrome)
        assertEquals(listOf(chrome, files), pins.items)
    }

    @Test
    fun `unpinning removes only that entry`() {
        val pins = Pins().plus(chrome).plus(files).minus(chrome)
        assertEquals(listOf(files), pins.items)
    }

    @Test
    fun `unpinning something absent is a no-op`() {
        val pins = Pins().plus(files)
        assertEquals(pins, pins.minus(chrome))
    }

    @Test
    fun `the same component in two profiles is two pins`() {
        // Pinning Chrome from the work profile must not put the personal Chrome in the dock.
        val pins = Pins().plus(chrome).plus(workChrome)
        assertEquals(2, pins.size)
        assertTrue(pins.contains(workChrome))
    }

    @Test
    fun `an empty store round-trips`() {
        assertEquals(Pins(), PinCodec.decode(PinCodec.encode(Pins())))
    }

    @Test
    fun `pins round-trip through the codec in order`() {
        val pins = Pins().plus(chrome).plus(files).plus(workChrome)
        assertEquals(pins, PinCodec.decode(PinCodec.encode(pins)))
    }

    @Test
    fun `a corrupted line loses one pin, not the dock`() {
        // Throwing here would lose the whole dock, and this is read on the way to drawing the
        // home screen.
        val raw = "com.a/.Main|0\ngarbage-with-no-serial\ncom.b/.Main|0"
        assertEquals(2, PinCodec.decode(raw).size)
    }

    @Test
    fun `a non-numeric serial is skipped`() {
        assertEquals(0, PinCodec.decode("com.a/.Main|notanumber").size)
    }

    @Test
    fun `null and blank decode to empty`() {
        assertTrue(PinCodec.decode(null).isEmpty)
        assertTrue(PinCodec.decode("   ").isEmpty)
    }

    @Test
    fun `duplicates in storage are collapsed on read`() {
        assertEquals(1, PinCodec.decode("com.a/.Main|0\ncom.a/.Main|0").size)
    }

    @Test
    fun `the in-memory store pins and unpins`() = runTest {
        val store = InMemoryPinStore()
        store.pin(chrome)
        store.pin(files)
        store.unpin(chrome)

        val pins = store.pins.first()
        assertEquals(listOf(files), pins.items)
        assertFalse(pins.contains(chrome))
    }
}

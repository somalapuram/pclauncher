package com.somalapuram.pclauncher.feature.shell.start

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PinResolutionTest {

    private val chrome = app("Chrome")
    private val files = app("Files")
    private val mail = app("Mail")
    private val all = listOf(chrome, files, mail)

    @Test
    fun `an empty store falls back to inventory order`() {
        // A first run must never show a bare bar.
        val resolved = PinResolution.resolve(all, emptyList(), fallbackLimit = 2)
        assertEquals(listOf(chrome, files), resolved)
    }

    @Test
    fun `pins render in stored order, not inventory order`() {
        // Order is what the user arranged; it is data, not a sort.
        val resolved = PinResolution.resolve(all, listOf(mail.id(), chrome.id()))
        assertEquals(listOf(mail, chrome), resolved)
    }

    @Test
    fun `a pin whose app is missing is skipped`() {
        val resolved = PinResolution.resolve(all, listOf(chrome.id(), "com.gone/.Main", files.id()))
        assertEquals(listOf(chrome, files), resolved)
    }

    @Test
    fun `skipping does not remove the pin from the caller's list`() {
        // A work profile switched off must not permanently lose its pins — resolution is a *view*,
        // so the input list is untouched and turning the profile back on restores them.
        val pins = listOf(chrome.id(), "com.gone/.Main")
        PinResolution.resolve(all, pins)
        assertEquals(2, pins.size)
    }

    @Test
    fun `when nothing resolves we fall back rather than show an empty dock`() {
        val resolved = PinResolution.resolve(all, listOf("com.gone/.Main"), fallbackLimit = 3)
        assertEquals(all, resolved)
    }

    @Test
    fun `an empty inventory yields an empty dock`() {
        assertTrue(PinResolution.resolve(emptyList(), listOf(chrome.id())).isEmpty())
    }

    @Test
    fun `isPinned matches on component`() {
        assertTrue(PinResolution.isPinned(listOf(chrome.id()), chrome))
        assertFalse(PinResolution.isPinned(listOf(chrome.id()), files))
    }

    @Test
    fun `nothing is pinned when the list is empty`() {
        assertFalse(PinResolution.isPinned(emptyList(), chrome))
    }
}

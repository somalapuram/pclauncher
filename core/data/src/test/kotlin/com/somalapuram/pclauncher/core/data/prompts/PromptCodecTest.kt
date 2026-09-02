package com.somalapuram.pclauncher.core.data.prompts

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How the record of "we asked" survives a restart (overlay-permission-ask.md).
 *
 * The failure this guards against is silent and annoying rather than loud: a decode that loses the
 * answer brings back a card the user already dismissed, on every launch.
 */
class PromptCodecTest {

    @Test
    fun `nothing asked encodes to nothing`() {
        assertEquals("", PromptCodec.encode(AskedPrompts()))
    }

    @Test
    fun `an asked prompt survives a round trip`() {
        val asked = AskedPrompts().plus(Prompt.OverlayPermission)
        assertEquals(asked, PromptCodec.decode(PromptCodec.encode(asked)))
    }

    @Test
    fun `every prompt survives a round trip together`() {
        val all = AskedPrompts(Prompt.entries.toSet())
        assertEquals(all, PromptCodec.decode(PromptCodec.encode(all)))
    }

    @Test
    fun `an absent value reads as nothing asked`() {
        assertEquals(AskedPrompts(), PromptCodec.decode(null))
    }

    @Test
    fun `a blank value reads as nothing asked`() {
        assertEquals(AskedPrompts(), PromptCodec.decode("   "))
    }

    /** A name written by a newer build, read by an older one. Dropping it beats crashing on it. */
    @Test
    fun `an unknown name is dropped and the rest kept`() {
        val decoded = PromptCodec.decode("OverlayPermission,SomethingFromTheFuture")
        assertTrue(decoded.contains(Prompt.OverlayPermission))
        assertEquals(1, decoded.shown.size)
    }

    @Test
    fun `whitespace around a name does not lose it`() {
        assertTrue(PromptCodec.decode(" OverlayPermission ").contains(Prompt.OverlayPermission))
    }

    /** Names, not ordinals: reordering the enum must not re-target an answer already stored. */
    @Test
    fun `the encoded form is the enum name`() {
        assertEquals(
            "OverlayPermission",
            PromptCodec.encode(AskedPrompts().plus(Prompt.OverlayPermission)),
        )
    }

    @Test
    fun `asking twice is asking once`() {
        val once = AskedPrompts().plus(Prompt.OverlayPermission)
        assertEquals(once, once.plus(Prompt.OverlayPermission))
    }

    @Test
    fun `nothing is asked to begin with`() {
        assertFalse(AskedPrompts().contains(Prompt.OverlayPermission))
    }

    @Test
    fun `the store remembers what it was told`() = runTest {
        val store = InMemoryPromptStore()
        store.markAsked(Prompt.OverlayPermission)
        assertTrue(store.asked.first().contains(Prompt.OverlayPermission))
    }
}

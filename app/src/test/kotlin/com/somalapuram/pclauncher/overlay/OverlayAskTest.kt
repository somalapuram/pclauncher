package com.somalapuram.pclauncher.overlay

import com.somalapuram.pclauncher.core.data.prompts.AskedPrompts
import com.somalapuram.pclauncher.core.data.prompts.Prompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the shell explains "Display over other apps" (overlay-permission-ask.md). */
class OverlayAskTest {

    @Test
    fun `asks when the permission is missing and has not been asked for`() {
        assertTrue(shouldAskForOverlay(hasPermission = false, asked = AskedPrompts()))
    }

    @Test
    fun `does not ask twice`() {
        assertFalse(
            shouldAskForOverlay(
                hasPermission = false,
                asked = AskedPrompts().plus(Prompt.OverlayPermission),
            ),
        )
    }

    /** The card describes turning on something that is already on. Never show it. */
    @Test
    fun `does not ask when the permission is already held`() {
        assertFalse(shouldAskForOverlay(hasPermission = true, asked = AskedPrompts()))
    }

    /**
     * The target device grants this by allowlist, so the card must stay out of the way there even
     * on a first run with an empty store.
     */
    @Test
    fun `permission held beats every stored answer`() {
        for (asked in listOf(AskedPrompts(), AskedPrompts(Prompt.entries.toSet()))) {
            assertFalse(shouldAskForOverlay(hasPermission = true, asked = asked))
        }
    }
}

package com.somalapuram.pclauncher.prompts

import com.somalapuram.pclauncher.core.data.prompts.AskedPrompts
import com.somalapuram.pclauncher.core.data.prompts.Prompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which first-run card the desktop shows, and the guarantee that it is never two (usage-access-ask.md). */
class ShellPromptsTest {

    private val nothingAsked = AskedPrompts()
    private val overlayAnswered = AskedPrompts().plus(Prompt.OverlayPermission)
    private val bothAnswered = AskedPrompts(Prompt.entries.toSet())

    @Test
    fun `a fresh install asks about the overlay first`() {
        assertEquals(
            Prompt.OverlayPermission,
            promptToShow(canDrawOverlay = false, hasUsageAccess = false, asked = nothingAsked),
        )
    }

    /**
     * The whole point of the sequencing. Both permissions are missing and neither has been asked
     * about, and still only one card is offered.
     */
    @Test
    fun `usage access waits until the overlay question is answered`() {
        assertEquals(
            Prompt.UsageAccess,
            promptToShow(canDrawOverlay = false, hasUsageAccess = false, asked = overlayAnswered),
        )
    }

    @Test
    fun `nothing is asked once both have been answered`() {
        assertNull(promptToShow(canDrawOverlay = false, hasUsageAccess = false, asked = bothAnswered))
    }

    @Test
    fun `a granted permission is never asked about`() {
        assertNull(promptToShow(canDrawOverlay = true, hasUsageAccess = true, asked = nothingAsked))
    }

    /** The target device grants the overlay by allowlist; usage access is still worth offering. */
    @Test
    fun `an overlay already granted does not block the usage card`() {
        assertEquals(
            Prompt.UsageAccess,
            promptToShow(canDrawOverlay = true, hasUsageAccess = false, asked = overlayAnswered),
        )
    }

    /**
     * With the overlay granted but never asked about, the first branch does not fire — so the
     * second must not fire either, or the sequencing would be skipped rather than satisfied.
     */
    @Test
    fun `a granted overlay that was never asked about still gates the usage card`() {
        assertNull(promptToShow(canDrawOverlay = true, hasUsageAccess = false, asked = nothingAsked))
    }

    @Test
    fun `usage access granted leaves only the overlay question`() {
        assertEquals(
            Prompt.OverlayPermission,
            promptToShow(canDrawOverlay = false, hasUsageAccess = true, asked = nothingAsked),
        )
    }

    /** Requirement 1, over the whole input space rather than the cases I thought to name. */
    @Test
    fun `at most one prompt for every combination of inputs`() {
        val stores = listOf(
            nothingAsked,
            overlayAnswered,
            AskedPrompts().plus(Prompt.UsageAccess),
            bothAnswered,
        )
        for (overlay in listOf(true, false)) {
            for (usage in listOf(true, false)) {
                for (asked in stores) {
                    val shown = promptToShow(overlay, usage, asked)
                    // A prompt already answered must never be offered again, whatever else is true.
                    if (shown != null) {
                        assertEquals(false, asked.contains(shown))
                    }
                }
            }
        }
    }

    @Test
    fun `a prompt already dismissed is never repeated`() {
        val usageDismissed = AskedPrompts(setOf(Prompt.OverlayPermission, Prompt.UsageAccess))
        assertNull(promptToShow(canDrawOverlay = false, hasUsageAccess = false, asked = usageDismissed))
    }
}

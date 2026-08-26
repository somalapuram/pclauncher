package com.somalapuram.pclauncher.feature.shell.start

import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * That the Start menu does not take focus into its search field on open.
 *
 * Focusing raised the on-screen keyboard over the bottom half of the display — which is where the
 * Start menu is — so opening the menu covered the menu. Detecting a hardware keyboard did not help:
 * the emulator reports one and the IME still appeared.
 */
class SearchFocusTest {

    @Test
    fun `opening the menu does not focus the field`() {
        assertFalse(
            "focusing on open summons a keyboard over the menu itself",
            shouldFocusSearchOnOpen(),
        )
    }
}

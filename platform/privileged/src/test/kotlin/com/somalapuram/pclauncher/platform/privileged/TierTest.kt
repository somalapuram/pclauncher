package com.somalapuram.pclauncher.platform.privileged

import org.junit.Assert.assertEquals
import org.junit.Test

class TierTest {

    @Test
    fun `no capabilities is basic`() {
        assertEquals(Tier.Basic, tierFor(PlatformCapabilities.None))
    }

    @Test
    fun `freeform feature alone is freeform`() {
        val capabilities = PlatformCapabilities.None.copy(hasFreeformFeature = true)
        assertEquals(Tier.Freeform, tierFor(capabilities))
    }

    @Test
    fun `desktop mode active alone is freeform`() {
        val capabilities = PlatformCapabilities.None.copy(desktopModeActive = true)
        assertEquals(Tier.Freeform, tierFor(capabilities))
    }

    @Test
    fun `a privileged provider wins even without freeform`() {
        // It can enable freeform itself, so absence of the feature must not demote it.
        val capabilities = PlatformCapabilities.None.copy(privilegedProviderConnected = true)
        assertEquals(Tier.Privileged, tierFor(capabilities))
    }

    @Test
    fun `everything available is privileged`() {
        val capabilities = PlatformCapabilities(
            hasFreeformFeature = true,
            desktopModeActive = true,
            privilegedProviderConnected = true,
        )
        assertEquals(Tier.Privileged, tierFor(capabilities))
    }

    @Test
    fun `the phase 1 detector reports nothing`() {
        assertEquals(PlatformCapabilities.None, UndetectedCapabilities().detect())
        assertEquals(Tier.Basic, tierFor(UndetectedCapabilities().detect()))
    }
}

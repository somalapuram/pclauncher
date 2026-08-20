package com.somalapuram.pclauncher

import com.somalapuram.pclauncher.di.StartupModule
import com.somalapuram.pclauncher.platform.privileged.PlatformCapabilities
import com.somalapuram.pclauncher.platform.privileged.Tier
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Hilt module's providers are plain functions, so the wiring can be checked without standing
 * up a component. That the graph *itself* resolves is proven at runtime by [HomeActivity] pulling
 * its entry point — and by the app booting to a desktop rather than safe mode.
 */
class StartupGraphTest {

    @Test
    fun `the phase 1 graph yields a basic tier`() {
        val detector = StartupModule.capabilityDetector()
        val source = StartupModule.desktopEnvironmentSource(detector)

        assertEquals(PlatformCapabilities.None, detector.detect())
        assertEquals(DesktopEnvironment(Tier.Basic), source.load())
    }

    @Test
    fun `the source reflects whatever the detector reports`() {
        val freeform = StartupModule.desktopEnvironmentSource {
            PlatformCapabilities.None.copy(hasFreeformFeature = true)
        }
        assertEquals(Tier.Freeform, freeform.load().tier)
    }
}

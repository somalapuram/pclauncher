package com.somalapuram.pclauncher.platform.privileged

/**
 * What the shell is allowed to do to windows on this device (SRS §5.3).
 *
 * Tier is *detected*, never assumed, and every window operation goes through a backend chosen by
 * it. Nothing outside this module may call a hidden API, write `Settings.Global`, or talk to a
 * privileged provider directly — that is the seam Stage B lands on (SRS §9).
 */
enum class Tier {
    /** No freeform. Apps launch fullscreen or split-screen; the rest of the shell still works. */
    Basic,

    /** Freeform is available: we choose launch bounds, the system decorates the window. */
    Freeform,

    /** Freeform plus live move/resize, the real window list, and thumbnails. */
    Privileged,
}

/**
 * The raw facts a tier is derived from. Kept separate from [Tier] so the *rule* can be tested
 * without a device and the *detection* can be replaced without touching the rule.
 */
data class PlatformCapabilities(
    /** `PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT` is declared. */
    val hasFreeformFeature: Boolean,
    /** The display is actually in a desktop/freeform windowing mode, not merely capable of it. */
    val desktopModeActive: Boolean,
    /** A privileged provider is connected — Shizuku in Stage A, the platform itself in Stage B. */
    val privilegedProviderConnected: Boolean,
) {
    companion object {
        /** What an ordinary phone reports, and what this slice assumes until phase 3. */
        val None = PlatformCapabilities(
            hasFreeformFeature = false,
            desktopModeActive = false,
            privilegedProviderConnected = false,
        )
    }
}

/**
 * The tier rule. Privilege wins outright: a connected provider can turn freeform on itself, so it
 * does not need freeform to already be available (SRS §5.3).
 */
fun tierFor(capabilities: PlatformCapabilities): Tier = when {
    capabilities.privilegedProviderConnected -> Tier.Privileged
    capabilities.hasFreeformFeature || capabilities.desktopModeActive -> Tier.Freeform
    else -> Tier.Basic
}

/** Supplies the facts [tierFor] runs on. */
fun interface CapabilityDetector {
    fun detect(): PlatformCapabilities
}

/**
 * Phase 1 detector: reports nothing, so the whole app runs at [Tier.Basic].
 *
 * Real detection — the freeform feature, the display's windowing mode, and provider connection —
 * arrives with `docs/requirements/windows/capability-tiers.md`. Stubbing it keeps this slice free
 * of platform calls (GATE 3) while the rule above is already real and tested.
 */
class UndetectedCapabilities : CapabilityDetector {
    override fun detect(): PlatformCapabilities = PlatformCapabilities.None
}

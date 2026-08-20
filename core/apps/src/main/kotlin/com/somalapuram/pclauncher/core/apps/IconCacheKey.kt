package com.somalapuram.pclauncher.core.apps

/**
 * What an icon is cached under.
 *
 * [versionCode] is the part that matters and the part that is easy to leave out: without it an app
 * update keeps serving the old icon until the cache is evicted, which looks like a bug in the
 * launcher rather than in the cache. [density] is here because the same entry has a different icon
 * per density bucket, and `pc_x86_64` is expected to change density when the Launcher3 workaround
 * is retired (SRS §4.2).
 */
data class IconCacheKey(
    val component: String,
    val userSerial: Long,
    val density: Int,
    val versionCode: Long,
    /**
     * Which treatment produced this bitmap (`design.icon.IconStyle.id`), and which revision of the
     * pipeline. Without the style, switching theme would serve dark-glass tiles on a light desktop;
     * without the version, changing the pipeline would leave old and new tiles side by side.
     */
    val styleId: String = "none",
    val treatmentVersion: Int = 0,
) {
    /** Stable, filesystem-safe name for the disk tier. */
    fun diskName(): String = buildString {
        append(component.replace('/', '.').replace(Regex("[^A-Za-z0-9._-]"), "_"))
        append('-').append(userSerial)
        append('-').append(density)
        append('-').append(versionCode)
        append('-').append(styleId)
        append("-v").append(treatmentVersion)
    }

    companion object {
        fun of(
            entry: AppEntry,
            userSerial: Long,
            density: Int,
            styleId: String = "none",
            treatmentVersion: Int = 0,
        ) = IconCacheKey(
            component = entry.key.component.flattenToShortString(),
            userSerial = userSerial,
            density = density,
            versionCode = entry.versionCode,
            styleId = styleId,
            treatmentVersion = treatmentVersion,
        )
    }
}

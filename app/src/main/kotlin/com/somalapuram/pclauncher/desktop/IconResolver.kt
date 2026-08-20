package com.somalapuram.pclauncher.desktop

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.UserManager
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.apps.IconCache
import com.somalapuram.pclauncher.core.apps.IconCacheKey
import com.somalapuram.pclauncher.core.design.icon.IconStyle
import com.somalapuram.pclauncher.di.iconStyleIdFor

/**
 * Resolves an [AppEntry] to its treated icon.
 *
 * Builds the cache key here rather than inside the cache so the *caller's* theme decides which
 * treatment is served — a cached bitmap can then never be handed back under a style it was not
 * baked for.
 */
class IconResolver(
    context: Context,
    private val cache: IconCache,
    private val darkTheme: Boolean,
    private val treatmentEnabled: Boolean = true,
    private val density: Int = context.resources.displayMetrics.densityDpi,
) {
    private val userManager = context.getSystemService(UserManager::class.java)

    fun iconFor(entry: AppEntry): Drawable? = runCatching {
        val serial = userManager?.getSerialNumberForUser(entry.key.user) ?: 0L
        cache.get(
            IconCacheKey.of(
                entry = entry,
                userSerial = serial,
                density = density,
                styleId = iconStyleIdFor(darkTheme, treatmentEnabled),
                treatmentVersion = IconStyle.TREATMENT_VERSION,
            ),
        )
    }.getOrNull()
}

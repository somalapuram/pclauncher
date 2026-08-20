package com.somalapuram.pclauncher.di

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.UserManager
import com.somalapuram.pclauncher.core.apps.FileIconDiskStore
import com.somalapuram.pclauncher.core.apps.IconCache
import com.somalapuram.pclauncher.core.apps.LauncherAppsIconLoader
import com.somalapuram.pclauncher.core.design.icon.IconStyle
import com.somalapuram.pclauncher.core.design.icon.iconStyleFor
import com.somalapuram.pclauncher.icons.TreatedIconLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Joins the app inventory's icon cache to the design system's icon treatment.
 *
 * Lives in `:app` because it is the only module that sees both `core:apps` (headless) and
 * `core:design` (Compose).
 */
@Module
@InstallIn(SingletonComponent::class)
object IconModule {

    @Provides
    @Singleton
    fun iconCache(@ApplicationContext context: Context): IconCache {
        val userManager = context.getSystemService(UserManager::class.java)

        val raw = LauncherAppsIconLoader(
            context = context,
            // Serial numbers survive a profile being removed and re-added; a raw user id does not,
            // which is why the cache keys on the serial.
            userFor = { serial -> runCatching { userManager.getUserForSerialNumber(serial) }.getOrNull() },
        )

        return IconCache(
            loader = TreatedIconLoader(
                delegate = raw,
                // The key already carries the style the caller asked for, so the loader does not
                // have to know about themes — and a cached bitmap can never be served under a
                // style it was not baked for.
                styleFor = { key -> styleById(key.styleId) },
            ),
            diskStore = FileIconDiskStore(File(context.cacheDir, "icons")),
            // Transparent rather than a generic app glyph: a placeholder that looks like an icon
            // reads as "this app has a blank icon" instead of "still loading".
            placeholder = ColorDrawable(0x00000000),
        )
    }

    private fun styleById(id: String): IconStyle? = when (id) {
        IconStyle.DarkGlass.id -> IconStyle.DarkGlass
        IconStyle.SoftClay.id -> IconStyle.SoftClay
        // "none" and anything unrecognised mean the treatment is off (requirement 10).
        else -> null
    }
}

/** Build the key a surface should ask for, given the current theme. */
fun iconStyleIdFor(darkTheme: Boolean, treatmentEnabled: Boolean): String =
    if (treatmentEnabled) iconStyleFor(darkTheme).id else "none"

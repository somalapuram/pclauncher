package com.somalapuram.pclauncher.core.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import java.io.File

/**
 * Loads an entry's icon from the framework.
 *
 * `getBadgedIcon` rather than `getIcon`: it applies the work-profile badge the system draws, so a
 * work app is recognisable as one without pclauncher inventing its own badge (requirement 3).
 */
class LauncherAppsIconLoader(
    private val context: Context,
    private val userFor: (Long) -> UserHandle?,
    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java),
) : IconLoader {

    override fun load(key: IconCacheKey): Drawable? = runCatching {
        val component = ComponentName.unflattenFromString(key.component) ?: return null
        val user = userFor(key.userSerial) ?: return null

        launcherApps
            .getActivityList(component.packageName, user)
            .firstOrNull { it.componentName == component }
            ?.getBadgedIcon(key.density)
    }.getOrNull()
}

/**
 * The disk tier: PNG under the app's cache directory.
 *
 * Rasterising discards the adaptive-icon layers, which is a real loss — but this tier exists to
 * make a *cold start* cheap, and the memory tier above it always holds the live drawable that came
 * from [LauncherAppsIconLoader]. So the flattened copy is only ever the first frame after a
 * restart, and the moment anything re-loads that entry the full drawable is back.
 *
 * Everything here is best-effort: cache files can be evicted by the system at any moment, so every
 * operation absorbs its own failure and the caller falls through to the loader.
 */
class FileIconDiskStore(
    private val directory: File,
    private val maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX,
) : IconDiskStore {

    override fun read(key: IconCacheKey): Drawable? = runCatching {
        val file = fileFor(key)
        if (!file.exists()) return null
        BitmapFactory.decodeFile(file.absolutePath)?.let { BitmapDrawable(null, it) }
    }.getOrNull()

    override fun write(key: IconCacheKey, drawable: Drawable) {
        runCatching {
            directory.mkdirs()
            val bitmap = drawable.toBitmap(maxDimensionPx)
            fileFor(key).outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }

    /**
     * Called when a package changes. Matches on the component prefix, so every density and version
     * of that component goes — including the entry for the version we just replaced.
     */
    override fun evictAllFor(component: String) {
        runCatching {
            val prefix = component.replace('/', '.').replace(Regex("[^A-Za-z0-9._-]"), "_")
            directory.listFiles()?.forEach { file ->
                if (file.name.startsWith(prefix)) file.delete()
            }
        }
    }

    private fun fileFor(key: IconCacheKey) = File(directory, "${key.diskName()}.png")

    companion object {
        const val DEFAULT_MAX_DIMENSION_PX = 192
    }
}

/** Rasterise, clamped — a drawable with no intrinsic size would otherwise be a 0×0 crash. */
internal fun Drawable.toBitmap(maxDimensionPx: Int): Bitmap {
    val width = intrinsicWidth.takeIf { it > 0 }?.coerceAtMost(maxDimensionPx) ?: maxDimensionPx
    val height = intrinsicHeight.takeIf { it > 0 }?.coerceAtMost(maxDimensionPx) ?: maxDimensionPx

    (this as? BitmapDrawable)?.bitmap?.let { existing ->
        if (existing.width == width && existing.height == height) return existing
    }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap
}

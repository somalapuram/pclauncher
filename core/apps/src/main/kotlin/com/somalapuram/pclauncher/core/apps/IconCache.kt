package com.somalapuram.pclauncher.core.apps

import android.graphics.drawable.Drawable

/** Loads an icon the expensive way. Called only on a cache miss. */
fun interface IconLoader {
    /** Returns null when the icon cannot be loaded — a missing app, a broken resource. */
    fun load(key: IconCacheKey): Drawable?
}

/** The disk tier. Separate from the memory tier so either can be faked in a test. */
interface IconDiskStore {
    fun read(key: IconCacheKey): Drawable?
    fun write(key: IconCacheKey, drawable: Drawable)
    fun evictAllFor(component: String)
}

/**
 * Memory over disk over loader.
 *
 * The contract that matters: **[get] never throws and never returns null.** A missing icon, a
 * broken resource, or an app uninstalled mid-load yields [placeholder]. Every caller is a UI
 * surface drawing a list, and a throw there takes down a shell surface for one bad icon.
 */
class IconCache(
    private val loader: IconLoader,
    private val diskStore: IconDiskStore?,
    private val placeholder: Drawable,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    // Access-ordered LRU: eldest is evicted once the map exceeds maxEntries.
    private val memory = object : LinkedHashMap<IconCacheKey, Drawable>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<IconCacheKey, Drawable>) =
            size > maxEntries
    }

    @Synchronized
    fun get(key: IconCacheKey): Drawable {
        memory[key]?.let { return it }

        val fromDisk = runCatching { diskStore?.read(key) }.getOrNull()
        if (fromDisk != null) {
            memory[key] = fromDisk
            return fromDisk
        }

        val loaded = runCatching { loader.load(key) }.getOrNull() ?: return placeholder

        memory[key] = loaded
        runCatching { diskStore?.write(key, loaded) }
        return loaded
    }

    /**
     * Drop everything cached for a component.
     *
     * Called when a package changes. The version is part of the key, so a stale icon could never be
     * *served* after an update — but without this the old entries linger in memory until evicted,
     * which is pure waste.
     */
    @Synchronized
    fun invalidate(component: String) {
        memory.keys.removeAll { it.component == component }
        runCatching { diskStore?.evictAllFor(component) }
    }

    @Synchronized
    fun sizeInMemory(): Int = memory.size

    companion object {
        const val DEFAULT_MAX_ENTRIES = 256
    }
}

package com.somalapuram.pclauncher.core.apps

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconCacheTest {

    private val placeholder: Drawable = ColorDrawable(0x00000000)

    private fun key(version: Long = 1L, density: Int = 240, component: String = "com.a/.Main") =
        IconCacheKey(component = component, userSerial = 0L, density = density, versionCode = version)

    @Test
    fun `an app update does not serve the old icon`() {
        // The whole reason versionCode is in the key. Without it the launcher shows a stale icon
        // after every update and looks broken.
        val v1 = key(version = 1)
        val v2 = key(version = 2)
        assertNotEquals(v1, v2)
        assertNotEquals(v1.diskName(), v2.diskName())
    }

    @Test
    fun `density is part of the key`() {
        // pc_x86_64 is expected to change density once the Launcher3 workaround is retired.
        assertNotEquals(key(density = 240), key(density = 160))
    }

    @Test
    fun `disk names are filesystem safe`() {
        val name = key(component = "com.example.app/.Main\$Inner").diskName()
        assertTrue(name.none { it in "/\\:*?\"<>|$" })
    }

    @Test
    fun `a miss loads once and is served from memory afterwards`() {
        var loads = 0
        val cache = IconCache(
            loader = { loads++; ColorDrawable(0xFF00FF00.toInt()) },
            diskStore = null,
            placeholder = placeholder,
        )

        val first = cache.get(key())
        val second = cache.get(key())

        assertEquals(1, loads)
        assertSame(first, second)
    }

    @Test
    fun `a failed load yields the placeholder rather than throwing`() {
        // Every caller is a UI surface drawing a list; a throw there takes down a shell surface
        // for one bad icon.
        val cache = IconCache(
            loader = { error("resource missing") },
            diskStore = null,
            placeholder = placeholder,
        )

        assertSame(placeholder, cache.get(key()))
    }

    @Test
    fun `a null load yields the placeholder`() {
        val cache = IconCache(loader = { null }, diskStore = null, placeholder = placeholder)
        assertSame(placeholder, cache.get(key()))
    }

    @Test
    fun `a broken disk store does not break the cache`() {
        var loads = 0
        val brokenDisk = object : IconDiskStore {
            override fun read(key: IconCacheKey): Drawable? = error("disk unreadable")
            override fun write(key: IconCacheKey, drawable: Drawable) = error("disk unwritable")
            override fun evictAllFor(component: String) = error("disk unwritable")
        }
        val cache = IconCache(
            loader = { loads++; ColorDrawable(0xFF0000FF.toInt()) },
            diskStore = brokenDisk,
            placeholder = placeholder,
        )

        val icon = cache.get(key())

        assertEquals(1, loads)
        assertNotEquals(placeholder, icon)
    }

    @Test
    fun `disk hits avoid the loader`() {
        var loads = 0
        val cached = ColorDrawable(0xFFFF0000.toInt())
        val disk = object : IconDiskStore {
            override fun read(key: IconCacheKey): Drawable = cached
            override fun write(key: IconCacheKey, drawable: Drawable) = Unit
            override fun evictAllFor(component: String) = Unit
        }
        val cache = IconCache({ loads++; ColorDrawable(0) }, disk, placeholder)

        assertSame(cached, cache.get(key()))
        assertEquals(0, loads)
    }

    @Test
    fun `memory is bounded`() {
        val cache = IconCache({ ColorDrawable(0) }, null, placeholder, maxEntries = 3)
        repeat(10) { cache.get(key(component = "com.a$it/.Main")) }
        assertEquals(3, cache.sizeInMemory())
    }

    @Test
    fun `invalidating a component drops only its entries`() {
        val cache = IconCache({ ColorDrawable(0) }, null, placeholder)
        cache.get(key(component = "com.a/.Main"))
        cache.get(key(component = "com.b/.Main"))

        cache.invalidate("com.a/.Main")

        assertEquals(1, cache.sizeInMemory())
    }
}

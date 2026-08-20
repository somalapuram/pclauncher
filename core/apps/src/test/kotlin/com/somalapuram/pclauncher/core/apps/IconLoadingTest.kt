package com.somalapuram.pclauncher.core.apps

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconLoadingTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun key(version: Long = 1L, component: String = "com.a/.Main") =
        IconCacheKey(component, userSerial = 0L, density = 240, versionCode = version)

    private fun icon() = BitmapDrawable(
        null,
        Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888),
    )

    @Test
    fun `a written icon reads back`() {
        val store = FileIconDiskStore(folder.newFolder("icons"))
        store.write(key(), icon())
        assertNotNull(store.read(key()))
    }

    @Test
    fun `reading an icon that was never written is null, not an error`() {
        val store = FileIconDiskStore(folder.newFolder("icons"))
        assertNull(store.read(key()))
    }

    @Test
    fun `a different version does not read the old file`() {
        val store = FileIconDiskStore(folder.newFolder("icons"))
        store.write(key(version = 1), icon())
        assertNull("an update must not be served the previous icon", store.read(key(version = 2)))
    }

    @Test
    fun `evicting a component removes every version and density of it`() {
        val dir = folder.newFolder("icons")
        val store = FileIconDiskStore(dir)
        store.write(key(version = 1), icon())
        store.write(key(version = 2), icon())
        store.write(key(component = "com.b/.Main"), icon())

        store.evictAllFor("com.a/.Main")

        assertNull(store.read(key(version = 1)))
        assertNull(store.read(key(version = 2)))
        assertNotNull("other components must survive", store.read(key(component = "com.b/.Main")))
    }

    @Test
    fun `writing to an unwritable directory does not throw`() {
        // Cache directories are evicted by the system at will; a caller drawing a list must not
        // care.
        val store = FileIconDiskStore(java.io.File("/proc/nonexistent/icons"))
        store.write(key(), icon())
        assertNull(store.read(key()))
    }

    @Test
    fun `a drawable with no intrinsic size still rasterises`() {
        // ColorDrawable reports -1 for both dimensions; a naive createBitmap would throw.
        val bitmap = ColorDrawable(0xFF00FF00.toInt()).toBitmap(maxDimensionPx = 64)
        assertEquals(64, bitmap.width)
        assertEquals(64, bitmap.height)
    }

    @Test
    fun `an oversized drawable is clamped`() {
        val huge = BitmapDrawable(null, Bitmap.createBitmap(2048, 2048, Bitmap.Config.ARGB_8888))
        val bitmap = huge.toBitmap(maxDimensionPx = 192)
        assertTrue(bitmap.width <= 192 && bitmap.height <= 192)
    }

    @Test
    fun `the cache prefers disk over the loader on a cold start`() {
        val dir = folder.newFolder("icons")
        val store = FileIconDiskStore(dir)
        store.write(key(), icon())

        var loads = 0
        val cache = IconCache({ loads++; icon() }, store, ColorDrawable(0))

        assertNotNull(cache.get(key()))
        assertEquals("a warm disk cache must not hit the loader", 0, loads)
    }

    @Test
    fun `invalidate clears both tiers`() {
        val dir = folder.newFolder("icons")
        val store = FileIconDiskStore(dir)
        val cache = IconCache({ icon() }, store, ColorDrawable(0))

        cache.get(key())
        cache.invalidate("com.a/.Main")

        assertEquals(0, cache.sizeInMemory())
        assertFalse(dir.listFiles().orEmpty().any { it.name.startsWith("com.a") })
    }
}

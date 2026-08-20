package com.somalapuram.pclauncher.feature.shell.interaction

import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.ui.geometry.Offset
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.apps.AppKey
import com.somalapuram.pclauncher.core.apps.ProfileKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DragStateTest {

    private val chrome = AppEntry(
        key = AppKey(ComponentName("com.chrome", "com.chrome.Main"), UserHandle.getUserHandleForUid(0)),
        label = "Chrome",
        packageName = "com.chrome",
        profile = ProfileKind.Personal,
    )

    private val barTop = 900f
    private val barBottom = 980f

    @Test
    fun `a fresh state is inactive`() {
        val drag = DragState()
        assertFalse(drag.isActive)
        assertNull(drag.end { false })
    }

    @Test
    fun `starting a drag makes it active and clears any target`() {
        val drag = DragState()
        drag.start(chrome, DragOrigin.Desktop, Offset(10f, 10f))
        assertTrue(drag.isActive)
        assertEquals(DropTarget.None, drag.target)
    }

    @Test
    fun `moving over the bar makes the dock the target`() {
        val drag = DragState()
        drag.start(chrome, DragOrigin.Desktop, Offset(10f, 10f))
        drag.moveTo(Offset(10f, 940f), barTop, barBottom)
        assertEquals(DropTarget.Dock, drag.target)
    }

    @Test
    fun `dropping an unpinned app on the bar pins it`() {
        val drag = DragState()
        drag.start(chrome, DragOrigin.Desktop, Offset(10f, 10f))
        drag.moveTo(Offset(10f, 940f), barTop, barBottom)

        val result = drag.end { false }
        assertEquals(chrome, result?.entry)
        assertTrue(result!!.pin)
    }

    @Test
    fun `dropping a pinned dock icon on the desktop unpins it`() {
        val drag = DragState()
        drag.start(chrome, DragOrigin.Dock, Offset(10f, 940f))
        drag.moveTo(Offset(400f, 300f), barTop, barBottom)

        val result = drag.end { true }
        assertFalse(result!!.pin)
    }

    @Test
    fun `dropping where it already belongs does nothing`() {
        // Dragging a pinned app back onto the bar is a no-op, not a second pin.
        val drag = DragState()
        drag.start(chrome, DragOrigin.Dock, Offset(10f, 940f))
        drag.moveTo(Offset(10f, 940f), barTop, barBottom)
        assertNull(drag.end { true })
    }

    @Test
    fun `releasing over nothing changes nothing and clears the drag`() {
        val drag = DragState()
        drag.start(chrome, DragOrigin.Desktop, Offset(10f, 10f))
        drag.moveTo(Offset(10f, 1050f), barTop, barBottom)

        assertNull(drag.end { false })
        assertFalse("the ghost must not linger", drag.isActive)
    }

    @Test
    fun `ending always deactivates, even on a no-op`() {
        val drag = DragState()
        drag.start(chrome, DragOrigin.Dock, Offset(10f, 940f))
        drag.moveTo(Offset(10f, 940f), barTop, barBottom)
        drag.end { true }
        assertFalse(drag.isActive)
    }

    @Test
    fun `cancelling clears the drag`() {
        val drag = DragState()
        drag.start(chrome, DragOrigin.Desktop, Offset(10f, 10f))
        drag.cancel()
        assertFalse(drag.isActive)
    }
}

package com.somalapuram.pclauncher.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * A Compose surface in a raw window, with the three owners attached.
 *
 * A `ComposeView` outside an activity crashes on attach unless the lifecycle, view-model store and
 * saved-state registry owners are all set on the view tree. That is boilerplate, and SRS §9 asks for
 * it to be written once rather than repeated per surface — which is also the only way to be sure
 * every surface got all three.
 *
 * Size and focusability are fixed for the life of the window, and deliberately so. Every
 * `updateViewLayout` that changes either one re-lays-out the window and re-creates its surface,
 * which the eye reads as the bar blinking. The chrome therefore uses one window per size it needs —
 * a bar-height one that never changes, and a full-screen menu one that is added and removed — so
 * nothing ever resizes underneath the user (overlay-service.md).
 *
 * Guarded on the way up and down: this hosts the shell, and a window that cannot be added must cost
 * the chrome rather than the process (GATE 4).
 */
class ComposeOverlayWindow(
    private val context: Context,
    private val gravity: Int = Gravity.BOTTOM or Gravity.START,
    private val spec: OverlayWindowSpec = BarWindowSpec,
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: ComposeView? = null

    /** Add the window. Returns false when it could not be shown, which is not a failure to crash on. */
    fun show(content: @Composable () -> Unit): Boolean {
        if (view != null) return true

        return runCatching {
            savedStateController.performAttach()
            savedStateController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED

            val composeView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(this@ComposeOverlayWindow)
                setViewTreeViewModelStoreOwner(this@ComposeOverlayWindow)
                setViewTreeSavedStateRegistryOwner(this@ComposeOverlayWindow)
                setContent { content() }
            }

            windowManager?.addView(composeView, layoutParams())
            view = composeView
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            true
        }.getOrDefault(false)
    }

    fun dismiss() {
        val current = view ?: return
        view = null
        runCatching { windowManager?.removeView(current) }
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        spec.height,
        overlayType(),
        windowFlags(spec.focusable),
        PixelFormat.TRANSLUCENT,
    ).apply {
        this.gravity = this@ComposeOverlayWindow.gravity
        // Draw under the system bars so the shell can inset itself the way the activity does.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
}

/** The only overlay type a non-privileged app may use since API 26. */
internal fun overlayType(): Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

/**
 * Window flags for the chrome.
 *
 * `NOT_FOCUSABLE` also implies not touch-modal, so touches outside the window still reach whatever
 * is behind — which is what makes a taskbar sit over an app without swallowing its input. Dropping
 * the flag to accept keys necessarily gives that up, so it is only dropped while a menu is open.
 */
internal fun windowFlags(focusable: Boolean): Int {
    val base = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
    return if (focusable) {
        base or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    } else {
        base or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    }
}

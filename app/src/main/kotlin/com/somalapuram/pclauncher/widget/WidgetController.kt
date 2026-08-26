package com.somalapuram.pclauncher.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import com.somalapuram.pclauncher.feature.shell.widget.BindOutcome
import com.somalapuram.pclauncher.core.data.layout.ResizePermission
import com.somalapuram.pclauncher.feature.shell.widget.bindOutcomeFor
import com.somalapuram.pclauncher.feature.shell.widget.cellsFor
import com.somalapuram.pclauncher.feature.shell.widget.shouldReleaseId

/**
 * Hosting other apps' views.
 *
 * The protocol is specific and order-dependent: allocate an id, bind it to a provider, run the
 * provider's configure activity if it has one, and only then ask the host for a view. Get the order
 * wrong and you get a blank rectangle with no error — which is why every step here is explicit
 * rather than folded together.
 *
 * Ids are a resource. An id allocated and never bound stays allocated in the host forever, so every
 * failure path releases its own (widget-host.md requirement 5).
 */
class WidgetController(
    private val context: Context,
    hostId: Int = HOST_ID,
) {
    private val host = AppWidgetHost(context, hostId)
    private val manager = AppWidgetManager.getInstance(context)

    fun startListening() = runCatching { host.startListening() }
    fun stopListening() = runCatching { host.stopListening() }

    /** Everything installed that can be placed. */
    fun availableProviders(): List<AppWidgetProviderInfo> =
        runCatching { manager.installedProviders }.getOrDefault(emptyList())

    fun allocateId(): Int = runCatching { host.allocateAppWidgetId() }.getOrDefault(INVALID_ID)

    fun releaseId(id: Int) {
        if (id == INVALID_ID) return
        views.remove(id)
        permissions.remove(id)
        runCatching { host.deleteAppWidgetId(id) }
    }

    /**
     * Try to bind without troubling the user, and say what to do if that is not possible.
     *
     * `bindAppWidgetIdIfAllowed` succeeds only for a launcher already holding `BIND_APPWIDGET`.
     * During Stage A pclauncher usually is not the default home, so the consent path is the common
     * one — not an edge case.
     */
    fun bind(id: Int, provider: AppWidgetProviderInfo): BindOutcome {
        val allowed = runCatching {
            manager.bindAppWidgetIdIfAllowed(id, provider.provider)
        }.getOrDefault(false)

        return bindOutcomeFor(allowedDirectly = allowed, canAskUser = true)
    }

    /** The system's own bind dialog, for when we may not bind silently. */
    fun bindConsentIntent(id: Int, provider: AppWidgetProviderInfo): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
        }

    /**
     * The provider's own configure screen, if it has one.
     *
     * A provider that declares one and never gets it renders nothing, which looks like our bug
     * rather than an unfinished setup.
     */
    fun configureIntent(id: Int, provider: AppWidgetProviderInfo): Intent? {
        val component = provider.configure ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            this.component = component
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
    }

    fun providerFor(id: Int): AppWidgetProviderInfo? =
        runCatching { manager.getAppWidgetInfo(id) }.getOrNull()

    /**
     * What the provider allows, translated into the shell's own terms.
     *
     * `minResizeWidth` is the floor *when resized*, which is often smaller than the widget's
     * default size — using `minWidth` here would refuse shrinks the provider explicitly permits.
     */
    fun resizePermission(id: Int, cellSizeDp: Int): ResizePermission {
        permissions[id]?.let { return it }

        val info = providerFor(id) ?: return ResizePermission.None
        val mode = info.resizeMode
        return ResizePermission(
            horizontal = mode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0,
            vertical = mode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0,
            minColumns = cellsFor(info.minResizeWidth.takeIf { it > 0 } ?: info.minWidth, cellSizeDp),
            minRows = cellsFor(info.minResizeHeight.takeIf { it > 0 } ?: info.minHeight, cellSizeDp),
        ).also { permissions[id] = it }
    }

    /**
     * Tell a widget how big it now is.
     *
     * Without this the provider keeps rendering for its old size and the resize is just a stretched
     * picture of the widget it used to be (widget-resize.md requirement 7).
     */
    fun applySize(id: Int, widthDp: Int, heightDp: Int) {
        val view = views[id] ?: return
        runCatching {
            view.updateAppWidgetSize(
                android.os.Bundle(),
                listOf(android.util.SizeF(widthDp.toFloat(), heightDp.toFloat())),
            )
        }
    }

    private val views = mutableMapOf<Int, android.appwidget.AppWidgetHostView>()

    /**
     * Cached because this is read during composition, and `getAppWidgetInfo` is a binder call — one
     * per widget per frame is a real cost on a device that already composites on the CPU. A
     * provider's declared resize policy does not change while it is installed.
     */
    private val permissions = mutableMapOf<Int, ResizePermission>()

    /**
     * The hosted view, created once per id.
     *
     * Cached deliberately: `createView` allocates a real `View` and registers it with the host, so
     * calling it again on every recomposition churns views and detaches the one already on screen.
     * A composable asking "what view is this widget?" must get the same answer every time.
     *
     * Null rather than a throw when a provider fails to inflate: one misbehaving widget must leave
     * a placeholder, not take the desktop down (GATE 4).
     */
    fun createView(id: Int): android.appwidget.AppWidgetHostView? {
        views[id]?.let { return it }
        val info = providerFor(id) ?: return null
        val view = runCatching { host.createView(context, id, info) }.getOrNull() ?: return null
        view.setAppWidget(id, info)
        fillItsCell(view)
        views[id] = view
        return view
    }

    fun forgetView(id: Int) {
        views.remove(id)
    }

    /**
     * Remove a widget for good.
     *
     * The same operation as abandoning a half-bound id: the view, the cached policy and the
     * framework binding all go. Dropping only the placement would leak the id — the host keeps the
     * binding alive and the provider keeps being asked to update a widget nobody can see.
     */
    fun removeWidget(id: Int) = releaseId(id)

    /** Release an id unless the whole flow completed. */
    fun releaseIfIncomplete(id: Int, bound: Boolean, configured: Boolean, cancelled: Boolean) {
        if (shouldReleaseId(bound, configured, cancelled)) releaseId(id)
    }

    companion object {
        /** Stable across restarts — the host reuses previously bound ids under the same host id. */
        const val HOST_ID = 0x7063
        const val INVALID_ID = -1

        const val REQUEST_BIND = 1001
        const val REQUEST_CONFIGURE = 1002
    }
}

/** Result plumbing an Activity has to forward for the bind and configure dialogs. */
fun Activity.widgetIdFrom(data: Intent?): Int =
    data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, WidgetController.INVALID_ID)
        ?: WidgetController.INVALID_ID

/**
 * Take the host's default padding off a widget view.
 *
 * `AppWidgetHostView` insets what it hosts by `getDefaultPaddingForWidget` — spacing meant for a
 * home screen that does none of its own, so widgets built for older platforms do not touch. Ours is
 * a grid: the cells *are* the spacing, so the host's padding is applied on top of a layout that
 * already accounts for it and the widget ends up centred inside its own cells rather than filling
 * them (widget-alignment.md).
 *
 * The widget's own internal padding is untouched — that belongs to its author.
 */
fun fillItsCell(view: AppWidgetHostView) {
    runCatching { view.setPadding(0, 0, 0, 0) }
}

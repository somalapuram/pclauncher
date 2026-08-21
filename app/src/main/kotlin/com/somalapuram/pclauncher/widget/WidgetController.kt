package com.somalapuram.pclauncher.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import com.somalapuram.pclauncher.feature.shell.widget.BindOutcome
import com.somalapuram.pclauncher.feature.shell.widget.bindOutcomeFor
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

    private val views = mutableMapOf<Int, android.appwidget.AppWidgetHostView>()

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
        views[id] = view
        return view
    }

    fun forgetView(id: Int) {
        views.remove(id)
    }

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

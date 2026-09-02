package com.somalapuram.pclauncher.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.view.Gravity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.somalapuram.pclauncher.di.HomeEntryPoint
import com.somalapuram.pclauncher.core.design.PcTheme
import com.somalapuram.pclauncher.core.design.chromeIsDark
import com.somalapuram.pclauncher.desktop.IconResolver
import com.somalapuram.pclauncher.feature.shell.tray.SystemTrayActions
import com.somalapuram.pclauncher.feature.shell.tray.SystemTraySource
import com.somalapuram.pclauncher.wallpaper.rememberWallpaperTone
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the shell's chrome window.
 *
 * A service rather than the home activity, because an overlay owned by an activity dies with it and
 * the home activity is restarted routinely — the bar has to outlive it (overlay-service.md).
 *
 * Everything here is guarded. The service exists to put the chrome *above* app windows; failing to
 * do that must leave the chrome in the home activity, never leave the device with no bar (GATE 4).
 */
class ShellOverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob())

    /** Window work has to happen on the main thread; the shell's own scope must not. */
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var barWindow: ComposeOverlayWindow? = null
    private var menuWindow: ComposeOverlayWindow? = null
    private var shell: com.somalapuram.pclauncher.desktop.ShellController? = null
    private var traySource: SystemTraySource? = null

    /**
     * Hoisted out of the composition because two windows read it: the bar draws the Start button
     * pressed, and the service adds and removes the menu window from it.
     */
    private val startOpen = MutableStateFlow(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()

        // Checked here as well as by the caller: the permission can be revoked while the service
        // is alive, and adding the window would then throw.
        if (!canDrawOverlay(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (barWindow == null) showChrome()

        // STICKY, because requirement 7 is that the window comes back after the service is killed.
        return START_STICKY
    }

    private fun showChrome() {
        val controller = runCatching { buildController() }.getOrNull()?.also { it.start() }
        shell = controller
        val tray = SystemTraySource(applicationContext).also { traySource = it }
        val trayActions = SystemTrayActions(applicationContext)
        val cache = runCatching { entryPoint().iconCache() }.getOrNull()

        val bar = ComposeOverlayWindow(this, spec = BarWindowSpec)
        val shown = bar.show {
            Chrome(cache) { iconFor ->
                val trayState by remember { tray.trayState() }
                    .collectAsState(initial = com.somalapuram.pclauncher.feature.shell.tray.TrayState())
                val open by startOpen.collectAsState()
                OverlayBar(
                    shell = controller,
                    iconFor = iconFor,
                    tray = trayState,
                    onTrayAction = { trayActions.perform(it) },
                    isStartOpen = open,
                    onStartToggle = { startOpen.value = !startOpen.value },
                )
            }
        }

        barWindow = if (shown) bar else null
        if (!shown) {
            stopSelf()
            return
        }
        chromeUp.value = true

        // The menu lives in a window of its own, added when it opens and removed when it closes.
        // Toggling one window's size and focus instead would blink the bar on every click.
        mainScope.launch {
            startOpen.collect { open -> if (open) showMenu(cache) else hideMenu() }
        }
    }

    private fun showMenu(cache: com.somalapuram.pclauncher.core.apps.IconCache?) {
        if (menuWindow != null) return
        // The Recent row is about to be drawn, and usage access may have been granted or revoked
        // since it was last read — there is no callback for that, and this service has no resume to
        // hang one on. Re-reading as the menu opens is both the cheapest and the latest possible
        // moment (usage-access-ask.md requirement 6).
        shell?.refreshUsage()
        // Focusable only here, and only while it is up: a focusable window at rest would take
        // every keystroke from the app the user is actually typing into.
        val menu = ComposeOverlayWindow(
            context = this,
            gravity = Gravity.BOTTOM or Gravity.START,
            spec = MenuWindowSpec,
        )
        val shown = menu.show {
            Chrome(cache) { iconFor ->
                OverlayStartMenu(
                    shell = shell,
                    iconFor = iconFor,
                    onPowerAction = {
                        com.somalapuram.pclauncher.power.performPowerAction(applicationContext, it)
                    },
                    onDismiss = { startOpen.value = false },
                )
            }
        }
        // A menu that could not be shown must not leave the Start button stuck pressed.
        if (shown) menuWindow = menu else startOpen.value = false
    }

    private fun hideMenu() {
        menuWindow?.dismiss()
        menuWindow = null
    }

    /**
     * The theme and icon resolver both windows share.
     *
     * Wallpaper-driven, like the activity's: the chrome sits on the wallpaper, so what it needs to
     * know is what is behind it.
     */
    @Composable
    private fun Chrome(
        cache: com.somalapuram.pclauncher.core.apps.IconCache?,
        content: @Composable (
            iconFor: (com.somalapuram.pclauncher.core.apps.AppEntry) -> android.graphics.drawable.Drawable?,
        ) -> Unit,
    ) {
        val dark = chromeIsDark(
            tone = rememberWallpaperTone(),
            systemDark = isSystemInDarkTheme(),
        )
        PcTheme(darkTheme = dark) {
            val iconFor = remember(dark, cache) {
                { entry: com.somalapuram.pclauncher.core.apps.AppEntry ->
                    cache?.let {
                        IconResolver(applicationContext, it, darkTheme = dark).iconFor(entry)
                    }
                }
            }
            content(iconFor)
        }
    }

    override fun onDestroy() {
        hideMenu()
        barWindow?.dismiss()
        barWindow = null
        chromeUp.value = false
        shell?.stop()
        mainScope.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildController(): com.somalapuram.pclauncher.desktop.ShellController {
        val entry = entryPoint()
        return com.somalapuram.pclauncher.desktop.ShellController(
            repository = entry.appInventoryRepository(),
            pinStore = entry.pinStore(),
            layoutStore = entry.desktopLayoutStore(),
            scope = scope,
            userSerial = 0L,
            usageStore = entry.usageStore(),
            usageSignals = entry.usageSignals(),
        )
    }

    private fun startInForeground() {
        runCatching {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    "Shell",
                    // Lowest that still satisfies a foreground service: this notification exists
                    // because the platform requires one, not because the user needs telling.
                    NotificationManager.IMPORTANCE_MIN,
                ),
            )
            val notification: Notification = Notification.Builder(this, CHANNEL)
                .setContentTitle("pclauncher shell")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build()
            startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        }
    }

    private fun entryPoint(): HomeEntryPoint =
        EntryPointAccessors.fromApplication(applicationContext, HomeEntryPoint::class.java)

    companion object {
        private const val CHANNEL = "pclauncher-shell"
        private const val ID = 1

        private val chromeUp = MutableStateFlow(false)

        /**
         * Whether the bar is actually on screen.
         *
         * The home activity hides its own bar only once this is true. Hiding it at the moment the
         * service is *asked* to start leaves a gap with no bar at all, which shows as a blink on
         * every launch.
         */
        val isChromeUp: StateFlow<Boolean> = chromeUp.asStateFlow()

        /** Start the chrome window, if the user has allowed it. A refusal is simply not starting. */
        fun start(context: Context) {
            if (!canDrawOverlay(context)) return
            runCatching {
                context.startForegroundService(Intent(context, ShellOverlayService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, ShellOverlayService::class.java)) }
        }
    }
}

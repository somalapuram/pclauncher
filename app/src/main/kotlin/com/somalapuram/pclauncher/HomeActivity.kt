package com.somalapuram.pclauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.os.UserManager
import androidx.lifecycle.lifecycleScope
import com.somalapuram.pclauncher.desktop.AppLauncher
import com.somalapuram.pclauncher.desktop.ShellController
import android.graphics.drawable.Drawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.somalapuram.pclauncher.core.design.chromeIsDark
import com.somalapuram.pclauncher.overlay.ChromeHost
import com.somalapuram.pclauncher.overlay.ShellOverlayService
import com.somalapuram.pclauncher.overlay.canDrawOverlay
import com.somalapuram.pclauncher.overlay.OverlayPermissionCard
import com.somalapuram.pclauncher.overlay.chromeHostFor
import com.somalapuram.pclauncher.overlay.overlayPermissionIntent
import com.somalapuram.pclauncher.overlay.shouldAskForOverlay
import com.somalapuram.pclauncher.core.data.prompts.AskedPrompts
import com.somalapuram.pclauncher.core.data.prompts.Prompt
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import com.somalapuram.pclauncher.wallpaper.rememberWallpaperTone
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.design.PcTheme
import android.content.Intent
import androidx.compose.runtime.collectAsState
import com.somalapuram.pclauncher.desktop.IconResolver
import com.somalapuram.pclauncher.feature.shell.tray.SystemTraySource
import com.somalapuram.pclauncher.feature.shell.tray.TrayState
import com.somalapuram.pclauncher.feature.shell.widget.BindOutcome
import com.somalapuram.pclauncher.feature.shell.widget.WidgetChoice
import com.somalapuram.pclauncher.feature.shell.widget.cellsFor
import com.somalapuram.pclauncher.widget.WidgetController
import com.somalapuram.pclauncher.di.HomeEntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * The desktop. This is the home screen (`CATEGORY_HOME`), so it renders behind everything else and
 * must never fail to come up.
 *
 * Note what is *not* here: no `@AndroidEntryPoint`, and no `@Inject lateinit`. Field injection
 * happens before `onCreate` runs, so a broken graph would crash the home screen before any guard
 * could catch it. Resolving the entry point inside [runCatching] instead means a dependency failure
 * lands on the fallback desktop rather than on the user (GATE 4, SRS §12).
 */
class HomeActivity : ComponentActivity() {

    private var shell: ShellController? = null

    /** Built lazily and guarded: no widget host must ever cost the user their desktop (GATE 4). */
    private val widgets: WidgetController? by lazy {
        runCatching { WidgetController(applicationContext) }.getOrNull()
    }

    /** The id being bound right now. Held so every failure path can release it. */
    private var pendingWidgetId: Int = WidgetController.INVALID_ID
    private var pendingProvider: android.appwidget.AppWidgetProviderInfo? = null

    /** Chosen against the *effective* layout, so a widget never lands on top of an icon. */
    private var pendingCell: com.somalapuram.pclauncher.core.data.layout.DesktopCell? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val outcome = resolveStartup(runCatching { loadEnvironment() })
        // Built inside the same guard as everything else on this path: no controller means an
        // empty dock and desktop, not a dead home screen (GATE 4).
        shell = runCatching { buildController() }.getOrNull()?.also { it.start() }

        // Only paid for when we are actually in safe mode, and guarded again on the way in:
        // the listing is a best effort, not a second thing that can strand the user.
        val safeModeApps = if (outcome is StartupOutcome.Fallback) {
            runCatching { entryPoint().safeModeApps().list() }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        // The inventory now belongs to the ViewModel, which also owns pin state so the dock, the
        // Start menu and the desktop cannot disagree about what is pinned.
        // Recording hangs off the launcher rather than each surface, so the desktop, the dock and
        // the Start menu cannot disagree about what was opened (recent-apps.md requirement 4).
        val launcher = AppLauncher(applicationContext) { shell?.recordLaunch(it) }
        widgets?.startListening()

        val traySource = SystemTraySource(applicationContext)
        // The activity, not the application context: `startActivity` from an application context
        // needs NEW_TASK, and these launches want to sit beside the desktop rather than inside it.
        val trayActions = com.somalapuram.pclauncher.feature.shell.tray.SystemTrayActions(this)

        setContent {
            // The wallpaper decides, not the system theme: the shell sits directly on the
            // wallpaper, so what it needs to know is what is behind it. A light theme over a dark
            // wallpaper is an ordinary configuration and is exactly where the old answer looked
            // worst (wallpaper-chrome.md). Falls through to the system setting when the wallpaper
            // says nothing.

            // Re-read on every resume, never cached: the user may have granted or revoked
            // "Display over other apps" in Settings while the shell was in the background — which
            // is exactly what happens when they come back from the card below
            // (overlay-permission-ask.md).
            var canOverlay by remember { mutableStateOf(canDrawOverlay(this@HomeActivity)) }
            LifecycleResumeEffect(Unit) {
                canOverlay = canDrawOverlay(this@HomeActivity)
                onPauseOrDispose {}
            }
            // Keyed on the permission, so a grant takes effect on return rather than on next boot.
            LaunchedEffect(canOverlay) {
                if (canOverlay) ShellOverlayService.start(this@HomeActivity)
            }
            // Started above, but reported by the service: hiding this activity's bar the moment
            // the service is *asked* to start leaves a gap with no bar at all while the window is
            // being added, which shows as a blink on every launch (overlay-service.md).
            val overlayRunning by ShellOverlayService.isChromeUp.collectAsState()

            // No store means no memory of an answer, so asking would nag on every launch.
            val promptStore = remember { runCatching { entryPoint().promptStore() }.getOrNull() }
            val asked by (promptStore?.asked ?: EmptyAsked)
                .collectAsState(initial = AskedPrompts(Prompt.entries.toSet()))
            val prompts = rememberCoroutineScope()

            val dark = chromeIsDark(
                tone = rememberWallpaperTone(),
                systemDark = isSystemInDarkTheme(),
            )
            val tray by remember { traySource.trayState() }
                .collectAsState(initial = TrayState())
            // Rebuilt when the theme flips, so the dock reloads icons baked for the new palette
            // rather than showing dark-glass tiles on a light desktop.
            val iconFor = remember(dark) { iconResolverFor(dark) }

            PcTheme(darkTheme = dark) {
                HomeScreen(
                    inventory = shell?.inventory ?: EmptyInventory,
                    recentApps = shell?.recent ?: EmptyRecent,
                    pins = shell?.pins ?: EmptyPins,
                    desktopLayout = shell?.layout ?: EmptyLayout,
                    iconFor = iconFor,
                    onLaunchApp = { launcher.launch(it) },
                    onTogglePin = { entry -> shell?.togglePin(entry) },
                    onPlace = { entry, cell -> shell?.place(entry, cell) },
                    tray = tray,
                    onTrayAction = { action -> trayActions.perform(action) },
                    onChangeWallpaper = { openWallpaperPicker() },
                    widgetChoices = { widgetChoices() },
                    onPickWidget = { choice, cell -> beginAddWidget(choice.id, cell) },
                    widgetViewFor = { id -> widgets?.createView(id) },
                    resizePermissionFor = { id ->
                        widgets?.resizePermission(id, DesktopCellDp)
                            ?: com.somalapuram.pclauncher.core.data.layout.ResizePermission.None
                    },
                    onMoveWidget = { id, cell -> shell?.moveWidget(id, cell) },
                    onRemoveWidget = { id ->
                        // Both halves, in this order: the placement first so the view leaves the
                        // desktop, then the id so the framework drops the binding behind it.
                        shell?.removeWidget(id)
                        widgets?.removeWidget(id)
                    },
                    onResizeStart = { id -> shell?.beginResize(id) },
                    onResizeEnd = { shell?.endResize() },
                    onResizeDrag = { id, edge, pixels, cellSize, columns, rows ->
                        val permission = widgets?.resizePermission(id, DesktopCellDp)
                            ?: com.somalapuram.pclauncher.core.data.layout.ResizePermission.None
                        shell?.resizeWidget(
                            widgetId = id,
                            edge = edge,
                            pixels = pixels,
                            cellSize = cellSize,
                            permission = permission,
                            columnsAvailable = columns,
                            rowsAvailable = rows,
                        ) { _, _ ->
                            // Nothing to do here. The size is reported from the placement itself
                            // (`onReportWidgetSize`), which covers this resize *and* the two cases
                            // this call site could never see: a widget merely placed, and one
                            // re-created after a restart (widget-sizing.md).
                        }
                    },
                    onReportWidgetSize = { id, widthDp, heightDp ->
                        widgets?.applySize(id, widthDp, heightDp)
                    },
                    chromeInOverlay = chromeHostFor(
                        hasPermission = canOverlay,
                        overlayRunning = overlayRunning,
                    ) == ChromeHost.Overlay,
                    deviceName = com.somalapuram.pclauncher.feature.shell.start.displayableDeviceName(
                        // The name the user actually set — what Bluetooth and Nearby show. The
                        // model only stands in when nothing has been set.
                        deviceName = runCatching {
                            android.provider.Settings.Global.getString(
                                contentResolver,
                                android.provider.Settings.Global.DEVICE_NAME,
                            )
                        }.getOrNull(),
                        model = android.os.Build.MODEL,
                    ),
                    powerPrivileges = com.somalapuram.pclauncher.power.powerPrivilegesOf(this),
                    onPowerAction = { action ->
                        com.somalapuram.pclauncher.power.performPowerAction(this, action)
                    },
                    outcome = outcome,
                    isDefaultHome = HomeRole.isDefault(this),
                    onSetDefaultHome = { startActivity(HomeRole.requestIntent(this)) },
                    onRetry = { recreate() },
                    safeModeApps = safeModeApps,
                )

                // After the desktop, so the explanation lands on a shell the user can already see
                // — the card describes what happens to that bar, and it reads as an answer to
                // something in front of them rather than a gate before it.
                if (shouldAskForOverlay(canOverlay, asked)) {
                    OverlayPermissionCard(
                        onAllow = {
                            prompts.launch { promptStore?.markAsked(Prompt.OverlayPermission) }
                            // A missing Settings screen costs the permission, not the desktop.
                            runCatching {
                                startActivity(overlayPermissionIntent(this@HomeActivity))
                            }
                        },
                        // Dismissing *is* the answer, and it is remembered. The consequence was
                        // stated in the card, so it has been said once and is not said again.
                        onNotNow = {
                            prompts.launch { promptStore?.markAsked(Prompt.OverlayPermission) }
                        },
                    )
                }
            }
        }
    }

    /**
     * Hand wallpaper selection to the system.
     *
     * A launcher has no business implementing a wallpaper picker: the user already has one, it
     * knows about live wallpapers and crops, and ours would be a worse copy (icon-grid.md
     * requirement 7).
     */
    private fun openWallpaperPicker() {
        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
        runCatching { startActivity(Intent.createChooser(intent, "Change wallpaper")) }
    }

    private fun buildController(): ShellController {
        val entryPoint = entryPoint()
        val userSerial = runCatching {
            getSystemService(UserManager::class.java)
                .getSerialNumberForUser(android.os.Process.myUserHandle())
        }.getOrDefault(0L)

        return ShellController(
            repository = entryPoint.appInventoryRepository(),
            pinStore = entryPoint.pinStore(),
            layoutStore = entryPoint.desktopLayoutStore(),
            scope = lifecycleScope,
            userSerial = userSerial,
            usageStore = entryPoint.usageStore(),
            usageSignals = entryPoint.usageSignals(),
        )
    }

    override fun onDestroy() {
        shell?.stop()
        widgets?.stopListening()
        super.onDestroy()
    }

    /** Providers as the picker shows them, sized in desktop cells. */
    fun widgetChoices(): List<WidgetChoice> {
        val controller = widgets ?: return emptyList()
        val cell = resources.displayMetrics.let { 96 }
        return controller.availableProviders().map { info ->
            WidgetChoice(
                id = info.provider.flattenToShortString(),
                label = runCatching { info.loadLabel(packageManager) }.getOrNull() ?: info.provider.className,
                preview = runCatching { info.loadPreviewImage(this, 0) }.getOrNull()
                    ?: runCatching { info.loadIcon(this, 0) }.getOrNull(),
                columns = cellsFor(info.minWidth, cell),
                rows = cellsFor(info.minHeight, cell),
            )
        }
    }

    /**
     * Start adding a widget: allocate, then try to bind without troubling the user.
     *
     * The order is the protocol — allocate, bind, configure, only then show. Folding steps together
     * yields a blank rectangle and no error.
     */
    fun beginAddWidget(choiceId: String, cell: com.somalapuram.pclauncher.core.data.layout.DesktopCell) {
        val controller = widgets ?: return
        val provider = controller.availableProviders()
            .firstOrNull { it.provider.flattenToShortString() == choiceId } ?: return

        val id = controller.allocateId()
        if (id == WidgetController.INVALID_ID) return

        pendingWidgetId = id
        pendingProvider = provider
        pendingCell = cell

        when (controller.bind(id, provider)) {
            BindOutcome.Bound -> continueAfterBind()
            BindOutcome.NeedsUserConsent -> runCatching {
                startActivityForResult(
                    controller.bindConsentIntent(id, provider),
                    WidgetController.REQUEST_BIND,
                )
            }.onFailure { abandonPendingWidget() }

            BindOutcome.Failed -> abandonPendingWidget()
        }
    }

    /** Bound. Configure if the provider asks for it, otherwise place it. */
    private fun continueAfterBind() {
        val controller = widgets ?: return abandonPendingWidget()
        val id = pendingWidgetId
        val provider = pendingProvider ?: return abandonPendingWidget()

        val configure = controller.configureIntent(id, provider)
        if (configure == null) {
            placePendingWidget()
        } else {
            runCatching { startActivityForResult(configure, WidgetController.REQUEST_CONFIGURE) }
                .onFailure { abandonPendingWidget() }
        }
    }

    private fun placePendingWidget() {
        val cell = pendingCell
        // The provider's own minimum, so a widget appears at the size it asked for rather than
        // squeezed into one cell and left for the user to fix.
        val info = widgets?.providerFor(pendingWidgetId)
        val span = com.somalapuram.pclauncher.core.data.layout.DesktopSpan(
            columns = com.somalapuram.pclauncher.feature.shell.widget.cellsFor(
                info?.minWidth ?: 0, DesktopCellDp,
            ),
            rows = com.somalapuram.pclauncher.feature.shell.widget.cellsFor(
                info?.minHeight ?: 0, DesktopCellHeightDp,
            ),
        )
        if (cell != null) shell?.addWidget(pendingWidgetId, cell, span)
        pendingWidgetId = WidgetController.INVALID_ID
        pendingProvider = null
        pendingCell = null
    }

    /** Any failure releases the id — otherwise a cancelled picker leaks one every time. */
    private fun abandonPendingWidget() {
        widgets?.releaseId(pendingWidgetId)
        pendingWidgetId = WidgetController.INVALID_ID
        pendingProvider = null
        pendingCell = null
    }

    @Deprecated("startActivityForResult is what the widget bind and configure dialogs use")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            WidgetController.REQUEST_BIND ->
                if (resultCode == RESULT_OK) continueAfterBind() else abandonPendingWidget()

            WidgetController.REQUEST_CONFIGURE ->
                if (resultCode == RESULT_OK) placePendingWidget() else abandonPendingWidget()
        }
    }

    /**
     * The dock's icon lookup, or a lookup that yields nothing.
     *
     * Guarded like everything else on this path: no icon cache costs the dock its artwork, not the
     * user their desktop (GATE 4).
     */
    private fun iconResolverFor(darkTheme: Boolean): (AppEntry) -> Drawable? {
        val cache = runCatching { entryPoint().iconCache() }.getOrNull() ?: return { null }
        val resolver = IconResolver(applicationContext, cache, darkTheme = darkTheme)
        return { entry -> resolver.iconFor(entry) }
    }

    private fun loadEnvironment(): DesktopEnvironment = entryPoint().desktopEnvironmentSource().load()

    private fun entryPoint(): HomeEntryPoint =
        EntryPointAccessors.fromApplication(applicationContext, HomeEntryPoint::class.java)
}

/** The desktop's cell size, in dp — the unit a widget's span is measured in. */
private const val DesktopCellDp = 96
private const val DesktopCellHeightDp = 104

/** Stand-ins for a shell that could not be built, so the desktop still renders. */
/**
 * No prompt store means no memory of an answer, so nothing is asked.
 *
 * Reported as "already asked" rather than "never asked": a card we cannot record the dismissal of
 * would come back on every launch (overlay-permission-ask.md requirement 3).
 */
private val EmptyAsked =
    kotlinx.coroutines.flow.MutableStateFlow(AskedPrompts(Prompt.entries.toSet()))

private val EmptyRecent =
    kotlinx.coroutines.flow.MutableStateFlow(emptyList<com.somalapuram.pclauncher.core.apps.AppEntry>())

private val EmptyInventory = kotlinx.coroutines.flow.MutableStateFlow(
    com.somalapuram.pclauncher.core.apps.AppInventory(),
)
private val EmptyPins = kotlinx.coroutines.flow.MutableStateFlow(
    com.somalapuram.pclauncher.core.data.pins.Pins(),
)
private val EmptyLayout = kotlinx.coroutines.flow.MutableStateFlow(
    com.somalapuram.pclauncher.core.data.layout.DesktopLayout(),
)

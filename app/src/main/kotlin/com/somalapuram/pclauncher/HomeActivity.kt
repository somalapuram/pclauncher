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
import androidx.compose.runtime.remember
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.design.PcTheme
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.somalapuram.pclauncher.desktop.IconResolver
import com.somalapuram.pclauncher.feature.shell.tray.SystemTraySource
import com.somalapuram.pclauncher.feature.shell.tray.TrayState
import com.somalapuram.pclauncher.feature.shell.widget.BindOutcome
import com.somalapuram.pclauncher.feature.shell.widget.WidgetChoice
import com.somalapuram.pclauncher.feature.shell.widget.cellsFor
import com.somalapuram.pclauncher.widget.WidgetController
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
        val launcher = AppLauncher(applicationContext)
        widgets?.startListening()

        val traySource = SystemTraySource(applicationContext)
        // The activity, not the application context: `startActivity` from an application context
        // needs NEW_TASK, and these launches want to sit beside the desktop rather than inside it.
        val trayActions = com.somalapuram.pclauncher.feature.shell.tray.SystemTrayActions(this)

        setContent {
            val dark = isSystemInDarkTheme()
            val tray by remember { traySource.trayState() }
                .collectAsState(initial = TrayState())
            // Rebuilt when the theme flips, so the dock reloads icons baked for the new palette
            // rather than showing dark-glass tiles on a light desktop.
            val iconFor = remember(dark) { iconResolverFor(dark) }

            PcTheme(darkTheme = dark) {
                HomeScreen(
                    inventory = shell?.inventory ?: EmptyInventory,
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
                    outcome = outcome,
                    isDefaultHome = HomeRole.isDefault(this),
                    onSetDefaultHome = { startActivity(HomeRole.requestIntent(this)) },
                    onRetry = { recreate() },
                    safeModeApps = safeModeApps,
                )
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
private val EmptyInventory = kotlinx.coroutines.flow.MutableStateFlow(
    com.somalapuram.pclauncher.core.apps.AppInventory(),
)
private val EmptyPins = kotlinx.coroutines.flow.MutableStateFlow(
    com.somalapuram.pclauncher.core.data.pins.Pins(),
)
private val EmptyLayout = kotlinx.coroutines.flow.MutableStateFlow(
    com.somalapuram.pclauncher.core.data.layout.DesktopLayout(),
)

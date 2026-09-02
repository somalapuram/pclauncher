package com.somalapuram.pclauncher.desktop

import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.apps.AppInventory
import com.somalapuram.pclauncher.core.apps.AppInventoryRepository
import com.somalapuram.pclauncher.core.apps.UsageSignalSource
import com.somalapuram.pclauncher.core.apps.UsageStore
import com.somalapuram.pclauncher.core.apps.recentlyUsed
import com.somalapuram.pclauncher.core.data.pins.Pin
import com.somalapuram.pclauncher.core.data.pins.PinStore
import com.somalapuram.pclauncher.core.data.layout.DesktopCell
import com.somalapuram.pclauncher.core.data.layout.DesktopLayout
import com.somalapuram.pclauncher.core.data.layout.DesktopLayoutStore
import com.somalapuram.pclauncher.core.data.layout.DesktopPlacement
import com.somalapuram.pclauncher.core.data.layout.DesktopSpan
import com.somalapuram.pclauncher.core.data.layout.ResizeEdge
import com.somalapuram.pclauncher.core.data.layout.ResizePermission
import com.somalapuram.pclauncher.core.data.layout.cellsDragged
import com.somalapuram.pclauncher.core.data.layout.resizedBy
import com.somalapuram.pclauncher.core.data.layout.widgetPlacementId
import com.somalapuram.pclauncher.core.data.pins.Pins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What is installed, and what is pinned — one owner for both.
 *
 * The dock, the Start menu and the desktop all read from here, so they cannot disagree about pin
 * state (pinning.md: the store is the single source of truth and every surface calls the same two
 * operations).
 *
 * Deliberately **not** a `@HiltViewModel`. Hilt injects an activity's fields before `onCreate`
 * runs, so a broken graph would take the home screen down before any guard could catch it. This is
 * a plain class the activity constructs inside `runCatching`, which keeps every dependency failure
 * on the fallback-desktop path (GATE 4).
 */
class ShellController(
    private val repository: AppInventoryRepository,
    private val pinStore: PinStore,
    private val layoutStore: DesktopLayoutStore,
    private val scope: CoroutineScope,
    private val userSerial: Long,
    /** Where a launch is written. `None` in safe mode, which must touch no store (GATE 4). */
    private val usageStore: UsageStore = UsageStore.None,
    /**
     * Where recency is read. Separate from [usageStore] because the two can differ: with usage
     * access granted the signals come from the system and know about launches that predate us,
     * while what we *write* is always our own counters.
     */
    private val usageSignals: UsageSignalSource = UsageSignalSource { emptyMap() },
) {
    val inventory: StateFlow<AppInventory> = repository.inventory

    private val _pins = MutableStateFlow(Pins())
    val pins: StateFlow<Pins> = _pins.asStateFlow()

    private val _layout = MutableStateFlow(DesktopLayout())
    val layout: StateFlow<DesktopLayout> = _layout.asStateFlow()

    /** The Start menu's Recent row, most recently launched first (recent-apps.md). */
    private val _recent = MutableStateFlow<List<AppEntry>>(emptyList())
    val recent: StateFlow<List<AppEntry>> = _recent.asStateFlow()

    /**
     * Bumped by each recorded launch.
     *
     * Signals are read, not observed — `UsageStatsManager` has no change callback and the local
     * counters are a plain key-value store. So the read is re-triggered by the one event that can
     * change the answer, which is what makes a launch show up in the row without reopening the menu
     * (recent-apps.md requirement 5).
     */
    private val usageRevision = MutableStateFlow(0L)

    fun start() {
        repository.start(scope)
        scope.launch {
            // A store that cannot be read yields an empty dock, never a crash.
            runCatching { pinStore.pins.collect { _pins.value = it } }
        }
        scope.launch {
            // Likewise: an unreadable layout costs the arrangement, not the desktop — icons fall
            // back to auto-placement (GATE 4).
            runCatching { layoutStore.layout.collect { _layout.value = it } }
        }
        scope.launch {
            // Recomputed when the inventory changes *or* a launch is recorded — an app that is
            // uninstalled has to leave the row, and one just opened has to enter it.
            runCatching {
                combine(repository.inventory, usageRevision) { inventory, _ -> inventory }
                    .collect { inventory ->
                        // `signals()` reads a DataStore synchronously and says so: never on the
                        // main thread.
                        _recent.value = withContext(Dispatchers.IO) {
                            recentlyUsed(inventory.entries, usageSignals.signals(), RecentAppLimit)
                        }
                    }
            }
        }
    }

    /**
     * Re-read the usage signals.
     *
     * The source can change without anything here changing: the user grants or revokes usage
     * access on a Settings screen, and `SystemUsageSignals` starts or stops answering. Nothing in
     * the inventory or the counters moves, so without a nudge the Recent row would keep showing
     * what the old source said until the next launch (usage-access-ask.md requirement 6).
     */
    fun refreshUsage() {
        scope.launch { usageRevision.value += 1 }
    }

    /**
     * Note that an app was launched.
     *
     * Fire-and-forget on the shell's own scope: the caller is on the main thread having just
     * started an activity, and must not wait for a disk write (recent-apps.md requirement 9).
     */
    fun recordLaunch(entry: AppEntry, atMillis: Long = System.currentTimeMillis()) {
        scope.launch {
            runCatching { usageStore.recordLaunch(entry.key, atMillis) }
            // Bumped even on a failed write: the system source may still have seen the launch, and
            // a re-read costs nothing.
            usageRevision.value += 1
        }
    }

    /**
     * Place a newly bound widget at a cell the caller chose.
     *
     * The cell comes from the *effective* layout — icons included — because the store only knows
     * about placements the user has made, and picking a free cell from it alone drops the widget
     * straight on top of an auto-placed icon.
     */
    fun addWidget(widgetId: Int, cell: DesktopCell, span: DesktopSpan) {
        scope.launch {
            runCatching { layoutStore.placeSpanning(widgetPlacementId(widgetId), cell, span) }
        }
    }

    /**
     * Move a widget to a cell.
     *
     * The same store operation an icon's drop calls: dragging is a second route to placement, never
     * a second implementation of it. The store keeps the widget's span and refuses an overlap, so a
     * bad drop costs the move and nothing else (widget-drag.md requirement 6).
     */
    fun moveWidget(widgetId: Int, cell: DesktopCell) {
        scope.launch { runCatching { layoutStore.place(widgetPlacementId(widgetId), cell) } }
    }

    /**
     * Forget a widget's placement.
     *
     * Releasing the widget id belongs to whoever owns the host, not here — this class knows about
     * layout and would need a framework dependency to do it (widget-removal.md requirement 5).
     */
    fun removeWidget(widgetId: Int) {
        scope.launch { runCatching { layoutStore.remove(widgetPlacementId(widgetId)) } }
    }

    /** Move an icon to a cell. A refused move leaves the store untouched and the icon springs back. */
    fun place(entry: AppEntry, cell: DesktopCell) {
        scope.launch { runCatching { layoutStore.place(pinIdOf(entry), cell) } }
    }

    fun isPinned(entry: AppEntry): Boolean = pinIdOf(entry) in _pins.value.items.map { it.component }

    /**
     * Toggle, off the main thread. A failed write costs the pin, never the interaction that
     * triggered it (pinning.md requirement 7).
     */
    fun togglePin(entry: AppEntry) {
        val pin = Pin(pinIdOf(entry), userSerial)
        scope.launch {
            runCatching {
                if (_pins.value.contains(pin)) pinStore.unpin(pin) else pinStore.pin(pin)
            }
        }
    }

    /**
     * The placement a resize is being measured against.
     *
     * Handle drags report *cumulative* pixels, so each report must be applied to where the widget
     * was when the drag began. Applying them to the current span instead compounds — a one-cell
     * drag reported ten times becomes ten cells.
     */
    private var resizeBase: DesktopPlacement? = null

    fun beginResize(widgetId: Int) {
        resizeBase = _layout.value.placementFor(widgetPlacementId(widgetId))
    }

    fun endResize() {
        resizeBase = null
    }

    /**
     * Apply a handle drag.
     *
     * The whole decision — permitted axis, minimum span, grid bounds, overlap — lives in
     * `resizedBy`, so this only turns pixels into cells and persists what comes back. A refused
     * resize returns null and nothing is written, which is what leaves the widget where it was.
     */
    fun resizeWidget(
        widgetId: Int,
        edge: ResizeEdge,
        pixels: Float,
        cellSize: Float,
        permission: ResizePermission,
        columnsAvailable: Int,
        rowsAvailable: Int,
        onApplied: (widthCells: Int, heightCells: Int) -> Unit,
    ) {
        val id = widgetPlacementId(widgetId)
        val cells = cellsDragged(pixels, cellSize)
        if (cells == 0) return

        // Measure from where the widget was when the drag started, not from where the last report
        // left it.
        val base = resizeBase ?: _layout.value.placementFor(id) ?: return
        val baseline = DesktopLayout(
            _layout.value.placements.filterNot { it.id == id } + base,
        )

        val next = resizedBy(
            layout = baseline,
            id = id,
            edge = edge,
            deltaCells = cells,
            permission = permission,
            columnsAvailable = columnsAvailable,
            rowsAvailable = rowsAvailable,
        ) ?: return

        val span = next.spanFor(id) ?: return
        scope.launch { runCatching { layoutStore.resize(id, span) } }
        onApplied(span.columns, span.rows)
    }

    fun stop() = repository.stop()

    private fun pinIdOf(entry: AppEntry) = entry.key.component.flattenToShortString()

    private companion object {
        /**
         * One row of the Start menu's grid.
         *
         * Tied to `StartColumns` by intent rather than by import — `core` must not depend on
         * `feature` — so a change there wants a change here (recent-apps.md notes).
         */
        const val RecentAppLimit = 5
    }
}

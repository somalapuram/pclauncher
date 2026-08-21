package com.somalapuram.pclauncher.core.data.layout

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Where the arrangement lives (SRS §10, `desktop_layout`). */
interface DesktopLayoutStore {
    val layout: Flow<DesktopLayout>

    suspend fun place(id: String, cell: DesktopCell)

    /**
     * Put a widget on the desktop.
     *
     * Widgets share the icons' cell space rather than living in a parallel store: two stores would
     * eventually disagree about which cells are free, and a widget landing on top of an icon is
     * exactly the bug that would produce.
     */
    suspend fun addWidget(widgetId: Int, rowsPerColumn: Int)
}

/** Widget placements are ordinary placements under a reserved id prefix. */
const val WIDGET_ID_PREFIX = "widget:"

fun widgetPlacementId(widgetId: Int): String = "$WIDGET_ID_PREFIX$widgetId"

/** The widget id inside a placement id, or null if this placement is not a widget. */
fun widgetIdOf(placementId: String): Int? =
    if (placementId.startsWith(WIDGET_ID_PREFIX)) {
        placementId.removePrefix(WIDGET_ID_PREFIX).toIntOrNull()
    } else {
        null
    }

class DataStoreDesktopLayoutStore(
    private val dataStore: DataStore<Preferences>,
) : DesktopLayoutStore {

    override val layout: Flow<DesktopLayout> = dataStore.data
        .map { DesktopLayoutCodec.decode(it[KEY]) }
        // A corrupt store costs the arrangement, not the desktop: icons fall back to
        // auto-placement rather than the home screen failing to come up (GATE 4).
        .catch { emit(DesktopLayout()) }

    override suspend fun place(id: String, cell: DesktopCell) {
        runCatching {
            dataStore.edit { prefs ->
                val current = DesktopLayoutCodec.decode(prefs[KEY])
                // A refused move leaves the store untouched, so the icon springs back.
                val next = current.moved(id, cell) ?: return@edit
                prefs[KEY] = DesktopLayoutCodec.encode(next)
            }
        }
        Unit
    }

    override suspend fun addWidget(widgetId: Int, rowsPerColumn: Int) {
        runCatching {
            dataStore.edit { prefs ->
                val current = DesktopLayoutCodec.decode(prefs[KEY])
                val id = widgetPlacementId(widgetId)
                // Adding the same widget twice must not move the one already placed.
                if (current.cellFor(id) != null) return@edit
                val next = current.moved(id, firstFreeCell(current, rowsPerColumn)) ?: return@edit
                prefs[KEY] = DesktopLayoutCodec.encode(next)
            }
        }
        Unit
    }

    private companion object {
        val KEY = stringPreferencesKey("desktop_layout")
    }
}

/** For tests and for safe mode, which must not touch a store. */
class InMemoryDesktopLayoutStore(initial: DesktopLayout = DesktopLayout()) : DesktopLayoutStore {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(initial)

    override val layout: Flow<DesktopLayout> = state

    override suspend fun place(id: String, cell: DesktopCell) {
        state.value = state.value.moved(id, cell) ?: state.value
    }

    override suspend fun addWidget(widgetId: Int, rowsPerColumn: Int) {
        val id = widgetPlacementId(widgetId)
        if (state.value.cellFor(id) != null) return
        state.value = state.value.moved(id, firstFreeCell(state.value, rowsPerColumn)) ?: state.value
    }
}

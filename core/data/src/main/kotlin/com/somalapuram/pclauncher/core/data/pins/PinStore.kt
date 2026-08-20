package com.somalapuram.pclauncher.core.data.pins

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Where pins live. The single source of truth — the Start menu, the desktop and the dock all go
 * through this, or they will eventually disagree about what is pinned.
 */
interface PinStore {
    val pins: Flow<Pins>
    suspend fun pin(pin: Pin)
    suspend fun unpin(pin: Pin)
}

class DataStorePinStore(
    private val dataStore: DataStore<Preferences>,
) : PinStore {

    override val pins: Flow<Pins> = flow {
        // A corrupt or unreadable store yields an empty dock, never a crash: this is read on the
        // way to drawing the home screen (GATE 4).
        emitAll(dataStore.data.map { PinCodec.decode(it[KEY]) }.catch { emit(Pins()) })
    }

    override suspend fun pin(pin: Pin) = update { it.plus(pin) }

    override suspend fun unpin(pin: Pin) = update { it.minus(pin) }

    private suspend fun update(transform: (Pins) -> Pins) {
        // A failed write costs the pin, never the interaction that triggered it.
        runCatching {
            dataStore.edit { prefs ->
                prefs[KEY] = PinCodec.encode(transform(PinCodec.decode(prefs[KEY])))
            }
        }
        Unit
    }

    private companion object {
        val KEY = stringPreferencesKey("pins")
    }
}

/** For tests and for safe mode, which must not touch a store. */
class InMemoryPinStore(initial: Pins = Pins()) : PinStore {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(initial)
    override val pins: Flow<Pins> = state
    override suspend fun pin(pin: Pin) { state.value = state.value.plus(pin) }
    override suspend fun unpin(pin: Pin) { state.value = state.value.minus(pin) }
}

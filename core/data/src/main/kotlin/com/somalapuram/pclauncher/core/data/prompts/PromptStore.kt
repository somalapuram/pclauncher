package com.somalapuram.pclauncher.core.data.prompts

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Where one-time prompts record that they have been shown. */
interface PromptStore {
    val asked: Flow<AskedPrompts>
    suspend fun markAsked(prompt: Prompt)
}

class DataStorePromptStore(
    private val dataStore: DataStore<Preferences>,
) : PromptStore {

    override val asked: Flow<AskedPrompts> = flow {
        // A store that cannot be read is a store that cannot be written either, so showing the
        // prompt would mean showing it again on the next launch, and the one after that. Report
        // everything as already asked instead: the user can still grant the permission from
        // Settings, and nobody gets nagged. Never a crash — this is read on the way to drawing the
        // home screen (GATE 4).
        emitAll(
            dataStore.data.map { PromptCodec.decode(it[KEY]) }
                .catch { emit(AskedPrompts(Prompt.entries.toSet())) },
        )
    }

    override suspend fun markAsked(prompt: Prompt) {
        // A failed write costs the memory of the answer, never the interaction that triggered it.
        runCatching {
            dataStore.edit { prefs ->
                prefs[KEY] = PromptCodec.encode(PromptCodec.decode(prefs[KEY]).plus(prompt))
            }
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("asked")
    }
}

/** For tests and for safe mode, which must not touch a store. */
class InMemoryPromptStore(initial: AskedPrompts = AskedPrompts()) : PromptStore {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(initial)
    override val asked: Flow<AskedPrompts> = state
    override suspend fun markAsked(prompt: Prompt) { state.value = state.value.plus(prompt) }
}

package com.somalapuram.pclauncher.core.apps

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * The `usage` store from SRS §10, backed by DataStore.
 *
 * **Preferences, not Proto.** SRS §10 names Proto DataStore, and that is right for the structured
 * stores — layout, geometry, pins. This one holds two scalars per key, so a protobuf schema and its
 * codegen buy nothing here. Recorded as a deliberate deviation rather than an oversight; if this
 * grows a third field it should move to Proto with the others.
 */
class DataStoreUsageStore(
    private val dataStore: DataStore<Preferences>,
) : UsageStore {

    /**
     * Read synchronously.
     *
     * Callers are ranking a list they are about to draw, and the data is a handful of ints. The
     * cost of the alternative — making every consumer suspend — is not worth paying here, but it
     * does mean this must not be called on the main thread.
     */
    override fun signals(): Map<AppKey, UsageSignal> = runCatching {
        runBlocking { dataStore.data.first() }.toSignals()
    }.getOrDefault(emptyMap())

    override suspend fun recordLaunch(key: AppKey, atMillis: Long) {
        val flat = key.component.flattenToShortString()
        runCatching {
            dataStore.edit { prefs ->
                prefs[lastUsedKey(flat)] = atMillis
                prefs[countKey(flat)] = (prefs[countKey(flat)] ?: 0) + 1
            }
        }
        // A failed write costs one launch's worth of ranking accuracy. It must never propagate:
        // the app the user asked for still has to open.
    }

    private fun Preferences.toSignals(): Map<AppKey, UsageSignal> = asMap()
        .keys
        .mapNotNull { it.name.removePrefixOrNull(LAST_USED_PREFIX) }
        .mapNotNull { flat ->
            val key = appKeyFrom(flat, userSerial = 0) ?: return@mapNotNull null
            key to UsageSignal(
                key = key,
                lastLaunchedAtMillis = this[lastUsedKey(flat)] ?: 0L,
                launchCount = this[countKey(flat)] ?: 0,
            )
        }
        .toMap()

    private fun lastUsedKey(flat: String) = longPreferencesKey("$LAST_USED_PREFIX$flat")
    private fun countKey(flat: String) = intPreferencesKey("$COUNT_PREFIX$flat")

    private companion object {
        const val LAST_USED_PREFIX = "last_used:"
        const val COUNT_PREFIX = "count:"
    }
}

private fun String.removePrefixOrNull(prefix: String): String? =
    if (startsWith(prefix)) removePrefix(prefix) else null

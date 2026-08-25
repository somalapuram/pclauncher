package com.somalapuram.pclauncher.core.apps

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * The inventory, as a stream.
 *
 * SRS §12 gives 500 ms to an interactive desktop (1.5 s on `pc_x86_64` under software rendering),
 * and building this touches every profile. So [inventory] emits an **empty, incomplete** value
 * immediately and fills in per profile: the desktop renders its wallpaper and grid while the list
 * is still arriving, rather than waiting on it.
 *
 * Changes are applied as deltas through [applyChange]. Nothing here rescans in response to a
 * package event.
 */
class AppInventoryRepository(
    private val source: AppSource,
    private val ioDispatcher: CoroutineDispatcher,
    private val locale: () -> Locale = { Locale.getDefault() },
) {
    private val _inventory = MutableStateFlow(AppInventory())
    val inventory: StateFlow<AppInventory> = _inventory.asStateFlow()

    private var subscription: AutoCloseable? = null

    /**
     * Build the list and start following changes.
     *
     * Profiles are loaded one at a time and published as each lands, so a slow or locked profile
     * cannot hold up the ones that are ready.
     *
     * **Safe to call again.** This object is a `@Singleton` while the thing that starts it is an
     * activity, and Android creates a second home activity readily — a configuration change, or
     * the system relaunching the home app when the default home role moves. So a repeat call has
     * to be correct rather than refused: refusing would leave an outgoing activity's `stop()` able
     * to unsubscribe the incoming one's inventory for good (inventory-identity.md).
     */
    fun start(scope: CoroutineScope) {
        scope.launch {
            // Before anything else: a subscription already held would otherwise stay registered
            // with nothing left pointing at it, and every package change would be applied twice
            // for the life of the process.
            closeSubscription()

            val profiles = withContext(ioDispatcher) { source.profiles() }

            for (user in profiles) {
                val entries = withContext(ioDispatcher) { source.entriesFor(user) }
                _inventory.value = _inventory.value.let { current ->
                    // Keyed, not concatenated — see `mergedByKey`. This is what makes a second
                    // build replace the list rather than double it.
                    current.copy(entries = sortedByLabel(mergedByKey(current.entries, entries), locale()))
                }
            }

            _inventory.value = _inventory.value.copy(isComplete = true)

            subscription = withContext(ioDispatcher) {
                source.observeChanges { change -> apply(change) }
            }
        }
    }

    /** Visible for the callback adapter and for tests; pure work happens in [applyChange]. */
    fun apply(change: AppChange) {
        _inventory.value = applyChange(_inventory.value, change, locale())
    }

    fun stop() = closeSubscription()

    private fun closeSubscription() {
        subscription?.let { runCatching { it.close() } }
        subscription = null
    }
}

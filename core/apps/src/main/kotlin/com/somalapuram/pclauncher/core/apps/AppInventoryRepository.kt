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
     */
    fun start(scope: CoroutineScope) {
        scope.launch {
            val profiles = withContext(ioDispatcher) { source.profiles() }

            for (user in profiles) {
                val entries = withContext(ioDispatcher) { source.entriesFor(user) }
                _inventory.value = _inventory.value.let { current ->
                    current.copy(entries = sortedByLabel(current.entries + entries, locale()))
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

    fun stop() {
        subscription?.close()
        subscription = null
    }
}

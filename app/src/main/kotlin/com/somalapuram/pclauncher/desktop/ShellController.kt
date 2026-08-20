package com.somalapuram.pclauncher.desktop

import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.apps.AppInventory
import com.somalapuram.pclauncher.core.apps.AppInventoryRepository
import com.somalapuram.pclauncher.core.data.pins.Pin
import com.somalapuram.pclauncher.core.data.pins.PinStore
import com.somalapuram.pclauncher.core.data.pins.Pins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val scope: CoroutineScope,
    private val userSerial: Long,
) {
    val inventory: StateFlow<AppInventory> = repository.inventory

    private val _pins = MutableStateFlow(Pins())
    val pins: StateFlow<Pins> = _pins.asStateFlow()

    fun start() {
        repository.start(scope)
        scope.launch {
            // A store that cannot be read yields an empty dock, never a crash.
            runCatching { pinStore.pins.collect { _pins.value = it } }
        }
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

    fun stop() = repository.stop()

    private fun pinIdOf(entry: AppEntry) = entry.key.component.flattenToShortString()
}

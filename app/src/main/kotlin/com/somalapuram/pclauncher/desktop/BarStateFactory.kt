package com.somalapuram.pclauncher.desktop

import android.graphics.drawable.Drawable
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.apps.AppInventory
import com.somalapuram.pclauncher.feature.shell.bar.BarState
import com.somalapuram.pclauncher.feature.shell.bar.DockItem

/**
 * Turns the app inventory into what the bar draws.
 *
 * Pure, so the choice of what lands in the dock is testable without a device — and so the dock's
 * contents cannot quietly depend on anything but the inventory.
 */
object BarStateFactory {

    /** How many apps the dock shows before pinning exists (`pins` store arrives with its own doc). */
    const val DEFAULT_DOCK_SIZE = 8

    /**
     * Until the `pins` store exists, the dock shows the first [limit] launchable apps in the
     * inventory's own order — alphabetical, locale-aware, stable. A deliberate placeholder: it is
     * predictable and never empty, which is what makes the bar reviewable now.
     */
    fun from(
        inventory: AppInventory,
        iconFor: (AppEntry) -> Drawable? = { null },
        runningIds: Set<String> = emptySet(),
        focusedId: String? = null,
        magnificationEnabled: Boolean = true,
        limit: Int = DEFAULT_DOCK_SIZE,
    ): BarState = BarState(
        dockItems = inventory.entries
            .take(limit)
            .map { it.toDockItem(iconFor, runningIds, focusedId) },
        windows = emptyList(),
        magnificationEnabled = magnificationEnabled,
    )

    private fun AppEntry.toDockItem(
        iconFor: (AppEntry) -> Drawable?,
        runningIds: Set<String>,
        focusedId: String?,
    ): DockItem {
        val id = key.component.flattenToShortString()
        return DockItem(
            id = id,
            label = label,
            icon = iconFor(this),
            isRunning = id in runningIds,
            isFocused = id == focusedId,
            // Matches the inventory's contract: suspended and unavailable entries are shown
            // greyed rather than dropped.
            isAvailable = isLaunchable,
        )
    }
}

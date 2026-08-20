package com.somalapuram.pclauncher.feature.shell.start

import com.somalapuram.pclauncher.core.apps.AppEntry

/**
 * Turning stored pins into the dock's list.
 *
 * Pure, because the interesting cases are all data: an empty store, a pin whose app was
 * uninstalled, a pin belonging to a profile that is currently off.
 */
object PinResolution {

    /**
     * Resolve [pinnedIds] against [entries], in **stored order**.
     *
     * An unresolvable pin is skipped here and — importantly — is *not* removed from the store by
     * this function. A work profile being switched off must not permanently lose its pins; turning
     * it back on has to restore them (pinning.md requirement 5).
     *
     * When nothing resolves, falls back to [fallbackLimit] entries in inventory order, so a first
     * run is never a bare bar.
     */
    fun resolve(
        entries: List<AppEntry>,
        pinnedIds: List<String>,
        fallbackLimit: Int = 8,
    ): List<AppEntry> {
        if (pinnedIds.isEmpty()) return entries.take(fallbackLimit)

        val byId = entries.associateBy { it.key.component.flattenToShortString() }
        val resolved = pinnedIds.mapNotNull { byId[it] }

        // Every pin points at something absent — an empty dock would look broken, so fall back.
        return resolved.ifEmpty { entries.take(fallbackLimit) }
    }

    fun isPinned(pinnedIds: List<String>, entry: AppEntry): Boolean =
        entry.key.component.flattenToShortString() in pinnedIds
}

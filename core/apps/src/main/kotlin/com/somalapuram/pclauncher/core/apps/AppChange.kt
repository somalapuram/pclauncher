package com.somalapuram.pclauncher.core.apps

import android.os.UserHandle
import java.text.Collator
import java.util.Locale

/**
 * Something that happened to the inventory.
 *
 * Modelled as data so that applying it is a pure function (SRS §12: no full rescan on a package
 * event). The `LauncherApps.Callback` is a thin adapter that turns framework callbacks into these;
 * everything worth testing happens in [applyChange].
 */
sealed interface AppChange {

    /** A package appeared, or was replaced by an update. Carries its current entries. */
    data class PackageUpserted(
        val packageName: String,
        val user: UserHandle,
        val entries: List<AppEntry>,
    ) : AppChange

    data class PackageRemoved(val packageName: String, val user: UserHandle) : AppChange

    /** Suspended by policy, or unsuspended. The entries stay; only their state changes. */
    data class PackagesSuspended(
        val packageNames: Set<String>,
        val user: UserHandle,
        val suspended: Boolean,
    ) : AppChange

    /** External storage attached/detached, or a profile entering or leaving quiet mode. */
    data class PackagesAvailability(
        val packageNames: Set<String>,
        val user: UserHandle,
        val available: Boolean,
    ) : AppChange

    /** A whole profile appeared — its entries arrive with it. */
    data class ProfileAdded(val user: UserHandle, val entries: List<AppEntry>) : AppChange

    /** A profile was removed; everything belonging to it goes with it. */
    data class ProfileRemoved(val user: UserHandle) : AppChange

    /**
     * The device locale changed.
     *
     * Easy to miss and expensive to get wrong: every label is now different, and so is the sort
     * order. This is the one change that legitimately replaces the whole list.
     */
    data class LocaleChanged(val entries: List<AppEntry>, val locale: Locale) : AppChange
}

/**
 * Apply one change to the inventory. Pure — this is the unit under test.
 *
 * The result is always re-sorted with [locale], because an upsert can change a label and an
 * inserted entry has to land in the right place; sorting a nearly-sorted list is cheap next to the
 * rescan this exists to avoid.
 */
fun applyChange(
    inventory: AppInventory,
    change: AppChange,
    locale: Locale = Locale.getDefault(),
): AppInventory {
    val entries = inventory.entries

    val updated: List<AppEntry> = when (change) {
        is AppChange.PackageUpserted ->
            entries.filterNot { it.packageName == change.packageName && it.key.user == change.user } +
                change.entries

        is AppChange.PackageRemoved ->
            entries.filterNot { it.packageName == change.packageName && it.key.user == change.user }

        is AppChange.PackagesSuspended ->
            entries.map { entry ->
                if (entry.packageName in change.packageNames && entry.key.user == change.user) {
                    entry.copy(isSuspended = change.suspended)
                } else {
                    entry
                }
            }

        is AppChange.PackagesAvailability ->
            entries.map { entry ->
                if (entry.packageName in change.packageNames && entry.key.user == change.user) {
                    entry.copy(isAvailable = change.available)
                } else {
                    entry
                }
            }

        is AppChange.ProfileAdded ->
            entries.filterNot { it.key.user == change.user } + change.entries

        is AppChange.ProfileRemoved ->
            entries.filterNot { it.key.user == change.user }

        is AppChange.LocaleChanged ->
            change.entries
    }

    val sortLocale = if (change is AppChange.LocaleChanged) change.locale else locale
    return inventory.copy(entries = sortedByLabel(updated, sortLocale))
}

/**
 * Default ordering: by label, using a locale [Collator].
 *
 * Not `String.compareTo` — that orders by UTF-16 code unit, which puts every accented and
 * non-Latin label in the wrong place and sorts "Ähnlich" after "Zulu" in German.
 *
 * The key is ties: two entries can share a label (the same app in the personal and work profiles),
 * so profile and component break the tie and keep the order stable across rebuilds.
 */
fun sortedByLabel(entries: List<AppEntry>, locale: Locale = Locale.getDefault()): List<AppEntry> {
    val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
    return entries.sortedWith(
        compareBy<AppEntry, String>(collator) { it.label }
            .thenBy { it.profile.ordinal }
            .thenBy { it.key.component.flattenToShortString() },
    )
}

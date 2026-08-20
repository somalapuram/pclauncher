package com.somalapuram.pclauncher.feature.shell.start

import com.somalapuram.pclauncher.core.apps.AppEntry
import java.text.Normalizer
import java.util.Locale

/**
 * Filtering for the Start menu.
 *
 * Substring, not fuzzy. Fuzzy matching earns its keep in the command palette, where the user is
 * aiming at one result and wants forgiveness; in a browsable list it mostly produces surprising
 * ordering and makes the menu feel unreliable.
 *
 * Pure, and run over the already-loaded inventory — never a re-query of `LauncherApps` per
 * keystroke (start-menu.md requirement 7).
 */
object AppSearch {

    /**
     * Strip case and diacritics so "cafe" finds "Café" and "ähnlich" finds "Ahnlich".
     *
     * NFD splits a letter from its accent, then the combining marks are dropped — someone on a US
     * keyboard should not have to produce a diacritic to find an app that has one.
     */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")

    /**
     * Filter and rank.
     *
     * Prefix matches first, then substring, each keeping the inventory's locale-aware order —
     * someone typing "cal" wants Calendar above "Google Calculator Sync".
     */
    fun filter(entries: List<AppEntry>, query: String): List<AppEntry> {
        val needle = normalize(query).trim()
        if (needle.isEmpty()) return entries

        val prefix = mutableListOf<AppEntry>()
        val contains = mutableListOf<AppEntry>()

        for (entry in entries) {
            val label = normalize(entry.label)
            // Order matters: the word-start case must be tested *before* plain containment, or a
            // label like "Google Calendar Sync" matches `contains` first and is ranked below an
            // incidental match such as "Vertical".
            when {
                label.startsWith(needle) -> prefix += entry
                label.split(' ').any { it.startsWith(needle) } -> prefix += entry
                label.contains(needle) -> contains += entry
            }
        }
        return prefix + contains
    }

    private val COMBINING_MARKS = Regex("\\p{Mn}+")
}

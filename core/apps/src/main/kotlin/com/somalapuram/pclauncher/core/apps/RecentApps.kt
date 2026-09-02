package com.somalapuram.pclauncher.core.apps

/**
 * The apps the user actually came back to, most recent first.
 *
 * Distinct from [byRecency], which orders *every* entry and falls back to the label when nothing
 * has been launched. That is right for ranking a full list; it is wrong for a "Recent" row, which
 * would then be filled with alphabetical strangers the user has never opened. Here an entry with no
 * recorded use is simply not recent (recent-apps.md requirement 2).
 */
fun recentlyUsed(
    entries: List<AppEntry>,
    signals: Map<AppKey, UsageSignal>,
    limit: Int,
): List<AppEntry> {
    if (limit <= 0) return emptyList()

    return entries
        .mapNotNull { entry ->
            val signal = signals[entry.key] ?: return@mapNotNull null
            // Zero is "never", not "at the epoch". A store that recorded a key without a timestamp
            // must not outrank an app the user opened a minute ago.
            if (signal.lastLaunchedAtMillis <= 0L) return@mapNotNull null
            entry to signal
        }
        .sortedWith(
            compareByDescending<Pair<AppEntry, UsageSignal>> { it.second.lastLaunchedAtMillis }
                // Same millisecond, which the system source produces readily: its timestamps are
                // bucketed, so several apps can share one. Fall through to how often, then to the
                // label, so the row is stable between reads rather than reshuffling on each open.
                .thenByDescending { it.second.launchCount }
                .thenBy { it.first.label },
        )
        .take(limit)
        .map { it.first }
}

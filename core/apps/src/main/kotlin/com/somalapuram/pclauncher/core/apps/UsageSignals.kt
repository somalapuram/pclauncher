package com.somalapuram.pclauncher.core.apps

/**
 * How recently and how often an entry was launched, used to order "Recommended" and to break ties
 * in search. Not a ranking function — scoring belongs to the command palette (phase 5).
 */
data class UsageSignal(
    val key: AppKey,
    val lastLaunchedAtMillis: Long,
    val launchCount: Int,
)

/** Where usage signals come from. */
enum class UsageSource {
    /** `UsageStatsManager` — richer, but needs usage access the user grants in Settings. */
    SystemUsageStats,

    /** Our own counters in the `usage` DataStore store (SRS §10). Always available. */
    LocalCounters,
}

/**
 * Pick the source.
 *
 * SRS §11: a denial is remembered and respected. The ungranted path is a **first-class behaviour**,
 * not a degraded one — the shell ranks sensibly either way, so nothing downstream is allowed to ask
 * "do we have the permission?" It asks the source for signals and gets them.
 */
fun usageSourceFor(hasUsageAccess: Boolean): UsageSource =
    if (hasUsageAccess) UsageSource.SystemUsageStats else UsageSource.LocalCounters

/** Supplies signals from whichever source applies. */
fun interface UsageSignalSource {
    fun signals(): Map<AppKey, UsageSignal>
}

/**
 * Most-recently-launched first, then by launch count, then by label so the order is stable when an
 * entry has never been launched (every signal zero — the first-run case).
 */
fun byRecency(
    entries: List<AppEntry>,
    signals: Map<AppKey, UsageSignal>,
    limit: Int = Int.MAX_VALUE,
): List<AppEntry> = entries
    .sortedWith(
        compareByDescending<AppEntry> { signals[it.key]?.lastLaunchedAtMillis ?: 0L }
            .thenByDescending { signals[it.key]?.launchCount ?: 0 }
            .thenBy { it.label },
    )
    .take(limit)

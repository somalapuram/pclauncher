package com.somalapuram.pclauncher.core.apps

import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.os.Process

/**
 * Where launch counts live when the user has not granted usage access.
 *
 * An interface rather than a concrete store so the ranking logic stays testable without a
 * filesystem, and so the fallback desktop can be handed a no-op that touches nothing (GATE 4).
 */
interface UsageStore {
    fun signals(): Map<AppKey, UsageSignal>
    suspend fun recordLaunch(key: AppKey, atMillis: Long)

    /** Reads nothing, writes nothing. What safe mode gets. */
    object None : UsageStore {
        override fun signals(): Map<AppKey, UsageSignal> = emptyMap()
        override suspend fun recordLaunch(key: AppKey, atMillis: Long) = Unit
    }
}

/**
 * Signals from `UsageStatsManager`. Richer than our own counters — it knows about launches that
 * happened before pclauncher was installed — but only available with usage access granted.
 *
 * Note the mismatch it has to bridge: usage stats are per **package**, while the inventory is keyed
 * by `(component, user)`. Every entry of a package therefore inherits that package's stats, which
 * is right for the common single-activity case and harmless for the rest.
 */
class SystemUsageSignals(
    private val context: Context,
    private val entries: () -> List<AppEntry>,
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    private val now: () -> Long = System::currentTimeMillis,
) : UsageSignalSource {

    override fun signals(): Map<AppKey, UsageSignal> {
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val end = now()

        val stats = runCatching {
            manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, end - windowMillis, end)
        }.getOrNull().orEmpty()

        // Revoking usage access mid-session returns an empty list rather than throwing. Treat it
        // as "no signals" and let the caller fall back, instead of reporting zeros as fact.
        if (stats.isEmpty()) return emptyMap()

        val byPackage = stats.associateBy({ it.packageName }, { it.lastTimeUsed })

        return entries().mapNotNull { entry ->
            val lastUsed = byPackage[entry.packageName] ?: return@mapNotNull null
            entry.key to UsageSignal(entry.key, lastLaunchedAtMillis = lastUsed, launchCount = 0)
        }.toMap()
    }

    companion object {
        /** A week is enough to order "Recommended" without weighting long-dead apps. */
        const val DEFAULT_WINDOW_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    }
}

/** Signals from our own counters — always available, and the reason the ungranted path is first-class. */
class LocalUsageSignals(private val store: UsageStore) : UsageSignalSource {
    override fun signals(): Map<AppKey, UsageSignal> = store.signals()
}

/**
 * Picks a source at call time rather than at construction.
 *
 * Usage access can be granted or revoked while the shell is running, and the answer has to follow
 * — hence [hasUsageAccess] as a function. When the system source returns nothing (revoked, or
 * simply no history) it falls through to local counters rather than showing an empty
 * "Recommended".
 */
class UsageSignals(
    private val hasUsageAccess: () -> Boolean,
    private val system: UsageSignalSource,
    private val local: UsageSignalSource,
) : UsageSignalSource {

    override fun signals(): Map<AppKey, UsageSignal> {
        if (usageSourceFor(hasUsageAccess()) == UsageSource.SystemUsageStats) {
            val fromSystem = system.signals()
            if (fromSystem.isNotEmpty()) return fromSystem
        }
        return local.signals()
    }
}

/** Rebuild an [AppKey] from its stored flat form. Null when the string is not a component. */
internal fun appKeyFrom(flatComponent: String, userSerial: Int): AppKey? {
    val component: ComponentName = ComponentName.unflattenFromString(flatComponent) ?: return null
    // Only our own user's counters are persisted locally; other profiles rely on system stats.
    return AppKey(component, Process.myUserHandle()).takeIf { userSerial >= 0 }
}

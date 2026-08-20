package com.somalapuram.pclauncher.core.apps

import android.content.ComponentName
import android.os.UserHandle

/**
 * Which profile an entry lives in.
 *
 * Private space (Android 15+) is just another profile to `LauncherApps.getProfiles()`, so it is
 * modelled as one rather than given a second mechanism — "present but locked" is the same shape as
 * quiet mode (SRS §5, requirement doc Notes).
 */
enum class ProfileKind { Personal, Work, Private }

/**
 * The identity of a launchable thing.
 *
 * **A package is not an identity.** The same package exists separately in the personal and work
 * profiles with different icons, availability, and launch targets; keying on package alone shows
 * up as a work app launching the personal one.
 */
data class AppKey(
    val component: ComponentName,
    val user: UserHandle,
)

/**
 * One launchable activity, as every shell surface sees it.
 *
 * [isResizeable] is captured here rather than looked up at launch time: the `ActivityInfo` is
 * already in hand while building the inventory, and fetching it again in phase 6 would mean a
 * second pass over every package (SRS §5.4).
 */
data class AppEntry(
    val key: AppKey,
    val label: String,
    val packageName: String,
    val profile: ProfileKind,
    /** Suspended by a device policy or digital-wellbeing rule. Shown, but not launchable. */
    val isSuspended: Boolean = false,
    /**
     * Available to launch. False for a profile in quiet mode, a locked private space, or an app on
     * detached external storage. Such entries stay in the list so the UI can grey them out — the
     * user's apps must never silently vanish.
     */
    val isAvailable: Boolean = true,
    val isResizeable: Boolean = true,
    val firstInstallTime: Long = 0L,
    /** The app's own version, part of the icon cache key so an update cannot serve a stale icon. */
    val versionCode: Long = 0L,
) {
    /** Launchable right now — as opposed to merely present in the list. */
    val isLaunchable: Boolean get() = isAvailable && !isSuspended
}

/**
 * The whole list, plus whether it is still being built.
 *
 * [isComplete] exists because SRS §12 requires the desktop to render before the inventory finishes:
 * surfaces show what they have and say so, rather than blocking on a full load.
 */
data class AppInventory(
    val entries: List<AppEntry> = emptyList(),
    val isComplete: Boolean = false,
) {
    val size: Int get() = entries.size

    fun entryFor(key: AppKey): AppEntry? = entries.firstOrNull { it.key == key }
}

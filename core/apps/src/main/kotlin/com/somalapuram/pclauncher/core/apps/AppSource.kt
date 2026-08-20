package com.somalapuram.pclauncher.core.apps

import android.os.UserHandle

/**
 * Everything the inventory needs from the platform, behind one interface.
 *
 * This is the only seam that touches `LauncherApps`. Keeping it this narrow is what lets every
 * test — delta application, ordering, profile mapping — run against a fake with no device, no real
 * work profile, and no granted permission (requirement 10).
 */
interface AppSource {

    /** Every profile the launcher can see: personal, work, private space. */
    fun profiles(): List<UserHandle>

    /** Entries for one profile. Empty is a legitimate answer for a locked or quiet profile. */
    fun entriesFor(user: UserHandle): List<AppEntry>

    /** Entries for a single package in one profile — used to service an upsert without a rescan. */
    fun entriesFor(packageName: String, user: UserHandle): List<AppEntry>

    /** Start delivering changes. Returns a handle that unregisters when closed. */
    fun observeChanges(onChange: (AppChange) -> Unit): AutoCloseable
}

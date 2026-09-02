package com.somalapuram.pclauncher.desktop

import android.content.Context
import android.content.pm.LauncherApps
import com.somalapuram.pclauncher.core.apps.AppEntry

/**
 * Starts an app.
 *
 * `LauncherApps.startMainActivity` rather than a plain intent: it is profile-aware, so a work-
 * profile entry launches in its own profile instead of failing or launching the personal copy.
 *
 * Bounds and windowing mode are **not** decided here — that is `WindowBackend`'s job
 * (`windows/capability-tiers.md`). This is the minimum needed to make the shell able to launch.
 *
 * Every launch in the shell goes through here, which is why [onLaunched] hangs off it: recording
 * use at each surface instead means one of them eventually forgets, and a recents list that is
 * quietly missing an app is a bug nobody reports (recent-apps.md requirement 4).
 */
class AppLauncher(
    private val context: Context,
    /** Called only for a launch that actually started. Must not block — see `recordLaunch`. */
    private val onLaunched: (AppEntry) -> Unit = {},
) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    /** Returns false when the launch failed, so the caller can say something useful. */
    fun launch(entry: AppEntry): Boolean {
        if (!entry.isLaunchable) return false
        return runCatching {
            launcherApps.startMainActivity(entry.key.component, entry.key.user, null, null)
            // After the launch, and guarded separately: bookkeeping must never be able to cost the
            // user the app they asked for (GATE 4).
            runCatching { onLaunched(entry) }
            true
        }.getOrDefault(false)
    }
}

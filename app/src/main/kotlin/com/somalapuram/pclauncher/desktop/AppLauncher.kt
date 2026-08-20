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
 */
class AppLauncher(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    /** Returns false when the launch failed, so the caller can say something useful. */
    fun launch(entry: AppEntry): Boolean {
        if (!entry.isLaunchable) return false
        return runCatching {
            launcherApps.startMainActivity(entry.key.component, entry.key.user, null, null)
            true
        }.getOrDefault(false)
    }
}

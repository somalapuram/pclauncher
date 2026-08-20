package com.somalapuram.pclauncher.core.apps

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager

/**
 * The real [AppSource]. **The only class in this module that touches the framework.**
 *
 * Deliberately not `PackageManager.queryIntentActivities`: that is not profile-aware, gives no
 * badged icons, and has no change callback — every launcher that starts there ends up rewriting it
 * against `LauncherApps` anyway.
 */
class LauncherAppsSource(
    private val context: Context,
    private val launcherApps: LauncherApps =
        context.getSystemService(LauncherApps::class.java),
    private val userManager: UserManager =
        context.getSystemService(UserManager::class.java),
) : AppSource {

    override fun profiles(): List<UserHandle> = runCatching { launcherApps.profiles }.getOrElse {
        // Fall back to our own user rather than showing nothing at all.
        listOf(android.os.Process.myUserHandle())
    }

    override fun entriesFor(user: UserHandle): List<AppEntry> = queryEntries(null, user)

    override fun entriesFor(packageName: String, user: UserHandle): List<AppEntry> =
        queryEntries(packageName, user)

    private fun queryEntries(packageName: String?, user: UserHandle): List<AppEntry> {
        // A locked private space or a profile in quiet mode throws rather than returning empty.
        // That is a normal state, not an error: report no entries and let availability carry it.
        val activities = runCatching { launcherApps.getActivityList(packageName, user) }
            .getOrElse { return emptyList() }

        val available = isProfileAvailable(user)
        return activities.map { it.toEntry(user, available) }
    }

    /**
     * Quiet mode (work profile paused) and a locked private space both mean "present but not
     * launchable". Treated as one state, so the UI greys entries out instead of losing them.
     */
    private fun isProfileAvailable(user: UserHandle): Boolean =
        runCatching { !userManager.isQuietModeEnabled(user) }.getOrDefault(true)

    private fun LauncherActivityInfo.toEntry(user: UserHandle, profileAvailable: Boolean): AppEntry {
        val appInfo = applicationInfo
        return AppEntry(
            key = AppKey(component = componentName, user = user),
            label = label.toString(),
            packageName = componentName.packageName,
            profile = profileKindFor(user),
            isSuspended = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SUSPENDED) != 0,
            isAvailable = profileAvailable,
            isResizeable = isResizeable(activityInfo),
            firstInstallTime = firstInstallTime,
            versionCode = versionCodeOf(componentName.packageName, user),
        )
    }

    private fun profileKindFor(user: UserHandle): ProfileKind = when {
        user == android.os.Process.myUserHandle() -> ProfileKind.Personal
        // The framework does not expose a public "is private space" predicate below the API this
        // module targets, so managed-profile is the discriminator and private space falls in with
        // Work until phase 9 needs to tell them apart. Recorded rather than guessed.
        else -> ProfileKind.Work
    }

    /**
     * SRS §5.4: an activity that refuses to resize must not be launched into a freeform window.
     * Captured here because the [ActivityInfo] is already in hand.
     *
     * **This is a partial signal.** `ActivityInfo.resizeMode` — the field that actually answers
     * the question — is `@hide`, so unprivileged code can only see fixed orientation, which is the
     * common case but not the whole of it. An activity declaring `resizeableActivity="false"`
     * without pinning its orientation reads as resizable here and will be discovered the hard way
     * at launch. Phase 9 can read the real value through the privileged provider; until then
     * phase 6 must also handle a launch that comes back non-freeform.
     */
    private fun isResizeable(info: ActivityInfo?): Boolean {
        if (info == null) return true
        val fixedOrientation = info.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        return !fixedOrientation
    }

    private fun versionCodeOf(packageName: String, user: UserHandle): Long = runCatching {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        pm.getPackageInfo(packageName, 0).longVersionCode
    }.getOrDefault(0L)

    override fun observeChanges(onChange: (AppChange) -> Unit): AutoCloseable {
        val callback = object : LauncherApps.Callback() {

            override fun onPackageAdded(packageName: String, user: UserHandle) =
                onChange(AppChange.PackageUpserted(packageName, user, entriesFor(packageName, user)))

            /** An update. Re-read rather than patch — the activity list itself may have changed. */
            override fun onPackageChanged(packageName: String, user: UserHandle) =
                onChange(AppChange.PackageUpserted(packageName, user, entriesFor(packageName, user)))

            override fun onPackageRemoved(packageName: String, user: UserHandle) =
                onChange(AppChange.PackageRemoved(packageName, user))

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) = onChange(
                AppChange.PackagesAvailability(packageNames.toSet(), user, available = true),
            )

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) = onChange(
                AppChange.PackagesAvailability(packageNames.toSet(), user, available = false),
            )

            override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) =
                onChange(AppChange.PackagesSuspended(packageNames.toSet(), user, suspended = true))

            override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) =
                onChange(AppChange.PackagesSuspended(packageNames.toSet(), user, suspended = false))
        }

        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        return AutoCloseable { runCatching { launcherApps.unregisterCallback(callback) } }
    }
}

/** Whether the user has granted usage access, for [usageSourceFor]. */
fun hasUsageAccess(context: Context): Boolean = runCatching {
    val appOps = context.getSystemService(android.app.AppOpsManager::class.java)
    val mode = appOps.unsafeCheckOpNoThrow(
        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName,
    )
    mode == android.app.AppOpsManager.MODE_ALLOWED
}.getOrDefault(false)

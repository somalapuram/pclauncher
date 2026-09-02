package com.somalapuram.pclauncher.core.apps

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process

/**
 * Whether this app may read `UsageStatsManager`.
 *
 * `PACKAGE_USAGE_STATS` is declared `signature|privileged|development|appop|retailDemo`, and those
 * flags are two different grant routes that have to be checked in the right order — which is why
 * this is not simply "is the app op allowed".
 */

/**
 * The framework's own rule, expressed as a decision over an app-op mode.
 *
 * `MODE_DEFAULT` is not "denied". It is the app op saying *nobody has expressed an opinion here*,
 * and the framework's answer in that case is to fall back to the permission — see
 * `UsageStatsService.BinderService.hasQueryPermission`, which this mirrors deliberately. Treating
 * `MODE_DEFAULT` as a denial makes the shell disagree with the service it is about to call: on a
 * build where the app holds `PACKAGE_USAGE_STATS` by signature or privilege, the user was never
 * sent to the Settings toggle, so the op stays at its default forever while the queries themselves
 * succeed. The shell would then rank from local counters it did not need and offer a permission
 * card for access it already has (`docs/requirements/launch/usage-access-ask.md`, requirement 5).
 *
 * [permissionGranted] is a lambda because it must not be evaluated in the `MODE_ALLOWED` case: the
 * op is then the whole answer, and asking the package manager as well is a binder call for a
 * result that cannot change the outcome.
 */
fun usageAccessGranted(opMode: Int, permissionGranted: () -> Boolean): Boolean = when (opMode) {
    AppOpsManager.MODE_ALLOWED -> true
    AppOpsManager.MODE_DEFAULT -> permissionGranted()
    else -> false
}

/** [usageAccessGranted] against the real system services. */
fun hasUsageAccess(context: Context): Boolean = runCatching {
    val appOps = context.getSystemService(AppOpsManager::class.java)
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName,
    )
    usageAccessGranted(mode) {
        context.checkSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
            PackageManager.PERMISSION_GRANTED
    }
}.getOrDefault(false)

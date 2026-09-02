package com.somalapuram.pclauncher.power

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import com.somalapuram.pclauncher.HomeActivity
import com.somalapuram.pclauncher.feature.shell.start.PowerAction
import com.somalapuram.pclauncher.feature.shell.start.PowerPrivileges
import com.somalapuram.pclauncher.feature.shell.start.isAvailable

/**
 * What the shell is actually allowed to do to this device.
 *
 * A runtime question, not a build-time one: the same APK is unprivileged in Stage A and a platform
 * app on `aosp-pc-x86_64`, where the allowlist grants both. Nothing above this knows which world it
 * is in (SRS §5.2).
 */
fun powerPrivilegesOf(context: Context): PowerPrivileges = PowerPrivileges(
    canReboot = context.holds(android.Manifest.permission.REBOOT),
    canShutDown = context.holds(SHUTDOWN),
)

private fun Context.holds(permission: String): Boolean =
    runCatching { checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED }
        .getOrDefault(false)

/**
 * Perform a footer action.
 *
 * Guarded end to end: a refused or missing action costs the action, never the home screen (GATE 4).
 * Availability is re-checked here rather than trusted from the UI, so a stale composition cannot
 * fire a privileged call.
 */
fun performPowerAction(context: Context, action: PowerAction) {
    val privileges = powerPrivilegesOf(context)
    if (!isAvailable(action, privileges)) return

    when (action) {
        PowerAction.OpenSettings -> context.launch(Settings.ACTION_SETTINGS)
        PowerAction.RestartShell -> restartShell(context)
        PowerAction.RestartDevice -> {
            runCatching { context.getSystemService(PowerManager::class.java)?.reboot(null) }
        }
        PowerAction.PowerOff -> {
            // No public API exists; this is the internal action the system's own power menu uses,
            // and it is gated on SHUTDOWN — exactly the permission checked above.
            runCatching {
                context.startActivity(
                    Intent(ACTION_REQUEST_SHUTDOWN)
                        .putExtra(EXTRA_KEY_CONFIRM, false)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}

private fun Context.launch(action: String) {
    runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/**
 * Restart pclauncher itself.
 *
 * Exits the process rather than recreating the activity: recreating rebuilds the UI but keeps every
 * singleton — the inventory repository included — so the states worth recovering from would survive
 * the recovery. The home activity is relaunched by the system by definition, which is what makes
 * this safe to do at all (GATE 4).
 */
private fun restartShell(context: Context) {
    runCatching {
        // Ask for the shell back *before* dying. Exiting alone left whatever was behind the
        // launcher in front and the desktop gone until the user pressed HOME — a home screen that
        // does not come back is precisely what GATE 4 forbids.
        //
        // Named explicitly rather than taken from the caller, because the caller may be the
        // overlay service, which has no activity of its own to relaunch.
        context.startActivity(
            Intent(context, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        (context as? Activity)?.finish()
        Runtime.getRuntime().exit(0)
    }
}

/** `Intent.ACTION_REQUEST_SHUTDOWN`, which is not public API. */
private const val ACTION_REQUEST_SHUTDOWN = "com.android.internal.intent.action.REQUEST_SHUTDOWN"
private const val EXTRA_KEY_CONFIRM = "android.intent.extra.KEY_CONFIRM"

/** `Manifest.permission.SHUTDOWN`, likewise not public. */
private const val SHUTDOWN = "android.permission.SHUTDOWN"

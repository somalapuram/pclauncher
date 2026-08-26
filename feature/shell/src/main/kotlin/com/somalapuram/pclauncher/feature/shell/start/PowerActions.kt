package com.somalapuram.pclauncher.feature.shell.start

/** What the Start menu's footer can ask for. */
enum class PowerAction { OpenSettings, RestartShell, RestartDevice, PowerOff }

/**
 * Which privileges the shell actually holds.
 *
 * Read at runtime rather than compiled in: the same APK runs unprivileged in Stage A and as a
 * platform app on `aosp-pc-x86_64`, where the privileged-permission allowlist grants both. Nothing
 * above this knows which world it is in — it asks what is held (SRS §5.2, §5.3).
 */
data class PowerPrivileges(
    val canReboot: Boolean = false,
    val canShutDown: Boolean = false,
)

/**
 * Whether an action can actually be performed.
 *
 * Restarting our own process and opening Settings need nothing. Rebooting and shutting down the
 * device are `signature|privileged` on Android — there is no public intent for either — so they are
 * available exactly when the permission has been granted.
 */
fun isAvailable(action: PowerAction, privileges: PowerPrivileges): Boolean = when (action) {
    PowerAction.OpenSettings, PowerAction.RestartShell -> true
    PowerAction.RestartDevice -> privileges.canReboot
    PowerAction.PowerOff -> privileges.canShutDown
}

/**
 * Why an action cannot be performed, or null when it can.
 *
 * Shown next to the disabled control. SRS §5.3 asks for what is unavailable *and why*, in plain
 * language — a greyed button with no explanation reads as a bug rather than a boundary.
 */
fun unavailableReason(action: PowerAction, privileges: PowerPrivileges): String? {
    if (isAvailable(action, privileges)) return null
    return "Needs platform privilege"
}

/** The label a footer control carries. */
fun labelFor(action: PowerAction): String = when (action) {
    PowerAction.OpenSettings -> "Settings"
    PowerAction.RestartShell -> "Restart shell"
    PowerAction.RestartDevice -> "Restart device"
    PowerAction.PowerOff -> "Power off"
}

/** The footer's controls, in the order they are shown. */
val PowerActionOrder: List<PowerAction> = listOf(
    PowerAction.OpenSettings,
    PowerAction.RestartShell,
    PowerAction.RestartDevice,
    PowerAction.PowerOff,
)

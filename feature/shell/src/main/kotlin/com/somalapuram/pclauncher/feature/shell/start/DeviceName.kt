package com.somalapuram.pclauncher.feature.shell.start

/**
 * The device's name, if it has one a person would recognise.
 *
 * SRS §6.4 puts the device name in the Start menu's footer, and on real hardware that is what
 * `Settings.Global.DEVICE_NAME` holds — the name the user set, the one Bluetooth and Nearby show.
 * Emulators and un-named builds return a build identifier instead (`sdk_gphone16k_x86_64`), and a
 * build identifier in the footer reads as debug text left in by mistake.
 *
 * So the slot shows a name or nothing. Null means the footer simply omits it rather than printing
 * something the user would not call their machine.
 */
fun displayableDeviceName(deviceName: String?, model: String?): String? {
    val candidate = deviceName?.takeIf { it.isNotBlank() } ?: model?.takeIf { it.isNotBlank() }
    val trimmed = candidate?.trim() ?: return null
    return if (looksLikeBuildIdentifier(trimmed)) null else trimmed
}

/**
 * Whether a name is really a build identifier.
 *
 * Deliberately conservative: it matches the shapes emulators and AOSP targets actually produce
 * rather than trying to judge what a "real" name looks like. Wrongly hiding a name costs a line of
 * furniture; wrongly showing `sdk_gphone16k_x86_64` costs the footer's credibility.
 */
fun looksLikeBuildIdentifier(name: String): Boolean {
    val lower = name.lowercase()
    return BuildIdentifierMarkers.any { lower.contains(it) }
}

private val BuildIdentifierMarkers = listOf(
    "sdk_", "sdk-", "generic", "emulator", "x86_64", "x86-64", "arm64-v8a", "aosp_",
)

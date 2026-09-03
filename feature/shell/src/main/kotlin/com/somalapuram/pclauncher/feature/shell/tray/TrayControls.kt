package com.somalapuram.pclauncher.feature.shell.tray

import kotlin.math.roundToInt

/** How loud the device is, as the tray needs to draw it. */
enum class VolumeGlyph { Muted, Low, Medium, High }

/**
 * The volume, in the terms the slider and the glyph both need.
 *
 * [max] of zero is a real answer on a device with no music stream, and every function here has to
 * survive it rather than divide by it.
 */
data class VolumeState(val level: Int = 0, val max: Int = 0) {
    val isKnown: Boolean get() = max > 0
}

/**
 * Which glyph a volume draws as.
 *
 * Thresholds rather than a continuous fill because the speaker's waves are discrete: there is no
 * half-wave to draw, and rounding one into existence makes the icon flicker between two forms
 * around the boundary.
 */
fun volumeGlyph(state: VolumeState): VolumeGlyph {
    if (!state.isKnown || state.level <= 0) return VolumeGlyph.Muted
    return when (fractionOf(state)) {
        in 0f..0.33f -> VolumeGlyph.Low
        in 0.33f..0.66f -> VolumeGlyph.Medium
        else -> VolumeGlyph.High
    }
}

/** Where the slider's handle sits, 0..1. An unknown volume reads as silent rather than as full. */
fun fractionOf(state: VolumeState): Float {
    if (!state.isKnown) return 0f
    return (state.level.toFloat() / state.max).coerceIn(0f, 1f)
}

/**
 * The stream index a slider position means.
 *
 * Rounded, not truncated: truncating makes the top of the slider unreachable — the handle sits
 * against the end and the volume is still one step short.
 */
fun streamIndexFor(fraction: Float, max: Int): Int {
    if (max <= 0) return 0
    return (fraction.coerceIn(0f, 1f) * max).roundToInt().coerceIn(0, max)
}

/** How full the battery glyph is drawn, 0..1. Unknown draws empty rather than guessing. */
fun batteryFill(battery: BatteryState): Float = when (battery) {
    is BatteryState.Known -> (battery.percent / 100f).coerceIn(0f, 1f)
    BatteryState.Unknown -> 0f
}

/** Signal arcs to fill, 0..3. We can tell connected from not; we cannot tell strength unprivileged. */
fun wifiBars(state: ConnectionState): Int = when (state) {
    ConnectionState.On -> 3
    ConnectionState.Off, ConnectionState.Unknown -> 0
}

/**
 * Whether a tile draws filled.
 *
 * A filled tile is a claim that the thing is *on*, so only a real on-state earns it. Unknown reads
 * as off — claiming a radio is connected because we could not tell is the one answer that is worse
 * than admitting ignorance. Battery is never filled at any level: it opens a screen and reports a
 * number, and a filled battery tile would advertise a switch that does not exist
 * (quick-settings-surface.md requirements 3-5).
 */
fun tileIsOn(indicator: TrayIndicator, state: TrayState): Boolean = when (indicator) {
    TrayIndicator.Wifi -> state.wifi == ConnectionState.On
    TrayIndicator.Bluetooth -> state.bluetooth == ConnectionState.On
    TrayIndicator.Battery -> false
    TrayIndicator.Volume -> state.volume.isKnown && state.volume.level > 0
}

/** The level as a whole percentage, for the label beside the slider. */
fun volumePercent(state: VolumeState): Int = Math.round(fractionOf(state) * 100f)

/** Which indicator was clicked. All of them open the same popover; the type is for the rows. */
enum class TrayIndicator { Bluetooth, Wifi, Battery, Volume }

/**
 * What a row in the popover does when it is activated.
 *
 * A closed set rather than a lambda per row, so the routing is a value that can be asserted on —
 * the alternative is testing the platform's intent dispatch, which a unit test cannot see.
 */
sealed interface TrayAction {
    /** The platform's own Wi-Fi panel. We cannot toggle the radio; this can. */
    data object OpenWifiPanel : TrayAction

    /** Ask the system to turn Bluetooth on. It shows its own confirmation. */
    data object EnableBluetooth : TrayAction

    data object OpenBluetoothSettings : TrayAction
    data object OpenBatterySettings : TrayAction

    /** The one thing we change ourselves. */
    data class SetVolume(val level: Int) : TrayAction
}

/**
 * Which action a row resolves to, given what the tray currently knows.
 *
 * Bluetooth is the only one that branches: offering "settings" for a radio that is off buries the
 * one thing the user wanted behind another screen, and offering "enable" for one already on does
 * nothing visible.
 */
fun bluetoothAction(state: ConnectionState): TrayAction = when (state) {
    ConnectionState.On -> TrayAction.OpenBluetoothSettings
    // Unknown included: asking to enable something already enabled is harmless, and the system
    // dialog tells the user the truth either way.
    ConnectionState.Off, ConnectionState.Unknown -> TrayAction.EnableBluetooth
}

/**
 * Where "turn Bluetooth on" actually goes.
 *
 * `ACTION_REQUEST_ENABLE` has needed `BLUETOOTH_CONNECT` since API 31, and without it the intent
 * fails silently — the click looks like it worked and nothing happens. Stage A does not hold that
 * permission (system-tray.md declined to ask for it), so the honest answer there is the Bluetooth
 * settings screen, where the toggle is one tap away and always present.
 *
 * Kept as a decision rather than a try/catch because a launch that starts an activity which
 * immediately finishes itself does not throw, so there is nothing to catch.
 */
fun bluetoothEnableAction(hasConnectPermission: Boolean): TrayAction =
    if (hasConnectPermission) TrayAction.EnableBluetooth else TrayAction.OpenBluetoothSettings

/** The action a slider position means, ready to hand to whatever can perform it. */
fun volumeAction(fraction: Float, max: Int): TrayAction =
    TrayAction.SetVolume(streamIndexFor(fraction, max))

/**
 * Whether acting on a row should close the quick-settings panel.
 *
 * A hand-off leaves for another screen, so the panel has done its job. The volume slider is the
 * exception: closing it on every drag step would make the control unusable.
 *
 * A function rather than a condition at the call site, because the panel is now drawn by two hosts
 * — the home activity and the overlay's menu window — and a rule written twice is a rule that will
 * eventually differ (tray-popover-host.md).
 */
fun dismissesPanel(action: TrayAction): Boolean = action !is TrayAction.SetVolume

package com.somalapuram.pclauncher.feature.shell.tray

/**
 * Everything the tray shows, in one value.
 *
 * One object rather than four independent subscriptions, so the indicators cannot disagree about
 * when they were last updated and there is a single place to see what the tray knows.
 */
data class TrayState(
    val timeText: String = "",
    val battery: BatteryState = BatteryState.Unknown,
    val wifi: ConnectionState = ConnectionState.Unknown,
    val bluetooth: ConnectionState = ConnectionState.Unknown,
)

/**
 * A value that could not be read shows as [Unknown] rather than blanking the tray or guessing —
 * "off" and "we could not tell" are different things and only one of them is actionable.
 */
enum class ConnectionState { On, Off, Unknown }

sealed interface BatteryState {
    data class Known(val percent: Int, val charging: Boolean) : BatteryState
    data object Unknown : BatteryState
}

/**
 * Battery percentage from the raw broadcast extras.
 *
 * `scale` is not always 100 — the broadcast reports level out of scale, and assuming 100 quietly
 * misreports on any device that says otherwise.
 */
fun batteryPercent(level: Int, scale: Int): Int? {
    if (level < 0 || scale <= 0) return null
    return ((level * 100f) / scale).toInt().coerceIn(0, 100)
}

/** Charging covers AC, USB and wireless — the user cares that it is going up, not how. */
fun isCharging(status: Int, plugged: Int): Boolean =
    status == BATTERY_STATUS_CHARGING || status == BATTERY_STATUS_FULL || plugged != 0

const val BATTERY_STATUS_CHARGING = 2
const val BATTERY_STATUS_FULL = 5

/** Which glyph an on/off/unknown state should draw as. Kept out of the composable so it is testable. */
fun connectionGlyph(state: ConnectionState, on: String, off: String, unknown: String): String =
    when (state) {
        ConnectionState.On -> on
        ConnectionState.Off -> off
        ConnectionState.Unknown -> unknown
    }

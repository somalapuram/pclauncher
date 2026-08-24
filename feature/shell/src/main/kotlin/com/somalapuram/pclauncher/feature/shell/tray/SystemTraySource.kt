package com.somalapuram.pclauncher.feature.shell.tray

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.Settings
import android.text.format.DateFormat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date

/**
 * The tray's data, pushed.
 *
 * SRS §12 requires idle CPU at ~0%, and a tray is exactly the thing that tempts a one-second timer.
 * Android broadcasts every value here: `ACTION_TIME_TICK` fires each minute, `ACTION_BATTERY_CHANGED`
 * is **sticky** so the first read is already correct, and connectivity has a callback. Nothing in
 * this file loops.
 */
class SystemTraySource(private val context: Context) {

    fun trayState(): Flow<TrayState> = callbackFlow {
        var state = TrayState(timeText = formatNow())

        fun push(update: (TrayState) -> TrayState) {
            state = update(state)
            trySend(state)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_TIME_TICK,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    -> push { it.copy(timeText = formatNow()) }

                    Intent.ACTION_BATTERY_CHANGED -> push { it.copy(battery = intent.toBattery()) }

                    BLUETOOTH_STATE_CHANGED -> push { it.copy(bluetooth = readBluetooth()) }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(BLUETOOTH_STATE_CHANGED)
        }

        // Not exported: these are system broadcasts, and declaring so is required on API 34+.
        val sticky = runCatching {
            androidx.core.content.ContextCompat.registerReceiver(
                context, receiver, filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }.getOrNull()

        // The sticky battery broadcast comes back from registration itself, so the first frame is
        // already correct rather than empty until something changes.
        sticky?.let { push { s -> s.copy(battery = it.toBattery()) } }
        push { it.copy(bluetooth = readBluetooth()) }
        push { it.copy(volume = readVolume()) }

        // Volume changed anywhere — the hardware keys, another app, the system panel — arrives
        // here. An observer rather than a poll, for the same reason nothing else in this file
        // loops (SRS §12).
        val volumeObserver = object : android.database.ContentObserver(
            android.os.Handler(android.os.Looper.getMainLooper()),
        ) {
            override fun onChange(selfChange: Boolean) = push { it.copy(volume = readVolume()) }
        }
        runCatching {
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI, true, volumeObserver,
            )
        }

        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = push { it.copy(wifi = ConnectionState.On) }
            override fun onLost(network: Network) = push { it.copy(wifi = ConnectionState.Off) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                if (wifi) push { it.copy(wifi = ConnectionState.On) }
            }
        }
        runCatching { connectivity?.registerDefaultNetworkCallback(networkCallback) }

        trySend(state)

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
            runCatching { connectivity?.unregisterNetworkCallback(networkCallback) }
            runCatching { context.contentResolver.unregisterContentObserver(volumeObserver) }
        }
    }

    /** The music stream: what "volume" means on a desktop, and the one an app may change. */
    private fun readVolume(): VolumeState = runCatching {
        val audio = context.getSystemService(android.media.AudioManager::class.java)
            ?: return@runCatching VolumeState()
        VolumeState(
            level = audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC),
            max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC),
        )
    }.getOrDefault(VolumeState())

    private fun formatNow(): String =
        DateFormat.getTimeFormat(context).format(Date())

    /**
     * Bluetooth from the system setting, not `BluetoothAdapter`.
     *
     * `isEnabled()` has required `BLUETOOTH_CONNECT` at runtime since API 31, and prompting for a
     * permission that says the app wants to "find, connect to, and determine the relative position
     * of nearby devices" — so we can draw a glyph — is not a trade worth making. The setting says
     * exactly what the indicator needs.
     */
    private fun readBluetooth(): ConnectionState = runCatching {
        val on = Settings.Global.getInt(context.contentResolver, "bluetooth_on", -1)
        when (on) {
            1 -> ConnectionState.On
            0 -> ConnectionState.Off
            else -> ConnectionState.Unknown
        }
    }.getOrDefault(ConnectionState.Unknown)

    private companion object {
        const val BLUETOOTH_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED"
    }
}

private fun Intent.toBattery(): BatteryState {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val percent = batteryPercent(level, scale) ?: return BatteryState.Unknown
    return BatteryState.Known(
        percent = percent,
        charging = isCharging(
            status = getIntExtra(BatteryManager.EXTRA_STATUS, -1),
            plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
        ),
    )
}

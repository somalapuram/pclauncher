package com.somalapuram.pclauncher.feature.shell.tray

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings

/**
 * Performs what a tray row resolved to.
 *
 * An interface so the routing can be tested without a device: `TrayAction` says *what* should
 * happen and a fake records it, while this is the only place that knows how.
 */
interface TrayActionPerformer {
    fun perform(action: TrayAction)
}

/**
 * The real one.
 *
 * Every launch is guarded. A device with no activity for one of these intents — a stripped AOSP
 * image is exactly that — must leave the shell standing (GATE 4), so a missing handler is a click
 * that does nothing rather than a home screen that dies.
 */
class SystemTrayActions(private val context: Context) : TrayActionPerformer {

    override fun perform(action: TrayAction) {
        when (action) {
            TrayAction.OpenWifiPanel -> launch(Settings.Panel.ACTION_WIFI)
            TrayAction.EnableBluetooth -> when (bluetoothEnableAction(hasConnectPermission())) {
                TrayAction.EnableBluetooth -> launch(ACTION_REQUEST_ENABLE)
                // Without the permission the enable dialog never appears, so send the user
                // somewhere the toggle actually exists rather than nowhere at all.
                else -> launch(Settings.ACTION_BLUETOOTH_SETTINGS)
            }
            TrayAction.OpenBluetoothSettings -> launch(Settings.ACTION_BLUETOOTH_SETTINGS)
            TrayAction.OpenBatterySettings -> launch(Intent.ACTION_POWER_USAGE_SUMMARY)
            is TrayAction.SetVolume -> setVolume(action.level)
        }
    }

    private fun hasConnectPermission(): Boolean =
        context.checkSelfPermission(BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun launch(action: String) {
        // NEW_TASK because the shell is the home activity and a settings screen belongs in its own
        // task rather than on top of the desktop's. CLEAR_TASK because without it a second hand-off
        // merely brings the existing Settings task forward at whatever page it was left on — ask
        // for Bluetooth after visiting Battery and Battery is what appears.
        val intent = Intent(action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        runCatching { context.startActivity(intent) }
    }

    /**
     * The one thing we change ourselves.
     *
     * `FLAG_SHOW_UI` is deliberately absent: the popover already shows the level, and the system's
     * own volume panel appearing on top of it would be two controls for one value. The write can
     * still be refused under a Do Not Disturb policy, which is why it is guarded — the slider then
     * springs back to whatever the device actually did.
     */
    private fun setVolume(level: Int) {
        runCatching {
            val audio = context.getSystemService(AudioManager::class.java) ?: return
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
        }
    }

    private companion object {
        /** `BluetoothAdapter.ACTION_REQUEST_ENABLE`, named here so this file needs no adapter. */
        const val ACTION_REQUEST_ENABLE = "android.bluetooth.adapter.action.REQUEST_ENABLE"
        const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"
    }
}

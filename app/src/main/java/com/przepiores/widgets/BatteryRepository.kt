package com.przepiores.widgets

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager

enum class DeviceKind(val icon: Int) {
    PHONE(R.drawable.ic_phone),
    HEADPHONES(R.drawable.ic_headphones),
    WATCH(R.drawable.ic_watch),
    OTHER(R.drawable.ic_bluetooth),
}

data class BatteryInfo(
    val label: String,
    val percent: Int?,
    val kind: DeviceKind,
    val charging: Boolean = false,
    val remainingMs: Long? = null,
)

object BatteryRepository {

    fun phone(context: Context): BatteryInfo {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else null
        return BatteryInfo(
            "Telefon", pct, DeviceKind.PHONE, charging,
            TimeEstimator.remainingMs(context, pct, charging),
        )
    }

    /**
     * Poziom baterii podłączonych urządzeń BT (słuchawki, zegarki itp.). Publiczne API tego nie ma
     * (BluetoothDevice.getBatteryLevel jest @hide), więc używamy refleksji;
     * działa na większości telefonów, w tym Samsungach z Galaxy Buds.
     */
    fun bluetoothDevices(context: Context): List<BatteryInfo> {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) return emptyList()
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return emptyList()
        return try {
            adapter.bondedDevices.orEmpty().filter { isConnected(it) }.mapNotNull { d ->
                val level = batteryLevel(d)
                if (level in 0..100) BatteryInfo(d.name ?: d.address, level, kindOf(d)) else null
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    private fun kindOf(d: BluetoothDevice): DeviceKind =
        when (d.bluetoothClass?.majorDeviceClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> DeviceKind.HEADPHONES
            BluetoothClass.Device.Major.WEARABLE -> DeviceKind.WATCH
            else -> DeviceKind.OTHER
        }

    private fun isConnected(d: BluetoothDevice): Boolean = try {
        BluetoothDevice::class.java.getMethod("isConnected").invoke(d) as Boolean
    } catch (_: Throwable) {
        false
    }

    private fun batteryLevel(d: BluetoothDevice): Int = try {
        BluetoothDevice::class.java.getMethod("getBatteryLevel").invoke(d) as Int
    } catch (_: Throwable) {
        -1
    }
}

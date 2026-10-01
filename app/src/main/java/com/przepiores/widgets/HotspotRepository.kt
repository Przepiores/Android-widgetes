package com.przepiores.widgets

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Android nie pozwala zwykłym aplikacjom przełączać hotspotu, więc widget tylko
 * pokazuje jego stan i otwiera ustawienia. Stan też nie ma publicznego API:
 * najpierw próbujemy ukrytej WifiManager.isWifiApEnabled, potem szukamy
 * aktywnego interfejsu hotspotu (swlan0 na Samsungu, ap0/softap0 gdzie indziej).
 */
object HotspotRepository {
    private val AP_INTERFACES = listOf("swlan", "softap", "ap")

    /** true/false, albo null gdy stanu nie da się odczytać. */
    fun isEnabled(context: Context): Boolean? = viaWifiManager(context) ?: viaInterfaces()

    private fun viaWifiManager(context: Context): Boolean? = try {
        val wm = context.getSystemService(WifiManager::class.java)
        wm.javaClass.getMethod("isWifiApEnabled").invoke(wm) as Boolean
    } catch (_: Throwable) {
        null
    }

    private fun viaInterfaces(): Boolean? = try {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any { ni ->
            ni.isUp && AP_INTERFACES.any { ni.name.startsWith(it) } &&
                ni.inetAddresses.toList().any { it is Inet4Address }
        }
    } catch (_: Throwable) {
        null
    }

    /** Otwiera ekran hotspotu; jeśli telefon go nie ma, ogólne ustawienia sieci. */
    fun openSettings(context: Context) {
        val candidates = listOf(
            Intent().setClassName("com.android.settings", "com.android.settings.Settings\$WifiTetherSettingsActivity"),
            Intent("android.settings.TETHER_SETTINGS"),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
        )
        for (intent in candidates) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
    }
}

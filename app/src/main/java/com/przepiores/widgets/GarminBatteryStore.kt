package com.przepiores.widgets

import android.content.Context

/**
 * Garmin nie udostępnia baterii zegarka żadnym publicznym API w telefonie.
 * Jedyna oficjalna droga: mała aplikacja Connect IQ na zegarku wysyła
 * System.getSystemStats().battery do tej aplikacji (Connect IQ Mobile SDK).
 * Ten magazyn przechowuje ostatnią odebraną wartość; odbiornik SDK wywoła write().
 */
object GarminBatteryStore {
    private const val PREFS = "garmin"
    private const val KEY_PCT = "pct"
    private const val KEY_TIME = "time"

    fun write(context: Context, percent: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_PCT, percent.coerceIn(0, 100))
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
    }

    fun read(context: Context): BatteryInfo {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pct = if (p.contains(KEY_PCT)) p.getInt(KEY_PCT, 0) else null
        return BatteryInfo("Garmin", pct)
    }
}

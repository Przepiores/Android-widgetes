package com.przepiores.widgets

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

data class Weather(val tempC: Int, val icon: Int, val label: String)

/**
 * Pogoda z Open-Meteo (bez klucza API). Wynik i ostatnia lokalizacja są w
 * SharedPreferences: widget czyta tylko pamięć podręczną, a sieć odpytuje
 * WidgetRefresher najwyżej co 30 minut.
 */
object WeatherRepository {
    private const val PREFS = "weather"
    private const val MAX_AGE_MS = 30 * 60 * 1000L

    fun cached(context: Context): Weather? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("temp")) return null
        return describe(p.getFloat("temp", 0f), p.getInt("code", 0), p.getBoolean("day", true))
    }

    /** Zapamiętuje lokalizację; wołane z aplikacji (na pierwszym planie) i przy odświeżaniu. */
    fun rememberLocation(context: Context) {
        val loc = lastKnown(context) ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("lat", loc.latitude.toFloat())
            .putFloat("lon", loc.longitude.toFloat())
            .apply()
    }

    suspend fun refreshIfStale(context: Context, force: Boolean = false) = withContext(Dispatchers.IO) {
        rememberLocation(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("lat")) return@withContext
        if (!force && System.currentTimeMillis() - p.getLong("time", 0) < MAX_AGE_MS) return@withContext
        try {
            val url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code,is_day",
                p.getFloat("lat", 0f), p.getFloat("lon", 0f),
            )
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val cur = JSONObject(body).getJSONObject("current")
            p.edit()
                .putFloat("temp", cur.getDouble("temperature_2m").toFloat())
                .putInt("code", cur.getInt("weather_code"))
                .putBoolean("day", cur.getInt("is_day") == 1)
                .putLong("time", System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {
            // Brak sieci: zostaje poprzedni wynik.
        }
    }

    private fun lastKnown(context: Context): Location? {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        return try {
            listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
        } catch (_: SecurityException) {
            null
        }
    }

    /** Kody pogody WMO używane przez Open-Meteo. */
    private fun describe(temp: Float, code: Int, day: Boolean): Weather {
        val (icon, label) = when (code) {
            0 -> (if (day) R.drawable.ic_w_sun else R.drawable.ic_w_moon) to "bezchmurnie"
            1, 2 -> R.drawable.ic_w_partly to "małe zachmurzenie"
            3 -> R.drawable.ic_w_cloud to "pochmurno"
            45, 48 -> R.drawable.ic_w_fog to "mgła"
            in 51..67, in 80..82 -> R.drawable.ic_w_rain to "deszcz"
            in 71..77, 85, 86 -> R.drawable.ic_w_snow to "śnieg"
            in 95..99 -> R.drawable.ic_bolt to "burza"
            else -> R.drawable.ic_w_cloud to "pochmurno"
        }
        return Weather(temp.roundToInt(), icon, label)
    }
}

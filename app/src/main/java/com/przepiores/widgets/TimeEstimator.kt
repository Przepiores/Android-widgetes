package com.przepiores.widgets

import android.content.Context
import android.os.BatteryManager

/**
 * Czas do naładowania: systemowe BatteryManager.computeChargeTimeRemaining().
 * Czas do rozładowania: publicznego API brak, więc liczymy z próbek
 * (procent, czas) zbieranych przy każdym odświeżeniu widgetu.
 */
object TimeEstimator {
    private const val PREFS = "samples"
    private const val MAX_SAMPLES = 24
    private const val MIN_SPAN_MS = 20 * 60 * 1000L

    /** Zwraca pozostały czas w ms lub null, gdy brak wiarygodnej oceny. */
    fun remainingMs(context: Context, pct: Int?, charging: Boolean): Long? {
        if (pct == null) return null
        val bm = context.getSystemService(BatteryManager::class.java)
        if (charging) {
            val ms = bm.computeChargeTimeRemaining()
            return if (ms > 0) ms else null
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        var samples = prefs.getString("s", "").orEmpty().split(';').filter { it.isNotBlank() }
            .mapNotNull { s -> s.split(',').takeIf { it.size == 2 }?.let { it[0].toLong() to it[1].toInt() } }
        // Po ładowaniu poziom rośnie; zaczynamy serię od nowa.
        if (samples.isNotEmpty() && pct > samples.last().second) samples = emptyList()
        samples = (samples + (now to pct)).takeLast(MAX_SAMPLES)
        prefs.edit().putString("s", samples.joinToString(";") { "${it.first},${it.second}" }).apply()

        val first = samples.first()
        val span = now - first.first
        val drop = first.second - pct
        if (span < MIN_SPAN_MS || drop <= 0) return null
        return (pct.toDouble() / drop * span).toLong()
    }

    fun format(ms: Long): String {
        val min = ms / 60000
        return if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
    }
}

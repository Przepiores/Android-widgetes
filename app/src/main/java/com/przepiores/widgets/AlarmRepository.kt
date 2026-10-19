package com.przepiores.widgets

import android.app.AlarmManager
import android.content.Context
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

object AlarmRepository {
    /** Krótki tekst najbliższego budzika: sama godzina w ciągu doby, dalej z dniem tygodnia ("śr. 07:00"). */
    fun next(context: Context): String? {
        val t = context.getSystemService(AlarmManager::class.java).nextAlarmClock?.triggerTime
            ?: return null
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(t))
        val withinDay = t - System.currentTimeMillis() < 24 * 60 * 60 * 1000L
        return if (withinDay) time else "${SimpleDateFormat("EEE").format(Date(t))} $time"
    }
}

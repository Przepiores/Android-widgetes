package com.przepiores.widgets

import android.app.AlarmManager
import android.content.Context
import java.text.DateFormat
import java.util.Date

object AlarmRepository {
    /** Tekst z godziną najbliższego budzika (lub z dniem, gdy nie jest dzisiaj), albo null. */
    fun next(context: Context): String? {
        val t = context.getSystemService(AlarmManager::class.java).nextAlarmClock?.triggerTime
            ?: return null
        val day = DateFormat.getDateInstance(DateFormat.SHORT)
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(t))
        val today = day.format(Date(t)) == day.format(Date())
        return if (today) time else "${day.format(Date(t))} $time"
    }
}

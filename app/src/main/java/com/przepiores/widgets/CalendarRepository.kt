package com.przepiores.widgets

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import java.text.DateFormat
import java.util.Date

data class NextEvent(val title: String, val whenText: String)

object CalendarRepository {
    fun next(context: Context): NextEvent? {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString()).appendPath((now + 7 * 24 * 3600 * 1000L).toString()).build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
        )
        return try {
            context.contentResolver.query(
                uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val allDay = c.getInt(2) == 1
                    val begin = c.getLong(1)
                    if (allDay) continue
                    val fmt = DateFormat.getTimeInstance(DateFormat.SHORT)
                    val day = DateFormat.getDateInstance(DateFormat.SHORT)
                    val sameDay = day.format(Date(begin)) == day.format(Date(now))
                    val text = if (sameDay) fmt.format(Date(begin))
                    else "${day.format(Date(begin))} ${fmt.format(Date(begin))}"
                    return NextEvent(c.getString(0)?.takeIf { it.isNotBlank() } ?: "(bez tytułu)", text)
                }
                null
            }
        } catch (_: SecurityException) {
            null
        }
    }
}

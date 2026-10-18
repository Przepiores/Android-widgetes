package com.przepiores.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

object WidgetPreviews {
    /**
     * Publikuje podgląd widgetu na liście widgetów launchera. System ogranicza
     * to do ok. dwóch wywołań na godzinę, więc błąd limitu po prostu pomijamy.
     */
    suspend fun publish(context: Context) {
        try {
            GlanceAppWidgetManager(context).setWidgetPreviews(BatteryWidgetReceiver::class)
        } catch (_: Exception) {
        }
    }
}

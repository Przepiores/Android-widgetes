package com.przepiores.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

object WidgetRefresher {
    /**
     * Zmienia znacznik w stanie każdego widgetu i go aktualizuje. Sam update()
     * nie wystarczy, bo przy działającej sesji Glance nie wczytałby danych od nowa.
     */
    suspend fun refreshAll(context: Context) {
        val widget = BatteryWidget()
        GlanceAppWidgetManager(context).getGlanceIds(BatteryWidget::class.java).forEach { id ->
            updateAppWidgetState(context, id) { it[WidgetStyle.TICK] = System.currentTimeMillis() }
            widget.update(context, id)
        }
    }
}

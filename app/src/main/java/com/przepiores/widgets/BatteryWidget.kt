package com.przepiores.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class BatteryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = buildList {
            add(BatteryRepository.phone(context))
            addAll(BatteryRepository.headphones(context))
        }
        val event = CalendarRepository.next(context)
        provideContent {
            // GlanceTheme sam bierze dynamiczne kolory Material You (z tapety) na Androidzie 12+.
            GlanceTheme { Content(items, event) }
        }
    }

    @Composable
    private fun Content(items: List<BatteryInfo>, event: NextEvent?) {
        val c = GlanceTheme.colors
        Column(
            modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
                .background(c.widgetBackground).cornerRadius(24.dp).padding(12.dp),
        ) {
            items.forEach { BatteryRow(it) }
            Spacer(GlanceModifier.defaultWeight().height(4.dp))
            Text(
                text = if (event != null) "📅 ${event.whenText}  ${event.title}" else "📅 Brak wydarzeń lub brak uprawnień",
                modifier = GlanceModifier.fillMaxWidth().background(c.secondaryContainer)
                    .cornerRadius(16.dp).padding(vertical = 8.dp, horizontal = 10.dp),
                maxLines = 1,
                style = TextStyle(color = c.onSecondaryContainer, fontSize = 13.sp),
            )
        }
    }

    @Composable
    private fun BatteryRow(info: BatteryInfo) {
        val c = GlanceTheme.colors
        Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info.label,
                    modifier = GlanceModifier.width(90.dp),
                    style = TextStyle(color = c.onSurface, fontSize = 13.sp),
                )
                LinearProgressIndicator(
                    progress = (info.percent ?: 0) / 100f,
                    modifier = GlanceModifier.defaultWeight(),
                    color = c.primary,
                    backgroundColor = c.secondaryContainer,
                )
                Text(
                    text = info.percent?.let { "$it%${if (info.charging) " ⚡" else ""}" } ?: "—",
                    modifier = GlanceModifier.width(52.dp),
                    style = TextStyle(
                        color = c.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        textAlign = androidx.glance.text.TextAlign.End,
                    ),
                )
            }
            info.remainingMs?.let {
                Text(
                    text = (if (info.charging) "Pełna za " else "Wystarczy na ") + TimeEstimator.format(it),
                    style = TextStyle(color = c.onSurfaceVariant, fontSize = 11.sp),
                )
            }
        }
    }
}

package com.przepiores.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.action.clickable
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
            add(BatteryRepository.garmin(context))
        }
        val mode = ModeController.current(context)
        provideContent {
            // GlanceTheme sam bierze dynamiczne kolory Material You (z tapety) na Androidzie 12+.
            GlanceTheme { Content(items, mode) }
        }
    }

    @Composable
    private fun Content(items: List<BatteryInfo>, mode: Mode) {
        val c = GlanceTheme.colors
        Column(
            modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
                .background(c.widgetBackground).cornerRadius(24.dp).padding(12.dp),
        ) {
            items.forEach { BatteryRow(it) }
            Spacer(GlanceModifier.defaultWeight().height(4.dp))
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Mode.entries.forEach { m ->
                    val active = m == mode
                    Text(
                        text = m.label,
                        modifier = GlanceModifier.defaultWeight()
                            .background(if (active) c.primary else c.secondaryContainer)
                            .cornerRadius(16.dp).padding(vertical = 8.dp, horizontal = 4.dp)
                            .clickable(actionRunCallback<SetModeAction>(actionParametersOf(MODE_KEY to m.name))),
                        style = TextStyle(
                            color = if (active) c.onPrimary else c.onSecondaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.glance.text.TextAlign.Center,
                        ),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                }
            }
        }
    }

    @Composable
    private fun BatteryRow(info: BatteryInfo) {
        val c = GlanceTheme.colors
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
    }

    companion object {
        val MODE_KEY = ActionParameters.Key<String>("mode")
    }
}

class SetModeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mode = parameters[BatteryWidget.MODE_KEY]?.let { Mode.valueOf(it) } ?: return
        ModeController.set(context, mode)
        BatteryWidget().updateAll(context)
    }
}

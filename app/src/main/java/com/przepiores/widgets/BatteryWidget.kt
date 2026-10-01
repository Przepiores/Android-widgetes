package com.przepiores.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

private val SMALL = DpSize(110.dp, 110.dp)
private val MEDIUM = DpSize(250.dp, 110.dp)
private val LARGE = DpSize(250.dp, 220.dp)

private data class WidgetData(
    val batteries: List<BatteryInfo>,
    val event: NextEvent?,
    val alarm: String?,
)

class BatteryWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData(
            batteries = listOf(BatteryRepository.phone(context)) + BatteryRepository.headphones(context),
            event = CalendarRepository.next(context),
            alarm = AlarmRepository.next(context),
        )
        provideContent {
            // GlanceTheme bierze dynamiczne kolory Material You (z tapety) na Androidzie 12+.
            GlanceTheme { Content(data) }
        }
    }

    @Composable
    private fun Content(d: WidgetData) {
        val c = GlanceTheme.colors
        val size = LocalSize.current
        Column(
            modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
                .background(c.widgetBackground).cornerRadius(24.dp).padding(12.dp),
        ) {
            when {
                size.width < MEDIUM.width -> SmallBody(d.batteries.first())
                size.height < LARGE.height -> {
                    d.batteries.take(2).forEach { BatteryRow(it) }
                    Spacer(GlanceModifier.defaultWeight().height(2.dp))
                    InfoChip(d.alarm?.let { "⏰ $it" } ?: "⏰ Brak budzika")
                }
                else -> {
                    d.batteries.forEach { BatteryRow(it) }
                    Spacer(GlanceModifier.defaultWeight().height(4.dp))
                    InfoChip(
                        d.event?.let { "📅 ${it.whenText}  ${it.title}" }
                            ?: "📅 Brak wydarzeń lub brak uprawnień",
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    InfoChip(d.alarm?.let { "⏰ $it" } ?: "⏰ Brak budzika")
                }
            }
        }
    }

    @Composable
    private fun SmallBody(b: BatteryInfo) {
        val c = GlanceTheme.colors
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = b.percent?.let { "$it%" } ?: "—",
                style = TextStyle(color = c.primary, fontSize = 30.sp, fontWeight = FontWeight.Bold),
            )
            b.remainingMs?.let {
                Text(
                    text = TimeEstimator.format(it),
                    style = TextStyle(color = c.onSurfaceVariant, fontSize = 12.sp),
                )
            }
        }
    }

    @Composable
    private fun InfoChip(text: String) {
        val c = GlanceTheme.colors
        Text(
            text = text,
            modifier = GlanceModifier.fillMaxWidth().background(c.secondaryContainer)
                .cornerRadius(16.dp).padding(vertical = 8.dp, horizontal = 10.dp),
            maxLines = 1,
            style = TextStyle(color = c.onSecondaryContainer, fontSize = 13.sp),
        )
    }

    @Composable
    private fun BatteryRow(info: BatteryInfo) {
        val c = GlanceTheme.colors
        Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info.label,
                    modifier = GlanceModifier.width(90.dp),
                    maxLines = 1,
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
                        textAlign = TextAlign.End,
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

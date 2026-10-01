package com.przepiores.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

private val SMALL = DpSize(110.dp, 110.dp)
private val MEDIUM = DpSize(250.dp, 110.dp)
private val LARGE = DpSize(250.dp, 220.dp)

private const val LOW_BATTERY = 20

data class WidgetData(
    val batteries: List<BatteryInfo>,
    val event: NextEvent?,
    val alarm: String?,
) {
    companion object {
        fun load(context: Context) = WidgetData(
            batteries = listOf(BatteryRepository.phone(context)) +
                BatteryRepository.bluetoothDevices(context),
            event = CalendarRepository.next(context),
            alarm = AlarmRepository.next(context),
        )
    }
}

class BatteryWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val style = WidgetStyle.from(prefs)
            val data = remember(prefs[WidgetStyle.TICK]) { WidgetData.load(context) }
            // GlanceTheme bierze dynamiczne kolory Material You (z tapety).
            GlanceTheme { Content(data, style) }
        }
    }

    @Composable
    private fun Content(d: WidgetData, s: WidgetStyle) {
        val size = LocalSize.current
        TintedBox(
            color = GlanceTheme.colors.widgetBackground,
            opacity = s.opacity,
            corner = s.cornerDp.dp,
            modifier = GlanceModifier.fillMaxSize().appWidgetBackground(),
        ) {
            Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
                when {
                    size.width < MEDIUM.width -> SmallBody(d.batteries.first(), s)
                    size.height < LARGE.height -> {
                        RingsRow(d.batteries.take(3), s, ring = 44.dp)
                        Spacer(GlanceModifier.defaultWeight())
                        InfoLine(d, s, withAlarm = true)
                    }
                    else -> {
                        RingsRow(d.batteries.take(4), s, ring = 60.dp)
                        Spacer(GlanceModifier.height(6.dp))
                        InfoLine(d, s, withAlarm = false)
                        Spacer(GlanceModifier.defaultWeight())
                        Chip(
                            R.drawable.ic_event,
                            d.event?.let { "${it.whenText}  ${it.title}" } ?: "brak wydarzeń",
                            s,
                        )
                        Spacer(GlanceModifier.height(6.dp))
                        Chip(R.drawable.ic_alarm, d.alarm ?: "brak budzika", s)
                    }
                }
            }
        }
    }

    @Composable
    private fun SmallBody(b: BatteryInfo, s: WidgetStyle) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Ring(b, s, ring = 48.dp)
            b.remainingMs?.let {
                Text(text = TimeEstimator.format(it), style = mono(s, 11f, GlanceTheme.colors.onSurfaceVariant))
            }
        }
    }

    @Composable
    private fun RingsRow(items: List<BatteryInfo>, s: WidgetStyle, ring: Dp) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { b ->
                Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                    Ring(b, s, ring)
                }
            }
        }
    }

    /** Pierścień z ikoną urządzenia w środku i procentem pod spodem. */
    @Composable
    private fun Ring(b: BatteryInfo, s: WidgetStyle, ring: Dp) {
        val c = GlanceTheme.colors
        val low = b.percent != null && b.percent < LOW_BATTERY && !b.charging
        val accent = if (low) c.error else c.primary
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = GlanceModifier.size(ring), contentAlignment = Alignment.Center) {
                Image(
                    provider = ImageProvider(RingRenderer.arc(100, s.ring.fraction)),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(c.secondaryContainer),
                )
                Image(
                    provider = ImageProvider(RingRenderer.arc(b.percent ?: 0, s.ring.fraction)),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(accent),
                )
                Image(
                    provider = ImageProvider(b.kind.icon),
                    contentDescription = b.label,
                    modifier = GlanceModifier.size(ring * 0.4f),
                    colorFilter = ColorFilter.tint(accent),
                )
            }
            Spacer(GlanceModifier.height(3.dp))
            Text(
                text = (b.percent?.let { "$it%" } ?: "--") + if (b.charging) "⚡" else "",
                style = mono(s, 13f, c.onSurface, FontWeight.Bold),
            )
        }
    }

    /** Czas do naładowania/rozładowania telefonu, opcjonalnie z budzikiem. */
    @Composable
    private fun InfoLine(d: WidgetData, s: WidgetStyle, withAlarm: Boolean) {
        val phone = d.batteries.first()
        val time = phone.remainingMs?.let {
            (if (phone.charging) "pełna za " else "zostało ") + TimeEstimator.format(it)
        }
        val alarm = if (withAlarm) d.alarm else null
        if (time == null && alarm == null) return
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            time?.let { IconText(R.drawable.ic_timer, it, s) }
            if (time != null && alarm != null) Spacer(GlanceModifier.width(12.dp))
            alarm?.let { IconText(R.drawable.ic_alarm, it, s) }
        }
    }

    @Composable
    private fun IconText(icon: Int, text: String, s: WidgetStyle) {
        val color = GlanceTheme.colors.onSurfaceVariant
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier.size((12 * s.text.scale).dp),
            colorFilter = ColorFilter.tint(color),
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(text = text, maxLines = 1, style = mono(s, 11f, color))
    }

    @Composable
    private fun Chip(icon: Int, text: String, s: WidgetStyle) {
        val c = GlanceTheme.colors
        // Przy przezroczystym widgecie "pigułki" zostają lekko widoczne.
        TintedBox(
            color = c.secondaryContainer,
            opacity = 0.4f + 0.6f * s.opacity,
            corner = (s.cornerDp * 2 / 3).dp,
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 7.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(icon),
                    contentDescription = null,
                    modifier = GlanceModifier.size((15 * s.text.scale).dp),
                    colorFilter = ColorFilter.tint(c.onSecondaryContainer),
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(text = text, maxLines = 1, style = mono(s, 12f, c.onSecondaryContainer))
            }
        }
    }

    /**
     * Tło w kolorze motywu z regulowanym kryciem. Glance nie pozwala dodać
     * przezroczystości do dynamicznego koloru, więc barwimy półprzezroczystą bitmapę.
     */
    @Composable
    private fun TintedBox(
        color: ColorProvider,
        opacity: Float,
        corner: Dp,
        modifier: GlanceModifier,
        content: @Composable () -> Unit,
    ) {
        Box(modifier = modifier.cornerRadius(corner)) {
            Image(
                provider = ImageProvider(RingRenderer.solid(opacity)),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(color),
            )
            content()
        }
    }

    private fun mono(
        s: WidgetStyle,
        size: Float,
        color: ColorProvider,
        weight: FontWeight = FontWeight.Normal,
    ) = TextStyle(
        color = color,
        fontSize = (size * s.text.scale).sp,
        fontWeight = weight,
        fontFamily = FontFamily.Monospace,
    )
}

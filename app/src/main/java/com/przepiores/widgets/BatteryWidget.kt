package com.przepiores.widgets

import android.content.Context
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
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
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

private val SMALL = DpSize(110.dp, 110.dp)
private val MEDIUM = DpSize(250.dp, 110.dp)
private val LARGE = DpSize(250.dp, 220.dp)

private const val LOW_BATTERY = 20

private val SCRIM = ColorProvider(day = Color.White.copy(alpha = 0.55f), night = Color.Black.copy(alpha = 0.42f))

data class WidgetData(
    val batteries: List<BatteryInfo>,
    val alarm: String?,
    val weather: Weather?,
) {
    companion object {
        fun load(context: Context) = WidgetData(
            batteries = listOf(BatteryRepository.phone(context)) +
                BatteryRepository.bluetoothDevices(context).filter { it.kind != DeviceKind.WATCH },
            alarm = AlarmRepository.next(context),
            weather = WeatherRepository.cached(context),
        )
    }
}

class BatteryWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    // Rozmiary renderowane w podglądzie na liście widgetów (Android 15+).
    override val previewSizeMode = SizeMode.Responsive(setOf(MEDIUM, LARGE))

    /** Podgląd na liście widgetów: prawdziwe dane, domyślny wygląd. */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val data = WidgetData.load(context)
        provideContent { GlanceTheme { Content(data, WidgetStyle()) } }
    }

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
        val phone = d.batteries.first()
        TintedBox(
            color = GlanceTheme.colors.widgetBackground,
            opacity = s.opacity,
            corner = s.cornerDp.dp,
            modifier = GlanceModifier.fillMaxSize().appWidgetBackground(),
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                when {
                    size.width < MEDIUM.width -> {
                        BigPercent(phone, s, capDp = 34f)
                        Spacer(GlanceModifier.height(8.dp))
                        Bar(phone, s)
                        Spacer(GlanceModifier.height(8.dp))
                        if (phone.remainingMs != null) Scrim {
                            Row(verticalAlignment = Alignment.CenterVertically) { TimeItem(phone, s, short = true) }
                        }
                    }
                    size.height < LARGE.height -> {
                        BigPercent(phone, s, capDp = 44f)
                        Spacer(GlanceModifier.height(10.dp))
                        Bar(phone, s)
                        Spacer(GlanceModifier.height(10.dp))
                        BottomRow(d, phone, s, clockSp = 26f, devices = false)
                    }
                    else -> {
                        BigPercent(phone, s, capDp = 58f)
                        Spacer(GlanceModifier.height(12.dp))
                        Bar(phone, s)
                        Spacer(GlanceModifier.height(12.dp))
                        BottomRow(d, phone, s, clockSp = 32f, devices = true)
                    }
                }
            }
        }
    }

    /** Pod paskiem: pigułka z informacjami po lewej, zegar z datą w prawym rogu. */
    @Composable
    private fun BottomRow(d: WidgetData, phone: BatteryInfo, s: WidgetStyle, clockSp: Float, devices: Boolean) {
        val context = LocalContext.current
        val clock = RemoteViews(context.packageName, R.layout.widget_clock).apply {
            setTextViewTextSize(R.id.clock_time, TypedValue.COMPLEX_UNIT_SP, clockSp * s.text.scale)
            setTextViewTextSize(R.id.clock_date, TypedValue.COMPLEX_UNIT_SP, 11f * s.text.scale)
        }
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Scrim {
                if (phone.remainingMs != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeItem(phone, s, last = true)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherItem(d.weather, s, last = d.alarm == null)
                    d.alarm?.let { Item(R.drawable.ic_alarm, it, s, last = true) }
                }
                val bt = d.batteries.drop(1).take(3).filter { it.percent != null }
                if (devices && bt.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                    bt.forEachIndexed { i, b -> Item(b.kind.icon, "${b.percent}%", s, last = i == bt.lastIndex) }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            AndroidRemoteViews(remoteViews = clock)
        }
    }

    /**
     * Wielki procent w Inter Tight: liczba grubo (700) w kolorze akcentu,
     * znak % cienko (200). Rysowane jako bitmapy, bo Glance nie ładuje własnych czcionek.
     */
    @Composable
    private fun BigPercent(b: BatteryInfo, s: WidgetStyle, capDp: Float) {
        val c = GlanceTheme.colors
        val context = LocalContext.current
        val density = context.resources.displayMetrics.density
        val capPx = (capDp * s.text.scale * density).toInt()
        val low = b.percent != null && b.percent < LOW_BATTERY && !b.charging
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextImage(b.percent?.toString() ?: "--", R.font.inter_tight_bold, capPx, density, if (low) c.error else c.primary)
            TextImage("%", R.font.inter_tight_extralight, capPx, density, c.onSurface)
            if (b.charging) {
                Spacer(GlanceModifier.width(6.dp))
                Image(
                    provider = ImageProvider(R.drawable.ic_bolt),
                    contentDescription = "ładuje się",
                    modifier = GlanceModifier.size((capDp * s.text.scale * 0.55f).dp),
                    colorFilter = ColorFilter.tint(c.primary),
                )
            }
        }
    }

    @Composable
    private fun TextImage(text: String, font: Int, capPx: Int, density: Float, color: ColorProvider) {
        val bmp = RingRenderer.text(LocalContext.current, text, font, capPx)
        Image(
            provider = ImageProvider(bmp),
            contentDescription = text,
            modifier = GlanceModifier.width((bmp.width / density).dp).height((bmp.height / density).dp),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(color),
        )
    }

    /** Cienka linia postępu: tło w tonie motywu, wypełnienie w kolorze akcentu. */
    @Composable
    private fun Bar(b: BatteryInfo, s: WidgetStyle) {
        val c = GlanceTheme.colors
        val low = b.percent != null && b.percent < LOW_BATTERY && !b.charging
        Box(modifier = GlanceModifier.fillMaxWidth().height(s.ring.barDp.dp)) {
            Image(
                provider = ImageProvider(RingRenderer.bar(100)),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(c.secondaryContainer),
            )
            Image(
                provider = ImageProvider(RingRenderer.bar(b.percent ?: 0)),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(if (low) c.error else c.primary),
            )
        }
    }

    @Composable
    private fun TimeItem(phone: BatteryInfo, s: WidgetStyle, short: Boolean = false, last: Boolean = false) {
        val ms = phone.remainingMs ?: return
        val time = TimeEstimator.format(ms)
        val text = when {
            short -> time
            phone.charging -> "pełna za $time"
            else -> time
        }
        Item(R.drawable.ic_timer, text, s, last)
    }

    @Composable
    private fun WeatherItem(w: Weather?, s: WidgetStyle, last: Boolean = false) {
        w ?: return
        Item(w.icon, "${w.tempC}° ${w.label}", s, last)
    }

    /** Ikona i tekst w systemowej czcionce, z odstępem za sobą (oprócz ostatniego). */
    @Composable
    private fun Item(icon: Int, text: String, s: WidgetStyle, last: Boolean = false) {
        ItemIcon(icon, s)
        Text(text = text, maxLines = 1, style = sans(s, 13.5f))
        if (!last) Spacer(GlanceModifier.width(14.dp))
    }

    /**
     * Półprzezroczysta pigułka pod drobnym tekstem, żeby był czytelny na jasnej
     * tapecie (Glance nie robi cieni tekstu). Ciemna w trybie ciemnym, jasna w jasnym.
     */
    @Composable
    private fun Scrim(content: @Composable () -> Unit) {
        Column(
            modifier = GlanceModifier
                .cornerRadius(14.dp)
                .background(SCRIM)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) { content() }
    }

    @Composable
    private fun ItemIcon(icon: Int, s: WidgetStyle) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier.size((15 * s.text.scale).dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
        )
        Spacer(GlanceModifier.width(6.dp))
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

    /** Drobny tekst w czcionce systemowej (na Samsungu: One UI Sans). */
    @Composable
    private fun sans(s: WidgetStyle, size: Float) = TextStyle(
        color = GlanceTheme.colors.onSurface,
        fontSize = (size * s.text.scale).sp,
        fontWeight = FontWeight.Medium,
    )
}

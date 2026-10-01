package com.przepiores.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.launch

/** Otwiera się przy dodawaniu widgetu i z menu "Ustawienia" po przytrzymaniu widgetu. */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, result)
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)

        setContent {
            val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            MaterialTheme(colorScheme = scheme) {
                var style by remember { mutableStateOf<WidgetStyle?>(null) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) {
                    style = WidgetStyle.from(
                        getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId),
                    )
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    style?.let { s ->
                        ConfigScreen(
                            style = s,
                            onChange = { style = it },
                            onSave = {
                                scope.launch {
                                    val ctx = this@WidgetConfigActivity
                                    updateAppWidgetState(ctx, glanceId) { s.write(it) }
                                    BatteryWidget().update(ctx, glanceId)
                                    setResult(RESULT_OK, result)
                                    finish()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigScreen(style: WidgetStyle, onChange: (WidgetStyle) -> Unit, onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Wygląd widgetu", style = MaterialTheme.typography.headlineSmall)
        WidgetPreview(style)

        Label("Przezroczystość tła", "${((1 - style.opacity) * 100).toInt()}%")
        Slider(
            value = 1 - style.opacity,
            onValueChange = { onChange(style.copy(opacity = 1 - it)) },
        )

        Label("Zaokrąglenie rogów", "${style.cornerDp} dp")
        Slider(
            value = style.cornerDp.toFloat(),
            onValueChange = { onChange(style.copy(cornerDp = it.toInt())) },
            valueRange = 0f..32f,
            steps = 7,
        )

        Label("Grubość pierścieni", null)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            RingThickness.entries.forEachIndexed { i, t ->
                SegmentedButton(
                    selected = style.ring == t,
                    onClick = { onChange(style.copy(ring = t)) },
                    shape = SegmentedButtonDefaults.itemShape(i, RingThickness.entries.size),
                ) { Text(t.label) }
            }
        }

        Label("Wielkość tekstu", null)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TextSize.entries.forEachIndexed { i, t ->
                SegmentedButton(
                    selected = style.text == t,
                    onClick = { onChange(style.copy(text = t)) },
                    shape = SegmentedButtonDefaults.itemShape(i, TextSize.entries.size),
                ) { Text(t.label) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Zapisz") }
    }
}

@Composable
private fun Label(text: String, value: String?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        value?.let { Text(it, fontFamily = FontFamily.Monospace) }
    }
}

/** Przybliżony podgląd widgetu na "tapecie" z kolorów motywu. */
@Composable
private fun WidgetPreview(s: WidgetStyle) {
    val cs = MaterialTheme.colorScheme
    // Te same kolory systemowe, których Glance używa jako widgetBackground.
    val bg = colorResource(
        if (isSystemInDarkTheme()) android.R.color.system_accent2_800 else android.R.color.system_accent2_50,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(cs.tertiary, cs.primary, cs.secondary))),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .width(260.dp)
                .clip(RoundedCornerShape(s.cornerDp.dp))
                .background(bg.copy(alpha = s.opacity))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            PreviewRing(72, R.drawable.ic_phone, s)
            PreviewRing(15, R.drawable.ic_headphones, s)
            PreviewHotspot(s)
        }
    }
}

@Composable
private fun PreviewHotspot(s: WidgetStyle) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(cs.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_hotspot), contentDescription = null, tint = cs.onPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(
            "wł.",
            color = cs.onSurface,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = (13 * s.text.scale).sp,
        )
    }
}

@Composable
private fun PreviewRing(pct: Int, icon: Int, s: WidgetStyle) {
    val cs = MaterialTheme.colorScheme
    val accent = if (pct < 20) cs.error else cs.primary
    val track = cs.secondaryContainer
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.minDimension * s.ring.fraction
                val topLeft = Offset(w / 2, w / 2)
                val arcSize = Size(size.width - w, size.height - w)
                val stroke = Stroke(width = w, cap = StrokeCap.Round)
                drawArc(track, -90f, 360f, false, topLeft, arcSize, style = stroke)
                drawArc(accent, -90f, 360f * pct / 100, false, topLeft, arcSize, style = stroke)
            }
            Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(
            "$pct%",
            color = cs.onSurface,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = (13 * s.text.scale).sp,
        )
    }
}

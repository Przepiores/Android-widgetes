package com.przepiores.widgets

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Grubość paska baterii (nazwa enuma zostaje, bo jest zapisana w ustawieniach widgetów). */
enum class RingThickness(val label: String, val barDp: Int) {
    THIN("Cienki", 3),
    NORMAL("Normalny", 5),
    THICK("Gruby", 8),
}

enum class TextSize(val label: String, val scale: Float) {
    SMALL("Mały", 0.85f),
    NORMAL("Normalny", 1f),
    LARGE("Duży", 1.2f),
}

/** Ustawienia wyglądu jednego widgetu, trzymane w jego stanie Glance. */
data class WidgetStyle(
    /** Krycie tła: 0 = całkiem przezroczyste, 1 = pełne. */
    val opacity: Float = 0f,
    val cornerDp: Int = 24,
    val ring: RingThickness = RingThickness.NORMAL,
    val text: TextSize = TextSize.NORMAL,
) {
    fun write(p: MutablePreferences) {
        p[OPACITY] = opacity
        p[CORNER] = cornerDp
        p[RING] = ring.name
        p[TEXT] = text.name
    }

    companion object {
        private val OPACITY = floatPreferencesKey("opacity")
        private val CORNER = intPreferencesKey("corner")
        private val RING = stringPreferencesKey("ring")
        private val TEXT = stringPreferencesKey("text")

        /** Zmieniany przy każdym odświeżeniu, żeby widget ponownie wczytał dane. */
        val TICK = longPreferencesKey("tick")

        fun from(p: Preferences) = WidgetStyle(
            opacity = p[OPACITY] ?: 0f,
            cornerDp = p[CORNER] ?: 24,
            ring = p[RING]?.let { n -> RingThickness.entries.firstOrNull { it.name == n } }
                ?: RingThickness.NORMAL,
            text = p[TEXT]?.let { n -> TextSize.entries.firstOrNull { it.name == n } }
                ?: TextSize.NORMAL,
        )
    }
}

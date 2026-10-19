package com.przepiores.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import kotlin.math.ceil

/**
 * Glance nie ma paska postępu o dowolnej wartości ani własnych czcionek w Text,
 * więc rysujemy białe bitmapy i barwimy je w widgecie kolorami motywu
 * (ColorFilter.tint). Kolory nadal zmieniają się razem z tapetą.
 */
object RingRenderer {
    private const val BAR_W = 400
    private const val BAR_H = 24
    private val cache = LruCache<String, Bitmap>(48)
    private val typefaces = HashMap<Int, Typeface>()

    /** Zaokrąglony pasek wypełniony w [percent] procentach szerokości. */
    fun bar(percent: Int): Bitmap {
        val pct = percent.coerceIn(0, 100)
        return cache.get("bar$pct") ?: Bitmap.createBitmap(BAR_W, BAR_H, Bitmap.Config.ARGB_8888).also { bmp ->
            if (pct > 0) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                val w = (BAR_W * pct / 100f).coerceAtLeast(BAR_H.toFloat())
                Canvas(bmp).drawRoundRect(RectF(0f, 0f, w, BAR_H.toFloat()), BAR_H / 2f, BAR_H / 2f, paint)
            }
            cache.put("bar$pct", bmp)
        }
    }

    /** Jednolita biała bitmapa o zadanym kryciu, barwiona na kolor tła. */
    fun solid(opacity: Float): Bitmap {
        val a = (opacity.coerceIn(0f, 1f) * 255).toInt()
        return cache.get("solid$a") ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.argb(a, 255, 255, 255))
        }.also { cache.put("solid$a", it) }
    }

    /**
     * Tekst w czcionce z zasobów (Inter Tight), przycięty do wysokości cyfr:
     * wysokość bitmapy to [capPx] plus mały zapas, linia bazowa przy dolnej krawędzi.
     * Bitmapy tej samej wysokości stoją więc w rzędzie na wspólnej linii bazowej.
     */
    fun text(context: Context, text: String, fontRes: Int, capPx: Int): Bitmap {
        val key = "t$fontRes/$capPx/$text"
        cache.get(key)?.let { return it }
        val tf = typefaces.getOrPut(fontRes) { context.resources.getFont(fontRes) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            color = Color.WHITE
            letterSpacing = -0.045f
            textSize = 100f
        }
        val bounds = Rect()
        paint.getTextBounds("0", 0, 1, bounds)
        paint.textSize = 100f * capPx / bounds.height()
        val pad = capPx * 0.04f
        val h = ceil(capPx + 2 * pad).toInt()
        val w = ceil(paint.measureText(text)).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawText(text, 0f, capPx + pad, paint)
        cache.put(key, bmp)
        return bmp
    }
}

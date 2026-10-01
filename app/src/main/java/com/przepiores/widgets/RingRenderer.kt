package com.przepiores.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache

/**
 * Glance nie ma okrągłego paska postępu z wartością, więc pierścienie rysujemy
 * jako białe bitmapy i barwimy je w widgecie kolorami motywu (ColorFilter.tint).
 * Dzięki temu kolory nadal zmieniają się razem z tapetą bez ponownego rysowania.
 */
object RingRenderer {
    private const val SIZE = 200
    private val cache = LruCache<String, Bitmap>(48)

    fun arc(percent: Int, strokeFraction: Float): Bitmap {
        val pct = percent.coerceIn(0, 100)
        return cache.get("arc$pct/$strokeFraction") ?: draw(pct, strokeFraction).also {
            cache.put("arc$pct/$strokeFraction", it)
        }
    }

    /** Jednolita biała bitmapa o zadanym kryciu, barwiona na kolor tła. */
    fun solid(opacity: Float): Bitmap {
        val a = (opacity.coerceIn(0f, 1f) * 255).toInt()
        return cache.get("solid$a") ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.argb(a, 255, 255, 255))
        }.also { cache.put("solid$a", it) }
    }

    private fun draw(percent: Int, strokeFraction: Float): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        if (percent == 0) return bmp
        val w = SIZE * strokeFraction
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w
            strokeCap = Paint.Cap.ROUND
            color = Color.WHITE
        }
        val r = RectF(w / 2, w / 2, SIZE - w / 2, SIZE - w / 2)
        Canvas(bmp).drawArc(r, -90f, 360f * percent / 100, false, paint)
        return bmp
    }
}

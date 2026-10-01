package com.przepiores.widgets

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Ekran konfiguracyjny: uprawnienia potrzebne widgetowi. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, pad, pad, pad)
            fitsSystemWindows = true
        }
        root.addView(TextView(this).apply {
            text = "1. Dodaj widget z listy widgetów ekranu głównego.\n" +
                "2. Nadaj uprawnienia poniżej."
        })
        root.addView(Button(this).apply {
            text = "Bluetooth (bateria słuchawek)"
            setOnClickListener { requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1) }
        })
        root.addView(Button(this).apply {
            text = "Kalendarz (następne wydarzenie)"
            setOnClickListener { requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 2) }
        })
        setContentView(root)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Pokaż nowe dane od razu, bez czekania na kolejne odświeżenie.
        CoroutineScope(Dispatchers.Default).launch { WidgetRefresher.refreshAll(applicationContext) }
    }
}

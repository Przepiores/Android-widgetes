package com.przepiores.widgets

import android.app.Activity
import android.os.Bundle

/**
 * Niewidoczna aktywność uruchamiana z przycisku w widgecie: otwiera ustawienia
 * hotspotu i planuje kilka odświeżeń, żeby widget pokazał nowy stan po powrocie.
 */
class HotspotActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotspotRepository.openSettings(this)
        RefreshWorker.scheduleSoon(this)
        finish()
    }
}

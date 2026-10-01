package com.przepiores.widgets

import android.app.NotificationManager
import android.content.Context
import android.content.Intent

enum class Mode(val label: String, val filter: Int) {
    NORMAL("Normalny", NotificationManager.INTERRUPTION_FILTER_ALL),
    PRIORITY("Priorytet", NotificationManager.INTERRUPTION_FILTER_PRIORITY),
    DND("Nie przeszkadzać", NotificationManager.INTERRUPTION_FILTER_NONE),
}

/**
 * Samsung nie ma publicznego API do przełączania trybów "Tryby i rutyny".
 * Przełączamy systemowe Nie przeszkadzać; w Trybach i rutynach tworzysz rutyny
 * z warunkiem "Nie przeszkadzać włączone", które aktywują wybrany tryb Samsunga.
 */
object ModeController {
    fun hasAccess(context: Context) =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun current(context: Context): Mode {
        val f = context.getSystemService(NotificationManager::class.java).currentInterruptionFilter
        return Mode.entries.firstOrNull { it.filter == f } ?: Mode.NORMAL
    }

    fun set(context: Context, mode: Mode) {
        if (!hasAccess(context)) {
            context.startActivity(
                Intent(MainActivity.ACTION_POLICY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        context.getSystemService(NotificationManager::class.java)
            .setInterruptionFilter(mode.filter)
    }
}

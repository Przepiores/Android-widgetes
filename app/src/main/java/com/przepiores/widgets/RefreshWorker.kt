package com.przepiores.widgets

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        WidgetRefresher.refreshAll(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "battery_refresh",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build(),
            )
        }

        /** Kilka odświeżeń w najbliższych minutach, np. po wejściu w ustawienia hotspotu. */
        fun scheduleSoon(context: Context) {
            val wm = WorkManager.getInstance(context)
            listOf(10L, 30L, 90L, 180L).forEach { s ->
                wm.enqueueUniqueWork(
                    "refresh_in_$s",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<RefreshWorker>().setInitialDelay(s, TimeUnit.SECONDS).build(),
                )
            }
        }
    }
}

package com.rajiv.paisatrack.notify

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.rajiv.paisatrack.R
import com.rajiv.paisatrack.data.Store
import com.rajiv.paisatrack.logic.Summary
import java.util.concurrent.TimeUnit

/** Daily check: if a card's statement closes within 3 days, notify. */
class DueReminder(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        val s = Summary.compute(Store.load(ctx))
        val soon = s.cards.filter { (it.daysLeft ?: 99) in 0..3 && it.total > 0 }
        if (soon.isNotEmpty()) {
            val line = soon.joinToString("; ") {
                "${it.bank} ···${it.source.trimStart('X', '*')} closes in ${it.daysLeft}d"
            }
            notify(ctx, "Card statement closing soon", line)
        }
        return Result.success()
    }

    private fun notify(ctx: Context, title: String, text: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val n = NotificationCompat.Builder(ctx, "bills")
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(101, n)
    }

    companion object {
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<DueReminder>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "due-reminder", ExistingPeriodicWorkPolicy.KEEP, req
            )
        }
    }
}

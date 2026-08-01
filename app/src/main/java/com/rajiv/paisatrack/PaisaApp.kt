package com.rajiv.paisatrack

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class PaisaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                "bills", "Bill reminders", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Reminds you before a card statement closes" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}

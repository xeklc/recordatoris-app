package com.example.recordatoris

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

fun createNotificationChannelIfNeeded(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(ReminderReceiver.CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    ReminderReceiver.CHANNEL_ID,
                    "Recordatoris",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }
}

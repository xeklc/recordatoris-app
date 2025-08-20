package com.example.recordatoris

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderReceiver : BroadcastReceiver() {

    private fun actionPi(
        context: Context,
        action: String,
        id: Int,
        mutable: Boolean
    ): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getBroadcast(
            context,
            "$action-$id".hashCode(),
            Intent(context, ReminderReceiver::class.java).apply {
                this.action = action
                putExtra("id", id)
            },
            flags
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", System.currentTimeMillis().toInt())
        val title = intent.getStringExtra("title") ?: "Recordatori"
        val text = intent.getStringExtra("text") ?: ""
        // If this onReceive is also used to HANDLE actions, do that first:
        when (intent.action) {
            ACTION_DONE -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    cancelReminder(context, id)
                    val repo = RecordatoriRepo(DbProvider.get(context).recordatoriDao())
                    repo.deleteById(id)
                    pending.finish()
                    NotificationManagerCompat.from(context).cancel(id)
                }
                return }
            ACTION_SNOOZE_10M -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch{
                    cancelReminder(context, id)
                    val repo = RecordatoriRepo(DbProvider.get(context).recordatoriDao())
                    repo.updateHourById(id)
                    val pickedDateTime: LocalDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.now()).plusMinutes(60)
                    val triggerAtMillis: Long = pickedDateTime
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    scheduleReminder(
                        context = context,
                        triggerAtMillis = triggerAtMillis,
                        id = id,     // e.g., DB id or a unique number
                        title = "Recordatori",
                        text = repo.getById(id).title
                    )
                    pending.finish()
                    NotificationManagerCompat.from(context).cancel(id)
                }
                return }

        }
        when (intent.getStringExtra(EXTRA_REPEAT)) {
            "MONTHLY" -> {
                val id    = intent.getIntExtra("id", -1)
                val title = intent.getStringExtra("title") ?: "Recordatori"
                val text  = intent.getStringExtra("text") ?: ""
                val dom   = intent.getIntExtra(EXTRA_DOM, 1)
                val hour  = intent.getIntExtra(EXTRA_HOUR, 9)
                val min   = intent.getIntExtra(EXTRA_MINUTE, 0)

                // Schedule next occurrence with the same config
                scheduleMonthlyReminder(
                    context = context,
                    id = id, dayOfMonth = dom, hour = hour, minute = min,
                    title = title, text = text
                )
            }
            else -> Unit // one-shot
        }

        val big = BitmapFactory.decodeResource(
            context.resources, R.drawable.recordatoris_img
        )

        // Build actions

        val doneAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_recordatoris_hdpi, "Marcar com a fet",
            actionPi(context, ACTION_DONE, id, mutable = false)   // immutable is fine
        ).build()

        val snoozeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_recordatoris_hdpi, "Recordar d'aqui 1 hora",
            actionPi(context, ACTION_SNOOZE_10M, id, mutable = false)
        ).build()

        createNotificationChannelIfNeeded(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_recordatoris_hdpi) // use a drawable name without density suffix
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setLargeIcon(big)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(big))
            .addAction(doneAction)
            .addAction(snoozeAction)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "recordatoris_channel"
        const val ACTION_DONE = "com.example.recordatoris.ACTION_DONE"
        const val ACTION_SNOOZE_10M = "com.example.recordatoris.ACTION_SNOOZE_10M"
    }
}

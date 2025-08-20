package com.example.recordatoris

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.min

const val EXTRA_REPEAT  = "repeat"      // values: "NONE" | "MONTHLY"
const val EXTRA_DOM     = "dom"         // day of month (1..31)
const val EXTRA_HOUR    = "hour"
const val EXTRA_MINUTE  = "minute"
fun canScheduleExact(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(AlarmManager::class.java)
    return am.canScheduleExactAlarms()
}
fun nextMonthlyTriggerMillis(
    dayOfMonth: Int,           // e.g., 1..31
    hour: Int,
    minute: Int,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    val now = ZonedDateTime.now(zone)

    fun atThisMonth(ym: YearMonth): ZonedDateTime {
        val dom = min(dayOfMonth, ym.lengthOfMonth()) // clamp to month length
        return ym.atDay(dom).atTime(hour, minute).atZone(zone)
    }

    var ym = YearMonth.from(now)
    var next = atThisMonth(ym)
    if (!next.isAfter(now)) {
        ym = ym.plusMonths(1)
        next = atThisMonth(ym)
    }
    return next.toInstant().toEpochMilli()
}
fun scheduleMonthlyReminder(
    context: Context,
    id: Int,
    dayOfMonth: Int,   // 1..31 (31 means "last day" via clamping)
    hour: Int,
    minute: Int,
    title: String,
    text: String
) {
    val trigger = nextMonthlyTriggerMillis(dayOfMonth, hour, minute)
    scheduleReminder(
        context = context,
        triggerAtMillis = trigger,
        id = id,
        title = title,
        text = text,
        repeat = "MONTHLY",
        dayOfMonth = dayOfMonth,
        hour = hour,
        minute = minute
    )
}
fun requestExactAlarmAccess(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/** Schedules an exact alarm and shows a notification via ReminderReceiver. */
fun scheduleReminder(
    context: Context,
    triggerAtMillis: Long,
    id: Int,
    title: String,
    text: String,
    repeat: String = "NONE",
    dayOfMonth: Int? = null,
    hour: Int? = null,
    minute: Int? = null
) {
    createNotificationChannelIfNeeded(context)

    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("id", id)
        putExtra("title", title)
        putExtra("text", text)
        putExtra(EXTRA_REPEAT, repeat)
        dayOfMonth?.let { putExtra(EXTRA_DOM, it) }
        hour?.let { putExtra(EXTRA_HOUR, it) }
        minute?.let { putExtra(EXTRA_MINUTE, it) }
    }
    val pi = PendingIntent.getBroadcast(
        context, id, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (!canScheduleExact(context)) {
        // Route user to system screen to enable "Alarms & reminders"
        requestExactAlarmAccess(context)
        return
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    } catch (_: SecurityException) {
        // Toggle missing or revoked after check
        requestExactAlarmAccess(context)
    }
}

fun cancelReminder(context: Context, id: Int) {
    val intent = Intent(context, ReminderReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context, id, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.cancel(pi)
}

fun hasPostNotificationsPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}


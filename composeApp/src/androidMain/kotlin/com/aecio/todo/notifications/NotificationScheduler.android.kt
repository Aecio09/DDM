package com.aecio.todo.notifications

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aecio.todo.R
import com.aecio.todo.db.appContext
import kotlinx.datetime.Instant

private const val CHANNEL_ID = "tasks_reminders"
private const val REQ_PERMISSION_NOTIFICATIONS = 1001

private var permissionActivity: Activity? = null

internal fun attachPermissionActivity(activity: Activity) {
    permissionActivity = activity
}

actual object NotificationScheduler {

    private val context: Context get() = appContext

    private fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembretes de tarefas",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notificações agendadas para tarefas com prazo"
        }
        manager.createNotificationChannel(channel)
    }

    actual fun currentPermission(): NotificationPermission {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            return if (granted) NotificationPermission.GRANTED else NotificationPermission.DENIED
        }
        return NotificationPermission.GRANTED
    }

    actual fun requestPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionActivity?.requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_PERMISSION_NOTIFICATIONS,
            )
        }
    }

    actual fun schedule(taskId: Long, title: String, dueAt: Instant): Long? {
        ensureChannel()
        val id = taskId.toInt()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationReceiver.EXTRA_TASK_TITLE, title)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = dueAt.toEpochMilliseconds()

        try {
            if (Build.VERSION.SDK_INT >= 31) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                }
            } else if (Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, at, pending)
            }
        } catch (e: SecurityException) {
            // Exact alarms not permitted: fall back to inexact scheduling.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
        return taskId
    }

    actual fun cancel(taskId: Long, notificationId: Long?) {
        val context = context
        val id = notificationId?.toInt() ?: taskId.toInt()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, NotificationReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
        NotificationManagerCompat.from(context).cancel(id)
    }

    internal fun postNotification(taskId: Long, title: String) {
        if (currentPermission() != NotificationPermission.GRANTED) return
        ensureChannel()
        val id = taskId.toInt()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Lembrete de tarefa")
            .setContentText("Task reminder: $title")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Task reminder: $title"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission revoked at insertion time: silently ignore.
        }
    }
}
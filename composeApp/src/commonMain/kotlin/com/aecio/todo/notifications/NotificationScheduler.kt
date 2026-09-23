package com.aecio.todo.notifications

import kotlinx.datetime.Instant

enum class NotificationPermission { GRANTED, DENIED, UNSUPPORTED }

/**
 * Schedules and cancels local notifications. The actual behavior is
 * platform specific (Android: AlarmManager + NotificationManager;
 * Desktop: Timer + system tray when available).
 */
expect object NotificationScheduler {
    fun currentPermission(): NotificationPermission
    fun requestPermission()
    fun schedule(taskId: Long, title: String, dueAt: Instant): Long?
    fun cancel(taskId: Long, notificationId: Long?)
}
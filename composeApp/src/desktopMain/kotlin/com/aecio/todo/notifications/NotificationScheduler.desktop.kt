package com.aecio.todo.notifications

import java.util.Collections
import java.util.Timer
import java.util.TimerTask
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

actual object NotificationScheduler {

    private val timers = Collections.synchronizedMap(mutableMapOf<Long, Timer>())

    actual fun currentPermission(): NotificationPermission = NotificationPermission.GRANTED

    actual fun requestPermission() {
        // Desktop has no OS notification permission model.
    }

    actual fun schedule(taskId: Long, title: String, dueAt: Instant): Long? {
        cancel(taskId, null)
        val delayMillis = (dueAt.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
            .coerceAtLeast(0L)
        val timer = Timer("todo-notification-$taskId", false)
        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    showDesktopNotification(taskId, title)
                    synchronized(timers) { timers.remove(taskId) }
                }
            },
            delayMillis,
        )
        synchronized(timers) { timers[taskId] = timer }
        return taskId
    }

    actual fun cancel(taskId: Long, notificationId: Long?) {
        synchronized(timers) {
            timers.remove(taskId)?.cancel()
        }
    }

    private fun showDesktopNotification(taskId: Long, title: String) {
        val text = "Task reminder: $title"
        try {
            if (java.awt.SystemTray.isSupported()) {
                val tray = java.awt.SystemTray.getSystemTray()
                val image = java.awt.Toolkit.getDefaultToolkit().createImage(byteArrayOf())
                    ?: return
                val icon = java.awt.TrayIcon(image, "Todo App")
                tray.add(icon)
                icon.displayMessage("Todo App", text, java.awt.TrayIcon.MessageType.INFO)
                // Delay removal so the balloon shows.
                val cleanup = Timer("tray-cleanup", false)
                cleanup.schedule(object : TimerTask() {
                    override fun run() {
                        tray.remove(icon)
                    }
                }, 5000)
            } else {
                println("NOTIFICAÇÃO ($taskId): $text")
            }
        } catch (e: Exception) {
            println("NOTIFICAÇÃO ($taskId): $text (tray falhou: ${e.message})")
        }
    }
}
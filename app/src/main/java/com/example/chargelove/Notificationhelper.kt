package com.example.chargelove

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color

object NotificationHelper {
    private const val CHANNEL_ALERTS = "charge_alerts"
    private const val CHANNEL_MONITOR = "charge_monitor"
    private const val ALERT_ID = 2001 // same id → a newer tier replaces the older one
    const val MONITOR_ID = 1001

    private val PINK = Color.parseColor("#FF61A9")

    /** Accent colour per tier: it tints the small icon, app name and buttons. */
    private fun accentFor(threshold: Int): Int = when (threshold) {
        10 -> Color.parseColor("#FB8C00")
        5 -> Color.parseColor("#E64A19")
        3 -> Color.parseColor("#B71C1C")
        else -> PINK
    }

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Charge reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    description = "Sweet (and increasingly dramatic) low battery nudges"
                    enableVibration(true)
                }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MONITOR, "Battery watcher", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Keeps the battery watcher running quietly" }
        )
    }

    fun monitorNotification(context: Context): Notification =
        Notification.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_battery_love)
            .setContentTitle("Watching your battery 💕")
            .setContentIntent(openAppIntent(context))
            .setColor(PINK)
            .setColorized(true) // full-colour background; only honoured for foreground-service notifications
            .setOngoing(true)
            .build()

    fun showAlert(context: Context, threshold: Int, level: Int) {
        createChannels(context)
        val body = Messages.body(threshold, AppPreferences(context).customMessage)
        val notification = Notification.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_battery_love) // monochrome silhouette (status bar)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_battery_love_large)) // full colour
            .setColor(accentFor(threshold))
            .setContentTitle(Messages.title(threshold, level))
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent(context))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(ALERT_ID, notification)
    }

    fun cancelAlert(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(ALERT_ID)
    }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
package com.example.chargelove

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Foreground service that listens for battery changes and fires tiered notifications.
 *
 * Why not WorkManager? Periodic work runs at most every 15 minutes, which can easily
 * skip past 5% / 3%. A live ACTION_BATTERY_CHANGED receiver reacts instantly.
 */
class BatteryMonitorService : Service() {

    private lateinit var prefs: AppPreferences

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = handle(BatteryState.from(intent))
    }

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        NotificationHelper.createChannels(this)

        val notification = NotificationHelper.monitorNotification(this)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NotificationHelper.MONITOR_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.MONITOR_ID, notification)
        }

        // registerReceiver returns the current sticky battery state, so check it right away.
        val sticky = registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        handle(BatteryState.from(sticky))
    }

    private fun handle(state: BatteryState) {
        if (state.isCharging) {
            // Plugged in → reset tracking and clear any lingering alert.
            if (prefs.lastNotifiedThreshold != AppPreferences.NONE) {
                prefs.lastNotifiedThreshold = AppPreferences.NONE
                NotificationHelper.cancelAlert(this)
            }
            return
        }

        val reached = Thresholds.lowestReached(state.level)
        if (reached == null) {
            // Unplugged but above 20% – nothing to warn about.
            prefs.lastNotifiedThreshold = AppPreferences.NONE
            return
        }

        // Only notify when we've dropped to a NEW, lower milestone (no spam at a fixed %).
        if (reached < prefs.lastNotifiedThreshold) {
            prefs.lastNotifiedThreshold = reached
            NotificationHelper.showAlert(this, reached, state.level)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, BatteryMonitorService::class.java))
            } catch (e: IllegalStateException) {
                // Background-start restrictions; the service will start next time the app opens.
            }
        }
    }
}
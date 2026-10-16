package com.example.chargelove

import android.content.Intent
import android.os.BatteryManager

data class BatteryState(val level: Int, val isCharging: Boolean) {
    companion object {
        fun from(intent: Intent?): BatteryState {
            if (intent == null) return BatteryState(level = 100, isCharging = false)
            val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = if (raw >= 0 && scale > 0) raw * 100 / scale else 100
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            return BatteryState(percent, plugged)
        }
    }
}
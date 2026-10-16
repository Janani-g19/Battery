package com.example.chargelove

import android.content.Context

/**
 * Tiny local storage manager (SharedPreferences, so it's safe to call from the
 * service and the UI without coroutines).
 */
class AppPreferences(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** The custom message shown at the 20% milestone. */
    var customMessage: String
        get() = prefs.getString(KEY_MESSAGE, null) ?: Messages.DEFAULT_CUSTOM
        set(value) {
            val clean = value.trim().ifBlank { Messages.DEFAULT_CUSTOM }
            prefs.edit().putString(KEY_MESSAGE, clean).apply()
        }

    /** Lowest threshold we've already notified for. [NONE] means "nothing sent yet". */
    var lastNotifiedThreshold: Int
        get() = prefs.getInt(KEY_LAST_THRESHOLD, NONE)
        set(value) = prefs.edit().putInt(KEY_LAST_THRESHOLD, value).apply()

    companion object {
        const val NONE = Int.MAX_VALUE
        private const val FILE_NAME = "charge_love_prefs"
        private const val KEY_MESSAGE = "custom_message_20"
        private const val KEY_LAST_THRESHOLD = "last_notified_threshold"
    }
}
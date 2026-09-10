package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.models.CallingMode

class DialerPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nuv_dialer_preferences", Context.MODE_PRIVATE)

    var callingMode: CallingMode
        get() {
            val raw = prefs.getString(KEY_CALLING_MODE, CallingMode.AUTO_CALL.name)
            return try {
                CallingMode.valueOf(raw ?: CallingMode.AUTO_CALL.name)
            } catch (e: Exception) {
                CallingMode.AUTO_CALL
            }
        }
        set(value) {
            prefs.edit().putString(KEY_CALLING_MODE, value.name).apply()
        }

    var autoAdvanceDelaySeconds: Int
        get() = prefs.getInt(KEY_AUTO_ADVANCE_DELAY, 3)
        set(value) {
            prefs.edit().putInt(KEY_AUTO_ADVANCE_DELAY, value).apply()
        }

    companion object {
        private const val KEY_CALLING_MODE = "pref_calling_mode"
        private const val KEY_AUTO_ADVANCE_DELAY = "pref_auto_advance_delay"
    }
}

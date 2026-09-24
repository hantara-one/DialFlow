package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.models.CallingMode
import com.example.models.PhoneNumberFormatPreference

class DialerPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nuv_dialer_preferences", Context.MODE_PRIVATE)

    var numberFormatPreference: PhoneNumberFormatPreference
        get() {
            val raw = prefs.getString(KEY_NUMBER_FORMAT, PhoneNumberFormatPreference.LOCAL.name)
            return try {
                PhoneNumberFormatPreference.valueOf(raw ?: PhoneNumberFormatPreference.LOCAL.name)
            } catch (e: Exception) {
                PhoneNumberFormatPreference.LOCAL
            }
        }
        set(value) {
            prefs.edit().putString(KEY_NUMBER_FORMAT, value.name).apply()
        }

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

    var languageCode: String
        get() = prefs.getString(KEY_LANGUAGE, LANGUAGE_EN) ?: LANGUAGE_EN
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value).apply()
        }

    companion object {
        const val LANGUAGE_EN = "en"
        const val LANGUAGE_ID = "id"

        private const val KEY_NUMBER_FORMAT = "pref_number_format"
        private const val KEY_CALLING_MODE = "pref_calling_mode"
        private const val KEY_AUTO_ADVANCE_DELAY = "pref_auto_advance_delay"
        private const val KEY_LANGUAGE = "pref_language"
    }
}

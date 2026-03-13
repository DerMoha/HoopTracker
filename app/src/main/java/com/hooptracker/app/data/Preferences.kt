package com.hooptracker.app.data

import android.content.Context
import android.content.SharedPreferences

class Preferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hoop_tracker_prefs", Context.MODE_PRIVATE)

    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()

    var voiceFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_FEEDBACK, false)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_FEEDBACK, value).apply()

    var autoStartSession: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_SESSION, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START_SESSION, value).apply()

    var dailyShotGoal: Int
        get() = prefs.getInt(KEY_DAILY_SHOT_GOAL, 100)
        set(value) = prefs.edit().putInt(KEY_DAILY_SHOT_GOAL, value).apply()

    var dailyPercentageGoal: Float
        get() = prefs.getFloat(KEY_DAILY_PERCENTAGE_GOAL, 50f)
        set(value) = prefs.edit().putFloat(KEY_DAILY_PERCENTAGE_GOAL, value).apply()

    var currentSessionId: Long?
        get() {
            val id = prefs.getLong(KEY_CURRENT_SESSION_ID, -1L)
            return if (id == -1L) null else id
        }
        set(value) = prefs.edit().putLong(KEY_CURRENT_SESSION_ID, value ?: -1L).apply()

    var trackingActive: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_TRACKING_ACTIVE, value).apply()

    companion object {
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_VOICE_FEEDBACK = "voice_feedback"
        private const val KEY_AUTO_START_SESSION = "auto_start_session"
        private const val KEY_DAILY_SHOT_GOAL = "daily_shot_goal"
        private const val KEY_DAILY_PERCENTAGE_GOAL = "daily_percentage_goal"
        private const val KEY_CURRENT_SESSION_ID = "current_session_id"
        private const val KEY_TRACKING_ACTIVE = "tracking_active"
    }
}

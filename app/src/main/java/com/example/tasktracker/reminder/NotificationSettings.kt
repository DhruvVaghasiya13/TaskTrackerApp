package com.example.tasktracker.reminder

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Global notification settings (stored on the device).
 *  - enabled     : master switch, ON by default -> every task sends its notification
 *  - defaultTime : "HH:mm", 05:00 by default. Every task that has no time of its own
 *                  (task.reminderTime blank) is notified at this time.
 *  - repeatHours / repeatMinutes : repeat interval. 0h 0m = OFF. When set, a task that is still
 *                  NOT completed is notified again every [interval] after its notification time
 *                  (same day), until the task is completed.
 */
object NotificationSettings {
    const val DEFAULT_TIME = "05:00"
    private const val PREFS = "reminder_prefs"
    private const val KEY_ENABLED = "notifications_enabled"
    private const val KEY_TIME = "default_reminder_time"
    private const val KEY_REPEAT_H = "repeat_hours"
    private const val KEY_REPEAT_M = "repeat_minutes"

    data class State(
        val enabled: Boolean = true,
        val defaultTime: String = DEFAULT_TIME,
        val repeatHours: Int = 0,
        val repeatMinutes: Int = 0
    ) {
        /** Repeat interval in minutes, 0 = no repeat. */
        val repeatIntervalMinutes: Int get() = repeatHours * 60 + repeatMinutes
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    /** Call once from Application.onCreate (also runs before any alarm receiver). */
    fun init(context: Context) {
        _state.value = read(context)
    }

    private fun read(context: Context): State {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return State(
            enabled = p.getBoolean(KEY_ENABLED, true),
            defaultTime = p.getString(KEY_TIME, DEFAULT_TIME)?.takeIf { it.isNotBlank() } ?: DEFAULT_TIME,
            repeatHours = p.getInt(KEY_REPEAT_H, 0).coerceIn(0, 23),
            repeatMinutes = p.getInt(KEY_REPEAT_M, 0).coerceIn(0, 59)
        )
    }

    fun isEnabled(context: Context) = read(context).enabled
    fun defaultTime(context: Context) = read(context).defaultTime
    fun repeatIntervalMinutes(context: Context) = read(context).repeatIntervalMinutes
    fun repeatHours(context: Context) = read(context).repeatHours
    fun repeatMinutes(context: Context) = read(context).repeatMinutes

    /** 0h 0m = repeat OFF. */
    fun setRepeat(context: Context, hours: Int, minutes: Int) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_REPEAT_H, hours.coerceIn(0, 23)).putInt(KEY_REPEAT_M, minutes.coerceIn(0, 59)).apply()
        _state.value = read(context)
    }

    /** (1,30) -> "1 hr 30 min", (0,0) -> "Off" */
    fun repeatDisplay(hours: Int, minutes: Int): String = when {
        hours == 0 && minutes == 0 -> "Off"
        hours == 0 -> "$minutes min"
        minutes == 0 -> "$hours hr"
        else -> "$hours hr $minutes min"
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.value = read(context)
    }

    fun setDefaultTime(context: Context, hhmm: String) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TIME, hhmm).apply()
        _state.value = read(context)
    }

    /** "14:05" -> "2:05 PM" */
    fun display(hhmm: String): String {
        val p = hhmm.split(":")
        val h = p.getOrNull(0)?.toIntOrNull() ?: return hhmm
        val m = p.getOrNull(1)?.toIntOrNull() ?: 0
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "%d:%02d %s".format(h12, m, if (h < 12) "AM" else "PM")
    }
}

package com.example.tasktracker.reminder

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Global notification settings (stored on the device).
 *  - enabled     : master switch, ON by default -> every task sends its notification
 *  - defaultTime : "HH:mm", 05:00 by default. Every task that has no time of its own
 *                  (task.reminderTime blank) is notified at this time.
 */
object NotificationSettings {
    const val DEFAULT_TIME = "05:00"
    private const val PREFS = "reminder_prefs"
    private const val KEY_ENABLED = "notifications_enabled"
    private const val KEY_TIME = "default_reminder_time"

    data class State(val enabled: Boolean = true, val defaultTime: String = DEFAULT_TIME)

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
            defaultTime = p.getString(KEY_TIME, DEFAULT_TIME)?.takeIf { it.isNotBlank() } ?: DEFAULT_TIME
        )
    }

    fun isEnabled(context: Context) = read(context).enabled
    fun defaultTime(context: Context) = read(context).defaultTime

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

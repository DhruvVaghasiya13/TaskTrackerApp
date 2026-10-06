package com.example.tasktracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tasktracker.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Fires at the reminder time: shows the notification (unless the task is already completed), then plans the
 * next moment = the next repeat (if a repeat interval is set and the task is still not completed) or the next occurrence.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: return
        val dateKey = intent.getStringExtra("date") ?: return
        val firedAt = intent.getLongExtra("at", 0L)
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(appContext)
                val task = db.taskDao().get(taskId)
                if (task != null && !task.deleted && task.reminder) {
                    val key = "${taskId}_$dateKey"
                    val done = db.completionDao().get(key)?.done == true
                    val selected = db.selectionDao().get(key)?.selected
                    if (!done && selected != false) {
                        ReminderScheduler.notify(appContext, task, dateKey)
                    }
                    // Next moment must be AFTER the slot that just fired. If the phone delivered the alarm very late
                    // (Doze), missed repeats are skipped instead of firing in a burst.
                    val now = LocalDateTime.now()
                    val slot = if (firedAt > 0) LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(firedAt), java.time.ZoneId.systemDefault()) else now
                    val floor = now.minusSeconds(30)
                    val after = if (slot.isAfter(floor)) slot else floor
                    ReminderScheduler.schedule(appContext, task, after)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

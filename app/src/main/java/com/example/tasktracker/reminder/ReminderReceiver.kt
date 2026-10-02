package com.example.tasktracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tasktracker.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Fires at the reminder time: shows the notification, then plans the next occurrence. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: return
        val dateKey = intent.getStringExtra("date") ?: return
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
                    // +1 min guard so an alarm that fires a moment early is not re-scheduled for the same minute
                    ReminderScheduler.schedule(appContext, task, LocalDateTime.now().plusMinutes(1))
                }
            } finally {
                pending.finish()
            }
        }
    }
}

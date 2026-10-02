package com.example.tasktracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tasktracker.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are wiped on reboot / app update - plan them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.sync(appContext, AppDatabase.get(appContext).taskDao().activeList())
            } finally {
                pending.finish()
            }
        }
    }
}

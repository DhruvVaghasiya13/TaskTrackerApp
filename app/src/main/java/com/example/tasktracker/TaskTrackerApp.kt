package com.example.tasktracker

import android.app.Application
import com.example.tasktracker.auth.AuthManager
import com.example.tasktracker.data.TaskRepository
import com.example.tasktracker.data.local.AppDatabase
import com.example.tasktracker.data.remote.FirestoreDataSource
import com.example.tasktracker.reminder.NotificationSettings
import com.example.tasktracker.reminder.ReminderScheduler
import com.example.tasktracker.ui.model.CategoryStore
import kotlinx.coroutines.flow.combine
import com.example.tasktracker.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskTrackerApp : Application() {
    lateinit var auth: AuthManager
    lateinit var repository: TaskRepository
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationSettings.init(this)
        // custom categories are needed to print the category name inside a notification (alarm may start the app process)
        CategoryStore.load(getSharedPreferences("task_tracker_prefs", MODE_PRIVATE).getString("custom_categories", null))
        auth = AuthManager()
        repository = TaskRepository(
            db = AppDatabase.get(this),
            remote = FirestoreDataSource(),
            auth = auth,
            onSyncFailed = { SyncWorker.syncWhenOnline(this) }
        )
        SyncWorker.schedule(this)
        // Re-plan notifications whenever tasks, completions (a completed task stops its repeats) or the notification
        // settings (on/off, default time, repeat interval) change (new / edited / deleted / synced from another device).
        appScope.launch {
            combine(
                repository.observeTasks(),
                repository.observeCompletions(),
                repository.observeSelections(),
                NotificationSettings.state
            ) { tasks, done, _, _ -> tasks to done }.collect { (tasks, done) ->
                runCatching { ReminderScheduler.sync(this@TaskTrackerApp, tasks) }
                // task completed -> its notification disappears from the tray
                val today = java.time.LocalDate.now().toString()
                runCatching {
                    ReminderScheduler.dismissCompleted(
                        this@TaskTrackerApp,
                        done.filter { it.date == today }.map { it.taskId to it.date }
                    )
                }
            }
        }
    }
}

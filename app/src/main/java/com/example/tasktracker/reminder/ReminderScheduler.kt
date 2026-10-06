package com.example.tasktracker.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.tasktracker.R
import com.example.tasktracker.data.local.AppDatabase
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.domain.occursOn
import com.example.tasktracker.ui.MainActivity
import com.example.tasktracker.ui.model.categoryById
import com.example.tasktracker.ui.model.priorityLabel
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Local task reminders. One alarm per reminder-enabled task = its NEXT occurrence
 * (due date for one-time tasks, next matching repeat day for repeating ones).
 * When an alarm fires, the notification is shown (unless the task is already done / deselected
 * for that day) and the next occurrence is scheduled.
 */
object ReminderScheduler {
    const val CHANNEL_ID = "task_reminders"
    const val ACTION = "com.example.tasktracker.REMINDER"
    private const val PREFS = "reminder_prefs"
    private const val KEY_IDS = "scheduled_ids"

    const val GROUP_KEY = "com.example.tasktracker.TASK_REMINDERS"
    private const val SUMMARY_ID = 1
    const val EXTRA_TASK_ID = "open_task_id"
    const val EXTRA_DATE = "open_task_date"
    const val EXTRA_OPEN_HOME = "open_home"

    /** "HH:mm" -> LocalTime. (Tasks without a chosen time are never scheduled, see schedule().) */
    fun parseTime(s: String): LocalTime {
        val p = s.trim().split(":")
        val h = p.getOrNull(0)?.toIntOrNull()
        val m = p.getOrNull(1)?.toIntOrNull() ?: 0
        return if (h != null && h in 0..23 && m in 0..59) LocalTime.of(h, m) else LocalTime.of(5, 0)
    }

    /**
     * Next reminder moment strictly after [after], or null if none.
     *
     * Every occurrence day has a base moment (task time or default time). When [repeatMinutes] > 0
     * the same day also has repeat moments: base + 1x, 2x, 3x ... interval (until midnight).
     * A repeat moment only counts while the task is still NOT completed ([isPending]).
     */
    suspend fun nextTrigger(
        task: TaskEntity,
        after: LocalDateTime = LocalDateTime.now(),
        defaultTime: String = NotificationSettings.DEFAULT_TIME,
        repeatMinutes: Int = 0,
        isPending: suspend (LocalDate) -> Boolean = { true }
    ): LocalDateTime? {
        // blank task time = "use the default time from Notification settings"
        val time = parseTime(task.reminderTime.ifBlank { defaultTime })
        val dueDate = runCatching { LocalDate.parse(task.due) }.getOrNull() ?: return null
        val isRepeating = task.repeatType == "daily" || task.repeatType == "weekly" || task.repeatType == "monthly"

        /** First moment on [day] after [after]: the base moment, or (if still pending) a repeat moment. */
        suspend fun slotOn(day: LocalDate): LocalDateTime? {
            val base = LocalDateTime.of(day, time)
            if (base.isAfter(after)) return base
            if (repeatMinutes <= 0) return null
            if (!isPending(day)) return null
            val step = repeatMinutes * 60L
            val passed = java.time.Duration.between(base, after).seconds
            val next = base.plusSeconds((passed / step + 1) * step)
            return if (next.toLocalDate() == day) next else null   // repeats stop at midnight
        }

        if (!isRepeating) return slotOn(dueDate)
        val start = after.toLocalDate()
        for (i in 0..400) {
            val day = start.plusDays(i.toLong())
            if (task.occursOn(day)) {
                slotOn(day)?.let { return it }
            }
        }
        return null
    }

    /** Task's own repeat interval if it has one (>= 0), otherwise the Profile default. */
    fun effectiveRepeatMinutes(task: TaskEntity, profileDefault: Int): Int =
        if (task.reminderRepeatMinutes >= 0) task.reminderRepeatMinutes else profileDefault

    /** Task is "pending" on [day] = not completed and not deselected for that day. */
    private suspend fun pendingOn(db: AppDatabase, task: TaskEntity, day: LocalDate): Boolean {
        val key = "${task.id}_$day"
        val done = db.completionDao().get(key)?.done == true
        val selected = db.selectionDao().get(key)?.selected
        return !done && selected != false
    }

    private fun pendingIntent(context: Context, taskId: String, dateKey: String, create: Boolean, atMillis: Long = 0L): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION
            data = Uri.parse("tasktracker://reminder/${Uri.encode(taskId)}")
            putExtra("taskId", taskId)
            putExtra("date", dateKey)
            putExtra("at", atMillis)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    private fun cancel(context: Context, taskId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        pendingIntent(context, taskId, "", create = false)?.let { am.cancel(it); it.cancel() }
    }

    /** Schedules (or cancels) the next reminder of one task (base time, or the next repeat while not completed). */
    suspend fun schedule(context: Context, task: TaskEntity, after: LocalDateTime = LocalDateTime.now()) {
        if (task.deleted || !task.reminder || !NotificationSettings.isEnabled(context)) { cancel(context, task.id); return }
        val db = AppDatabase.get(context)
        val next = nextTrigger(
            task, after,
            NotificationSettings.defaultTime(context),
            effectiveRepeatMinutes(task, NotificationSettings.repeatIntervalMinutes(context))
        ) { day -> pendingOn(db, task, day) }
        if (next == null) { cancel(context, task.id); return }

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = pendingIntent(context, task.id, next.toLocalDate().toString(), create = true, atMillis = millis) ?: return
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    /** Re-plans every reminder from the current task list (and drops alarms of deleted / changed tasks). */
    suspend fun sync(context: Context, activeTasks: List<TaskEntity>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()
        val eligible = if (!NotificationSettings.isEnabled(context)) emptyList()
        else activeTasks.filter { it.reminder && !it.deleted }
        val newIds = eligible.map { it.id }.toSet()
        (old - newIds).forEach { cancel(context, it) }
        eligible.forEach { schedule(context, it) }
        prefs.edit().putStringSet(KEY_IDS, newIds).apply()
    }

    /**
     * A completed task must stop bothering: remove its notification from the tray
     * (and the common summary when no task notification is left).
     */
    fun dismissCompleted(context: Context, doneTaskDateKeys: Collection<Pair<String, String>>) {
        if (doneTaskDateKeys.isEmpty()) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shown = runCatching { nm.activeNotifications.toList() }.getOrDefault(emptyList())
        if (shown.isEmpty()) return
        var cancelledAny = false
        doneTaskDateKeys.forEach { (taskId, dateKey) ->
            val id = (taskId + dateKey).hashCode()
            if (shown.any { it.id == id }) { nm.cancel(id); cancelledAny = true }
        }
        if (cancelledAny) {
            val left = runCatching { nm.activeNotifications.toList() }.getOrDefault(emptyList())
                .count { it.id != SUMMARY_ID && it.notification.group == GROUP_KEY }
            if (left == 0) nm.cancel(SUMMARY_ID)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Task reminders", NotificationManager.IMPORTANCE_HIGH)
                        .apply { description = "Reminders for your tasks" }
                )
            }
        }
    }

    private fun openAppIntent(context: Context, requestCode: Int, taskId: String?, dateKey: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (taskId != null && dateKey != null) {
                action = "com.example.tasktracker.OPEN_TASK"
                data = Uri.parse("tasktracker://task/${Uri.encode(taskId)}/$dateKey")
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_DATE, dateKey)
            } else {
                action = "com.example.tasktracker.OPEN_HOME"
                data = Uri.parse("tasktracker://home")
                putExtra(EXTRA_OPEN_HOME, true)
            }
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * One notification per task: title = task name, collapsed text = category + purpose,
     * expanded = category, date, priority, repeat, purpose. Tapping it opens THAT task's details.
     * All task notifications are grouped under ONE common summary notification;
     * tapping the summary opens the Home page with the task list.
     */
    @SuppressLint("MissingPermission")
    fun notify(context: Context, task: TaskEntity, dateKey: String) {
        if (!NotificationSettings.isEnabled(context)) return
        val nmc = NotificationManagerCompat.from(context)
        if (!nmc.areNotificationsEnabled()) return
        ensureChannel(context)

        val category = categoryById(task.category)
        val categoryText = listOfNotNull(
            category?.label,
            task.subcategory.takeIf { it.isNotBlank() }
        ).joinToString(" \u2022 ").ifBlank { "No category" }

        val dateText = runCatching {
            LocalDate.parse(dateKey).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
        }.getOrDefault(dateKey)

        // Short definition of the task = its PURPOSE (falls back to notes).
        val purpose = task.purpose.trim().ifBlank { task.notes.trim() }.take(200)
            .ifBlank { "It's time for this task." }

        val bigText = buildString {
            append("Category: ").append(categoryText).append('\n')
            append("Date: ").append(dateText).append('\n')
            append("Priority: ").append(priorityLabel(task.priority)).append('\n')
            append("Repeat: ").append(com.example.tasktracker.ui.model.repeatSummary(task)).append('\n')
            append("Purpose: ").append(purpose)
        }

        val taskNotificationId = (task.id + dateKey).hashCode()
        val open = openAppIntent(context, taskNotificationId, task.id, dateKey)

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.name)
            .setContentText("$categoryText  -  $purpose")
            .setSubText(dateText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(GROUP_KEY)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY) // the common notification alerts, not every task
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        nmc.notify(taskNotificationId, n)
        postSummary(context, nmc, taskNotificationId, task.name)
    }

    /** The common notification: "N tasks" + list of names. Tap -> Home page. */
    @SuppressLint("MissingPermission")
    private fun postSummary(context: Context, nmc: NotificationManagerCompat, currentId: Int, currentName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val active = runCatching { nm.activeNotifications.toList() }.getOrDefault(emptyList())

        // A summary left over from an earlier batch / earlier repeat is replaced, so the new one alerts again.
        // (20 s: tasks that fire in the same minute still make just ONE sound, but a repeat reminder always alerts.)
        val oldSummary = active.firstOrNull { it.id == SUMMARY_ID }
        if (oldSummary != null && System.currentTimeMillis() - oldSummary.postTime > 20 * 1000L) {
            nmc.cancel(SUMMARY_ID)
        }

        val names = active
            .filter { it.id != SUMMARY_ID && it.notification.group == GROUP_KEY && it.id != currentId }
            .mapNotNull { it.notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() }
            .toMutableList()
        names.add(currentName)

        val count = names.size
        val inbox = NotificationCompat.InboxStyle()
        names.take(6).forEach { inbox.addLine(it) }
        if (count > 6) inbox.setSummaryText("+${count - 6} more")

        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task Tracker")
            .setContentText(if (count == 1) "1 task to do" else "$count tasks to do")
            .setStyle(inbox)
            .setNumber(count)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, SUMMARY_ID, null, null))
            .build()
        nmc.notify(SUMMARY_ID, summary)
    }
}

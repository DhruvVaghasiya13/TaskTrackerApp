package com.example.tasktracker.data

import com.example.tasktracker.auth.AuthManager
import com.example.tasktracker.data.local.AppDatabase
import com.example.tasktracker.data.local.CompletionEntity
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.data.local.TaskSelectionEntity
import com.example.tasktracker.data.remote.FirestoreDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID

class TaskRepository(
    private val db: AppDatabase,
    private val remote: FirestoreDataSource,
    private val auth: AuthManager,
    /** Called when a background sync fails, so a retry can be scheduled for when the network is back. */
    private val onSyncFailed: () -> Unit = {}
) {
    private val tasks = db.taskDao()
    private val completions = db.completionDao()
    private val selections = db.selectionDao()
    private val syncLock = Mutex()
    private companion object { const val OVERLAP_MS = 10 * 60 * 1000L }

    fun observeTasks() = tasks.observeActive()

    suspend fun getTask(id: String): TaskEntity? = tasks.get(id)

    /** Every task goes back to "use the default notification time" (blank reminderTime). */
    suspend fun useDefaultReminderTimeForAll() {
        val now = System.currentTimeMillis()
        val changed = tasks.activeList().filter { it.reminderTime.isNotBlank() }
            .map { it.copy(reminderTime = "", updatedAt = now, dirty = true) }
        if (changed.isNotEmpty()) {
            tasks.upsertAll(changed)
            trySyncQuietly()
        }
    }

    /** Live tasks + deleted / replaced ones (still needed to show past days correctly). */
    fun observeHistoryTasks() = tasks.observeHistory()
    fun observeCompletions() = completions.observeDone()
    fun observeSelections() = selections.observeAll()

    fun newTask(name: String, due: String): TaskEntity {
        val now = System.currentTimeMillis()
        return TaskEntity(id = UUID.randomUUID().toString(), name = name, due = due, createdAt = now, updatedAt = now)
    }

    /** Fields that decide WHICH days a task shows on and how past days look in history / progress. */
    private fun historyChanged(a: TaskEntity, b: TaskEntity) =
        a.name != b.name || a.category != b.category || a.subcategory != b.subcategory ||
        a.priority != b.priority || a.due != b.due || a.repeatType != b.repeatType ||
        a.weeklyMode != b.weeklyMode || a.weeklyDays != b.weeklyDays ||
        a.monthlyMode != b.monthlyMode || a.monthlyDates != b.monthlyDates ||
        a.customRulesJson != b.customRulesJson

    /**
     * Saves a task WITHOUT rewriting the past.
     *  - new task / task that started today or later / edit that does not touch schedule or name
     *    -> plain in-place save.
     *  - edit of a task that already has past days -> the old version is frozen (endDate = today,
     *    keeps its past completions) and the edited version starts TODAY as a fresh task.
     */
    suspend fun saveTask(task: TaskEntity) {
        val now = System.currentTimeMillis()
        val today = LocalDate.now().toString()
        val old = tasks.get(task.id)

        if (old != null && !old.deleted && old.due < today && historyChanged(old, task)) {
            // 1) freeze the old version: it keeps every day before today exactly as it was
            tasks.upsert(old.copy(deleted = true, endDate = today, reminder = false, updatedAt = now, dirty = true))
            // 2) the edited version is a new task that starts today (never earlier)
            val newId = UUID.randomUUID().toString()
            tasks.upsert(
                task.copy(
                    id = newId,
                    due = if (task.due < today) today else task.due,
                    deleted = false, endDate = "", updatedAt = now, dirty = true
                )
            )
            // 3) carry today's / future ticks over to the new version, so a task ticked today stays ticked
            completions.doneFrom(old.id, today).forEach { c ->
                completions.upsert(CompletionEntity("${newId}_${c.date}", newId, c.date, true, now))
                completions.upsert(c.copy(done = false, updatedAt = now, dirty = true))
            }
            selections.fromDate(old.id, today).forEach { s ->
                selections.upsert(TaskSelectionEntity("${newId}_${s.date}", newId, s.date, s.selected, now))
            }
        } else {
            tasks.upsert(task.copy(updatedAt = now, dirty = true))
        }
        trySyncQuietly()
    }

    /**
     * Deletes a task from TODAY onwards only. Past days keep it (and whether it was done),
     * so calendar history and progress never change retroactively.
     */
    suspend fun deleteTask(id: String) {
        val t = tasks.get(id) ?: return
        val now = System.currentTimeMillis()
        val today = LocalDate.now().toString()
        tasks.upsert(t.copy(deleted = true, endDate = today, updatedAt = now, dirty = true))
        completions.clearForTaskFrom(id, today, now)
        trySyncQuietly()
    }

    suspend fun toggleDone(taskId: String, date: String) {
        val id = "${taskId}_$date"
        val existing = completions.get(id)
        completions.upsert(
            CompletionEntity(id, taskId, date, done = !(existing?.done ?: false), updatedAt = System.currentTimeMillis())
        )
        trySyncQuietly()
    }

    suspend fun setSelection(taskId: String, date: String, selected: Boolean) {
        val id = "${taskId}_$date"
        selections.upsert(
            TaskSelectionEntity(id, taskId, date, selected, System.currentTimeMillis())
        )
        trySyncQuietly()
    }

    /**
     * Push local changes, then pull the cloud copy. A failed push must NOT stop the pull
     * (and vice-versa) - each step is attempted, and the first error is re-thrown at the end
     * so the caller can show it instead of failing silently.
     */
    suspend fun sync() {
        val uid = auth.uid ?: return
        syncLock.withLock {
            var firstError: Exception? = null
            try { push(uid) } catch (e: CancellationException) { throw e } catch (e: Exception) { firstError = e }
            try { pull(uid) } catch (e: CancellationException) { throw e } catch (e: Exception) { if (firstError == null) firstError = e }
            firstError?.let { throw it }
        }
    }

    /** How many local rows are still waiting to be uploaded to the cloud. */
    suspend fun pendingCount(): Int =
        tasks.dirtyRows().size + completions.dirtyRows().size + selections.dirtyRows().size

    private suspend fun push(uid: String) {
        tasks.dirtyRows().chunked(FirestoreDataSource.BATCH_LIMIT).forEach { chunk ->
            remote.pushTasks(uid, chunk)
            chunk.forEach { tasks.markSynced(it.id, it.updatedAt) }
        }
        completions.dirtyRows().chunked(FirestoreDataSource.BATCH_LIMIT).forEach { chunk ->
            remote.pushCompletions(uid, chunk)
            chunk.forEach { completions.markSynced(it.id, it.updatedAt) }
        }
        selections.dirtyRows().chunked(FirestoreDataSource.BATCH_LIMIT).forEach { chunk ->
            remote.pushSelections(uid, chunk)
            chunk.forEach { selections.markSynced(it.id, it.updatedAt) }
        }
    }

    // Incremental pull: first pull after app start is full, later pulls only fetch what changed
    // (keeps Firestore reads tiny even with the 30-second auto-sync).
    private var pulledUid: String? = null
    private var lastPullAt = 0L

    private suspend fun pull(uid: String) {
        val startedAt = System.currentTimeMillis()
        val since = if (pulledUid == uid && lastPullAt > 0) lastPullAt - OVERLAP_MS else 0L

        tasks.upsertAll(remote.fetchTasks(uid, since).filter { r ->
            val l = tasks.get(r.id); l == null || r.updatedAt > l.updatedAt
        })
        completions.upsertAll(remote.fetchCompletions(uid, since).filter { r ->
            val l = completions.get(r.id); l == null || r.updatedAt > l.updatedAt
        })
        selections.upsertAll(remote.fetchSelections(uid, since).filter { r ->
            val l = selections.get(r.id); l == null || r.updatedAt > l.updatedAt
        })
        pulledUid = uid
        lastPullAt = startedAt
    }

    suspend fun fetchSettings(): Map<String, Any>? {
        val uid = auth.uid ?: return null
        return remote.fetchSettings(uid)
    }

    suspend fun fetchPhoto(): String? {
        val uid = auth.uid ?: return null
        return remote.fetchPhoto(uid)
    }

    suspend fun pushPhoto(base64: String, stamp: Long) {
        val uid = auth.uid ?: return
        remote.pushPhoto(uid, base64, stamp)
    }

    suspend fun pushSettings(data: Map<String, Any>) {
        val uid = auth.uid ?: return
        remote.pushSettings(uid, data)
    }

    suspend fun wipeLocal() {
        tasks.clear(); completions.clear(); selections.clear()
    }

    private suspend fun trySyncQuietly() {
        try {
            sync()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            onSyncFailed()
        }
    }
}

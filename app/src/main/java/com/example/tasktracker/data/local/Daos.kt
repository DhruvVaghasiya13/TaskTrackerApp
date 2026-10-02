package com.example.tasktracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY due, createdAt")
    fun observeActive(): Flow<List<TaskEntity>>

    /**
     * Everything the calendar / progress history needs: live tasks PLUS tasks that were deleted or
     * replaced (they carry an endDate and still belong to the days before it).
     * Old-style tombstones without an endDate stay hidden.
     */
    @Query("SELECT * FROM tasks WHERE deleted = 0 OR endDate != '' ORDER BY due, createdAt")
    fun observeHistory(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deleted = 0")
    suspend fun activeList(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE dirty = 1")
    suspend fun dirtyRows(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    /** Only clears the flag if nobody edited the row while the push was in flight. */
    @Query("UPDATE tasks SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("DELETE FROM tasks")
    suspend fun clear()
}

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions WHERE done = 1")
    fun observeDone(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE id = :id")
    suspend fun get(id: String): CompletionEntity?

    @Query("SELECT * FROM completions WHERE dirty = 1")
    suspend fun dirtyRows(): List<CompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: CompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<CompletionEntity>)

    @Query("UPDATE completions SET done = 0, dirty = 1, updatedAt = :now WHERE taskId = :taskId AND done = 1")
    suspend fun clearForTask(taskId: String, now: Long)

    /** Un-completes a task from [fromDate] onwards only - earlier days are history and stay untouched. */
    @Query("UPDATE completions SET done = 0, dirty = 1, updatedAt = :now WHERE taskId = :taskId AND done = 1 AND date >= :fromDate")
    suspend fun clearForTaskFrom(taskId: String, fromDate: String, now: Long)

    @Query("SELECT * FROM completions WHERE taskId = :taskId AND done = 1 AND date >= :fromDate")
    suspend fun doneFrom(taskId: String, fromDate: String): List<CompletionEntity>

    @Query("UPDATE completions SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("DELETE FROM completions")
    suspend fun clear()
}

@Dao
interface SelectionDao {
    @Query("SELECT * FROM task_selections")
    fun observeAll(): Flow<List<TaskSelectionEntity>>

    @Query("SELECT * FROM task_selections WHERE id = :id")
    suspend fun get(id: String): TaskSelectionEntity?

    @Query("SELECT * FROM task_selections WHERE dirty = 1")
    suspend fun dirtyRows(): List<TaskSelectionEntity>

    @Query("SELECT * FROM task_selections WHERE taskId = :taskId AND date >= :fromDate")
    suspend fun fromDate(taskId: String, fromDate: String): List<TaskSelectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: TaskSelectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<TaskSelectionEntity>)

    @Query("UPDATE task_selections SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("DELETE FROM task_selections")
    suspend fun clear()
}

package com.example.tasktracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per (task, date). id = "<taskId>_<yyyy-MM-dd>", same key format as the web tracker. */
@Entity(tableName = "completions")
data class CompletionEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val date: String,
    val done: Boolean,
    val updatedAt: Long,
    val dirty: Boolean = true
)

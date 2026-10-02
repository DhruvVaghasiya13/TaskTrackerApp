package com.example.tasktracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_selections")
data class TaskSelectionEntity(
    @PrimaryKey val id: String, // "<taskId>_<yyyy-MM-dd>"
    val taskId: String,
    val date: String,
    val selected: Boolean,
    val updatedAt: Long,
    val dirty: Boolean = true
)

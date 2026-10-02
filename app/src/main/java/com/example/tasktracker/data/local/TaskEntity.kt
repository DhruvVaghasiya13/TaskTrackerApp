package com.example.tasktracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors the tracker's task object. Sync bookkeeping:
 *  - updatedAt : last-write-wins clock (epoch millis)
 *  - deleted   : tombstone, so deletions propagate to the cloud and other devices
 *  - dirty     : true = has local changes not yet pushed to Firestore
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String = "",
    val subcategory: String = "",
    val priority: String = "med",            // high | med | low
    val due: String,                         // yyyy-MM-dd
    val repeatType: String = "none",         // none | daily | weekly | monthly | custom
    val weeklyMode: String = "include",      // include | exclude
    val weeklyDays: List<Int> = emptyList(), // 0=Sun..6=Sat (same as JS getDay)
    val monthlyMode: String = "include",
    val monthlyDates: List<Int> = emptyList(),
    val customRulesJson: String = "[]",      // [{type, value, time}]
    val reminder: Boolean = false,
    val reminderTime: String = "",
    val purpose: String = "",
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    /**
     * First day the task NO LONGER applies (yyyy-MM-dd, "" = still running).
     * Set when a task is deleted or replaced by an edited version. Days BEFORE endDate keep
     * showing it in the calendar history, so deleting / editing never rewrites the past.
     */
    val endDate: String = "",
    val dirty: Boolean = true
)

package com.example.tasktracker.data.remote

import com.example.tasktracker.data.local.CompletionEntity
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.data.local.TaskSelectionEntity
import com.google.firebase.firestore.DocumentSnapshot

fun TaskEntity.toMap(): Map<String, Any> = mapOf(
    "name" to name, "category" to category, "subcategory" to subcategory,
    "priority" to priority, "due" to due, "repeat" to repeatType,
    "weeklyMode" to weeklyMode, "weeklyDays" to weeklyDays,
    "monthlyMode" to monthlyMode, "monthlyDates" to monthlyDates,
    "customRules" to customRulesJson, "reminder" to reminder,
    "reminderTime" to reminderTime, "reminderRepeatMinutes" to reminderRepeatMinutes, "purpose" to purpose, "notes" to notes,
    "createdAt" to createdAt, "updatedAt" to updatedAt, "deleted" to deleted,
    "endDate" to endDate
)

private fun Any?.ints(): List<Int> =
    (this as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()

fun DocumentSnapshot.toTask(id: String) = TaskEntity(
    id = id,
    name = getString("name") ?: "",
    category = getString("category") ?: "",
    subcategory = getString("subcategory") ?: "",
    priority = getString("priority") ?: "med",
    due = getString("due") ?: "",
    repeatType = getString("repeat") ?: "none",
    weeklyMode = getString("weeklyMode") ?: "include",
    weeklyDays = get("weeklyDays").ints(),
    monthlyMode = getString("monthlyMode") ?: "include",
    monthlyDates = get("monthlyDates").ints(),
    customRulesJson = getString("customRules") ?: "[]",
    reminder = getBoolean("reminder") ?: false,
    reminderTime = getString("reminderTime") ?: "",
    reminderRepeatMinutes = getLong("reminderRepeatMinutes")?.toInt() ?: -1,
    purpose = getString("purpose") ?: "",
    notes = getString("notes") ?: "",
    createdAt = getLong("createdAt") ?: 0L,
    updatedAt = getLong("updatedAt") ?: 0L,
    deleted = getBoolean("deleted") ?: false,
    endDate = getString("endDate") ?: "",
    dirty = false
)

fun CompletionEntity.toMap(): Map<String, Any> = mapOf(
    "taskId" to taskId, "date" to date, "done" to done, "updatedAt" to updatedAt
)

fun DocumentSnapshot.toCompletion(id: String) = CompletionEntity(
    id = id,
    taskId = getString("taskId") ?: "",
    date = getString("date") ?: "",
    done = getBoolean("done") ?: false,
    updatedAt = getLong("updatedAt") ?: 0L,
    dirty = false
)

fun TaskSelectionEntity.toMap(): Map<String, Any> = mapOf(
    "taskId" to taskId, "date" to date, "selected" to selected, "updatedAt" to updatedAt
)

fun DocumentSnapshot.toSelection(id: String) = TaskSelectionEntity(
    id = id,
    taskId = getString("taskId") ?: "",
    date = getString("date") ?: "",
    selected = getBoolean("selected") ?: false,
    updatedAt = getLong("updatedAt") ?: 0L,
    dirty = false
)

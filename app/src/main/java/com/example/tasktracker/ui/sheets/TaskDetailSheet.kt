package com.example.tasktracker.ui.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.tasktracker.reminder.NotificationSettings
import java.time.LocalDate
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.ui.components.CategoryTag
import com.example.tasktracker.ui.components.PriorityPill
import com.example.tasktracker.ui.model.categoryById
import com.example.tasktracker.ui.model.repeatSummary
import com.example.tasktracker.ui.theme.extraColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    task: TaskEntity,
    isDone: Boolean,
    isOverdue: Boolean,
    /** The day (yyyy-MM-dd) this task was opened for (today, a calendar day, or a notification day). */
    dateKey: String = "",
    /** false for past days: history is locked, the task can only be changed from today onwards. */
    canEdit: Boolean = true,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.extraColors
    val category = categoryById(task.category)
    val createdStr = try {
        Instant.ofEpochMilli(task.createdAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (_: Exception) { "—" }

    val notif by NotificationSettings.state.collectAsState()
    val timeText = if (task.reminderTime.isBlank())
        NotificationSettings.display(notif.defaultTime) + " (default)"
    else NotificationSettings.display(task.reminderTime)
    val repeatMin = if (task.reminderRepeatMinutes >= 0) task.reminderRepeatMinutes else notif.repeatIntervalMinutes
    val repeatSuffix = if (task.reminderRepeatMinutes >= 0) "" else " (default)"
    val notifText = when {
        !task.reminder -> "Off"
        !notif.enabled -> "Turned off in Notification settings"
        repeatMin > 0 ->
            "On - at $timeText, repeats every " + NotificationSettings.repeatDisplay(repeatMin / 60, repeatMin % 60) + repeatSuffix + " until completed"
        else -> "On - at $timeText"
    }
    val dayText = runCatching {
        LocalDate.parse(dateKey).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
    }.getOrNull()
    val dueText = runCatching {
        LocalDate.parse(task.due).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
    }.getOrDefault(task.due)
    val updatedStr = try {
        Instant.ofEpochMilli(task.updatedAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a"))
    } catch (_: Exception) { "—" }
    val endText = if (task.endDate.isNotBlank()) runCatching {
        LocalDate.parse(task.endDate).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
    }.getOrDefault(task.endDate) else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // open fully so every detail is visible (the content scrolls if it is long)
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.paper,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = task.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ink,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DetailRow(label = "Status") {
                Text(
                    text = if (isDone) "✓ Completed" else if (isOverdue) "Overdue" else "Not done",
                    fontSize = 14.sp,
                    color = if (isDone) colors.accent else if (isOverdue) colors.high else colors.ink,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (dayText != null) {
                DetailRow(label = "Date") {
                    Text(dayText, fontSize = 14.sp, color = colors.ink)
                }
            }

            DetailRow(label = "Category") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (category != null) {
                        CategoryTag(category = category)
                    }
                    if (task.subcategory.isNotBlank()) {
                        Text(
                            text = "— ${task.subcategory}",
                            fontSize = 14.sp,
                            color = colors.ink
                        )
                    }
                    if (category == null && task.subcategory.isBlank()) {
                        Text("None", fontSize = 14.sp, color = colors.muted)
                    }
                }
            }

            DetailRow(label = "Priority") {
                PriorityPill(priority = task.priority)
            }

            DetailRow(label = "Start / Due Date") {
                Text(dueText, fontSize = 14.sp, color = colors.ink)
            }

            if (endText != null) {
                DetailRow(label = "Ended On") {
                    Text(endText, fontSize = 14.sp, color = colors.ink)
                }
            }

            DetailRow(label = "Repeat") {
                Text(repeatSummary(task), fontSize = 14.sp, color = colors.ink)
            }

            DetailRow(label = "Notification") {
                Text(notifText, fontSize = 14.sp, color = colors.ink)
            }

            DetailRow(label = "Created On") {
                Text(createdStr, fontSize = 14.sp, color = colors.ink)
            }

            DetailRow(label = "Last Updated") {
                Text(updatedStr, fontSize = 14.sp, color = colors.ink)
            }

            DetailRow(label = "Purpose") {
                Text(
                    text = task.purpose.ifBlank { "None added" },
                    fontSize = 14.sp,
                    color = if (task.purpose.isBlank()) colors.muted else colors.ink
                )
            }

            DetailRow(label = "Notes") {
                Text(
                    text = task.notes.ifBlank { "None added" },
                    fontSize = 14.sp,
                    color = if (task.notes.isBlank()) colors.muted else colors.ink
                )
            }

            Spacer(Modifier.height(18.dp))

            if (canEdit) {
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.paper),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Edit task", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "This day is history and is locked. Changes you make to a task apply from today onwards.",
                    fontSize = 12.sp,
                    color = colors.muted,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.extraColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.muted
        )
        Spacer(Modifier.height(2.dp))
        content()
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = colors.line)
    }
}

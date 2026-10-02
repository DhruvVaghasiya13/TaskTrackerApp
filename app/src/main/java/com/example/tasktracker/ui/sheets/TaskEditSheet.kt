package com.example.tasktracker.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.ui.components.CategoryChipButton
import com.example.tasktracker.ui.components.SegmentedControl
import com.example.tasktracker.ui.model.allCategories
import com.example.tasktracker.ui.model.categoryById
import com.example.tasktracker.ui.theme.extraColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditSheet(
    task: TaskEntity?,
    onSave: (TaskEntity) -> Unit,
    onDelete: ((String) -> Unit)?,
    onDismiss: () -> Unit,
    /** adds a user category, returns its id */
    onAddCategory: (String) -> String? = { null },
    /** adds a user task type to a category (categoryId, name), returns the stored name */
    onAddSubcategory: (String, String) -> String? = { _, _ -> null }
) {
    val colors = MaterialTheme.extraColors

    var name by remember { mutableStateOf(task?.name ?: "") }
    var selectedCatId by remember { mutableStateOf(task?.category ?: "") }
    var subcategory by remember { mutableStateOf(task?.subcategory ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: "med") }
    var due by remember { mutableStateOf(task?.due ?: LocalDate.now().toString()) }
    var repeatType by remember { mutableStateOf(task?.repeatType ?: "daily") } // Default save in daily for new tasks

    var weeklyMode by remember { mutableStateOf(task?.weeklyMode ?: "include") }
    var weeklyDays by remember { mutableStateOf(task?.weeklyDays ?: emptyList()) }

    var monthlyMode by remember { mutableStateOf(task?.monthlyMode ?: "include") }
    var monthlyDates by remember { mutableStateOf(task?.monthlyDates ?: emptyList()) }

    // Notification time: always on. Blank = follow the default time from Notification settings (5:00 AM unless changed).
    val notifState by com.example.tasktracker.reminder.NotificationSettings.state.collectAsState()
    val defaultTime = notifState.defaultTime
    var reminderTime by remember { mutableStateOf(task?.reminderTime.orEmpty()) }
    val effectiveTime = reminderTime.ifBlank { defaultTime }
    var purpose by remember { mutableStateOf(task?.purpose ?: "") }
    var notes by remember { mutableStateOf(task?.notes ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }
    var showAddSubcategory by remember { mutableStateOf(false) }

    // read straight from the store so newly added categories / task types show up immediately
    val categoryObj = categoryById(selectedCatId)
    val availableSubcats = categoryObj?.subcategories ?: emptyList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.paper,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (task != null) "Edit task" else "New task",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ink
            )

            // Task Name
            Column {
                FieldLabel("TASK NAME")
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it },
                    placeholder = { Text("e.g. Post client testimonial") },
                    singleLine = true,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category
            Column {
                FieldLabel("CATEGORY")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryChipButton(
                        category = null,
                        selected = selectedCatId.isBlank(),
                        onSelect = { selectedCatId = ""; subcategory = "" },
                        label = "None"
                    )
                    allCategories().forEach { cat ->
                        CategoryChipButton(
                            category = cat,
                            selected = selectedCatId == cat.id,
                            onSelect = { selectedCatId = cat.id; subcategory = "" }
                        )
                    }
                    CategoryChipButton(
                        category = null,
                        selected = false,
                        onSelect = { showAddCategory = true },
                        label = "+ Add category"
                    )
                }
            }

            // Subcategory Dropdown
            if (selectedCatId.isNotBlank()) {
                Column {
                    FieldLabel("SUBCATEGORY / TASK TYPE")
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (subcategory.isBlank()) "None" else subcategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(colors.card)
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = { subcategory = ""; expanded = false }
                            )
                            availableSubcats.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub) },
                                    onClick = { subcategory = sub; expanded = false }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Add new task type", fontWeight = FontWeight.Bold, color = colors.accent) },
                                onClick = { expanded = false; showAddSubcategory = true }
                            )
                        }
                    }
                }
            }

            // Priority
            Column {
                FieldLabel("PRIORITY")
                SegmentedControl(
                    options = listOf("low" to "Low", "med" to "Medium", "high" to "High"),
                    selectedId = priority,
                    onSelect = { priority = it }
                )
            }

            // Due Date
            Column {
                FieldLabel("DUE DATE")
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = due,
                        onValueChange = {},
                        readOnly = true,
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
            }

            // Repeat Mode (None, Daily, Weekly, Monthly)
            Column {
                FieldLabel("REPEAT")
                SegmentedControl(
                    options = listOf(
                        "none" to "None",
                        "daily" to "Daily",
                        "weekly" to "Weekly",
                        "monthly" to "Monthly"
                    ),
                    selectedId = repeatType,
                    onSelect = { repeatType = it }
                )
            }

            // Weekly: Repeated days / Non-repeated days + weekday chips
            if (repeatType == "weekly") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("WEEKLY DAYS")
                    SegmentedControl(
                        options = listOf("include" to "Repeated days", "exclude" to "Non-repeated days"),
                        selectedId = weeklyMode,
                        onSelect = { weeklyMode = it }
                    )
                    Text(
                        text = if (weeklyMode == "include")
                            "Task repeats ONLY on the days you select."
                        else
                            "Task repeats on every day EXCEPT the days you select.",
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        val daysList = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        daysList.forEachIndexed { index, dayName ->
                            val isSelected = index in weeklyDays
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) colors.accent else colors.card)
                                    .border(1.dp, if (isSelected) colors.accent else colors.line, RoundedCornerShape(20.dp))
                                    .clickable {
                                        weeklyDays = if (isSelected) weeklyDays - index else weeklyDays + index
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = dayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) colors.paper else colors.ink
                                )
                            }
                        }
                    }
                }
            }

            // Monthly: Repeated dates / Non-repeated dates + tap-to-select date grid (1-31)
            if (repeatType == "monthly") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("MONTHLY DATES")
                    SegmentedControl(
                        options = listOf("include" to "Repeated days", "exclude" to "Non-repeated days"),
                        selectedId = monthlyMode,
                        onSelect = { monthlyMode = it }
                    )
                    Text(
                        text = if (monthlyMode == "include")
                            "Task repeats ONLY on the dates you select."
                        else
                            "Task repeats on every date EXCEPT the dates you select.",
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                    (1..31).chunked(7).forEach { rowDates ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            rowDates.forEach { day ->
                                val isSelected = day in monthlyDates
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) colors.accent else colors.card)
                                        .border(1.dp, if (isSelected) colors.accent else colors.line, RoundedCornerShape(10.dp))
                                        .clickable {
                                            monthlyDates = if (isSelected) monthlyDates - day else monthlyDates + day
                                        }
                                ) {
                                    Text(
                                        text = day.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) colors.paper else colors.ink
                                    )
                                }
                            }
                            repeat(7 - rowDates.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Text(
                        text = "Date 29-31 falls on the last day of shorter months.",
                        fontSize = 11.sp,
                        color = colors.muted
                    )
                }
            }

            // Notification time (default 5:00 AM)
            Column {
                FieldLabel("TIME (NOTIFICATION)")
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = displayTime(effectiveTime),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent, modifier = Modifier.padding(end = 12.dp)) },
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // transparent layer on top: a plain tap anywhere on the field opens the clock
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showTimePicker = true }
                    )
                }
                Text(
                    text = run {
                        val when_ = if (repeatType == "none") "on the due date" else "on every repeat day"
                        if (reminderTime.isBlank())
                            "Default time from Notification settings - tap to use a different time for this task. The notification arrives at this time $when_."
                        else
                            "Custom time for this task. The notification arrives at this time $when_."
                    },
                    fontSize = 12.sp,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Purpose
            Column {
                FieldLabel("PURPOSE")
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { if (it.length <= 150) purpose = it },
                    placeholder = { Text("Short definition - shown in the notification") },
                    singleLine = true,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Notes
            Column {
                FieldLabel("NOTES")
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 300) notes = it },
                    placeholder = { Text("Complete details, chapter, or checklist") },
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val dueDateParsed = runCatching { LocalDate.parse(due) }.getOrDefault(LocalDate.now())
                    // "Repeated days" with nothing selected -> use the due date's weekday / day-of-month
                    val finalWeeklyDays =
                        if (repeatType == "weekly" && weeklyMode == "include" && weeklyDays.isEmpty())
                            listOf(if (dueDateParsed.dayOfWeek == java.time.DayOfWeek.SUNDAY) 0 else dueDateParsed.dayOfWeek.value)
                        else weeklyDays.distinct().sorted()
                    val finalMonthlyDates =
                        if (repeatType == "monthly" && monthlyMode == "include" && monthlyDates.isEmpty())
                            listOf(dueDateParsed.dayOfMonth)
                        else monthlyDates.distinct().sorted()

                    val now = System.currentTimeMillis()
                    val newTask = TaskEntity(
                        id = task?.id ?: (now.toString() + (100..999).random()),
                        name = name.trim(),
                        category = selectedCatId,
                        subcategory = subcategory,
                        priority = priority,
                        due = due,
                        repeatType = repeatType,
                        weeklyMode = weeklyMode,
                        weeklyDays = finalWeeklyDays,
                        monthlyMode = monthlyMode,
                        monthlyDates = finalMonthlyDates,
                        customRulesJson = "[]",
                        reminder = true,
                        reminderTime = reminderTime, // "" = default time from Notification settings
                        purpose = purpose.trim(),
                        notes = notes.trim(),
                        createdAt = task?.createdAt ?: now,
                        updatedAt = now,
                        deleted = false,
                        dirty = true
                    )
                    onSave(newTask)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.paper),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Save task", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            if (task != null && onDelete != null) {
                TextButton(
                    onClick = {
                        onDelete(task.id)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete task", color = colors.high, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showTimePicker) {
        val parts = effectiveTime.split(":")
        val timeState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 5,
            initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
            is24Hour = false
        )
        val cancelTime = { showTimePicker = false }
        AlertDialog(
            onDismissRequest = cancelTime,
            confirmButton = {
                TextButton(onClick = {
                    val picked = "%02d:%02d".format(timeState.hour, timeState.minute)
                    reminderTime = if (picked == defaultTime) "" else picked
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = cancelTime) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }

    if (showAddCategory) {
        AddNameDialog(
            title = "New category",
            placeholder = "e.g. Fitness",
            maxLen = 24,
            onConfirm = { label ->
                onAddCategory(label)?.let { id -> selectedCatId = id; subcategory = "" }
                showAddCategory = false
            },
            onDismiss = { showAddCategory = false }
        )
    }

    if (showAddSubcategory) {
        AddNameDialog(
            title = "New task type",
            placeholder = "e.g. Yoga",
            maxLen = 30,
            onConfirm = { label ->
                onAddSubcategory(selectedCatId, label)?.let { subcategory = it }
                showAddSubcategory = false
            },
            onDismiss = { showAddSubcategory = false }
        )
    }

    if (showDatePicker) {
        val initialDate = runCatching { LocalDate.parse(due) }.getOrDefault(LocalDate.now())
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        due = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** "14:05" -> "2:05 PM" */
private fun displayTime(hhmm: String): String {
    val p = hhmm.split(":")
    val h = p.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val m = p.getOrNull(1)?.toIntOrNull() ?: 0
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(h12, m, if (h < 12) "AM" else "PM")
}

@Composable
private fun AddNameDialog(
    title: String,
    placeholder: String,
    maxLen: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= maxLen) text = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                colors = textFieldColors(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FieldLabel(text: String) {
    val colors = MaterialTheme.extraColors
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = colors.muted,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.extraColors.accent,
    unfocusedBorderColor = MaterialTheme.extraColors.line,
    focusedContainerColor = MaterialTheme.extraColors.card,
    unfocusedContainerColor = MaterialTheme.extraColors.card,
    focusedTextColor = MaterialTheme.extraColors.ink,
    unfocusedTextColor = MaterialTheme.extraColors.ink
)

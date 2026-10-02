package com.example.tasktracker.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.domain.occursOn
import com.example.tasktracker.ui.components.CategoryChipButton
import com.example.tasktracker.ui.components.FilterChipButton
import com.example.tasktracker.ui.components.TaskCard
import com.example.tasktracker.ui.model.allCategories
import com.example.tasktracker.ui.model.PriorityFilter
import com.example.tasktracker.ui.theme.extraColors
import java.time.LocalDate

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    selectionsMap: Map<String, Boolean>,
    onToggleSelection: (TaskEntity, String) -> Unit,
    onQuickAdd: (String) -> Unit,
    onSelectTask: (TaskEntity, String) -> Unit,
    onEditTask: (TaskEntity) -> Unit
) {
    val colors = MaterialTheme.extraColors
    val todayKey = LocalDate.now().toString()

    val isTaskSelected: (TaskEntity, String) -> Boolean = remember(selectionsMap) {
        // same rule as Home / Calendar: explicit choice wins, otherwise "is it scheduled that day?"
        { task, dateKey ->
            selectionsMap["${task.id}_$dateKey"]
                ?: task.occursOn(runCatching { LocalDate.parse(dateKey) }.getOrDefault(LocalDate.now()))
        }
    }

    var searchText by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(PriorityFilter.ALL) }
    var selectedCategory by remember { mutableStateOf("all") }

    val filteredTasks = remember(tasks, searchText, selectedPriority, selectedCategory) {
        tasks.filter { t ->
            if (selectedPriority != PriorityFilter.ALL && t.priority.lowercase() != selectedPriority.id) return@filter false
            if (selectedCategory != "all" && t.category != selectedCategory) return@filter false
            if (searchText.isNotBlank() && !t.name.contains(searchText, ignoreCase = true)) return@filter false
            true
        }.sortedBy { it.due }
    }

    val unselectedTasks = remember(filteredTasks, selectionsMap, todayKey) {
        filteredTasks.filter { !isTaskSelected(it, todayKey) }
    }
    val selectedTasks = remember(filteredTasks, selectionsMap, todayKey) {
        filteredTasks.filter { isTaskSelected(it, todayKey) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Tasks",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.ink,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Search Input
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search tasks by name…", fontSize = 14.sp) },
            singleLine = true,
            colors = textFieldColors(),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 10.dp)
        ) {
            PriorityFilter.entries.forEach { p ->
                FilterChipButton(
                    label = p.label,
                    selected = selectedPriority == p,
                    onSelect = { selectedPriority = p }
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 14.dp)
        ) {
            CategoryChipButton(
                category = null,
                selected = selectedCategory == "all",
                onSelect = { selectedCategory = "all" },
                label = "All"
            )
            allCategories().forEach { cat ->
                CategoryChipButton(
                    category = cat,
                    selected = selectedCategory == cat.id,
                    onSelect = { selectedCategory = cat.id }
                )
            }
        }

        // Two buttons: Perform Task (ticked, default) / Not Perform Task (unticked)
        var showPerform by remember { mutableStateOf(true) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            FilterChipButton(
                label = "Perform Task (${selectedTasks.size})",
                selected = showPerform,
                onSelect = { showPerform = true }
            )
            FilterChipButton(
                label = "Not Perform Task (${unselectedTasks.size})",
                selected = !showPerform,
                onSelect = { showPerform = false }
            )
        }

        val visibleTasks = if (showPerform) selectedTasks else unselectedTasks

        if (visibleTasks.isEmpty()) {
            Text(
                text = if (filteredTasks.isEmpty()) "No matching tasks."
                else if (showPerform) "No tasks to perform today." else "No tasks in Not Perform.",
                fontSize = 14.sp,
                color = colors.muted
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(visibleTasks, key = { "task_${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        done = false,
                        selected = showPerform,
                        showCompletion = false,
                        onToggleSelection = { onToggleSelection(task, todayKey) },
                        onClick = { onSelectTask(task, task.due) },
                        onEdit = { onEditTask(task) }
                    )
                }
            }
        }
    }
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

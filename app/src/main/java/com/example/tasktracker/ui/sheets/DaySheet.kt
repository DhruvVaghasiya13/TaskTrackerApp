package com.example.tasktracker.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.ui.components.HistoryTaskCard
import com.example.tasktracker.ui.theme.extraColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(
    dateKey: String,
    tasks: List<TaskEntity>,
    isDone: (TaskEntity) -> Boolean,
    isTaskSelected: (TaskEntity, String) -> Boolean,
    onSelectTask: (TaskEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.extraColors
    val titleDateStr = try {
        LocalDate.parse(dateKey).format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    } catch (_: Exception) { dateKey }

    val selectedTasks = tasks.filter { isTaskSelected(it, dateKey) }
    // Completed = done, Incomplete = selected but not done, Not Performed = unticked
    val (completedTasks, uncompletedTasks) = selectedTasks.partition { isDone(it) }
    val notPerformedTasks = tasks.filter { !isTaskSelected(it, dateKey) }

    var selectedFilter by remember { mutableStateOf("all") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.paper,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = titleDateStr,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ink
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp)
            ) {
                FilterTabButton(
                    label = "All (${tasks.size})",
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" }
                )
                FilterTabButton(
                    label = "Complete (${completedTasks.size})",
                    selected = selectedFilter == "complete",
                    onClick = { selectedFilter = "complete" }
                )
                FilterTabButton(
                    label = "Incomplete (${uncompletedTasks.size})",
                    selected = selectedFilter == "incomplete",
                    onClick = { selectedFilter = "incomplete" }
                )
                FilterTabButton(
                    label = "Not Performed (${notPerformedTasks.size})",
                    selected = selectedFilter == "not_performed",
                    onClick = { selectedFilter = "not_performed" }
                )
            }

            // "All" shows every group (Completed / Incomplete / Not Performed), a tab shows just that group
            val groups = when (selectedFilter) {
                "complete" -> listOf(Triple("Completed", completedTasks, 0))
                "incomplete" -> listOf(Triple("Incomplete", uncompletedTasks, 1))
                "not_performed" -> listOf(Triple("Not Performed", notPerformedTasks, 2))
                else -> listOf(
                    Triple("Completed", completedTasks, 0),
                    Triple("Incomplete", uncompletedTasks, 1),
                    Triple("Not Performed", notPerformedTasks, 2)
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
            ) {
                groups.forEach { (title, list, kind) ->
                    item(key = "sheet_header_$kind") {
                        Text(
                            text = "$title (${list.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (kind == 2) colors.high else colors.muted,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    if (list.isEmpty()) {
                        item(key = "sheet_empty_$kind") {
                            Text("No tasks", fontSize = 12.sp, color = colors.muted)
                        }
                    } else {
                        items(list, key = { "sheet_task_${kind}_${it.id}" }) { task ->
                            HistoryTaskCard(
                                task = task,
                                done = kind != 2 && isDone(task),
                                isNotPerformed = kind == 2,
                                onClick = { onSelectTask(task) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.extraColors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.accent else colors.card)
            .border(1.dp, if (selected) colors.accent else colors.line, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) colors.paper else colors.muted
        )
    }
}

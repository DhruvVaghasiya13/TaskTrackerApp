package com.example.tasktracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.ui.components.FilterChipButton
import com.example.tasktracker.ui.components.HistoryTaskCard
import com.example.tasktracker.ui.components.TaskCard
import com.example.tasktracker.ui.theme.extraColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    incompleteTasks: List<Pair<TaskEntity, String>>,
    completedTasks: List<Pair<TaskEntity, String>>,
    notPerformedTasks: List<Pair<TaskEntity, String>>,
    doneCount: Int,
    totalCount: Int,
    onToggleTask: (TaskEntity, String) -> Unit,
    onSelectTask: (TaskEntity, String) -> Unit,
    onEditTask: (TaskEntity) -> Unit
) {
    val colors = MaterialTheme.extraColors
    val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = todayStr,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.ink
        )
        Spacer(Modifier.height(16.dp))

        // Progress Overview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Circular Progress Indicator
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(64.dp),
                        color = colors.accent,
                        trackColor = colors.line,
                        strokeWidth = 6.dp,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                }

                Column {
                    Text(
                        text = "$doneCount / $totalCount",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "completed this day's tasks",
                        fontSize = 13.sp,
                        color = colors.muted
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Three buttons: Complete / Incomplete / Not Perform - each shows only its own tasks
        var filter by remember { mutableStateOf("incomplete") }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            FilterChipButton(
                label = "Complete (${completedTasks.size})",
                selected = filter == "complete",
                onSelect = { filter = "complete" }
            )
            FilterChipButton(
                label = "Incomplete (${incompleteTasks.size})",
                selected = filter == "incomplete",
                onSelect = { filter = "incomplete" }
            )
            FilterChipButton(
                label = "Not Perform (${notPerformedTasks.size})",
                selected = filter == "not_performed",
                onSelect = { filter = "not_performed" }
            )
        }

        val visible = when (filter) {
            "complete" -> completedTasks
            "not_performed" -> notPerformedTasks
            else -> incompleteTasks
        }

        if (visible.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = when (filter) {
                        "complete" -> "No completed tasks yet."
                        "not_performed" -> "No tasks in Not Perform."
                        else -> "No incomplete tasks for today."
                    },
                    fontSize = 14.sp,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(visible, key = { "${filter}_${it.first.id}_${it.second}" }) { (task, dateKey) ->
                    if (filter == "not_performed") {
                        HistoryTaskCard(
                            task = task,
                            done = false,
                            isNotPerformed = true,
                            onClick = { onSelectTask(task, dateKey) }
                        )
                    } else {
                        TaskCard(
                            task = task,
                            done = filter == "complete",
                            selected = true,
                            showCompletion = true,
                            onToggle = { onToggleTask(task, dateKey) },
                            onClick = null,
                            onEdit = null
                        )
                    }
                }
            }
        }
    }
}

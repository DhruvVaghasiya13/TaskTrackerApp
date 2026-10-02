package com.example.tasktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.ui.model.Category
import com.example.tasktracker.ui.model.categoryById
import com.example.tasktracker.ui.model.priorityLabel
import com.example.tasktracker.ui.theme.extraColors

@Composable
fun TaskCard(
    task: TaskEntity,
    done: Boolean = false,
    selected: Boolean = true,
    showCompletion: Boolean = true,
    onToggle: (() -> Unit)? = null,
    onToggleSelection: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.extraColors
    val category = categoryById(task.category)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(
                1.dp,
                if (onToggleSelection != null && selected) colors.accent else colors.line,
                RoundedCornerShape(14.dp)
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onToggleSelection != null) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selected) colors.accent else Color.Transparent)
                    .border(2.dp, colors.accent, RoundedCornerShape(4.dp))
                    .clickable { onToggleSelection() },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected for today",
                        tint = colors.paper,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        if (showCompletion) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (done) colors.accent else Color.Transparent)
                    .border(2.dp, colors.accent, CircleShape)
                    .then(if (onToggle != null) Modifier.clickable { onToggle() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = colors.paper,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = task.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (done) colors.muted else colors.ink,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                category?.let { cat ->
                    CategoryTag(category = cat)
                }
                if (task.subcategory.isNotBlank()) {
                    TagBadge(text = task.subcategory)
                }
                PriorityPill(priority = task.priority)
            }
        }

        if (onEdit != null) {
            IconButton(
                onClick = { onEdit() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Task",
                    tint = colors.muted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryTaskCard(
    task: TaskEntity,
    done: Boolean,
    isNotPerformed: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.extraColors
    val category = categoryById(task.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> colors.accent
                        isNotPerformed -> colors.high.copy(alpha = 0.2f)
                        else -> colors.muted.copy(alpha = 0.2f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                done -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = colors.paper,
                        modifier = Modifier.size(12.dp)
                    )
                }
                isNotPerformed -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Not Performed",
                        tint = colors.high,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = task.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (done) colors.muted else if (isNotPerformed) colors.high else colors.ink,
                textDecoration = if (done || isNotPerformed) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                category?.let { CategoryTag(category = it) }
                if (task.subcategory.isNotBlank()) {
                    TagBadge(text = task.subcategory)
                }
                PriorityPill(priority = task.priority)
            }
        }

        Text(
            text = when {
                done -> "Completed"
                isNotPerformed -> "Not Performed"
                else -> "Pending"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (done) colors.accent else if (isNotPerformed) colors.high else colors.muted
        )
    }
}

@Composable
fun SegmentedControl(
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    val colors = MaterialTheme.extraColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (id, label) ->
            val selected = id == selectedId
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) colors.accent else Color.Transparent)
                    .clickable { onSelect(id) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) colors.paper else colors.ink
                )
            }
        }
    }
}

@Composable
fun CategoryTag(category: Category) {
    val colors = MaterialTheme.extraColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.accentSoft)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.label,
            tint = colors.accent,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = category.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.accent
        )
    }
}

@Composable
fun TagBadge(text: String) {
    val colors = MaterialTheme.extraColors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.line.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = colors.ink
        )
    }
}

@Composable
fun PriorityPill(priority: String) {
    val colors = MaterialTheme.extraColors
    val label = priorityLabel(priority)
    val color = when (priority.lowercase()) {
        "high" -> colors.high
        "low" -> colors.muted
        else -> colors.accent
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun FilterChipButton(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val colors = MaterialTheme.extraColors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.accent else colors.card)
            .border(1.dp, if (selected) colors.accent else colors.line, RoundedCornerShape(20.dp))
            .clickable { onSelect() }
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

@Composable
fun CategoryChipButton(
    category: Category?,
    selected: Boolean,
    onSelect: () -> Unit,
    label: String? = null
) {
    val colors = MaterialTheme.extraColors
    val textLabel = label ?: category?.label ?: "All"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.accent else colors.card)
            .border(1.dp, if (selected) colors.accent else colors.line, RoundedCornerShape(20.dp))
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        category?.let {
            Icon(
                imageVector = it.icon,
                contentDescription = it.label,
                tint = if (selected) colors.paper else colors.muted,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = textLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) colors.paper else colors.muted
        )
    }
}

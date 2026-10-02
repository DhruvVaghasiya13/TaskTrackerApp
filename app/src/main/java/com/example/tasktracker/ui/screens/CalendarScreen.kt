package com.example.tasktracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.ui.components.FilterChipButton
import com.example.tasktracker.ui.components.HistoryTaskCard
import com.example.tasktracker.ui.model.Period
import com.example.tasktracker.ui.theme.extraColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    period: Period,
    refDate: LocalDate,
    tasks: List<TaskEntity>,
    isDoneOnDate: (TaskEntity, String) -> Boolean,
    isTaskSelected: (TaskEntity, String) -> Boolean,
    isOnDay: (TaskEntity, String) -> Boolean,
    onPeriodSelect: (Period) -> Unit,
    onShiftRef: (Int) -> Unit,
    onSelectMonth: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSelectTask: (TaskEntity, String) -> Unit
) {
    val colors = MaterialTheme.extraColors

    // `tasks` is the full history list: deleted / replaced tasks still belong to the days before their endDate
    val calendarTasks = tasks
    val today = LocalDate.now()

    // done / total (selected or done) / not-performed counts of one day
    fun dayStats(dKey: String): DayStats {
        var total = 0; var done = 0; var notPerformed = 0
        calendarTasks.forEach { t ->
            if (isOnDay(t, dKey)) {
                if (isTaskSelected(t, dKey)) {
                    total++                                    // ticked = performed (done or incomplete)
                    if (isDoneOnDate(t, dKey)) done++
                } else notPerformed++                          // unticked = not performed
            }
        }
        return DayStats(total, done, notPerformed)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            Period.entries.forEach { p ->
                FilterChipButton(
                    label = p.label,
                    selected = period == p,
                    onSelect = { onPeriodSelect(p) }
                )
            }
        }

        val labelText = when (period) {
            Period.DAILY -> refDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
            Period.WEEKLY -> {
                val start = refDate.minusDays(refDate.dayOfWeek.value % 7L)
                val end = start.plusDays(6)
                "${start.format(DateTimeFormatter.ofPattern("MMM d"))} – ${end.format(DateTimeFormatter.ofPattern("MMM d"))}"
            }
            Period.MONTHLY -> refDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            Period.YEARLY -> refDate.format(DateTimeFormatter.ofPattern("yyyy"))
            else -> ""
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onShiftRef(-1) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.card)
                    .border(1.dp, colors.line, CircleShape)
            ) {
                Text("‹", fontSize = 20.sp, color = colors.ink, fontWeight = FontWeight.Bold)
            }

            Text(
                text = labelText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink
            )

            IconButton(
                onClick = { onShiftRef(1) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.card)
                    .border(1.dp, colors.line, CircleShape)
            ) {
                Text("›", fontSize = 20.sp, color = colors.ink, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (period) {
                Period.DAILY -> {
                    val dateKey = refDate.toString()
                    val dayTasks = calendarTasks.filter { t -> isOnDay(t, dateKey) }
                        .distinctBy { t -> t.id }

                    if (refDate.isAfter(today)) {
                        // Upcoming day: only the date and weekday, no data yet
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = refDate.dayOfMonth.toString(),
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ink
                            )
                            Text(
                                text = refDate.format(DateTimeFormatter.ofPattern("EEEE")),
                                fontSize = 20.sp,
                                color = colors.muted
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Upcoming day - no data yet.", fontSize = 14.sp, color = colors.muted)
                        }
                    } else {
                        // Always show all three groups: Completed / Incomplete / Not Performed
                        val selectedDayTasks = dayTasks.filter { isTaskSelected(it, dateKey) }
                        val (completedDayTasks, uncompletedDayTasks) = selectedDayTasks.partition { isDoneOnDate(it, dateKey) }
                        val notPerformedTasks = dayTasks.filter { !isTaskSelected(it, dateKey) }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            historySection("Completed", completedDayTasks, "daily_completed", dateKey, isDoneOnDate, false, colors.muted, colors.high, onSelectTask)
                            historySection("Incomplete", uncompletedDayTasks, "daily_incomplete", dateKey, isDoneOnDate, false, colors.muted, colors.high, onSelectTask)
                            historySection("Not Performed", notPerformedTasks, "daily_notperf", dateKey, isDoneOnDate, true, colors.muted, colors.high, onSelectTask)
                        }
                    }
                }

                Period.WEEKLY -> {
                    val startOfWeek = refDate.minusDays(refDate.dayOfWeek.value % 7L)

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(7) { i ->
                            val d = startOfWeek.plusDays(i.toLong())
                            val dKey = d.toString()
                            val isFuture = d.isAfter(today)
                            val isToday = d == today
                            val stats = if (isFuture) DayStats(0, 0, 0) else dayStats(dKey)
                            val dateStr = d.format(DateTimeFormatter.ofPattern("EEE, MMM d"))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.card)
                                    .border(
                                        if (isToday) 2.dp else 1.dp,
                                        if (isToday) colors.accent else colors.line,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .then(if (isFuture) Modifier else Modifier.clickable { onSelectDate(d) })
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateStr,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isFuture) colors.muted else colors.ink,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isFuture) {
                                        Text("Upcoming", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.muted)
                                    } else if (stats.total == 0 && stats.notPerformed == 0) {
                                        Text("No tasks", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.muted)
                                    } else {
                                        // completed | total | not performed
                                        CountTriple(stats, fontSize = 18, modifier = Modifier.width(132.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Period.MONTHLY -> {
                    val firstDayOfMonth = refDate.withDayOfMonth(1)
                    val leadPadding = firstDayOfMonth.dayOfWeek.value % 7
                    val daysInMonth = refDate.lengthOfMonth()

                    Column(modifier = Modifier.fillMaxSize()) {
                        WeekdayHeader()
                        Spacer(Modifier.height(6.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(leadPadding) {
                                Box(Modifier.aspectRatio(1f))
                            }
                            items(daysInMonth) { index ->
                                val d = refDate.withDayOfMonth(index + 1)
                                val isFuture = d.isAfter(today)
                                val stats = if (isFuture) DayStats(0, 0, 0) else dayStats(d.toString())
                                CalendarCell(
                                    date = d,
                                    stats = stats,
                                    isFuture = isFuture,
                                    onClick = { onSelectDate(d) }
                                )
                            }
                        }
                    }
                }

                Period.YEARLY -> {
                    val year = refDate.year
                    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(12) { index ->
                            val monthNum = index + 1
                            val mDate = LocalDate.of(year, monthNum, 1)
                            val monthStarted = !mDate.isAfter(today)

                            var total = 0
                            var done = 0
                            if (monthStarted) {
                                for (day in 1..mDate.lengthOfMonth()) {
                                    val date = LocalDate.of(year, monthNum, day)
                                    if (!date.isAfter(today)) {
                                        val st = dayStats(date.toString())
                                        total += st.total
                                        done += st.done
                                    }
                                }
                            }
                            val pct = if (total > 0) (100f * done / total).toInt() else null
                            val isCurrent = year == today.year && monthNum == today.monthValue

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.card)
                                    .border(
                                        if (isCurrent) 2.dp else 1.dp,
                                        if (isCurrent) colors.accent else colors.line,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        onPeriodSelect(Period.MONTHLY)
                                        onSelectMonth(mDate)
                                    }
                                    .padding(vertical = 14.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        monthNames[index],
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (monthStarted) colors.ink else colors.muted
                                    )
                                    Text(
                                        text = if (pct == null) "—" else "$pct%",
                                        fontSize = 11.sp,
                                        color = colors.muted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One group of the day history: a header with the count, then its tasks (or "No tasks"). */
private fun LazyListScope.historySection(
    title: String,
    list: List<TaskEntity>,
    keyPrefix: String,
    dateKey: String,
    isDoneOnDate: (TaskEntity, String) -> Boolean,
    notPerformed: Boolean,
    headerNormal: androidx.compose.ui.graphics.Color,
    headerBad: androidx.compose.ui.graphics.Color,
    onSelectTask: (TaskEntity, String) -> Unit
) {
    item(key = "${keyPrefix}_header") {
        Text(
            text = "$title (${list.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (notPerformed) headerBad else headerNormal,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
        )
    }
    if (list.isEmpty()) {
        item(key = "${keyPrefix}_empty") {
            Text("No tasks", fontSize = 12.sp, color = headerNormal, modifier = Modifier.padding(start = 2.dp))
        }
    } else {
        items(list, key = { "${keyPrefix}_${it.id}" }) { task ->
            HistoryTaskCard(
                task = task,
                done = !notPerformed && isDoneOnDate(task, dateKey),
                isNotPerformed = notPerformed,
                onClick = { onSelectTask(task, dateKey) }
            )
        }
    }
}

private data class DayStats(val total: Int, val done: Int, val notPerformed: Int) {
    val incomplete: Int get() = total - done
}

/** Three short numbers, no words: first = completed, middle = total, last = not performed. */
@Composable
private fun CountTriple(stats: DayStats, fontSize: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.extraColors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stats.done.toString(),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.accent,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stats.total.toString(),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stats.notPerformed.toString(),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.high,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WeekdayHeader() {
    val colors = MaterialTheme.extraColors
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { d ->
            Text(
                text = d,
                fontSize = 11.sp,
                color = colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate,
    stats: DayStats,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.extraColors
    val today = LocalDate.now()
    val isToday = date == today
    val allDone = !isFuture && stats.total > 0 && stats.done == stats.total
    val hasMissed = !isFuture && date.isBefore(today) && stats.incomplete > 0

    val bgColor = if (allDone) colors.accentSoft else colors.card
    val borderColor = if (isToday) colors.accent else if (hasMissed) colors.high else colors.line
    val borderWidth = if (isToday) 2.dp else 1.dp

    Box(
        modifier = modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .then(if (isFuture) Modifier else Modifier.clickable { onClick() })
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFuture) colors.muted else colors.ink
            )
            if (isFuture) {
                // upcoming day: just the date and weekday, no data
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEE")),
                    fontSize = 9.sp,
                    color = colors.muted
                )
            } else if (stats.total > 0 || stats.notPerformed > 0) {
                Spacer(Modifier.height(3.dp))
                // completed | total | not performed
                CountTriple(stats, fontSize = 12, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

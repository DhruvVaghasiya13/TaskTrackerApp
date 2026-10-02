package com.example.tasktracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tasktracker.domain.Bucket
import com.example.tasktracker.domain.DayPoint
import com.example.tasktracker.domain.DayStatus
import com.example.tasktracker.domain.GroupStat
import com.example.tasktracker.domain.ProgressReport
import com.example.tasktracker.ui.components.FilterChipButton
import com.example.tasktracker.ui.components.PriorityPill
import com.example.tasktracker.ui.model.Period
import com.example.tasktracker.ui.theme.extraColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProgressScreen(
    period: Period,
    report: ProgressReport?,
    onPeriodSelect: (Period) -> Unit,
    onShift: (Int) -> Unit,
    onToday: () -> Unit
) {
    val colors = MaterialTheme.extraColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Progress", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = colors.ink)
        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 10.dp)
        ) {
            listOf(Period.DAILY, Period.WEEKLY, Period.MONTHLY, Period.YEARLY).forEach { p ->
                FilterChipButton(label = p.label, selected = period == p, onSelect = { onPeriodSelect(p) })
            }
        }

        if (report == null) {
            Text("Loading your progress...", fontSize = 13.sp, color = colors.muted, modifier = Modifier.padding(top = 24.dp))
            return@Column
        }

        RangeNavigator(report = report, onShift = onShift, onToday = onToday)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
        ) {
            item { HeroCard(report) }
            item { CountsRow(report) }

            // ───── DAILY: just the day's tasks, grouped by status ─────
            if (report.period == Period.DAILY) {
                item { DayTasksSection(report) }
                return@LazyColumn
            }

            if (report.total == 0 && report.skipped == 0) {
                item {
                    SectionCard(title = "Nothing to show yet") {
                        Text(
                            "No tasks were planned in this period. Add tasks and tick them off - your charts will appear here.",
                            fontSize = 13.sp, color = colors.muted
                        )
                    }
                }
                return@LazyColumn
            }

            // ───── main chart: completed vs incomplete ─────
            item {
                SectionCard(
                    title = when (report.period) {
                        Period.WEEKLY -> "Completed vs incomplete - each day"
                        Period.MONTHLY -> "Completed vs incomplete - each week"
                        else -> "Completed vs incomplete - each month"
                    },
                    trailing = { Legend() }
                ) { BarChart(report.bars) }
            }

            // ───── period specific ─────
            when (report.period) {
                Period.WEEKLY -> item {
                    SectionCard(title = "Day by day") {
                        report.days.forEach { DayRow(it) }
                    }
                }
                Period.MONTHLY -> {
                    item { SectionCard(title = "Daily activity") { MonthHeatmap(report) } }
                    item { SectionCard(title = "Daily completion trend (%)") { LineChart(report.trend) } }
                }
                Period.YEARLY -> {
                    item { SectionCard(title = "Year at a glance") { YearHeatmap(report) } }
                }
                else -> {}
            }

            item { HighlightsCard(report) }

            if (report.categories.isNotEmpty()) {
                item {
                    SectionCard(title = "By category") {
                        report.categories.forEach { g ->
                            BarRow(g.label, "${g.done}/${g.total}  ·  ${g.pct}%", g.pct / 100f, colors.accent)
                        }
                    }
                }
            }
            if (report.bestTasks.isNotEmpty()) {
                item {
                    SectionCard(title = "Most consistent tasks") {
                        report.bestTasks.forEach { g -> TaskBarRow(g, colors.accent) }
                    }
                }
            }
            if (report.weakTasks.isNotEmpty()) {
                item {
                    SectionCard(title = "Needs attention") {
                        report.weakTasks.forEach { g -> TaskBarRow(g, colors.high) }
                    }
                }
            }
        }
    }
}

// ───────────────────────── navigation ─────────────────────────

@Composable
private fun RangeNavigator(report: ProgressReport, onShift: (Int) -> Unit, onToday: () -> Unit) {
    val colors = MaterialTheme.extraColors
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onShift(-1) },
            contentAlignment = Alignment.Center
        ) { Text("‹", fontSize = 28.sp, color = colors.ink) }

        Text(
            text = report.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(if (report.canGoNext) Modifier.clickable { onShift(1) } else Modifier),
            contentAlignment = Alignment.Center
        ) { Text("›", fontSize = 28.sp, color = if (report.canGoNext) colors.ink else colors.line) }

        if (!report.isCurrent) {
            Spacer(Modifier.width(6.dp))
            FilterChipButton(label = "Today", selected = true, onSelect = onToday)
        }
    }
}

// ───────────────────────── cards ─────────────────────────

@Composable
private fun SectionCard(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.extraColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.ink, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        content()
    }
}

@Composable
private fun HeroCard(report: ProgressReport) {
    val colors = MaterialTheme.extraColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Ring(report.pct)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (report.total == 0) "No tasks planned" else "${report.done} of ${report.total} tasks done",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.ink
            )
            val prev = report.prevPct
            if (report.total > 0 && prev != null) {
                val diff = report.pct - prev
                val arrow = if (diff > 0) "▲" else if (diff < 0) "▼" else "■"
                val sign = if (diff > 0) "+" else ""
                Text(
                    "$arrow $sign$diff% ${report.prevLabel}",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (diff >= 0) colors.accent else colors.high
                )
            } else if (report.total > 0) {
                Text("Nothing planned earlier to compare with", fontSize = 12.sp, color = colors.muted)
            }
            Text(
                if (report.period == Period.DAILY) "Completion today" else "Completion rate",
                fontSize = 12.sp, color = colors.muted
            )
        }
    }
}

@Composable
private fun Ring(pct: Int) {
    val colors = MaterialTheme.extraColors
    val track = colors.line
    val progress = colors.accent
    Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val d = size.minDimension - stroke
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = Size(d, d), style = Stroke(width = stroke)
            )
            if (pct > 0) {
                drawArc(
                    color = progress, startAngle = -90f, sweepAngle = 360f * pct.coerceIn(0, 100) / 100f,
                    useCenter = false, topLeft = topLeft, size = Size(d, d),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text("$pct%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.ink)
    }
}

@Composable
private fun CountsRow(report: ProgressReport) {
    val colors = MaterialTheme.extraColors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("${report.done}", "Completed", colors.accent, Modifier.weight(1f))
        StatTile("${report.missed}", "Incomplete", colors.high, Modifier.weight(1f))
        StatTile("${report.skipped}", "Not performed", colors.muted, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.extraColors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = colors.muted, maxLines = 1)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = MaterialTheme.extraColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = colors.muted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.ink)
    }
}

/** Only facts that mean something for the chosen period. */
@Composable
private fun HighlightsCard(report: ProgressReport) {
    val dayFmt = DateTimeFormatter.ofPattern("EEE, d MMM")
    fun dayText(d: DayPoint) = "${d.date.format(dayFmt)}  ·  ${d.pct}% (${d.done}/${d.total})"
    fun bucketText(b: Bucket) = "${b.label}  ·  ${b.pct}% (${b.done}/${b.total})"

    SectionCard(title = "Highlights") {
        when (report.period) {
            Period.WEEKLY -> {
                report.bestDay?.let { InfoRow("Best day", dayText(it)) }
                report.worstDay?.let { InfoRow("Lowest day", dayText(it)) }
                InfoRow("Perfect days", "${report.perfectDays} of ${report.activeDays}")
                InfoRow("Current streak", "${report.currentStreak} days")
            }
            Period.MONTHLY -> {
                report.bestDay?.let { InfoRow("Best day", dayText(it)) }
                InfoRow("Active days", "${report.activeDays}")
                InfoRow("Perfect days", "${report.perfectDays}")
                InfoRow("Current streak", "${report.currentStreak} days")
            }
            else -> {
                report.bestBucket?.let { InfoRow("Best month", bucketText(it)) }
                report.worstBucket?.let { InfoRow("Lowest month", bucketText(it)) }
                InfoRow("Perfect days", "${report.perfectDays}")
                InfoRow("Longest streak", "${report.bestStreak} days")
                InfoRow("Current streak", "${report.currentStreak} days")
            }
        }
    }
}

@Composable
private fun DayRow(p: DayPoint) {
    val colors = MaterialTheme.extraColors
    val fmt = DateTimeFormatter.ofPattern("EEE d")
    val future = p.date.isAfter(LocalDate.now())
    val sub = when {
        future -> "-"
        p.total == 0 -> "no tasks"
        else -> "${p.done}/${p.total}  ·  ${p.pct}%"
    }
    BarRow(p.date.format(fmt), sub, if (future || p.total == 0) 0f else p.pct / 100f, colors.accent)
}

/** Daily view: completed / incomplete / not performed lists of the chosen day. */
@Composable
private fun DayTasksSection(report: ProgressReport) {
    val colors = MaterialTheme.extraColors
    if (report.start.isAfter(LocalDate.now())) {
        SectionCard(title = "Upcoming day") {
            Text("This day has not happened yet, so there is no progress to show.", fontSize = 13.sp, color = colors.muted)
        }
        return
    }
    if (report.dayTasks.isEmpty()) {
        SectionCard(title = "No tasks") {
            Text("No tasks belong to this day.", fontSize = 13.sp, color = colors.muted)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(
            Triple("Completed", DayStatus.DONE, colors.accent),
            Triple("Incomplete", DayStatus.INCOMPLETE, colors.high),
            Triple("Not performed", DayStatus.NOT_PERFORMED, colors.muted)
        ).forEach { (title, status, tint) ->
            val rows = report.dayTasks.filter { it.status == status }
            if (rows.isNotEmpty()) {
                SectionCard(title = "$title (${rows.size})") {
                    rows.forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (status == DayStatus.DONE) tint else Color.Transparent)
                                    .border(1.5.dp, tint, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (status == DayStatus.DONE) Text("✓", fontSize = 12.sp, color = colors.paper, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    row.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (status == DayStatus.DONE) colors.muted else colors.ink
                                )
                                Text(row.category, fontSize = 11.sp, color = colors.muted)
                            }
                            PriorityPill(priority = row.priority)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    val colors = MaterialTheme.extraColors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LegendDot(colors.accent, "Completed")
        LegendDot(colors.high.copy(alpha = 0.55f), "Incomplete")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val colors = MaterialTheme.extraColors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = colors.muted)
    }
}

// ───────────────────────── charts ─────────────────────────

@Composable
private fun BarChart(bars: List<Bucket>) {
    val colors = MaterialTheme.extraColors
    val maxTotal = (bars.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)
    val chartHeight: Dp = 130.dp
    val barWidth: Dp = if (bars.size > 8) 14.dp else 22.dp

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        bars.forEach { b ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(chartHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (b.total > 0) {
                        Column(modifier = Modifier.width(barWidth).clip(RoundedCornerShape(5.dp))) {
                            if (b.missed > 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(maxOf(chartHeight * (b.missed.toFloat() / maxTotal), 3.dp))
                                        .background(colors.high.copy(alpha = 0.55f))
                                )
                            }
                            if (b.done > 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(maxOf(chartHeight * (b.done.toFloat() / maxTotal), 3.dp))
                                        .background(colors.accent)
                                )
                            }
                        }
                    } else {
                        Box(Modifier.width(barWidth).height(3.dp).clip(RoundedCornerShape(2.dp)).background(colors.line))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    b.label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = colors.muted, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible
                )
                Text(
                    text = b.pct?.let { "$it%" } ?: "-",
                    fontSize = 9.sp, color = colors.muted, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible
                )
            }
        }
    }
}

@Composable
private fun LineChart(points: List<Bucket>) {
    val colors = MaterialTheme.extraColors
    val lineColor = colors.accent
    val gridColor = colors.line
    val chartHeight: Dp = 140.dp
    val step = when {
        points.size > 24 -> 5
        points.size > 12 -> 2
        else -> 1
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("100", "50", "0").forEach { Text(it, fontSize = 9.sp, color = colors.muted) }
            }
            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val w = size.width
                val h = size.height
                val pad = 6.dp.toPx()
                listOf(0f, 0.5f, 1f).forEach { f ->
                    val y = pad + (h - 2 * pad) * f
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                }
                if (points.isEmpty()) return@Canvas
                fun xAt(i: Int) = if (points.size == 1) w / 2f else w * i / (points.size - 1).toFloat()
                fun yAt(p: Int) = pad + (h - 2 * pad) * (1f - p / 100f)

                val path = Path()
                var open = false
                points.forEachIndexed { i, b ->
                    val p = b.pct
                    if (p == null) { open = false } else {
                        val x = xAt(i); val y = yAt(p)
                        if (!open) { path.moveTo(x, y); open = true } else path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path, color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                val r = if (points.size > 16) 2.5.dp.toPx() else 4.dp.toPx()
                points.forEachIndexed { i, b ->
                    b.pct?.let { drawCircle(lineColor, radius = r, center = Offset(xAt(i), yAt(it))) }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp)) {
            points.forEachIndexed { i, b ->
                Text(
                    text = if (i % step == 0) b.label else "",
                    fontSize = 9.sp, color = colors.muted, textAlign = TextAlign.Center,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Visible,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun heatColor(p: DayPoint, today: LocalDate, accent: Color, empty: Color): Color = when {
    p.date.isAfter(today) -> Color.Transparent
    p.total == 0 -> empty.copy(alpha = 0.35f)
    else -> accent.copy(alpha = 0.18f + 0.82f * (p.pct / 100f))
}

@Composable
private fun MonthHeatmap(report: ProgressReport) {
    val colors = MaterialTheme.extraColors
    val today = LocalDate.now()
    val offset = report.start.dayOfWeek.value % 7
    val cells: List<DayPoint?> = List(offset) { null } + report.days
    val rows = cells.chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, fontSize = 10.sp, color = colors.muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until 7) {
                    val p = row.getOrNull(i)
                    if (p == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val bg = heatColor(p, today, colors.accent, colors.line)
                        val strong = !p.date.isAfter(today) && p.total > 0 && p.pct >= 55
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bg)
                                .border(
                                    1.dp,
                                    if (p.date.isAfter(today)) colors.line else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                p.date.dayOfMonth.toString(), fontSize = 10.sp,
                                color = if (strong) colors.paper else colors.ink
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", fontSize = 10.sp, color = colors.muted)
            listOf(0.18f, 0.4f, 0.65f, 1f).forEach {
                Box(Modifier.padding(start = 4.dp).size(12.dp).clip(RoundedCornerShape(3.dp)).background(colors.accent.copy(alpha = it)))
            }
            Text("  More", fontSize = 10.sp, color = colors.muted)
        }
    }
}

@Composable
private fun YearHeatmap(report: ProgressReport) {
    val colors = MaterialTheme.extraColors
    val today = LocalDate.now()
    val accent = colors.accent
    val empty = colors.line
    val offset = report.start.dayOfWeek.value % 7
    val days = report.days

    Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(54f / 7f)) {
        val cell = size.width / 54f
        val gap = cell * 0.18f
        days.forEachIndexed { idx, p ->
            val col = (idx + offset) / 7
            val row = (idx + offset) % 7
            val color = heatColor(p, today, accent, empty)
            drawRoundRect(
                color = if (p.date.isAfter(today)) empty.copy(alpha = 0.12f) else color,
                topLeft = Offset(col * cell + gap / 2f, row * cell + gap / 2f),
                size = Size(cell - gap, cell - gap),
                cornerRadius = CornerRadius(cell * 0.2f)
            )
        }
    }
}

// ───────────────────────── rows ─────────────────────────

@Composable
private fun BarRow(label: String, sub: String, fraction: Float, color: Color) {
    val colors = MaterialTheme.extraColors
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Text(sub, fontSize = 12.sp, color = colors.muted)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.line.copy(alpha = 0.6f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun TaskBarRow(g: GroupStat, color: Color) {
    BarRow(g.label, "${g.done}/${g.total}  ·  ${g.pct}%", g.pct / 100f, color)
}

package com.example.tasktracker.domain

import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.ui.model.Period
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One calendar day.
 *  total   = tasks that were ticked (planned) for the day
 *  done    = planned AND completed
 *  skipped = tasks explicitly un-ticked for that day ("Not Performed")
 */
data class DayPoint(val date: LocalDate, val total: Int, val done: Int, val skipped: Int = 0) {
    val missed: Int get() = (total - done).coerceAtLeast(0)
    val pct: Int get() = if (total == 0) 0 else (100 * done / total)
}

/** One bar / point of a chart (a day, a week of the month, a month ...). */
data class Bucket(val label: String, val done: Int, val total: Int, val future: Boolean = false) {
    val missed: Int get() = (total - done).coerceAtLeast(0)
    /** null = nothing planned (or future) -> chart shows a gap instead of a fake 0 % */
    val pct: Int? get() = if (future || total == 0) null else (100 * done / total)
}

data class GroupStat(val label: String, val done: Int, val total: Int) {
    val pct: Int get() = if (total == 0) 0 else (100 * done / total)
}

enum class DayStatus { DONE, INCOMPLETE, NOT_PERFORMED }

data class DayTaskRow(val name: String, val category: String, val priority: String, val status: DayStatus)

data class ProgressReport(
    val period: Period,
    val title: String,
    val isCurrent: Boolean,
    val canGoNext: Boolean,
    val start: LocalDate,
    val end: LocalDate,

    val total: Int,
    val done: Int,
    val missed: Int,       // planned but not completed
    val skipped: Int,      // not performed (un-ticked)
    val pct: Int,
    val prevPct: Int?,
    val prevLabel: String,

    val currentStreak: Int,
    val bestStreak: Int,
    val activeDays: Int,
    val perfectDays: Int,
    val bestDay: DayPoint?,
    val worstDay: DayPoint?,
    val bestBucket: Bucket?,
    val worstBucket: Bucket?,

    val days: List<DayPoint>,
    val bars: List<Bucket>,
    val trend: List<Bucket>,
    val categories: List<GroupStat>,
    val bestTasks: List<GroupStat>,
    val weakTasks: List<GroupStat>,
    val dayTasks: List<DayTaskRow>   // only for the Daily view
)

private fun TaskEntity.endedOn(key: String) = endDate.isNotEmpty() && key >= endDate

private class Evaluator(
    val tasks: List<TaskEntity>,
    val doneIds: Set<String>,
    val sel: Map<String, Boolean>,
    val today: LocalDate
) {
    private val cache = HashMap<LocalDate, DayPoint>()

    fun isPlanned(t: TaskEntity, d: LocalDate): Boolean {
        val key = d.toString()
        if (t.endedOn(key)) return false
        return sel["${t.id}_$key"] ?: t.occursOn(d)
    }

    fun day(d: LocalDate): DayPoint {
        if (d.isAfter(today)) return DayPoint(d, 0, 0, 0)
        return cache.getOrPut(d) {
            val key = d.toString()
            var total = 0; var done = 0; var skipped = 0
            for (t in tasks) {
                if (t.endedOn(key)) continue
                val s = sel["${t.id}_$key"]
                if (s == false) { skipped++; continue }
                if (s ?: t.occursOn(d)) {
                    total++
                    if ("${t.id}_$key" in doneIds) done++
                }
            }
            DayPoint(d, total, done, skipped)
        }
    }

    fun isDone(t: TaskEntity, d: LocalDate) = "${t.id}_$d" in doneIds

    /** Same rules as the Calendar day sheet: today shows every task, past days only what belonged to them. */
    fun dayRows(d: LocalDate, categoryLabel: (String) -> String): List<DayTaskRow> {
        val key = d.toString()
        return tasks.filter { t ->
            if (t.endedOn(key)) false
            else d == today || t.occursOn(d) || "${t.id}_$key" in doneIds || sel.containsKey("${t.id}_$key")
        }.map { t ->
            val selected = sel["${t.id}_$key"] ?: t.occursOn(d)
            val status = when {
                !selected -> DayStatus.NOT_PERFORMED
                isDone(t, d) -> DayStatus.DONE
                else -> DayStatus.INCOMPLETE
            }
            DayTaskRow(t.name, categoryLabel(t.category), t.priority, status)
        }
    }
}

private fun rangeFor(period: Period, ref: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
    Period.DAILY -> ref to ref
    Period.WEEKLY -> {
        val s = ref.minusDays(ref.dayOfWeek.value % 7L)   // weeks start on Sunday, like the rest of the app
        s to s.plusDays(6)
    }
    Period.MONTHLY -> ref.withDayOfMonth(1) to ref.withDayOfMonth(ref.lengthOfMonth())
    Period.YEARLY -> LocalDate.of(ref.year, 1, 1) to LocalDate.of(ref.year, 12, 31)
}

private fun shift(period: Period, ref: LocalDate, dir: Long): LocalDate = when (period) {
    Period.DAILY -> ref.plusDays(dir)
    Period.WEEKLY -> ref.plusWeeks(dir)
    Period.MONTHLY -> ref.plusMonths(dir)
    Period.YEARLY -> ref.plusYears(dir)
}

private fun titleFor(period: Period, start: LocalDate, end: LocalDate): String {
    val loc = Locale.getDefault()
    return when (period) {
        Period.DAILY -> start.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", loc))
        Period.WEEKLY -> "${start.format(DateTimeFormatter.ofPattern("d MMM", loc))} - ${end.format(DateTimeFormatter.ofPattern("d MMM yyyy", loc))}"
        Period.MONTHLY -> start.format(DateTimeFormatter.ofPattern("MMMM yyyy", loc))
        Period.YEARLY -> start.year.toString()
    }
}

private fun prevLabelFor(period: Period) = when (period) {
    Period.DAILY -> "vs previous day"
    Period.WEEKLY -> "vs previous week"
    Period.MONTHLY -> "vs previous month"
    Period.YEARLY -> "vs previous year"
}

fun buildProgressReport(
    tasks: List<TaskEntity>,
    doneIds: Set<String>,
    selections: Map<String, Boolean>,
    period: Period,
    ref: LocalDate,
    today: LocalDate = LocalDate.now(),
    categoryLabel: (String) -> String = { it.ifBlank { "Other" } }
): ProgressReport {
    val ev = Evaluator(tasks, doneIds, selections, today)
    val loc = Locale.getDefault()

    val (start, end) = rangeFor(period, ref)
    fun daysOf(s: LocalDate, e: LocalDate) =
        generateSequence(s) { it.plusDays(1) }.takeWhile { !it.isAfter(e) }.map { ev.day(it) }.toList()

    val days = daysOf(start, end)
    val dayRows = if (period == Period.DAILY && !ref.isAfter(today)) ev.dayRows(ref, categoryLabel) else emptyList()

    val total = days.sumOf { it.total }
    val done = days.sumOf { it.done }
    val skipped = if (period == Period.DAILY) dayRows.count { it.status == DayStatus.NOT_PERFORMED } else days.sumOf { it.skipped }
    val pct = if (total == 0) 0 else 100 * done / total

    val (pStart, pEnd) = rangeFor(period, shift(period, ref, -1))
    val prevDays = daysOf(pStart, pEnd)
    val prevTotal = prevDays.sumOf { it.total }
    val prevPct = if (prevTotal == 0) null else 100 * prevDays.sumOf { it.done } / prevTotal

    val active = days.filter { it.total > 0 }
    val perfect = active.count { it.done == it.total }
    val bestDay = active.maxWithOrNull(compareBy<DayPoint>({ it.pct }, { it.done }))
    // today is still in progress, so it is never called the "lowest" day
    val worstDay = active.filter { it.date != today }
        .minWithOrNull(compareBy<DayPoint>({ it.pct }, { -it.total }))
        ?.takeIf { it != bestDay }

    fun bucketOf(label: String, from: LocalDate, to: LocalDate): Bucket {
        var t = 0; var dn = 0
        var d = from
        while (!d.isAfter(to)) { val p = ev.day(d); t += p.total; dn += p.done; d = d.plusDays(1) }
        return Bucket(label, dn, t, future = from.isAfter(today))
    }

    val bars: List<Bucket>
    val trend: List<Bucket>
    when (period) {
        Period.DAILY -> { bars = emptyList(); trend = emptyList() }
        Period.WEEKLY -> {
            bars = days.map { p -> bucketOf(p.date.format(DateTimeFormatter.ofPattern("EEE", loc)), p.date, p.date) }
            trend = bars
        }
        Period.MONTHLY -> {
            val weekly = ArrayList<Bucket>()
            var s = start; var n = 1
            while (!s.isAfter(end)) {
                val e = minOf(s.plusDays(6), end)
                weekly.add(bucketOf("W$n", s, e))
                s = e.plusDays(1); n++
            }
            bars = weekly
            trend = days.map { p -> bucketOf(p.date.dayOfMonth.toString(), p.date, p.date) }
        }
        Period.YEARLY -> {
            bars = (1..12).map { m ->
                val f = LocalDate.of(start.year, m, 1)
                bucketOf(f.format(DateTimeFormatter.ofPattern("MMM", loc)), f, f.withDayOfMonth(f.lengthOfMonth()))
            }
            trend = bars
        }
    }
    val scored = bars.filter { it.pct != null }
    val bestBucket = scored.maxByOrNull { it.pct!! }
    val worstBucket = scored.minByOrNull { it.pct!! }?.takeIf { it != bestBucket }

    // ---------- categories and tasks ----------
    val catMap = LinkedHashMap<String, IntArray>()
    val taskMap = HashMap<String, Pair<String, IntArray>>()
    if (period != Period.DAILY) {
        for (p in days) {
            if (p.total == 0) continue
            for (t in tasks) {
                if (!ev.isPlanned(t, p.date)) continue
                val isDone = ev.isDone(t, p.date)
                val c = catMap.getOrPut(categoryLabel(t.category)) { IntArray(2) }
                c[1]++; if (isDone) c[0]++
                val e = taskMap.getOrPut(t.name.trim().lowercase()) { t.name.trim() to IntArray(2) }
                e.second[1]++; if (isDone) e.second[0]++
            }
        }
    }
    val categories = catMap.map { (k, v) -> GroupStat(k, v[0], v[1]) }.sortedByDescending { it.total }
    val taskStats = taskMap.values.map { GroupStat(it.first, it.second[0], it.second[1]) }
    val bestTasks = taskStats.filter { it.done > 0 && it.pct >= 60 }
        .sortedWith(compareByDescending<GroupStat> { it.pct }.thenByDescending { it.total }).take(5)
    val weakTasks = taskStats.filter { it.pct < 60 }
        .sortedWith(compareBy<GroupStat> { it.pct }.thenByDescending { it.total }).take(5)

    // ---------- streaks (days without any task neither extend nor break a streak) ----------
    val earliest = tasks.mapNotNull { runCatching { LocalDate.parse(it.due) }.getOrNull() }.minOrNull()
        ?.let { maxOf(it, today.minusDays(1095)) } ?: today

    var current = 0
    run {
        var d = today
        val tp = ev.day(d)
        if (tp.total > 0 && tp.done < tp.total) d = d.minusDays(1)   // today still running must not break a streak
        while (!d.isBefore(earliest)) {
            val p = ev.day(d)
            if (p.total > 0) { if (p.done == p.total) current++ else break }
            d = d.minusDays(1)
        }
    }
    var best = 0
    run {
        var streakRun = 0
        var d = earliest
        while (!d.isAfter(today)) {
            val p = ev.day(d)
            if (p.total > 0) {
                if (p.done == p.total) { streakRun++; if (streakRun > best) best = streakRun } else streakRun = 0
            }
            d = d.plusDays(1)
        }
    }

    return ProgressReport(
        period = period,
        title = titleFor(period, start, end),
        isCurrent = !today.isBefore(start) && !today.isAfter(end),
        canGoNext = end.isBefore(today),
        start = start, end = end,
        total = total, done = done, missed = (total - done).coerceAtLeast(0), skipped = skipped, pct = pct,
        prevPct = prevPct, prevLabel = prevLabelFor(period),
        currentStreak = current, bestStreak = best,
        activeDays = active.size, perfectDays = perfect,
        bestDay = bestDay, worstDay = worstDay,
        bestBucket = bestBucket, worstBucket = worstBucket,
        days = days, bars = bars, trend = trend,
        categories = categories, bestTasks = bestTasks, weakTasks = weakTasks,
        dayTasks = dayRows
    )
}

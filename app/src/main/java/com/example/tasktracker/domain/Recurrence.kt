package com.example.tasktracker.domain

import com.example.tasktracker.data.local.TaskEntity
import java.time.DayOfWeek
import java.time.LocalDate

/** JS getDay(): Sunday=0 ... Saturday=6 */
fun LocalDate.jsDay(): Int = if (dayOfWeek == DayOfWeek.SUNDAY) 0 else dayOfWeek.value

/**
 * Does this task fall on [date]?
 *
 *  - none    : only on the due date (an overdue one-time task is also shown on today until done)
 *  - daily   : every day from the due date (start date) onwards
 *  - weekly  : weeklyMode "include" = ONLY the selected weekdays, "exclude" = every weekday EXCEPT the selected ones
 *  - monthly : monthlyMode "include" = ONLY the selected dates, "exclude" = every date EXCEPT the selected ones
 *
 * Repeating tasks never appear BEFORE their start (due) date, so history of earlier days stays clean.
 */
fun TaskEntity.occursOn(date: LocalDate): Boolean {
    // deleted / replaced task: it no longer applies from endDate onwards, earlier days are untouched history
    if (endDate.isNotEmpty() && date.toString() >= endDate) return false

    val today = LocalDate.now()
    val dueDate = runCatching { LocalDate.parse(due) }.getOrNull() ?: today

    if (repeatType != "daily" && repeatType != "weekly" && repeatType != "monthly") {
        return date == dueDate || (date == today && dueDate.isBefore(today))
    }
    if (date.isBefore(dueDate)) return false

    return when (repeatType) {
        "daily" -> true
        "weekly" -> {
            val listed = date.jsDay() in weeklyDays
            if (weeklyMode == "exclude") !listed else listed
        }
        "monthly" -> {
            if (monthlyMode == "exclude") {
                date.dayOfMonth !in monthlyDates
            } else {
                val last = date.lengthOfMonth()
                // a date like 31 falls on the last day of shorter months
                monthlyDates.any { it == date.dayOfMonth || (it > last && date.dayOfMonth == last) }
            }
        }
        else -> false
    }
}

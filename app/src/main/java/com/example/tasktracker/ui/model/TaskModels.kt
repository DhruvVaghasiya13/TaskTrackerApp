package com.example.tasktracker.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import org.json.JSONArray
import org.json.JSONObject
import com.example.tasktracker.data.local.TaskEntity

enum class AppTab(val label: String) {
    HOME("Home"),
    CALENDAR("Calendar"),
    TASKS("Tasks"),
    PROGRESS("Progress"),
    PROFILE("Profile")
}

enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "System Default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark")
}

enum class Period(val id: String, val label: String) {
    DAILY("daily", "Daily"),
    WEEKLY("weekly", "Weekly"),
    MONTHLY("monthly", "Monthly"),
    YEARLY("yearly", "Yearly")
}

enum class PriorityFilter(val id: String, val label: String) {
    ALL("all", "All"),
    HIGH("high", "High"),
    MED("med", "Medium"),
    LOW("low", "Low")
}

data class Category(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val subcategories: List<String>
)

val CATEGORIES = listOf(
    Category("work", "Work", Icons.Default.Work, listOf("Office", "Projects", "Meetings", "Emails", "Clients", "Team", "Administration", "Career Growth", "Job Search", "Freelancing")),
    Category("study", "Study", Icons.Default.School, listOf("Classes", "Assignments", "Exams", "Revision", "Reading", "Research", "Practical", "Presentation", "Skills")),
    Category("personal", "Personal", Icons.Default.Person, listOf("Family", "Friends", "Calls", "Events", "Personal Admin", "Important Dates", "Hobbies", "Habits", "Journaling")),
    Category("health", "Health", Icons.Default.Favorite, listOf("Workout", "Walking", "Sports", "Nutrition", "Sleep", "Self-Care", "Appointments", "Routine")),
    Category("finance", "Finance", Icons.Default.AttachMoney, listOf("Expenses", "Income", "Budget", "Bills", "Savings", "Banking", "Subscriptions", "Investments")),
    Category("home", "Home", Icons.Default.Home, listOf("Cleaning", "Kitchen", "Laundry", "Shopping", "Repairs", "Organization", "Bills", "Maintenance")),
    Category("business", "Business", Icons.Default.ShowChart, listOf("Sales", "Marketing", "Customers", "Leads", "Operations", "Networking", "Strategy")),
    Category("digital", "Digital", Icons.Default.Smartphone, listOf("Computer", "Phone", "Files", "Security", "Software", "Social Media", "Cloud", "Backup", "Content Creation")),
    Category("travel", "Travel", Icons.Default.Flight, listOf("Planning", "Booking", "Documents", "Packing", "Transport", "Accommodation", "Activities", "Budget")),
    Category("goals", "Goals", Icons.Default.AdsClick, listOf("Daily Goals", "Weekly Goals", "Monthly Goals", "Long-Term Goals", "Career Goals", "Financial Goals", "Learning Goals"))
)

/** A category created by the user (built-in ones live in CATEGORIES). */
data class CustomCategory(val id: String, val label: String, val subcategories: List<String>)

/**
 * User-added categories and task types ("subcategories").
 * Held in Compose state so every screen updates instantly; saved in SharedPreferences
 * and backed up to Firebase together with the other settings (see TaskViewModel).
 */
object CategoryStore {
    var customCategories by mutableStateOf<List<CustomCategory>>(emptyList())
        private set
    /** extra task types added to the BUILT-IN categories: categoryId -> task types */
    var extraSubs by mutableStateOf<Map<String, List<String>>>(emptyMap())
        private set

    fun hasData() = customCategories.isNotEmpty() || extraSubs.isNotEmpty()

    fun load(json: String?) {
        customCategories = emptyList(); extraSubs = emptyMap()
        mergeFrom(json)
    }

    /** Union-merge (nothing is ever removed, so merging a cloud copy can never lose local additions). */
    fun mergeFrom(json: String?) {
        if (json.isNullOrBlank()) return
        runCatching {
            val o = JSONObject(json)
            val cats = customCategories.toMutableList()
            val cArr = o.optJSONArray("cats") ?: JSONArray()
            for (i in 0 until cArr.length()) {
                val co = cArr.getJSONObject(i)
                val id = co.getString("id")
                val subs = co.optJSONArray("subs").toStrings()
                val idx = cats.indexOfFirst { it.id == id }
                if (idx < 0) cats.add(CustomCategory(id, co.getString("label"), subs))
                else cats[idx] = cats[idx].copy(subcategories = unionIgnoreCase(cats[idx].subcategories, subs))
            }
            customCategories = cats
            val extra = extraSubs.toMutableMap()
            val sObj = o.optJSONObject("subs") ?: JSONObject()
            sObj.keys().forEach { k -> extra[k] = unionIgnoreCase(extra[k].orEmpty(), sObj.optJSONArray(k).toStrings()) }
            extraSubs = extra
        }
    }

    fun toJson(): String = JSONObject().apply {
        put("cats", JSONArray().apply {
            customCategories.forEach { c ->
                put(JSONObject().put("id", c.id).put("label", c.label).put("subs", JSONArray(c.subcategories)))
            }
        })
        put("subs", JSONObject().apply { extraSubs.forEach { (k, v) -> put(k, JSONArray(v)) } })
    }.toString()

    /** Returns the id of the new category (or of the existing one with the same name). */
    fun addCategory(label: String): String? {
        val clean = label.trim().take(24)
        if (clean.isBlank()) return null
        allCategories().firstOrNull { it.label.equals(clean, ignoreCase = true) }?.let { return it.id }
        val id = "custom_" + System.currentTimeMillis()
        customCategories = customCategories + CustomCategory(id, clean, emptyList())
        return id
    }

    /** Returns the stored name of the task type (or of the existing one with the same name). */
    fun addSubcategory(categoryId: String, label: String): String? {
        val clean = label.trim().take(30)
        if (clean.isBlank()) return null
        val cat = categoryById(categoryId) ?: return null
        cat.subcategories.firstOrNull { it.equals(clean, ignoreCase = true) }?.let { return it }
        if (categoryId.startsWith("custom_")) {
            customCategories = customCategories.map {
                if (it.id == categoryId) it.copy(subcategories = it.subcategories + clean) else it
            }
        } else {
            extraSubs = extraSubs + (categoryId to (extraSubs[categoryId].orEmpty() + clean))
        }
        return clean
    }

    private fun unionIgnoreCase(a: List<String>, b: List<String>): List<String> {
        val out = a.toMutableList()
        b.forEach { x -> if (out.none { it.equals(x, ignoreCase = true) }) out.add(x) }
        return out
    }

    private fun JSONArray?.toStrings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }
}

/** Built-in categories + the user's own, each with its task types. */
fun allCategories(): List<Category> {
    val builtIn = CATEGORIES.map { c ->
        val extra = CategoryStore.extraSubs[c.id].orEmpty().filter { e -> c.subcategories.none { it.equals(e, ignoreCase = true) } }
        if (extra.isEmpty()) c else c.copy(subcategories = c.subcategories + extra)
    }
    val custom = CategoryStore.customCategories.map { Category(it.id, it.label, Icons.Default.Star, it.subcategories) }
    return builtIn + custom
}

fun categoryById(id: String): Category? = allCategories().firstOrNull { it.id == id }

fun priorityLabel(p: String): String = when (p.lowercase()) {
    "high" -> "High"
    "med", "medium" -> "Medium"
    else -> "Low"
}

fun repeatSummary(task: TaskEntity): String {
    val wdNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    return when (task.repeatType) {
        "none" -> "One-time"
        "daily" -> "Daily"
        "weekly" -> {
            val daysStr = task.weeklyDays.sorted().mapNotNull { wdNames.getOrNull(it) }.joinToString(", ")
            if (task.weeklyMode == "exclude") {
                if (daysStr.isEmpty()) "Weekly (every day)" else "Weekly, except $daysStr"
            } else {
                "Weekly on ${daysStr.ifEmpty { "no days set" }}"
            }
        }
        "monthly" -> {
            val datesStr = task.monthlyDates.sorted().joinToString(", ")
            if (task.monthlyMode == "exclude") {
                if (datesStr.isEmpty()) "Monthly (every date)" else "Monthly, except date(s) $datesStr"
            } else {
                "Monthly on date(s) ${datesStr.ifEmpty { "none set" }}"
            }
        }
        else -> task.repeatType
    }
}

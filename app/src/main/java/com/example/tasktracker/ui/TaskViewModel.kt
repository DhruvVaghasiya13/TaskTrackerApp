package com.example.tasktracker.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasktracker.TaskTrackerApp
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.domain.ProgressReport
import com.example.tasktracker.domain.buildProgressReport
import com.example.tasktracker.domain.occursOn
import com.example.tasktracker.reminder.NotificationSettings
import com.example.tasktracker.ui.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app as TaskTrackerApp
    private val repo = container.repository
    private val auth = container.auth
    private val prefs = app.getSharedPreferences("task_tracker_prefs", Context.MODE_PRIVATE)

    // User & Sync State
    val userUid = auth.uidFlow.stateIn(viewModelScope, SharingStarted.Eagerly, auth.uid)
    val userEmail = auth.emailFlow.stateIn(viewModelScope, SharingStarted.Eagerly, auth.email)
    val syncing = MutableStateFlow(false)
    /** Human readable result of the last cloud sync (shown on the Profile tab). */
    val syncStatus = MutableStateFlow<String?>(null)
    val error = MutableStateFlow<String?>(null)
    val infoMessage = MutableStateFlow<String?>(null)
    val isResettingPassword = MutableStateFlow(false)

    // UI Navigation & Theme State
    val currentTab = MutableStateFlow(AppTab.HOME)
    val themeMode = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.id == prefs.getString("theme_mode", "system") } ?: ThemeMode.SYSTEM
    )
    val profileName = MutableStateFlow("")
    val profileContact = MutableStateFlow("")
    val profileImageUri = MutableStateFlow("")

    // must be declared BEFORE init{} (init starts the sync loop immediately)
    private val isForeground = MutableStateFlow(false)

    /** Called from MainActivity onStart/onStop. */
    fun setForeground(value: Boolean) { isForeground.value = value }

    init {
        CategoryStore.load(prefs.getString("custom_categories", null))
        loadProfileForCurrentUid()
        // AUTO SYNC: as soon as an account is signed in, keep pulling/pushing by itself
        // (no button needed). Restarts on account change, pauses while the app is in background.
        viewModelScope.launch {
            userUid.collectLatest { uid ->
                if (!uid.isNullOrEmpty()) {
                    while (true) {
                        isForeground.first { it }
                        val ok = performSync()
                        delay(if (ok) 30_000L else 10_000L)
                    }
                }
            }
        }
    }

    /** Runs a sync (tasks + settings) and records the outcome - errors are shown, not swallowed. */
    private suspend fun performSync(): Boolean {
        var err: Exception? = null
        try { repo.sync() } catch (e: CancellationException) { throw e } catch (e: Exception) { err = e }
        try { syncSettings() } catch (e: CancellationException) { throw e } catch (e: Exception) { if (err == null) err = e }

        val e = err
        if (e == null) {
            syncStatus.value = "Synced \u2713 - your tasks and settings are backed up and up to date."
            return true
        }
        val pending = runCatching { repo.pendingCount() }.getOrDefault(0)
        val reason = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
        syncStatus.value = "Sync failed: $reason" +
            if (pending > 0) " ($pending changes not uploaded yet)" else ""
        return false
    }

    // ---------- Settings sync (profile name, contact, theme) ----------
    private val settingsLock = Mutex()

    private fun markSettingsChanged() {
        val uid = auth.uid ?: return
        prefs.edit()
            .putLong("settings_updated_$uid", System.currentTimeMillis())
            .putBoolean("settings_dirty_$uid", true)
            .apply()
        viewModelScope.launch { performSync() } // upload right away
    }

    /** Last-write-wins on settings_updated: newer cloud copy is applied here, newer local copy is uploaded. */
    private suspend fun syncSettings() {
        val uid = auth.uid ?: return
        settingsLock.withLock {
            val remote = repo.fetchSettings()
            val remoteUpdated = (remote?.get("updatedAt") as? Number)?.toLong() ?: 0L
            val localUpdated = prefs.getLong("settings_updated_$uid", 0L)
            val dirty = prefs.getBoolean("settings_dirty_$uid", false)

            if (remote != null && remoteUpdated > localUpdated) {
                var name = remote["profileName"] as? String ?: ""
                var contact = remote["profileContact"] as? String ?: ""
                val themeId = remote["themeMode"] as? String ?: "system"
                val remoteCats = remote["customCategories"] as? String
                // Never wipe details typed on this device before they were ever uploaded.
                var keepLocalDirty = false
                if (localUpdated == 0L) {
                    if (name.isBlank() && profileName.value.isNotBlank()) { name = profileName.value; keepLocalDirty = true }
                    if (contact.isBlank() && profileContact.value.isNotBlank()) { contact = profileContact.value; keepLocalDirty = true }
                }
                // Custom categories / task types: union with the cloud copy (nothing is ever lost).
                if (!remoteCats.isNullOrBlank()) {
                    CategoryStore.mergeFrom(remoteCats)
                    prefs.edit().putString("custom_categories", CategoryStore.toJson()).apply()
                }
                if (CategoryStore.hasData() && CategoryStore.toJson() != remoteCats) keepLocalDirty = true
                profileName.value = name
                profileContact.value = contact
                themeMode.value = ThemeMode.entries.firstOrNull { it.id == themeId } ?: ThemeMode.SYSTEM
                // notification settings (on/off + default time) follow the account too
                (remote["notifEnabled"] as? Boolean)?.let { NotificationSettings.setEnabled(getApplication<Application>(), it) }
                (remote["notifDefaultTime"] as? String)?.takeIf { it.isNotBlank() }
                    ?.let { NotificationSettings.setDefaultTime(getApplication<Application>(), it) }
                val remoteRepeatH = (remote["notifRepeatHours"] as? Number)?.toInt()
                val remoteRepeatM = (remote["notifRepeatMinutes"] as? Number)?.toInt()
                if (remoteRepeatH != null || remoteRepeatM != null) {
                    NotificationSettings.setRepeat(getApplication<Application>(), remoteRepeatH ?: 0, remoteRepeatM ?: 0)
                }
                prefs.edit()
                    .putString("profile_name", name).putString("profile_name_$uid", name)
                    .putString("profile_contact", contact).putString("profile_contact_$uid", contact)
                    .putString("theme_mode", themeMode.value.id)
                    .putLong("settings_updated_$uid", if (keepLocalDirty) 0L else remoteUpdated)
                    .putBoolean("settings_dirty_$uid", keepLocalDirty)
                    .apply()
            }

            val hasLocalData = profileName.value.isNotBlank() || profileContact.value.isNotBlank() ||
                themeMode.value != ThemeMode.SYSTEM || CategoryStore.hasData() ||
                !NotificationSettings.isEnabled(getApplication<Application>()) ||
                NotificationSettings.defaultTime(getApplication<Application>()) != NotificationSettings.DEFAULT_TIME ||
                NotificationSettings.repeatIntervalMinutes(getApplication<Application>()) > 0
            val needsUpload = (remote == null && hasLocalData) ||
                prefs.getBoolean("settings_dirty_$uid", false)
            if (needsUpload) {
                val stamp = prefs.getLong("settings_updated_$uid", 0L).takeIf { it > 0 } ?: System.currentTimeMillis()
                repo.pushSettings(
                    mapOf(
                        "profileName" to profileName.value,
                        "profileContact" to profileContact.value,
                        "themeMode" to themeMode.value.id,
                        "customCategories" to CategoryStore.toJson(),
                        "notifEnabled" to NotificationSettings.isEnabled(getApplication<Application>()),
                        "notifDefaultTime" to NotificationSettings.defaultTime(getApplication<Application>()),
                        "notifRepeatHours" to NotificationSettings.repeatHours(getApplication<Application>()),
                        "notifRepeatMinutes" to NotificationSettings.repeatMinutes(getApplication<Application>()),
                        "updatedAt" to stamp
                    )
                )
                prefs.edit()
                    .putLong("settings_updated_$uid", stamp)
                    .putBoolean("settings_dirty_$uid", false)
                    .apply()
            }

            syncPhoto(uid, (remote?.get("photoUpdatedAt") as? Number)?.toLong() ?: 0L)
        }
    }

    // ---------- Profile picture sync ----------
    private suspend fun syncPhoto(uid: String, remotePhotoAt: Long) {
        val localAt = prefs.getLong("photo_updated_$uid", 0L)
        val dirty = prefs.getBoolean("photo_dirty_$uid", false)

        if (remotePhotoAt > localAt) {
            // Newer picture exists in the cloud -> download it.
            val data = repo.fetchPhoto() ?: return
            val ctx = getApplication<Application>()
            var uriStr = ""
            if (data.isNotBlank()) {
                val bytes = Base64.decode(data, Base64.DEFAULT)
                val file = File(ctx.filesDir, "profile_synced_$remotePhotoAt.jpg")
                file.writeBytes(bytes)
                ctx.filesDir.listFiles()?.filter {
                    it.name.startsWith("profile_synced_") && it.name != file.name
                }?.forEach { it.delete() }
                uriStr = Uri.fromFile(file).toString()
            }
            profileImageUri.value = uriStr
            prefs.edit()
                .putString("profile_image_uri", uriStr)
                .putString("profile_image_uri_$uid", uriStr)
                .putLong("photo_updated_$uid", remotePhotoAt)
                .putBoolean("photo_dirty_$uid", false)
                .apply()
            return
        }

        val hasLocalImage = profileImageUri.value.isNotBlank()
        if (dirty || (remotePhotoAt == 0L && localAt == 0L && hasLocalImage)) {
            val b64 = if (hasLocalImage) encodeImage(profileImageUri.value) else ""
            if (b64 != null) {
                val stamp = localAt.takeIf { it > 0 } ?: System.currentTimeMillis()
                repo.pushPhoto(b64, stamp)
                prefs.edit()
                    .putLong("photo_updated_$uid", stamp)
                    .putBoolean("photo_dirty_$uid", false)
                    .apply()
            }
        }
    }

    /** Shrinks the picture to max 320 px JPEG (~20-40 KB) and returns it as base64, or null if unreadable. */
    private fun encodeImage(uriStr: String): String? {
        val bmp = (try {
            val u = Uri.parse(uriStr)
            if (u.scheme == "file") BitmapFactory.decodeFile(u.path)
            else getApplication<Application>().contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { null }) ?: return null
        val scale = minOf(1f, 320f / maxOf(bmp.width, bmp.height))
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * scale).toInt().coerceAtLeast(1),
            (bmp.height * scale).toInt().coerceAtLeast(1),
            true
        ) else bmp
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun loadProfileForCurrentUid() {
        val uid = auth.uid ?: "guest"
        profileName.value = prefs.getString("profile_name_$uid", prefs.getString("profile_name", "")) ?: ""
        profileContact.value = prefs.getString("profile_contact_$uid", prefs.getString("profile_contact", "")) ?: ""
        profileImageUri.value = prefs.getString("profile_image_uri_$uid", prefs.getString("profile_image_uri", "")) ?: ""
    }

    // Active Sheet State
    val editingTask = MutableStateFlow<TaskEntity?>(null)
    val isEditSheetOpen = MutableStateFlow(false)

    val detailTask = MutableStateFlow<Pair<TaskEntity, String>?>(null)
    val isDetailSheetOpen = MutableStateFlow(false)

    // Calendar Sheet State
    val selectedDayKey = MutableStateFlow<String?>(null)
    val isDaySheetOpen = MutableStateFlow(false)

    // Calendar Screen State
    val calPeriod = MutableStateFlow(Period.MONTHLY)
    val calRefDate = MutableStateFlow(LocalDate.now())

    // Progress Screen State
    val progPeriod = MutableStateFlow(Period.MONTHLY)
    /** Any day inside the range being looked at on the Progress tab (today = current range). */
    val progRefDate = MutableStateFlow(LocalDate.now())

    // Database Sources
    val allTasks: StateFlow<List<TaskEntity>> = repo.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Live tasks + tasks that were deleted / replaced. Calendar, day sheet and progress read THIS list,
     * so deleting or editing a task today never changes how earlier days look.
     */
    val historyTasks: StateFlow<List<TaskEntity>> = repo.observeHistoryTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completionsSet: StateFlow<Set<String>> = repo.observeCompletions()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val selectionsMap: StateFlow<Map<String, Boolean>> = repo.observeSelections()
        .map { list -> list.associate { it.id to it.selected } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun isDone(task: TaskEntity, dateKey: String): Boolean =
        "${task.id}_$dateKey" in completionsSet.value

    fun isSelected(task: TaskEntity, dateKey: String): Boolean {
        val id = "${task.id}_$dateKey"
        val override = selectionsMap.value[id]
        if (override != null) return override
        val date = runCatching { LocalDate.parse(dateKey) }.getOrDefault(LocalDate.now())
        return task.occursOn(date)
    }

    /** Is this task part of that day's history (scheduled, done, or explicitly selected / deselected)? */
    fun isOnDay(task: TaskEntity, dateKey: String): Boolean {
        val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return false
        // deleted / replaced task: only the days BEFORE its endDate are still part of history
        if (task.endDate.isNotEmpty() && dateKey >= task.endDate) return false
        // today: EVERY task is part of the day (performed or not performed), same as the Tasks tab
        if (date == LocalDate.now()) return true
        val id = "${task.id}_$dateKey"
        return task.occursOn(date) || id in completionsSet.value || selectionsMap.value.containsKey(id)
    }

    fun toggleTaskSelection(task: TaskEntity, dateKey: String) = viewModelScope.launch {
        val current = isSelected(task, dateKey)
        repo.setSelection(task.id, dateKey, !current)
    }

    // Derived: Home Screen Incomplete Tasks
    val homeIncompleteTasks: StateFlow<List<Pair<TaskEntity, String>>> =
        combine(allTasks, completionsSet, selectionsMap) { tasks, doneSet, selMap ->
            val todayKey = LocalDate.now().toString()
            val todayDate = LocalDate.parse(todayKey)
            val everDoneIds = doneSet.map { it.substringBeforeLast('_') }.toSet()
            val list = mutableListOf<Pair<TaskEntity, String>>()
            tasks.filter { t ->
                val override = selMap["${t.id}_$todayKey"]
                val selected = override ?: t.occursOn(todayDate)
                val carriedOverButAlreadyDone = override == null && t.repeatType == "none" &&
                    t.due < todayKey && t.id in everDoneIds
                selected && ("${t.id}_$todayKey" !in doneSet) && !carriedOverButAlreadyDone
            }.forEach { t ->
                list.add(t to todayKey)
            }
            list
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived: Home Screen Completed Tasks
    val homeCompletedTasks: StateFlow<List<Pair<TaskEntity, String>>> =
        combine(allTasks, completionsSet, selectionsMap) { tasks, doneSet, selMap ->
            val todayKey = LocalDate.now().toString()
            val todayDate = LocalDate.parse(todayKey)
            val list = mutableListOf<Pair<TaskEntity, String>>()
            tasks.filter { t ->
                val override = selMap["${t.id}_$todayKey"]
                val selected = override ?: t.occursOn(todayDate)
                selected && ("${t.id}_$todayKey" in doneSet)
            }.forEach { t ->
                list.add(t to todayKey)
            }
            list
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived: Home Screen Not Performed Tasks (= unticked in the Tasks tab for today)
    val homeNotPerformedTasks: StateFlow<List<Pair<TaskEntity, String>>> =
        combine(allTasks, selectionsMap) { tasks, selMap ->
            val todayKey = LocalDate.now().toString()
            val todayDate = LocalDate.parse(todayKey)
            tasks.filter { t ->
                val selected = selMap["${t.id}_$todayKey"] ?: t.occursOn(todayDate)
                !selected
            }.map { t -> t to todayKey }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived: Home Progress
    val homeProgress: StateFlow<Pair<Int, Int>> =
        combine(homeIncompleteTasks, homeCompletedTasks) { incomplete, completed ->
            val done = completed.size
            val total = incomplete.size + completed.size
            done to total
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

    val homeTasksList: StateFlow<List<TaskEntity>> =
        combine(homeIncompleteTasks, homeCompletedTasks) { inc, comp ->
            (inc.map { it.first } + comp.map { it.first }).distinctBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived: Day Sheet Tasks
    val daySheetTasks: StateFlow<List<TaskEntity>> =
        combine(historyTasks, selectedDayKey, completionsSet, selectionsMap) { tasks, key, doneSet, selMap ->
            if (key == null) emptyList()
            else {
                val date = runCatching { LocalDate.parse(key) }.getOrNull()
                if (date != null) {
                    tasks.filter { t ->
                        if (t.endDate.isNotEmpty() && key >= t.endDate) return@filter false
                        val id = "${t.id}_$key"
                        date == LocalDate.now() || t.occursOn(date) || id in doneSet || selMap.containsKey(id)
                    }.distinctBy { t -> t.id }
                } else emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived: Progress report (built from the full history, so deleted tasks keep counting for their past days)
    val progressReport: StateFlow<ProgressReport?> =
        combine(historyTasks, completionsSet, selectionsMap, progPeriod, progRefDate) { tasks, doneSet, selMap, period, ref ->
            buildProgressReport(
                tasks = tasks,
                doneIds = doneSet,
                selections = selMap,
                period = period,
                ref = ref,
                today = LocalDate.now(),
                categoryLabel = { id -> categoryById(id)?.label ?: "Other" }
            )
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Operations
    fun selectTab(tab: AppTab) { currentTab.value = tab }

    fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.id).apply()
        markSettingsChanged()
    }

    fun updateProfileImage(uriStr: String) {
        profileImageUri.value = uriStr
        val uid = auth.uid ?: "guest"
        prefs.edit()
            .putString("profile_image_uri", uriStr)
            .putString("profile_image_uri_$uid", uriStr)
            .apply()
        if (auth.uid != null) {
            prefs.edit()
                .putLong("photo_updated_$uid", System.currentTimeMillis())
                .putBoolean("photo_dirty_$uid", true)
                .apply()
            viewModelScope.launch { performSync() } // upload right away
        }
    }

    fun updateProfileDetails(name: String, contact: String) {
        profileName.value = name
        profileContact.value = contact
        val uid = auth.uid ?: "guest"
        prefs.edit()
            .putString("profile_name", name)
            .putString("profile_contact", contact)
            .putString("profile_name_$uid", name)
            .putString("profile_contact_$uid", contact)
            .apply()
        markSettingsChanged()
    }

    // ---------- Custom categories / task types ----------
    private fun persistCategories() {
        prefs.edit().putString("custom_categories", CategoryStore.toJson()).apply()
        markSettingsChanged() // backs them up to Firebase with the other settings
    }

    /** Returns the category id (new, or the existing one with the same name). */
    fun addCustomCategory(label: String): String? {
        val id = CategoryStore.addCategory(label) ?: return null
        persistCategories()
        return id
    }

    /** Returns the stored task type name (new, or the existing one with the same name). */
    fun addCustomSubcategory(categoryId: String, label: String): String? {
        val name = CategoryStore.addSubcategory(categoryId, label) ?: return null
        persistCategories()
        return name
    }

    fun openNewTaskSheet() {
        editingTask.value = null
        isEditSheetOpen.value = true
    }

    fun openEditTaskSheet(task: TaskEntity) {
        editingTask.value = task
        isEditSheetOpen.value = true
    }

    fun closeEditSheet() { isEditSheetOpen.value = false }

    fun openDetailSheet(task: TaskEntity, dateKey: String) {
        detailTask.value = task to dateKey
        isDetailSheetOpen.value = true
    }

    fun closeDetailSheet() { isDetailSheetOpen.value = false }

    // ---------- Opened from a notification ----------
    private fun closeAllSheets() {
        isEditSheetOpen.value = false
        isDaySheetOpen.value = false
        isDetailSheetOpen.value = false
    }

    /** Tap on the common (summary) notification: Home page with the task list. */
    fun openHomeFromNotification() {
        closeAllSheets()
        currentTab.value = AppTab.HOME
    }

    /** Tap on one task's notification: Home page + only that task's details. */
    fun openTaskFromNotification(taskId: String, dateKey: String) {
        closeAllSheets()
        currentTab.value = AppTab.HOME
        viewModelScope.launch {
            val task = repo.getTask(taskId)
            if (task != null && !task.deleted) openDetailSheet(task, dateKey)
        }
    }

    // ---------- Notification settings ----------
    val notificationSettings = NotificationSettings.state

    fun setNotificationsEnabled(enabled: Boolean) {
        NotificationSettings.setEnabled(getApplication<Application>(), enabled)
        markSettingsChanged()
    }

    /** [applyToAll] = also reset tasks that had their own time, so every notification comes at [hhmm]. */
    fun setDefaultReminderTime(hhmm: String, applyToAll: Boolean) {
        NotificationSettings.setDefaultTime(getApplication<Application>(), hhmm)
        markSettingsChanged()
        if (applyToAll) viewModelScope.launch { repo.useDefaultReminderTimeForAll() }
    }

    /** Repeat interval for not-yet-completed tasks. 0 h 0 min = repeat OFF. */
    /** [applyToAll] = also reset tasks that had their own repeat interval, so every task follows [hours]:[minutes]. */
    fun setRepeatInterval(hours: Int, minutes: Int, applyToAll: Boolean = false) {
        NotificationSettings.setRepeat(getApplication<Application>(), hours, minutes)
        markSettingsChanged()
        if (applyToAll) viewModelScope.launch { repo.useDefaultRepeatForAll() }
    }

    fun openDaySheet(date: LocalDate) {
        selectedDayKey.value = date.toString()
        isDaySheetOpen.value = true
    }

    fun closeDaySheet() { isDaySheetOpen.value = false }

    fun setProgPeriod(p: Period) {
        progPeriod.value = p
        progRefDate.value = LocalDate.now()
    }

    fun shiftProgRef(dir: Int) {
        val curr = progRefDate.value
        progRefDate.value = when (progPeriod.value) {
            Period.DAILY -> curr.plusDays(dir.toLong())
            Period.WEEKLY -> curr.plusWeeks(dir.toLong())
            Period.MONTHLY -> curr.plusMonths(dir.toLong())
            Period.YEARLY -> curr.plusYears(dir.toLong())
        }
    }

    fun progressToToday() { progRefDate.value = LocalDate.now() }

    fun shiftCalRef(dir: Int) {
        val curr = calRefDate.value
        val next = when (calPeriod.value) {
            Period.DAILY -> curr.plusDays(dir.toLong())
            Period.WEEKLY -> curr.plusWeeks(dir.toLong())
            Period.MONTHLY -> curr.plusMonths(dir.toLong())
            Period.YEARLY -> curr.plusYears(dir.toLong())
            else -> curr
        }
        calRefDate.value = next
    }

    fun quickAdd(name: String) = viewModelScope.launch {
        val todayStr = LocalDate.now().toString()
        repo.saveTask(repo.newTask(name, todayStr))
    }

    fun toggleTaskDone(task: TaskEntity, dateKey: String) = viewModelScope.launch {
        repo.toggleDone(task.id, dateKey)
    }

    fun saveTask(task: TaskEntity) = viewModelScope.launch {
        repo.saveTask(task)
    }

    fun deleteTask(id: String) = viewModelScope.launch {
        repo.deleteTask(id)
    }

    fun clearMessages() {
        error.value = null
        infoMessage.value = null
    }

    fun openForgotPassword() {
        clearMessages()
        isResettingPassword.value = true
    }

    fun closeForgotPassword() {
        clearMessages()
        isResettingPassword.value = false
    }

    fun signIn(email: String, pw: String) = viewModelScope.launch {
        error.value = null
        infoMessage.value = null
        if (email.isBlank() || pw.isBlank()) {
            error.value = "Please enter email and password."
            return@launch
        }
        syncing.value = true
        try {
            try {
                auth.signIn(email, pw)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("no user record", ignoreCase = true) ||
                    msg.contains("user-not-found", ignoreCase = true) ||
                    msg.contains("invalid-user", ignoreCase = true)) {
                    error.value = "Email is not registered. Please register first."
                } else if (msg.contains("password", ignoreCase = true) ||
                    msg.contains("credential", ignoreCase = true) ||
                    msg.contains("wrong", ignoreCase = true)) {
                    error.value = "Incorrect password. Please check and try again."
                } else {
                    error.value = msg.ifBlank { "Sign in failed." }
                }
                return@launch
            }
            loadProfileForCurrentUid()
            // Pull this account's tasks from the cloud right away.
            performSync()
        } finally {
            syncing.value = false
        }
    }

    fun registerUser(name: String, contact: String, email: String, pw: String) = viewModelScope.launch {
        error.value = null
        infoMessage.value = null
        if (email.isBlank() || pw.isBlank()) {
            error.value = "Email and Password are required."
            return@launch
        }
        syncing.value = true
        try {
            auth.register(email, pw)
            auth.signOut()

            if (name.isNotBlank()) {
                profileName.value = name
                val uid = auth.uid ?: "guest"
                prefs.edit()
                    .putString("profile_name", name)
                    .putString("profile_name_$uid", name)
                    .apply()
            }
            if (contact.isNotBlank()) {
                profileContact.value = contact
                val uid = auth.uid ?: "guest"
                prefs.edit()
                    .putString("profile_contact", contact)
                    .putString("profile_contact_$uid", contact)
                    .apply()
            }

            infoMessage.value = "Registration completed! Please sign in with your email and password."
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("already in use", ignoreCase = true) || msg.contains("collision", ignoreCase = true)) {
                error.value = "This email is already registered. Please sign in directly."
            } else {
                error.value = msg.ifBlank { "Registration failed." }
            }
        } finally {
            syncing.value = false
        }
    }

    fun sendPasswordReset(email: String) = viewModelScope.launch {
        error.value = null
        infoMessage.value = null
        if (email.isBlank()) {
            error.value = "Please enter your registered email address."
            return@launch
        }
        syncing.value = true
        try {
            auth.sendPasswordResetEmail(email)
            infoMessage.value = "Password reset email sent! Please check your inbox and follow the instructions."
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("no user record", ignoreCase = true) || msg.contains("user-not-found", ignoreCase = true)) {
                error.value = "No account found with this email address. Please register first."
            } else {
                error.value = msg.ifBlank { "Failed to send reset email. Check your address and try again." }
            }
        } finally {
            syncing.value = false
        }
    }

    fun signOut() = viewModelScope.launch {
        error.value = null
        infoMessage.value = null
        isResettingPassword.value = false
        syncing.value = true
        try {
            // Upload anything still pending so it is not stranded on this device.
            withTimeoutOrNull(8_000) { runCatching { repo.sync() } }
            auth.signOut()
            syncStatus.value = null
            // Do NOT call repo.wipeLocal()! Task data must persist permanently across logouts and logins.
            profileName.value = ""
            profileContact.value = ""
            profileImageUri.value = ""
        } catch (e: Exception) {
            error.value = e.message
        } finally {
            syncing.value = false
        }
    }
}

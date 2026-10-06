package com.example.tasktracker.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tasktracker.reminder.ReminderScheduler
import com.example.tasktracker.ui.model.AppTab
import com.example.tasktracker.ui.model.ThemeMode
import com.example.tasktracker.ui.screens.*
import com.example.tasktracker.ui.sheets.DaySheet
import com.example.tasktracker.ui.sheets.TaskDetailSheet
import com.example.tasktracker.ui.sheets.TaskEditSheet
import com.example.tasktracker.ui.theme.TaskTrackerTheme
import com.example.tasktracker.ui.theme.extraColors
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val vm: TaskViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        vm.setForeground(true)
    }

    override fun onStop() {
        super.onStop()
        vm.setForeground(false)
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    /** Notification tap: task notification -> that task's details, common notification -> Home. */
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID)
        val date = intent.getStringExtra(ReminderScheduler.EXTRA_DATE)
        if (taskId != null && date != null) {
            vm.openTaskFromNotification(taskId, date)
        } else if (intent.getBooleanExtra(ReminderScheduler.EXTRA_OPEN_HOME, false)) {
            vm.openHomeFromNotification()
        }
        intent.removeExtra(ReminderScheduler.EXTRA_TASK_ID)
        intent.removeExtra(ReminderScheduler.EXTRA_DATE)
        intent.removeExtra(ReminderScheduler.EXTRA_OPEN_HOME)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // app was closed and a notification was tapped (not on rotation / recreate)
        if (savedInstanceState == null) handleNotificationIntent(intent)
        // Android 13+: reminders need the notification permission
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch("android.permission.POST_NOTIFICATIONS")
        }
        setContent {
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()

            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemInDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            TaskTrackerTheme(darkTheme = useDarkTheme) {
                MainAppScreen(vm = vm)
            }
        }
    }
}

@Composable
fun MainAppScreen(vm: TaskViewModel) {
    val colors = MaterialTheme.extraColors
    val isResettingPassword by vm.isResettingPassword.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val authError by vm.error.collectAsStateWithLifecycle()
    val infoMessage by vm.infoMessage.collectAsStateWithLifecycle()
    val userEmail by vm.userEmail.collectAsStateWithLifecycle()
    val syncStatus by vm.syncStatus.collectAsStateWithLifecycle()

    if (isResettingPassword) {
        PasswordResetScreen(
            initialEmail = userEmail ?: "",
            isSyncing = syncing,
            authError = authError,
            infoMessage = infoMessage,
            onSendReset = { email -> vm.sendPasswordReset(email) },
            onBackToSignIn = { vm.closeForgotPassword() }
        )
        return
    }

    val currentTab by vm.currentTab.collectAsStateWithLifecycle()
    val userUid by vm.userUid.collectAsStateWithLifecycle()
    val profileName by vm.profileName.collectAsStateWithLifecycle()
    val profileContact by vm.profileContact.collectAsStateWithLifecycle()
    val profileImageUri by vm.profileImageUri.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val notifSettings by vm.notificationSettings.collectAsStateWithLifecycle()

    val homeIncompleteTasks by vm.homeIncompleteTasks.collectAsStateWithLifecycle()
    val homeCompletedTasks by vm.homeCompletedTasks.collectAsStateWithLifecycle()
    val homeNotPerformedTasks by vm.homeNotPerformedTasks.collectAsStateWithLifecycle()
    val homeProgressPair by vm.homeProgress.collectAsStateWithLifecycle()
    val (doneCount, totalCount) = homeProgressPair

    val allTasks by vm.allTasks.collectAsStateWithLifecycle()
    val homeTasks by vm.homeTasksList.collectAsStateWithLifecycle()
    val selectionsMap by vm.selectionsMap.collectAsStateWithLifecycle()
    val completionsSet by vm.completionsSet.collectAsStateWithLifecycle()
    val calPeriod by vm.calPeriod.collectAsStateWithLifecycle()
    val calRefDate by vm.calRefDate.collectAsStateWithLifecycle()

    val historyTasks by vm.historyTasks.collectAsStateWithLifecycle()
    val progPeriod by vm.progPeriod.collectAsStateWithLifecycle()
    val progressReport by vm.progressReport.collectAsStateWithLifecycle()

    val isEditOpen by vm.isEditSheetOpen.collectAsStateWithLifecycle()
    val editTask by vm.editingTask.collectAsStateWithLifecycle()

    val isDetailOpen by vm.isDetailSheetOpen.collectAsStateWithLifecycle()
    val detailPair by vm.detailTask.collectAsStateWithLifecycle()

    val isDayOpen by vm.isDaySheetOpen.collectAsStateWithLifecycle()
    val dayKey by vm.selectedDayKey.collectAsStateWithLifecycle()
    val dayTasks by vm.daySheetTasks.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = colors.paper,
        bottomBar = {
            NavigationBar(
                containerColor = colors.card,
                tonalElevation = 0.dp
            ) {
                val tabs = listOf(
                    AppTab.HOME to Icons.Default.Home,
                    AppTab.CALENDAR to Icons.Default.CalendarToday,
                    AppTab.TASKS to Icons.AutoMirrored.Filled.List,
                    AppTab.PROGRESS to Icons.Default.BarChart,
                    AppTab.PROFILE to Icons.Default.Person
                )

                tabs.forEach { (tab, icon) ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { vm.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label,
                                tint = if (selected) colors.accent else colors.muted
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) colors.accent else colors.muted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = colors.accentSoft.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == AppTab.HOME || currentTab == AppTab.TASKS) {
                FloatingActionButton(
                    onClick = { vm.openNewTaskSheet() },
                    containerColor = colors.accent,
                    contentColor = colors.paper,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(colors.paper)
        ) {
            when (currentTab) {
                AppTab.HOME -> HomeScreen(
                    incompleteTasks = homeIncompleteTasks,
                    completedTasks = homeCompletedTasks,
                    notPerformedTasks = homeNotPerformedTasks,
                    doneCount = doneCount,
                    totalCount = totalCount,
                    onToggleTask = { task, dateKey -> vm.toggleTaskDone(task, dateKey) },
                    onSelectTask = { task, dateKey -> vm.openDetailSheet(task, dateKey) },
                    onEditTask = { task -> vm.openEditTaskSheet(task) }
                )

                AppTab.CALENDAR -> CalendarScreen(
                    period = calPeriod,
                    refDate = calRefDate,
                    tasks = historyTasks,
                    // read the live state here so the calendar numbers refresh as soon as a task is done / deselected
                    isDoneOnDate = { task, dateKey -> "${task.id}_$dateKey" in completionsSet },
                    isTaskSelected = { task, dateKey ->
                        selectionsMap["${task.id}_$dateKey"] ?: vm.isSelected(task, dateKey)
                    },
                    isOnDay = { task, dateKey ->
                        // a deleted / replaced task is history only for the days before its endDate
                        if (task.endDate.isNotEmpty() && dateKey >= task.endDate) false
                        else {
                            val id = "${task.id}_$dateKey"
                            selectionsMap.containsKey(id) || id in completionsSet || vm.isOnDay(task, dateKey)
                        }
                    },
                    onPeriodSelect = { p -> vm.calPeriod.value = p },
                    onShiftRef = { dir -> vm.shiftCalRef(dir) },
                    onSelectMonth = { mDate -> vm.calRefDate.value = mDate },
                    onSelectDate = { d -> vm.openDaySheet(d) },
                    onSelectTask = { task, dateKey -> vm.openDetailSheet(task, dateKey) }
                )

                AppTab.TASKS -> TasksScreen(
                    tasks = allTasks,
                    selectionsMap = selectionsMap,
                    onToggleSelection = { task, dateKey -> vm.toggleTaskSelection(task, dateKey) },
                    onQuickAdd = { name -> vm.quickAdd(name) },
                    onSelectTask = { task, dateKey -> vm.openDetailSheet(task, dateKey) },
                    onEditTask = { task -> vm.openEditTaskSheet(task) }
                )

                AppTab.PROGRESS -> ProgressScreen(
                    period = progPeriod,
                    report = progressReport,
                    onPeriodSelect = { p -> vm.setProgPeriod(p) },
                    onShift = { dir -> vm.shiftProgRef(dir) },
                    onToday = { vm.progressToToday() }
                )

                AppTab.PROFILE -> ProfileScreen(
                    userUid = userUid,
                    userEmail = userEmail,
                    profileName = profileName,
                    profileContact = profileContact,
                    profileImageUri = profileImageUri,
                    themeMode = themeMode,
                    isSyncing = syncing,
                    authError = authError,
                    infoMessage = infoMessage,
                    onSignIn = { email, pw -> vm.signIn(email, pw) },
                    onRegister = { name, contact, email, pw -> vm.registerUser(name, contact, email, pw) },
                    onForgotPassword = { vm.openForgotPassword() },
                    onUpdateProfileImage = { uriStr -> vm.updateProfileImage(uriStr) },
                    onUpdateProfileDetails = { name, contact -> vm.updateProfileDetails(name, contact) },
                    onSetThemeMode = { mode -> vm.setThemeMode(mode) },
                    onLogout = { vm.signOut() },
                    onClearMessages = { vm.clearMessages() },
                    syncStatus = syncStatus,
                    notificationsEnabled = notifSettings.enabled,
                    defaultReminderTime = notifSettings.defaultTime,
                    repeatHours = notifSettings.repeatHours,
                    repeatMinutes = notifSettings.repeatMinutes,
                    onSetRepeatInterval = { h, m, all -> vm.setRepeatInterval(h, m, all) },
                    onSetNotificationsEnabled = { vm.setNotificationsEnabled(it) },
                    onSetDefaultReminderTime = { t, all -> vm.setDefaultReminderTime(t, all) }
                )
            }
        }
    }

    // Bottom Sheets
    if (isEditOpen) {
        TaskEditSheet(
            task = editTask,
            onSave = { task -> vm.saveTask(task) },
            onDelete = { id -> vm.deleteTask(id) },
            onDismiss = { vm.closeEditSheet() },
            onAddCategory = { label -> vm.addCustomCategory(label) },
            onAddSubcategory = { catId, label -> vm.addCustomSubcategory(catId, label) }
        )
    }

    if (isDetailOpen && detailPair != null) {
        val (task, dateKey) = detailPair!!
        val isDone = vm.isDone(task, dateKey)
        val todayKey = LocalDate.now().toString()
        val isOverdue = task.repeatType == "none" && dateKey < todayKey && !isDone

        TaskDetailSheet(
            task = task,
            isDone = isDone,
            isOverdue = isOverdue,
            dateKey = dateKey,
            canEdit = dateKey >= todayKey && !task.deleted,
            onEdit = {
                vm.closeDetailSheet()
                vm.openEditTaskSheet(task)
            },
            onDismiss = { vm.closeDetailSheet() }
        )
    }

    if (isDayOpen && dayKey != null) {
        DaySheet(
            dateKey = dayKey!!,
            tasks = dayTasks,
            isDone = { task -> vm.isDone(task, dayKey!!) },
            isTaskSelected = { task, dateKey -> vm.isSelected(task, dateKey) },
            onSelectTask = { task -> vm.openDetailSheet(task, dayKey!!) },
            onDismiss = { vm.closeDaySheet() }
        )
    }
}

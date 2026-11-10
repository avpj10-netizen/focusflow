package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.StudyAnalyticsSummary
import com.example.reminder.ReminderScheduler
import com.example.timer.TimerStatus
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.domain.DomainBackupDialog
import com.example.ui.exams.ExamsScreen
import com.example.ui.flashcards.FlashcardsScreen
import com.example.ui.focus.FocusScreen
import com.example.ui.tasks.TasksScreen
import com.example.ui.theme.ChronoStudyTheme
import com.example.ui.theme.IosDynamicIslandCapsule
import com.example.ui.theme.IosSegmentedControl
import com.example.ui.theme.iosBounceClick
import com.example.ui.timetable.TimetableScreen
import com.example.widget.ChronoStudyScheduleWidgetProvider
import com.example.widget.ChronoStudyWidgetProvider
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
    FOCUS("Focus"),
    TASKS("Tasks"),
    TIMETABLE("Schedule"),
    TOOLS("Study Tools"),
    ANALYTICS("Insights")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialTabString = intent.getStringExtra("EXTRA_NAV_TAB")

        setContent {
            ChronoStudyTheme {
                MainAppScreen(
                    initialTab = when (initialTabString) {
                        "tasks" -> AppTab.TASKS
                        "timetable" -> AppTab.TIMETABLE
                        "analytics" -> AppTab.ANALYTICS
                        else -> AppTab.FOCUS
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainAppScreen(initialTab: AppTab = AppTab.FOCUS) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val app = ChronoStudyApplication.instance
    val repository = app.repository
    val pomodoroEngine = app.pomodoroEngine

    var currentTab by remember { mutableStateOf(initialTab) }
    var toolsSubTab by remember { mutableIntStateOf(0) } // 0 = Exams, 1 = Flashcards
    var showDomainBackupDialog by remember { mutableStateOf(false) }

    // Request notification permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // BackHandler: if not on Focus tab, pressing back returns to Focus tab
    BackHandler(enabled = currentTab != AppTab.FOCUS) {
        currentTab = AppTab.FOCUS
    }

    val timerState by pomodoroEngine.state.collectAsState()
    val domains by repository.domains.collectAsState(initial = emptyList())
    val subjects by repository.subjects.collectAsState(initial = emptyList())
    val tasks by repository.tasks.collectAsState(initial = emptyList())
    val sessions by repository.sessions.collectAsState(initial = emptyList())
    val timetableSlots by repository.timetableSlots.collectAsState(initial = emptyList())
    val exams by repository.exams.collectAsState(initial = emptyList())
    val flashcards by repository.flashcards.collectAsState(initial = emptyList())
    val analytics by repository.getAnalytics().collectAsState(
        initial = StudyAnalyticsSummary(
            totalMinutesAllTime = 0,
            totalMinutesThisWeek = 0,
            currentStreakDays = 0,
            completedTasksCount = 0,
            totalTasksCount = 0,
            taskCompletionRatePercent = 0,
            subjectStats = emptyList(),
            domainStats = emptyList()
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.WaterBackdropBrush)
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                com.example.ui.theme.IosWaterTabBar(
                    items = AppTab.values().toList(),
                    selectedItem = currentTab,
                    onItemSelected = { currentTab = it },
                    getIcon = { tab, isSelected ->
                        when (tab) {
                            AppTab.FOCUS -> if (isSelected) Icons.Default.HourglassTop else Icons.Outlined.HourglassTop
                            AppTab.TASKS -> if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle
                            AppTab.TIMETABLE -> if (isSelected) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth
                            AppTab.TOOLS -> if (isSelected) Icons.Default.School else Icons.Outlined.School
                            AppTab.ANALYTICS -> if (isSelected) Icons.Default.QueryStats else Icons.Outlined.QueryStats
                        }
                    },
                    getLabel = { it.label }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Global Banner: iOS Dynamic Island Live Activity (shown when timer is running and user is on another tab)
                val isTimerRunning = timerState.status == TimerStatus.RUNNING
                AnimatedVisibility(
                    visible = isTimerRunning && currentTab != AppTab.FOCUS,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    val minutes = timerState.remainingSeconds / 60
                    val seconds = timerState.remainingSeconds % 60
                    val timeStr = String.format("%02d:%02d", minutes, seconds)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 4.dp)
                    ) {
                        IosDynamicIslandCapsule(
                            isRunning = isTimerRunning,
                            mode = timerState.mode,
                            timeText = timeStr,
                            subjectName = timerState.activeSubjectName,
                            onToggle = { pomodoroEngine.toggle() },
                            onExpand = { currentTab = AppTab.FOCUS }
                        )
                    }
                }

                // Main Content View based on Active Tab
                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        AppTab.FOCUS -> {
                            FocusScreen(
                                pomodoroEngine = pomodoroEngine,
                                subjects = subjects,
                                currentStreakDays = analytics.currentStreakDays,
                                onNavigateToTasks = { currentTab = AppTab.TASKS }
                            )
                        }
                        AppTab.TASKS -> {
                            TasksScreen(
                                tasks = tasks,
                                subjects = subjects,
                                onToggleTask = { taskId, completed ->
                                    coroutineScope.launch {
                                        repository.toggleTaskCompleted(taskId, completed)
                                        ChronoStudyWidgetProvider.updateAllWidgets(context)
                                        ChronoStudyScheduleWidgetProvider.updateAllWidgets(context)
                                    }
                                },
                                onDeleteTask = { task ->
                                    coroutineScope.launch {
                                        ReminderScheduler.cancelTaskReminder(context, task.id)
                                        repository.deleteTask(task)
                                        ChronoStudyWidgetProvider.updateAllWidgets(context)
                                        ChronoStudyScheduleWidgetProvider.updateAllWidgets(context)
                                    }
                                },
                                onAddTask = { title, notes, subjectId, dueDate, hasReminder, priority, estimatedPomodoros ->
                                    coroutineScope.launch {
                                        val newId = repository.insertTask(
                                            title = title,
                                            notes = notes,
                                            subjectId = subjectId,
                                            dueDate = dueDate,
                                            hasReminder = hasReminder,
                                            priority = priority,
                                            estimatedPomodoros = estimatedPomodoros
                                        )
                                        if (hasReminder && dueDate != null) {
                                            val subjName = subjects.find { it.id == subjectId }?.name ?: "Study"
                                            ReminderScheduler.scheduleTaskReminder(
                                                context = context,
                                                taskId = newId,
                                                taskTitle = title,
                                                subjectName = subjName,
                                                triggerAtMillis = dueDate
                                            )
                                        }
                                        ChronoStudyWidgetProvider.updateAllWidgets(context)
                                        ChronoStudyScheduleWidgetProvider.updateAllWidgets(context)
                                    }
                                },
                                onAddSubject = { name, colorHex ->
                                    coroutineScope.launch {
                                        repository.insertSubject(name, colorHex)
                                    }
                                },
                                onStartFocusForTask = { task, subject ->
                                    pomodoroEngine.setActiveTask(task.id, task.title)
                                    if (subject != null) {
                                        pomodoroEngine.setActiveSubject(subject.id, subject.name)
                                    }
                                    currentTab = AppTab.FOCUS
                                }
                            )
                        }
                        AppTab.TIMETABLE -> {
                            TimetableScreen(
                                slots = timetableSlots,
                                subjects = subjects,
                                onAddSlot = { subjectId, day, startMin, endMin, note ->
                                    coroutineScope.launch {
                                        repository.insertTimetableSlot(
                                            subjectId = subjectId,
                                            dayOfWeek = day,
                                            startMinute = startMin,
                                            endMinute = endMin,
                                            roomOrNote = note
                                        )
                                        ChronoStudyScheduleWidgetProvider.updateAllWidgets(context)
                                    }
                                },
                                onDeleteSlot = { slot ->
                                    coroutineScope.launch {
                                        repository.deleteTimetableSlot(slot)
                                        ChronoStudyScheduleWidgetProvider.updateAllWidgets(context)
                                    }
                                }
                            )
                        }
                        AppTab.TOOLS -> {
                            // Sub-navigation for Exams and Flashcards with iOS Segmented Control
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                    val toolOptions = listOf("Exams Countdown", "Recall Flashcards")
                                    IosSegmentedControl(
                                        items = toolOptions,
                                        selectedIndex = toolsSubTab,
                                        itemLabel = { it },
                                        onItemSelected = { toolsSubTab = it }
                                    )
                                }

                                if (toolsSubTab == 0) {
                                    ExamsScreen(
                                        exams = exams,
                                        subjects = subjects,
                                        onAddExam = { title, subjId, date, loc, cov, target, notes ->
                                            coroutineScope.launch {
                                                repository.insertExam(title, subjId, date, loc, cov, target, notes)
                                            }
                                        },
                                        onDeleteExam = { exam ->
                                            coroutineScope.launch {
                                                repository.deleteExam(exam)
                                            }
                                        },
                                        onUpdateCoverage = { exam, cov ->
                                            coroutineScope.launch {
                                                repository.updateExam(exam.copy(syllabusCoveragePercent = cov))
                                            }
                                        }
                                    )
                                } else {
                                    FlashcardsScreen(
                                        flashcards = flashcards,
                                        subjects = subjects,
                                        onAddFlashcard = { subjId, front, back ->
                                            coroutineScope.launch {
                                                repository.insertFlashcard(subjId, front, back)
                                            }
                                        },
                                        onToggleMastered = { cardId, mastered ->
                                            coroutineScope.launch {
                                                repository.toggleFlashcardMastered(cardId, mastered)
                                            }
                                        },
                                        onDeleteFlashcard = { card ->
                                            coroutineScope.launch {
                                                repository.deleteFlashcard(card)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        AppTab.ANALYTICS -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Top bar action for Academic Domains & Data Backup
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showDomainBackupDialog = true },
                                        modifier = Modifier
                                            .iosBounceClick { showDomainBackupDialog = true }
                                            .testTag("open_domains_backup_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderSpecial,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Academic Domains & Backup", fontSize = 12.sp)
                                    }
                                }

                                AnalyticsScreen(
                                    analytics = analytics,
                                    recentSessions = sessions,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    if (showDomainBackupDialog) {
        DomainBackupDialog(
            domains = domains,
            subjects = subjects,
            repository = repository,
            onDismiss = { showDomainBackupDialog = false },
            onAddDomain = { name, desc, colorHex, icon, hours ->
                coroutineScope.launch {
                    repository.insertDomain(name, desc, colorHex, icon, hours)
                }
            },
            onDeleteDomain = { domain ->
                coroutineScope.launch {
                    repository.deleteDomain(domain)
                }
            }
        )
    }
}

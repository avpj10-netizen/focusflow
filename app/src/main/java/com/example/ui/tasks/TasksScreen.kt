package com.example.ui.tasks

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.reminder.ReminderScheduler
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SubjectBadge
import com.example.ui.components.parseHexColor
import com.example.ui.theme.BreakGreen
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.FocusGold
import com.example.ui.theme.SubjectColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    subjects: List<SubjectEntity>,
    onToggleTask: (Long, Boolean) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onAddTask: (String, String, Long?, Long?, Boolean, String, Int) -> Unit,
    onAddSubject: (String, String) -> Unit,
    onStartFocusForTask: (TaskEntity, SubjectEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Pending") } // "All", "Pending", "Today", "High", "Completed"
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }
    val endOfToday = todayCal.timeInMillis

    val filteredTasks = tasks.filter { task ->
        val matchesSubject = selectedSubjectId == null || task.subjectId == selectedSubjectId
        val matchesStatus = when (selectedFilter) {
            "Pending" -> !task.isCompleted
            "Completed" -> task.isCompleted
            "Today" -> !task.isCompleted && task.dueDate != null && task.dueDate <= endOfToday
            "High" -> !task.isCompleted && task.priority.uppercase() == "HIGH"
            else -> true
        }
        matchesSubject && matchesStatus
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "Study Tasks",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${tasks.count { !it.isCompleted }} pending assignments & goals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = { showAddSubjectDialog = true },
                    modifier = Modifier.testTag("new_subject_button")
                ) {
                    Text("+ New Subject")
                }
            }

            // Quick Status Filter Chips
            val statusFilters = listOf("Pending", "Today", "High", "Completed", "All")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                items(statusFilters) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { selectedFilter = filter }
                            .testTag("task_filter_${filter.lowercase()}")
                    ) {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Subject Filter Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                item {
                    val isAll = selectedSubjectId == null
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAll) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = if (!isAll) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null,
                        modifier = Modifier.clickable { selectedSubjectId = null }
                    ) {
                        Text(
                            text = "All Subjects",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                items(subjects) { subject ->
                    val isSelected = selectedSubjectId == subject.id
                    val color = parseHexColor(subject.colorHex)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { selectedSubjectId = subject.id }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Task List or Empty State
            if (filteredTasks.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.TaskAlt,
                    title = "No Tasks Found",
                    message = if (selectedFilter == "Completed") "No completed tasks yet. Finish a study task to see it here!" else "You have no tasks matching this filter. Tap '+' below to add one!",
                    actionLabel = if (selectedFilter != "Completed") "Add Study Task" else null,
                    onAction = { showAddTaskDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        val subject = subjects.find { it.id == task.subjectId }
                        TaskItemCard(
                            task = task,
                            subject = subject,
                            onToggle = { completed -> onToggleTask(task.id, completed) },
                            onDelete = { onDeleteTask(task) },
                            onFocus = { onStartFocusForTask(task, subject) }
                        )
                    }
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            subjects = subjects,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, notes, subjectId, dueDate, hasReminder, priority, estimatedPomodoros ->
                onAddTask(title, notes, subjectId, dueDate, hasReminder, priority, estimatedPomodoros)
                showAddTaskDialog = false
            }
        )
    }

    // Add Subject Dialog
    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onSave = { name, colorHex ->
                onAddSubject(name, colorHex)
                showAddSubjectDialog = false
            }
        )
    }
}

@Composable
fun TaskItemCard(
    task: TaskEntity,
    subject: SubjectEntity?,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (task.isCompleted) Color(0x121E293B) else Color(0x281E293B),
        shadowElevation = if (task.isCompleted) 1.dp else 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                com.example.ui.theme.WaterBorderBrush,
                RoundedCornerShape(18.dp)
            )
            .testTag("task_item_${task.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle(it) },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("task_checkbox_${task.id}")
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )

                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badges row: Subject, Priority, Due Date, Pomodoro Count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (subject != null) {
                            SubjectBadge(name = subject.name, colorHex = subject.colorHex)
                        }

                        PriorityBadge(priority = task.priority)

                        if (task.dueDate != null) {
                            val isOverdue = !task.isCompleted && task.dueDate < System.currentTimeMillis()
                            val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isOverdue) DangerCoral.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = if (isOverdue) DangerCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = formatter.format(Date(task.dueDate)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isOverdue) DangerCoral else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (task.hasReminder) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder active",
                                tint = FocusGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Pomodoro counter badge
                        Text(
                            text = "🍅 ${task.completedPomodoros}/${task.estimatedPomodoros}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!task.isCompleted) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onFocus,
                        shape = RoundedCornerShape(8.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.height(32.dp).testTag("task_focus_button_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Focus on Task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, Long?, Long?, Boolean, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf<Long?>(subjects.firstOrNull()?.id) }
    var priority by remember { mutableStateOf("MEDIUM") }
    var estimatedPoms by remember { mutableIntStateOf(2) }
    var hasReminder by remember { mutableStateOf(true) }
    var daysUntilDue by remember { mutableIntStateOf(1) } // 0 = Today, 1 = Tomorrow, 3 = In 3 days, 7 = Next week

    val calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysUntilDue)
        set(Calendar.HOUR_OF_DAY, 18) // 6:00 PM
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val calculatedDueDate = calendar.timeInMillis

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Study Task", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Complete Chapter 4 Quiz") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Formulas to memorize, pages 120-145...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subject picker
                Text("Subject", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjects) { subj ->
                        val isSelected = selectedSubjectId == subj.id
                        val color = parseHexColor(subj.colorHex)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
                            modifier = Modifier.clickable { selectedSubjectId = subj.id }
                        ) {
                            Text(
                                text = subj.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Due Date presets
                Text("Due Date", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val dateOptions = listOf(0 to "Today", 1 to "Tomorrow", 3 to "In 3 Days", 7 to "Next Week")
                    dateOptions.forEach { (days, label) ->
                        val isSelected = daysUntilDue == days
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { daysUntilDue = days }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Priority & Estimated Pomodoros
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Priority", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("LOW" to "Low", "MEDIUM" to "Med", "HIGH" to "High").forEach { (pri, label) ->
                                val isSelected = priority == pri
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { priority = pri }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Est. Pomodoros", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (estimatedPoms > 1) estimatedPoms-- },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("-", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "🍅 $estimatedPoms",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = { if (estimatedPoms < 10) estimatedPoms++ },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("+", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reminder switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remind me before due date", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = hasReminder,
                        onCheckedChange = { hasReminder = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title,
                            notes,
                            selectedSubjectId,
                            calculatedDueDate,
                            hasReminder,
                            priority,
                            estimatedPoms
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(SubjectColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Subject", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name *") },
                    placeholder = { Text("e.g. Organic Chemistry") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Theme Color", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SubjectColors.take(6).forEach { hex ->
                        val isSelected = selectedColor == hex
                        val color = parseHexColor(hex)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedColor)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_subject_button")
            ) {
                Text("Create Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

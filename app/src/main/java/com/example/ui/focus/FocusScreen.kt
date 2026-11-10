package com.example.ui.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubjectEntity
import com.example.timer.AmbientSound
import com.example.timer.PomodoroEngine
import com.example.timer.PomodoroMode
import com.example.timer.TimerStatus
import com.example.ui.components.SubjectBadge
import com.example.ui.components.parseHexColor
import com.example.ui.theme.BreakGreen
import com.example.ui.theme.FocusGold
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IosSegmentedControl
import com.example.ui.theme.iosBounceClick

@Composable
fun FocusScreen(
    pomodoroEngine: PomodoroEngine,
    subjects: List<SubjectEntity>,
    currentStreakDays: Int,
    onNavigateToTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by pomodoroEngine.state.collectAsState()
    var showSubjectDropdown by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Streak Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = FocusGold.copy(alpha = 0.15f),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = FocusGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentStreakDays > 0) "$currentStreakDays Day Study Streak!" else "Start your study streak today!",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = FocusGold
                )
            }
        }

        // iOS-style Segmented Control for Mode
        val modes = listOf(PomodoroMode.WORK, PomodoroMode.SHORT_BREAK, PomodoroMode.LONG_BREAK)
        IosSegmentedControl(
            items = modes,
            selectedIndex = modes.indexOf(state.mode).coerceAtLeast(0),
            itemLabel = { it.label },
            onItemSelected = { index -> pomodoroEngine.setMode(modes[index]) },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Active Subject Selector Pill
        Box(modifier = Modifier.padding(bottom = 20.dp)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier
                    .clickable { showSubjectDropdown = true }
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    )
                    .testTag("select_subject_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.activeSubjectName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change Subject",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showSubjectDropdown,
                onDismissRequest = { showSubjectDropdown = false }
            ) {
                DropdownMenuItem(
                    text = { Text("General Focus") },
                    onClick = {
                        pomodoroEngine.setActiveSubject(null, "General Focus")
                        showSubjectDropdown = false
                    }
                )
                subjects.forEach { subject ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(parseHexColor(subject.colorHex), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(subject.name)
                            }
                        },
                        onClick = {
                            pomodoroEngine.setActiveSubject(subject.id, subject.name)
                            showSubjectDropdown = false
                        }
                    )
                }
            }
        }

        // Active Task Banner (if set)
        if (state.activeTaskTitle != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Working on: ${state.activeTaskTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1
                    )
                }
            }
        }

        // Circular Timer Canvas
        val progress = if (state.totalDurationSeconds > 0) {
            1f - (state.remainingSeconds.toFloat() / state.totalDurationSeconds.toFloat())
        } else 0f

        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(durationMillis = 500),
            label = "timer_progress"
        )

        val accentColor = when (state.mode) {
            PomodoroMode.WORK -> FocusGold
            PomodoroMode.SHORT_BREAK -> BreakGreen
            PomodoroMode.LONG_BREAK -> IndigoPrimary
        }

        val trackColor = MaterialTheme.colorScheme.surfaceVariant

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(270.dp)
                .background(Color(0x1F1E293B), CircleShape)
                .border(1.dp, com.example.ui.theme.WaterBorderBrush, CircleShape)
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                // Background track
                drawCircle(
                    color = trackColor,
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // Progress Arc
                val sweepAngle = animatedProgress * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(accentColor.copy(alpha = 0.7f), accentColor),
                        center = centerOffset
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val minutes = state.remainingSeconds / 60
                val seconds = state.remainingSeconds % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)

                Text(
                    text = timeString,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (state.status) {
                        TimerStatus.RUNNING -> "In Progress"
                        TimerStatus.PAUSED -> "Paused"
                        TimerStatus.IDLE -> "Ready"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = accentColor
                )
                if (state.completedSessionsCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Completed: ${state.completedSessionsCount} sessions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Timer Controls Row: Reset, Play/Pause, Skip, Tune
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { pomodoroEngine.reset() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .iosBounceClick { pomodoroEngine.reset() }
                    .testTag("reset_timer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Primary Start/Pause Button with iOS bounce
            val isRunning = state.status == TimerStatus.RUNNING
            Button(
                onClick = { pomodoroEngine.toggle() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(76.dp)
                    .iosBounceClick(scaleDown = 0.90f) { pomodoroEngine.toggle() }
                    .testTag("play_pause_timer_button")
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { pomodoroEngine.skip() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .iosBounceClick { pomodoroEngine.skip() }
                    .testTag("skip_timer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = { showDurationDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .iosBounceClick { showDurationDialog = true }
                    .testTag("customize_duration_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Durations",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Ambient Sound Controls
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Focus Ambient Sound",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AmbientSound.values().forEach { sound ->
                    val isSelected = state.ambientSound == sound
                    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0x221E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0x26FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .iosBounceClick { pomodoroEngine.setAmbientSound(sound) }
                            .testTag("ambient_${sound.name.lowercase()}")
                    ) {
                        Text(
                            text = sound.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = tint,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Custom Duration Dialog
    if (showDurationDialog) {
        var workMins by remember { mutableFloatStateOf(state.workDurationMinutes.toFloat()) }
        var breakMins by remember { mutableFloatStateOf(state.shortBreakDurationMinutes.toFloat()) }

        AlertDialog(
            onDismissRequest = { showDurationDialog = false },
            title = { Text("Session Durations", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Text(
                        text = "Focus: ${workMins.toInt()} min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = workMins,
                        onValueChange = { workMins = it },
                        valueRange = 10f..60f,
                        steps = 9
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Short Break: ${breakMins.toInt()} min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = breakMins,
                        onValueChange = { breakMins = it },
                        valueRange = 3f..20f,
                        steps = 16
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pomodoroEngine.setDurations(
                            workMinutes = workMins.toInt(),
                            shortBreakMinutes = breakMins.toInt(),
                            longBreakMinutes = 15
                        )
                        showDurationDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDurationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

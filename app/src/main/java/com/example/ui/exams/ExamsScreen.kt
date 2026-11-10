package com.example.ui.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamEntity
import com.example.data.model.SubjectEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SubjectBadge
import com.example.ui.components.parseHexColor
import com.example.ui.theme.BreakGreen
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.FocusGold
import com.example.ui.theme.iosBounceClick
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ExamsScreen(
    exams: List<ExamEntity>,
    subjects: List<SubjectEntity>,
    onAddExam: (String, Long, Long, String, Int, String, String) -> Unit,
    onDeleteExam: (ExamEntity) -> Unit,
    onUpdateCoverage: (ExamEntity, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .iosBounceClick { showAddDialog = true }
                    .testTag("add_exam_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Exam")
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
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Exam Countdown & Milestones",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track countdowns, revision coverage & target grades",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (exams.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Assignment,
                    title = "No Upcoming Exams Tracked",
                    message = "Add midterms, finals, or certification dates to monitor countdowns and revision progress.",
                    actionLabel = "Add Exam",
                    onAction = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(exams, key = { it.id }) { exam ->
                        val subject = subjects.find { it.id == exam.subjectId }
                        ExamCard(
                            exam = exam,
                            subject = subject,
                            onDelete = { onDeleteExam(exam) },
                            onCoverageChange = { newPercent -> onUpdateCoverage(exam, newPercent) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExamDialog(
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onSave = { title, subjectId, dateMillis, loc, coverage, target, notes ->
                onAddExam(title, subjectId, dateMillis, loc, coverage, target, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExamCard(
    exam: ExamEntity,
    subject: SubjectEntity?,
    onDelete: () -> Unit,
    onCoverageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val diffMillis = exam.examDate - now
    val daysRemaining = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

    val (urgencyColor, urgencyLabel) = when {
        daysRemaining < 0 -> DangerCoral to "Passed"
        daysRemaining == 0 -> DangerCoral to "TODAY!"
        daysRemaining <= 3 -> DangerCoral to "$daysRemaining days left! 🔥"
        daysRemaining <= 7 -> FocusGold to "$daysRemaining days left"
        else -> BreakGreen to "$daysRemaining days"
    }

    val formatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x281E293B),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                com.example.ui.theme.WaterBorderBrush,
                RoundedCornerShape(20.dp)
            )
            .testTag("exam_card_${exam.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title, Subject, Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (subject != null) {
                            SubjectBadge(name = subject.name, colorHex = subject.colorHex)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Grade,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Target ${exam.targetGrade}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Days remaining pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = urgencyColor.copy(alpha = 0.18f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = urgencyLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Exam",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details: Date, Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatter.format(Date(exam.examDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (exam.location.isNotBlank()) {
                    Spacer(modifier = Modifier.width(14.dp))
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = exam.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Syllabus Coverage Progress
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Syllabus Revision Coverage",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${exam.syllabusCoveragePercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { exam.syllabusCoveragePercent / 100f },
                    color = if (exam.syllabusCoveragePercent >= 80) BreakGreen else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun AddExamDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long, String, Int, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var daysInFuture by remember { mutableIntStateOf(7) }
    var location by remember { mutableStateOf("") }
    var targetGrade by remember { mutableStateOf("A") }
    var coverage by remember { mutableFloatStateOf(50f) }
    var notes by remember { mutableStateOf("") }

    val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysInFuture)
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track New Exam", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Exam / Test Title *") },
                    placeholder = { Text("e.g. Physics Midterm II") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("exam_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Subject", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    subjects.take(4).forEach { subj ->
                        val isSelected = selectedSubjectId == subj.id
                        val color = parseHexColor(subj.colorHex)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
                            modifier = Modifier
                                .weight(1f)
                                .iosBounceClick { selectedSubjectId = subj.id }
                        ) {
                            Text(
                                text = subj.name.take(8),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Days Until Exam: $daysInFuture days", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    listOf(3 to "3d", 7 to "1w", 14 to "2w", 30 to "1m").forEach { (d, label) ->
                        val isSelected = daysInFuture == d
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .iosBounceClick { daysInFuture = d }
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Hall / Room") },
                        placeholder = { Text("Room 204") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = targetGrade,
                        onValueChange = { targetGrade = it },
                        label = { Text("Target Grade") },
                        placeholder = { Text("A") },
                        singleLine = true,
                        modifier = Modifier.width(100.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Current Syllabus Coverage: ${coverage.toInt()}%", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = coverage,
                    onValueChange = { coverage = it },
                    valueRange = 0f..100f,
                    steps = 19
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), selectedSubjectId, cal.timeInMillis, location, coverage.toInt(), targetGrade, notes)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_exam_button")
            ) {
                Text("Track Exam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

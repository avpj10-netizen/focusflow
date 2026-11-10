package com.example.ui.timetable

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubjectEntity
import com.example.data.model.TimetableSlotEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SubjectBadge
import com.example.ui.components.parseHexColor
import java.util.Calendar

private val DaysOfWeek = listOf(
    1 to "Monday",
    2 to "Tuesday",
    3 to "Wednesday",
    4 to "Thursday",
    5 to "Friday",
    6 to "Saturday",
    7 to "Sunday"
)

fun formatMinuteOfDay(minuteOfDay: Int): String {
    val hours = minuteOfDay / 60
    val minutes = minuteOfDay % 60
    val amPm = if (hours >= 12) "PM" else "AM"
    val displayHours = if (hours % 12 == 0) 12 else hours % 12
    return String.format("%d:%02d %s", displayHours, minutes, amPm)
}

@Composable
fun TimetableScreen(
    slots: List<TimetableSlotEntity>,
    subjects: List<SubjectEntity>,
    onAddSlot: (Long, Int, Int, Int, String) -> Unit,
    onDeleteSlot: (TimetableSlotEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Determine current day of week (Calendar.MONDAY is 2, etc.)
    val currentDayIndex = remember {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Convert Calendar.DAY_OF_WEEK (Sun=1, Mon=2..Sat=7) to 1=Mon..7=Sun
        if (dow == Calendar.SUNDAY) 7 else dow - 1
    }

    var selectedDay by remember { mutableIntStateOf(currentDayIndex) }
    var showAddSlotDialog by remember { mutableStateOf(false) }

    val daySlots = slots
        .filter { it.dayOfWeek == selectedDay }
        .sortedBy { it.startMinuteOfDay }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSlotDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_slot_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Class/Slot")
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
            // Header with Calendar Sync Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "Weekly Timetable",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Classes, study sessions & lectures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Device Calendar Sync Button
                OutlinedButton(
                    onClick = {
                        syncDaySlotsToCalendar(context, daySlots, subjects, selectedDay)
                    },
                    modifier = Modifier.testTag("sync_calendar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync to Calendar", fontSize = 12.sp)
                }
            }

            // Weekday Tab Selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(DaysOfWeek) { (dayNum, dayName) ->
                    val isSelected = selectedDay == dayNum
                    val isToday = currentDayIndex == dayNum

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isToday && !isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .clickable { selectedDay = dayNum }
                            .testTag("day_tab_$dayNum")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = dayName.take(3).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(4.dp)
                                        .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slots list
            if (daySlots.isEmpty()) {
                val selectedDayName = DaysOfWeek.find { it.first == selectedDay }?.second ?: "this day"
                EmptyStateView(
                    icon = Icons.Default.CalendarMonth,
                    title = "No Classes on $selectedDayName",
                    message = "You have no scheduled lectures or study sessions. Tap '+' to schedule one!",
                    actionLabel = "Add Study Block",
                    onAction = { showAddSlotDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(daySlots, key = { it.id }) { slot ->
                        val subject = subjects.find { it.id == slot.subjectId }
                        TimetableSlotCard(
                            slot = slot,
                            subject = subject,
                            onDelete = { onDeleteSlot(slot) },
                            onExportSingle = {
                                exportSingleSlotToCalendar(context, slot, subject, selectedDay)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddSlotDialog) {
        AddSlotDialog(
            subjects = subjects,
            initialDay = selectedDay,
            onDismiss = { showAddSlotDialog = false },
            onSave = { subjectId, day, startMin, endMin, note ->
                onAddSlot(subjectId, day, startMin, endMin, note)
                showAddSlotDialog = false
            }
        )
    }
}

@Composable
fun TimetableSlotCard(
    slot: TimetableSlotEntity,
    subject: SubjectEntity?,
    onDelete: () -> Unit,
    onExportSingle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = parseHexColor(subject?.colorHex ?: "#6366F1")

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0x281E293B),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                com.example.ui.theme.WaterBorderBrush,
                RoundedCornerShape(18.dp)
            )
            .testTag("slot_item_${slot.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            // Visual Accent Color Bar
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 48.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = subject?.name ?: "Study Block",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onExportSingle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Export to Calendar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatMinuteOfDay(slot.startMinuteOfDay)} - ${formatMinuteOfDay(slot.endMinuteOfDay)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (slot.roomOrNote.isNotBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = slot.roomOrNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Slot",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddSlotDialog(
    subjects: List<SubjectEntity>,
    initialDay: Int,
    onDismiss: () -> Unit,
    onSave: (Long, Int, Int, Int, String) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }
    var startHour by remember { mutableFloatStateOf(9f) } // 9:00 AM default
    var durationMins by remember { mutableIntStateOf(60) } // 60 mins default
    var roomOrNote by remember { mutableStateOf("") }

    val startMinuteOfDay = (startHour * 60).toInt()
    val endMinuteOfDay = startMinuteOfDay + durationMins

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Timetable Slot", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
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

                Text("Day of Week", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DaysOfWeek) { (dayNum, name) ->
                        val isSelected = selectedDay == dayNum
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedDay = dayNum }
                        ) {
                            Text(
                                text = name.take(3),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Start Time: ${formatMinuteOfDay(startMinuteOfDay)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = startHour,
                    onValueChange = { startHour = it },
                    valueRange = 7f..21f,
                    steps = 27 // Every 30 mins
                )

                Text(
                    text = "Duration: $durationMins minutes (Ends ${formatMinuteOfDay(endMinuteOfDay)})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    listOf(45, 60, 90, 120).forEach { mins ->
                        val isSelected = durationMins == mins
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { durationMins = mins }
                        ) {
                            Text(
                                text = "${mins}m",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = roomOrNote,
                    onValueChange = { roomOrNote = it },
                    label = { Text("Room / Location (Optional)") },
                    placeholder = { Text("e.g. Science Block 101 or Zoom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedSubjectId, selectedDay, startMinuteOfDay, endMinuteOfDay, roomOrNote)
                },
                enabled = selectedSubjectId > 0,
                modifier = Modifier.testTag("save_slot_button")
            ) {
                Text("Save Slot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Calendar Sync Intent helpers
private fun syncDaySlotsToCalendar(
    context: Context,
    slots: List<TimetableSlotEntity>,
    subjects: List<SubjectEntity>,
    dayOfWeek: Int
) {
    if (slots.isEmpty()) {
        Toast.makeText(context, "No classes scheduled for this day to sync.", Toast.LENGTH_SHORT).show()
        return
    }

    val firstSlot = slots.first()
    exportSingleSlotToCalendar(context, firstSlot, subjects.find { it.id == firstSlot.subjectId }, dayOfWeek)
}

private fun exportSingleSlotToCalendar(
    context: Context,
    slot: TimetableSlotEntity,
    subject: SubjectEntity?,
    dayOfWeek: Int
) {
    val cal = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        // Adjust to the matching day of week
        val currentDow = if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else get(Calendar.DAY_OF_WEEK) - 1
        val diff = dayOfWeek - currentDow
        add(Calendar.DAY_OF_YEAR, diff)
        set(Calendar.HOUR_OF_DAY, slot.startMinuteOfDay / 60)
        set(Calendar.MINUTE, slot.startMinuteOfDay % 60)
        set(Calendar.SECOND, 0)
    }
    val startMillis = cal.timeInMillis

    val endCal = Calendar.getInstance().apply {
        timeInMillis = startMillis
        set(Calendar.HOUR_OF_DAY, slot.endMinuteOfDay / 60)
        set(Calendar.MINUTE, slot.endMinuteOfDay % 60)
    }
    val endMillis = endCal.timeInMillis

    val title = "${subject?.name ?: "Study Session"} Class"
    val description = "ChronoStudy Scheduled Study Block. Location: ${slot.roomOrNote}"

    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        .putExtra(CalendarContract.Events.TITLE, title)
        .putExtra(CalendarContract.Events.DESCRIPTION, description)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, slot.roomOrNote)
        .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)

    try {
        context.startActivity(intent)
        Toast.makeText(context, "Opening Calendar for $title", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "No calendar application found on device.", Toast.LENGTH_SHORT).show()
    }
}

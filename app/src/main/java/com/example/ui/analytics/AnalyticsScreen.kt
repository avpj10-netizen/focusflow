package com.example.ui.analytics

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudyAnalyticsSummary
import com.example.data.SubjectStudyStat
import com.example.data.model.FocusSessionEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatMetricCard
import com.example.ui.components.SubjectBadge
import com.example.ui.components.parseHexColor
import com.example.ui.theme.BreakGreen
import com.example.ui.theme.FocusGold
import com.example.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatMinutes(minutes: Int): String {
    val hrs = minutes / 60
    val rem = minutes % 60
    return if (hrs > 0) "${hrs}h ${rem}m" else "${rem}m"
}

@Composable
fun AnalyticsScreen(
    analytics: StudyAnalyticsSummary,
    recentSessions: List<FocusSessionEntity>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("analytics_screen")
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "Study Insights",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track your focus momentum and study velocity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Streak Banner Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x281E293B),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, com.example.ui.theme.WaterBorderBrush, RoundedCornerShape(20.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(FocusGold.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = FocusGold,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "${analytics.currentStreakDays} Day Focus Streak! 🔥",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (analytics.currentStreakDays > 0)
                                "Consistency is key! Keep studying daily to maintain your momentum."
                            else
                                "Complete a Pomodoro session today to kick off your streak!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Metrics Grid (Total Hours, This Week, Completion Rate)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatMetricCard(
                    title = "This Week",
                    value = formatMinutes(analytics.totalMinutesThisWeek),
                    subtitle = "Active focus",
                    icon = Icons.Default.DateRange,
                    accentColor = IndigoPrimary,
                    modifier = Modifier.weight(1f)
                )

                StatMetricCard(
                    title = "All Time",
                    value = formatMinutes(analytics.totalMinutesAllTime),
                    subtitle = "Logged study",
                    icon = Icons.Default.HourglassBottom,
                    accentColor = FocusGold,
                    modifier = Modifier.weight(1f)
                )

                StatMetricCard(
                    title = "Task Rate",
                    value = "${analytics.taskCompletionRatePercent}%",
                    subtitle = "${analytics.completedTasksCount}/${analytics.totalTasksCount} done",
                    icon = Icons.Default.AssignmentTurnedIn,
                    accentColor = BreakGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Subject Breakdown Donut & Progress Section
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x241E293B),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, com.example.ui.theme.WaterBorderBrush, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Subject Study Time Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (analytics.subjectStats.none { it.totalMinutesStudied > 0 }) {
                        Text(
                            text = "Complete your first Pomodoro focus session to visualize your subject study time distribution.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Donut Chart
                        val studiedSubjects = analytics.subjectStats.filter { it.totalMinutesStudied > 0 }
                        val totalMinutes = studiedSubjects.sumOf { it.totalMinutesStudied }.coerceAtLeast(1)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(130.dp)
                                    .padding(8.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 20.dp.toPx()
                                    val radius = (size.minDimension - strokeWidth) / 2
                                    val centerOffset = Offset(size.width / 2, size.height / 2)

                                    var currentAngle = -90f
                                    studiedSubjects.forEach { stat ->
                                        val sweep = (stat.totalMinutesStudied.toFloat() / totalMinutes) * 360f
                                        val color = parseHexColor(stat.subject.colorHex)
                                        drawArc(
                                            color = color,
                                            startAngle = currentAngle,
                                            sweepAngle = sweep - 2f, // Small gap
                                            useCenter = false,
                                            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                                            size = Size(radius * 2, radius * 2),
                                            style = Stroke(width = strokeWidth)
                                        )
                                        currentAngle += sweep
                                    }
                                }

                                Text(
                                    text = formatMinutes(totalMinutes),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Legend Column
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                studiedSubjects.take(4).forEach { stat ->
                                    val percent = (stat.totalMinutesStudied * 100) / totalMinutes
                                    val color = parseHexColor(stat.subject.colorHex)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(color, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stat.subject.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "$percent%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress bars per subject
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            analytics.subjectStats.take(5).forEach { stat ->
                                val color = parseHexColor(stat.subject.colorHex)
                                val progressRatio = (stat.totalMinutesStudied.toFloat() / stat.targetMinutes.toFloat()).coerceIn(0f, 1f)
                                Column {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stat.subject.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${formatMinutes(stat.totalMinutesStudied)} / ${formatMinutes(stat.targetMinutes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progressRatio },
                                        color = color,
                                        trackColor = MaterialTheme.colorScheme.surface,
                                        strokeCap = StrokeCap.Round,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Academic Domains Progress Card
        if (analytics.domainStats.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x241E293B),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, com.example.ui.theme.WaterBorderBrush, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueryStats,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Academic Domains Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            analytics.domainStats.forEach { dStat ->
                                val color = parseHexColor(dStat.domain.colorHex)
                                val targetMinutes = dStat.targetWeeklyHours * 60
                                val ratio = (dStat.totalMinutesStudied.toFloat() / targetMinutes.toFloat()).coerceIn(0f, 1f)
                                Column {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = dStat.domain.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${formatMinutes(dStat.totalMinutesStudied)} / ${dStat.targetWeeklyHours}h",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        color = color,
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
                }
            }
        }

        // Recent Sessions Log
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Study Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (recentSessions.isEmpty()) {
            item {
                Text(
                    text = "No focus sessions recorded yet. Start a Pomodoro timer to see your session logs here!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(recentSessions.take(10), key = { it.id }) { session ->
                val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                val isWork = session.sessionType == "WORK"

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isWork) FocusGold.copy(alpha = 0.15f) else BreakGreen.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isWork) "🍅" else "☕",
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isWork) "Focus Session (${session.durationMinutes} min)" else "Break Session (${session.durationMinutes} min)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatter.format(Date(session.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

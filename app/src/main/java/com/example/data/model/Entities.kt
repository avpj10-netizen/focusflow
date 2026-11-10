package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "domains")
data class StudyDomainEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#6366F1",
    val iconName: String = "school", // school, code, science, book, business, palette
    val targetWeeklyHours: Int = 15
)

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = StudyDomainEntity::class,
            parentColumns = ["id"],
            childColumns = ["domainId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("domainId")]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val targetWeeklyMinutes: Int = 300,
    val domainId: Long? = null
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val subjectId: Long? = null,
    val dueDate: Long? = null, // epoch millis
    val hasReminder: Boolean = false,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val estimatedPomodoros: Int = 1,
    val completedPomodoros: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectId"), Index("taskId")]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long? = null,
    val taskId: Long? = null,
    val durationMinutes: Int,
    val sessionType: String, // WORK, SHORT_BREAK, LONG_BREAK
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ... 7 = Sunday
    val startMinuteOfDay: Int, // e.g., 540 = 09:00 AM
    val endMinuteOfDay: Int,   // e.g., 600 = 10:00 AM
    val roomOrNote: String = ""
)

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subjectId: Long,
    val examDate: Long, // epoch millis
    val location: String = "",
    val syllabusCoveragePercent: Int = 0,
    val targetGrade: String = "A",
    val notes: String = ""
)

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val frontText: String,
    val backText: String,
    val isMastered: Boolean = false,
    val reviewCount: Int = 0
)

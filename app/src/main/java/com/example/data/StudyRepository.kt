package com.example.data

import com.example.data.model.ExamEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.StudyDomainEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TimetableSlotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class SubjectStudyStat(
    val subject: SubjectEntity,
    val totalMinutesStudied: Int,
    val targetMinutes: Int
)

data class DomainStudyStat(
    val domain: StudyDomainEntity,
    val totalMinutesStudied: Int,
    val targetWeeklyHours: Int,
    val subjectCount: Int
)

data class StudyAnalyticsSummary(
    val totalMinutesAllTime: Int,
    val totalMinutesThisWeek: Int,
    val currentStreakDays: Int,
    val completedTasksCount: Int,
    val totalTasksCount: Int,
    val taskCompletionRatePercent: Int,
    val subjectStats: List<SubjectStudyStat>,
    val domainStats: List<DomainStudyStat>
)

class StudyRepository(private val database: AppDatabase) {
    val domains: Flow<List<StudyDomainEntity>> = database.domainDao().getAllDomains()
    val subjects: Flow<List<SubjectEntity>> = database.subjectDao().getAllSubjects()
    val tasks: Flow<List<TaskEntity>> = database.taskDao().getAllTasks()
    val topUpcomingTasks: Flow<List<TaskEntity>> = database.taskDao().getTopUpcomingTasks(limit = 4)
    val sessions: Flow<List<FocusSessionEntity>> = database.focusSessionDao().getAllSessions()
    val timetableSlots: Flow<List<TimetableSlotEntity>> = database.timetableDao().getAllSlots()
    val exams: Flow<List<ExamEntity>> = database.examDao().getAllExams()
    val flashcards: Flow<List<FlashcardEntity>> = database.flashcardDao().getAllFlashcards()

    suspend fun getUpcomingTasksSync(): List<TaskEntity> = database.taskDao().getUpcomingTasksSync()

    // Domain operations
    suspend fun insertDomain(name: String, description: String, colorHex: String, iconName: String, targetWeeklyHours: Int = 15): Long {
        return database.domainDao().insertDomain(
            StudyDomainEntity(
                name = name.trim(),
                description = description.trim(),
                colorHex = colorHex,
                iconName = iconName,
                targetWeeklyHours = targetWeeklyHours
            )
        )
    }

    suspend fun deleteDomain(domain: StudyDomainEntity) {
        database.domainDao().deleteDomain(domain)
    }

    // Subject operations
    suspend fun insertSubject(name: String, colorHex: String, targetWeeklyMinutes: Int = 300, domainId: Long? = null): Long {
        return database.subjectDao().insertSubject(
            SubjectEntity(name = name.trim(), colorHex = colorHex, targetWeeklyMinutes = targetWeeklyMinutes, domainId = domainId)
        )
    }

    suspend fun deleteSubject(subject: SubjectEntity) {
        database.subjectDao().deleteSubject(subject)
    }

    // Task operations
    suspend fun insertTask(
        title: String,
        notes: String,
        subjectId: Long?,
        dueDate: Long?,
        hasReminder: Boolean,
        priority: String,
        estimatedPomodoros: Int
    ): Long {
        return database.taskDao().insertTask(
            TaskEntity(
                title = title.trim(),
                notes = notes.trim(),
                subjectId = subjectId,
                dueDate = dueDate,
                hasReminder = hasReminder,
                priority = priority,
                estimatedPomodoros = estimatedPomodoros
            )
        )
    }

    suspend fun updateTask(task: TaskEntity) {
        database.taskDao().updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        database.taskDao().deleteTask(task)
    }

    suspend fun toggleTaskCompleted(taskId: Long, isCompleted: Boolean) {
        database.taskDao().setTaskCompleted(taskId, isCompleted)
    }

    // Focus Session operations
    suspend fun recordFocusSession(
        subjectId: Long?,
        taskId: Long?,
        durationMinutes: Int,
        sessionType: String
    ): Long {
        val id = database.focusSessionDao().insertSession(
            FocusSessionEntity(
                subjectId = subjectId,
                taskId = taskId,
                durationMinutes = durationMinutes,
                sessionType = sessionType,
                timestamp = System.currentTimeMillis()
            )
        )
        if (sessionType == "WORK" && taskId != null) {
            database.taskDao().incrementCompletedPomodoro(taskId)
        }
        return id
    }

    // Timetable operations
    suspend fun insertTimetableSlot(
        subjectId: Long,
        dayOfWeek: Int,
        startMinute: Int,
        endMinute: Int,
        roomOrNote: String
    ): Long {
        return database.timetableDao().insertSlot(
            TimetableSlotEntity(
                subjectId = subjectId,
                dayOfWeek = dayOfWeek,
                startMinuteOfDay = startMinute,
                endMinuteOfDay = endMinute,
                roomOrNote = roomOrNote.trim()
            )
        )
    }

    suspend fun deleteTimetableSlot(slot: TimetableSlotEntity) {
        database.timetableDao().deleteSlot(slot)
    }

    // Exam operations
    suspend fun insertExam(
        title: String,
        subjectId: Long,
        examDate: Long,
        location: String,
        syllabusCoveragePercent: Int,
        targetGrade: String,
        notes: String
    ): Long {
        return database.examDao().insertExam(
            ExamEntity(
                title = title.trim(),
                subjectId = subjectId,
                examDate = examDate,
                location = location.trim(),
                syllabusCoveragePercent = syllabusCoveragePercent,
                targetGrade = targetGrade.trim(),
                notes = notes.trim()
            )
        )
    }

    suspend fun updateExam(exam: ExamEntity) {
        database.examDao().updateExam(exam)
    }

    suspend fun deleteExam(exam: ExamEntity) {
        database.examDao().deleteExam(exam)
    }

    // Flashcard operations
    suspend fun insertFlashcard(subjectId: Long, front: String, back: String): Long {
        return database.flashcardDao().insertFlashcard(
            FlashcardEntity(subjectId = subjectId, frontText = front.trim(), backText = back.trim())
        )
    }

    suspend fun toggleFlashcardMastered(cardId: Long, isMastered: Boolean) {
        database.flashcardDao().setCardMastered(cardId, isMastered)
    }

    suspend fun deleteFlashcard(flashcard: FlashcardEntity) {
        database.flashcardDao().deleteFlashcard(flashcard)
    }

    // Complete Study Data Export to JSON (Domain/Cloud backup)
    suspend fun exportAllDataAsJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val domainList = domains.first()
        val subjectList = subjects.first()
        val taskList = tasks.first()
        val sessionList = sessions.first()
        val slotList = timetableSlots.first()
        val examList = exams.first()
        val flashcardList = flashcards.first()

        val domainsArr = JSONArray()
        domainList.forEach { d ->
            val obj = JSONObject()
            obj.put("name", d.name)
            obj.put("description", d.description)
            obj.put("colorHex", d.colorHex)
            obj.put("iconName", d.iconName)
            obj.put("targetWeeklyHours", d.targetWeeklyHours)
            domainsArr.put(obj)
        }
        root.put("domains", domainsArr)

        val subjectsArr = JSONArray()
        subjectList.forEach { s ->
            val obj = JSONObject()
            obj.put("name", s.name)
            obj.put("colorHex", s.colorHex)
            obj.put("targetWeeklyMinutes", s.targetWeeklyMinutes)
            subjectsArr.put(obj)
        }
        root.put("subjects", subjectsArr)

        val tasksArr = JSONArray()
        taskList.forEach { t ->
            val obj = JSONObject()
            obj.put("title", t.title)
            obj.put("notes", t.notes)
            obj.put("dueDate", t.dueDate ?: 0L)
            obj.put("priority", t.priority)
            obj.put("estimatedPomodoros", t.estimatedPomodoros)
            obj.put("completedPomodoros", t.completedPomodoros)
            obj.put("isCompleted", t.isCompleted)
            tasksArr.put(obj)
        }
        root.put("tasks", tasksArr)

        root.put("totalSessionsRecorded", sessionList.size)
        root.put("totalExamsTracked", examList.size)
        root.put("totalFlashcardsCount", flashcardList.size)

        return root.toString(2)
    }

    fun getAnalytics(): Flow<StudyAnalyticsSummary> {
        return combine(domains, subjects, tasks, sessions) { domainList, subjectList, taskList, sessionList ->
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val startOfWeek = calendar.timeInMillis

            val workSessions = sessionList.filter { it.sessionType == "WORK" }
            val totalMinutesAllTime = workSessions.sumOf { it.durationMinutes }
            val totalMinutesThisWeek = workSessions
                .filter { it.timestamp >= startOfWeek }
                .sumOf { it.durationMinutes }

            val completedTasks = taskList.count { it.isCompleted }
            val totalTasks = taskList.size
            val completionRate = if (totalTasks > 0) (completedTasks * 100) / totalTasks else 0

            val streakDays = calculateDailyStreak(workSessions)

            // Subject breakdown
            val subjectStats = subjectList.map { subject ->
                val minutes = workSessions
                    .filter { it.subjectId == subject.id }
                    .sumOf { it.durationMinutes }
                SubjectStudyStat(
                    subject = subject,
                    totalMinutesStudied = minutes,
                    targetMinutes = subject.targetWeeklyMinutes
                )
            }.sortedByDescending { it.totalMinutesStudied }

            // Domain breakdown
            val domainStats = domainList.map { domain ->
                val domainSubjects = subjectList.filter { it.domainId == domain.id }
                val domainSubjectIds = domainSubjects.map { it.id }.toSet()
                val minutesInDomain = workSessions
                    .filter { it.subjectId != null && domainSubjectIds.contains(it.subjectId) }
                    .sumOf { it.durationMinutes }
                DomainStudyStat(
                    domain = domain,
                    totalMinutesStudied = minutesInDomain,
                    targetWeeklyHours = domain.targetWeeklyHours,
                    subjectCount = domainSubjects.size
                )
            }.sortedByDescending { it.totalMinutesStudied }

            StudyAnalyticsSummary(
                totalMinutesAllTime = totalMinutesAllTime,
                totalMinutesThisWeek = totalMinutesThisWeek,
                currentStreakDays = streakDays,
                completedTasksCount = completedTasks,
                totalTasksCount = totalTasks,
                taskCompletionRatePercent = completionRate,
                subjectStats = subjectStats,
                domainStats = domainStats
            )
        }
    }

    private fun calculateDailyStreak(workSessions: List<FocusSessionEntity>): Int {
        if (workSessions.isEmpty()) return 0

        val sessionDays = workSessions.map { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.toSet()

        var streak = 0
        val checkCal = Calendar.getInstance()

        val todayKey = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"
        val hasStudiedToday = sessionDays.contains(todayKey)

        if (hasStudiedToday) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayKey = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"
            if (!sessionDays.contains(yesterdayKey)) {
                return 0
            }
        }

        while (true) {
            val key = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"
            if (sessionDays.contains(key)) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }
}

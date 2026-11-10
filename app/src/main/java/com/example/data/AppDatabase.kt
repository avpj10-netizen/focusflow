package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.DomainDao
import com.example.data.dao.ExamDao
import com.example.data.dao.FlashcardDao
import com.example.data.dao.FocusSessionDao
import com.example.data.dao.SubjectDao
import com.example.data.dao.TaskDao
import com.example.data.dao.TimetableDao
import com.example.data.model.ExamEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.StudyDomainEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TimetableSlotEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        StudyDomainEntity::class,
        SubjectEntity::class,
        TaskEntity::class,
        FocusSessionEntity::class,
        TimetableSlotEntity::class,
        ExamEntity::class,
        FlashcardEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun domainDao(): DomainDao
    abstract fun subjectDao(): SubjectDao
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun timetableDao(): TimetableDao
    abstract fun examDao(): ExamDao
    abstract fun flashcardDao(): FlashcardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chronostudy_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabasePrepopulateCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabasePrepopulateCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                // Seed Academic Domains
                val stemDomainId = database.domainDao().insertDomain(
                    StudyDomainEntity(
                        name = "STEM & Engineering",
                        description = "Mathematics, Computer Science, Algorithms & Logic",
                        colorHex = "#6366F1",
                        iconName = "code",
                        targetWeeklyHours = 18
                    )
                )
                val scienceDomainId = database.domainDao().insertDomain(
                    StudyDomainEntity(
                        name = "Natural Sciences & Pre-Med",
                        description = "Physics, Chemistry, Cell Biology & Anatomy",
                        colorHex = "#10B981",
                        iconName = "science",
                        targetWeeklyHours = 14
                    )
                )
                val humanitiesDomainId = database.domainDao().insertDomain(
                    StudyDomainEntity(
                        name = "Humanities & Social Sciences",
                        description = "Literature, History, Philosophy & Critical Theory",
                        colorHex = "#EC4899",
                        iconName = "book",
                        targetWeeklyHours = 10
                    )
                )

                // Seed Subjects linked to Domains
                val mathId = database.subjectDao().insertSubject(
                    SubjectEntity(name = "Mathematics", colorHex = "#6366F1", targetWeeklyMinutes = 360, domainId = stemDomainId)
                )
                val csId = database.subjectDao().insertSubject(
                    SubjectEntity(name = "Computer Science", colorHex = "#0EA5E9", targetWeeklyMinutes = 300, domainId = stemDomainId)
                )
                val physicsId = database.subjectDao().insertSubject(
                    SubjectEntity(name = "Physics", colorHex = "#F59E0B", targetWeeklyMinutes = 240, domainId = scienceDomainId)
                )
                val bioId = database.subjectDao().insertSubject(
                    SubjectEntity(name = "Biology", colorHex = "#10B981", targetWeeklyMinutes = 200, domainId = scienceDomainId)
                )
                val litId = database.subjectDao().insertSubject(
                    SubjectEntity(name = "Literature", colorHex = "#EC4899", targetWeeklyMinutes = 180, domainId = humanitiesDomainId)
                )

                val now = System.currentTimeMillis()
                val oneDay = 24 * 60 * 60 * 1000L

                // Seed Tasks
                database.taskDao().insertTask(
                    TaskEntity(
                        title = "Linear Algebra Problem Set 4",
                        notes = "Eigenvalues, trace and matrix diagonalization",
                        subjectId = mathId,
                        dueDate = now + oneDay,
                        hasReminder = true,
                        priority = "HIGH",
                        estimatedPomodoros = 4,
                        completedPomodoros = 1,
                        isCompleted = false
                    )
                )
                database.taskDao().insertTask(
                    TaskEntity(
                        title = "Implement AVL Tree Self-Balancing",
                        notes = "Write unit tests and optimize left/right rotation",
                        subjectId = csId,
                        dueDate = now + (2 * oneDay),
                        hasReminder = true,
                        priority = "HIGH",
                        estimatedPomodoros = 3,
                        completedPomodoros = 0,
                        isCompleted = false
                    )
                )
                database.taskDao().insertTask(
                    TaskEntity(
                        title = "Read Hamlet Act III Analysis",
                        notes = "Prepare notes for group seminar discussion",
                        subjectId = litId,
                        dueDate = now + (3 * oneDay),
                        hasReminder = false,
                        priority = "MEDIUM",
                        estimatedPomodoros = 2,
                        completedPomodoros = 0,
                        isCompleted = false
                    )
                )

                // Seed Timetable Slots
                database.timetableDao().insertSlot(
                    TimetableSlotEntity(subjectId = mathId, dayOfWeek = 1, startMinuteOfDay = 540, endMinuteOfDay = 630, roomOrNote = "Hall 302")
                )
                database.timetableDao().insertSlot(
                    TimetableSlotEntity(subjectId = csId, dayOfWeek = 1, startMinuteOfDay = 660, endMinuteOfDay = 750, roomOrNote = "Lab B4")
                )
                database.timetableDao().insertSlot(
                    TimetableSlotEntity(subjectId = physicsId, dayOfWeek = 2, startMinuteOfDay = 600, endMinuteOfDay = 690, roomOrNote = "Sci Block 1")
                )
                database.timetableDao().insertSlot(
                    TimetableSlotEntity(subjectId = mathId, dayOfWeek = 3, startMinuteOfDay = 540, endMinuteOfDay = 630, roomOrNote = "Hall 302")
                )
                database.timetableDao().insertSlot(
                    TimetableSlotEntity(subjectId = bioId, dayOfWeek = 3, startMinuteOfDay = 840, endMinuteOfDay = 930, roomOrNote = "Bio Lab 2")
                )

                // Seed Focus Sessions
                database.focusSessionDao().insertSession(
                    FocusSessionEntity(subjectId = mathId, durationMinutes = 25, sessionType = "WORK", timestamp = now - (oneDay + 3600000))
                )
                database.focusSessionDao().insertSession(
                    FocusSessionEntity(subjectId = csId, durationMinutes = 25, sessionType = "WORK", timestamp = now - 7200000)
                )

                // Seed Exams
                database.examDao().insertExam(
                    ExamEntity(
                        title = "Calculus III Midterm Exam",
                        subjectId = mathId,
                        examDate = now + (5 * oneDay),
                        location = "Auditorium A",
                        syllabusCoveragePercent = 75,
                        targetGrade = "A",
                        notes = "Covers partial derivatives, Lagrange multipliers, double integrals"
                    )
                )
                database.examDao().insertExam(
                    ExamEntity(
                        title = "Algorithms & Data Structures Final",
                        subjectId = csId,
                        examDate = now + (14 * oneDay),
                        location = "Turing Hall 101",
                        syllabusCoveragePercent = 60,
                        targetGrade = "A+",
                        notes = "Graph traversal, Dijkstra, Dynamic Programming & NP completeness"
                    )
                )

                // Seed Flashcards
                database.flashcardDao().insertFlashcard(
                    FlashcardEntity(
                        subjectId = mathId,
                        frontText = "What defines an Eigenvector & Eigenvalue?",
                        backText = "A non-zero vector v such that A·v = λ·v, where λ is the scalar eigenvalue."
                    )
                )
                database.flashcardDao().insertFlashcard(
                    FlashcardEntity(
                        subjectId = csId,
                        frontText = "Time Complexity of QuickSort (Average vs Worst)?",
                        backText = "Average: O(n log n). Worst Case (already sorted without randomized pivot): O(n²)."
                    )
                )
                database.flashcardDao().insertFlashcard(
                    FlashcardEntity(
                        subjectId = bioId,
                        frontText = "What is the primary function of ATP Synthase?",
                        backText = "Synthesizes ATP from ADP and inorganic phosphate using the electrochemical proton gradient across the inner mitochondrial membrane."
                    )
                )
            }
        }
    }
}

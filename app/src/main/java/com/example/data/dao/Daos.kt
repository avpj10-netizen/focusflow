package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExamEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.StudyDomainEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TimetableSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DomainDao {
    @Query("SELECT * FROM domains ORDER BY name ASC")
    fun getAllDomains(): Flow<List<StudyDomainEntity>>

    @Query("SELECT * FROM domains WHERE id = :id LIMIT 1")
    suspend fun getDomainById(id: Long): StudyDomainEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomain(domain: StudyDomainEntity): Long

    @Update
    suspend fun updateDomain(domain: StudyDomainEntity)

    @Delete
    suspend fun deleteDomain(domain: StudyDomainEntity)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE domainId = :domainId ORDER BY name ASC")
    fun getSubjectsByDomain(domainId: Long): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDate ASC, priority DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC, priority DESC LIMIT :limit")
    fun getTopUpcomingTasks(limit: Int = 5): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC, priority DESC")
    suspend fun getUpcomingTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET completedPomodoros = completedPomodoros + 1 WHERE id = :taskId")
    suspend fun incrementCompletedPomodoro(taskId: Long)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getSessionsSince(sinceTimestamp: Long): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE sessionType = 'WORK'")
    fun getTotalWorkMinutes(): Flow<Int?>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE sessionType = 'WORK' AND timestamp >= :sinceTimestamp")
    fun getTotalWorkMinutesSince(sinceTimestamp: Long): Flow<Int?>
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startMinuteOfDay ASC")
    fun getAllSlots(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startMinuteOfDay ASC")
    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlotEntity): Long

    @Delete
    suspend fun deleteSlot(slot: TimetableSlotEntity)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY id DESC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE subjectId = :subjectId ORDER BY isMastered ASC, id DESC")
    fun getFlashcardsForSubject(subjectId: Long): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Query("UPDATE flashcards SET isMastered = :isMastered, reviewCount = reviewCount + 1 WHERE id = :cardId")
    suspend fun setCardMastered(cardId: Long, isMastered: Boolean)

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)
}

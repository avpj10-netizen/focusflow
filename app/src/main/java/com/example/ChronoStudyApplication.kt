package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.StudyRepository
import com.example.timer.PomodoroEngine

class ChronoStudyApplication : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: StudyRepository
        private set
    lateinit var pomodoroEngine: PomodoroEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        repository = StudyRepository(database)
        pomodoroEngine = PomodoroEngine(this, repository)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val pomodoroChannel = NotificationChannel(
                CHANNEL_POMODORO,
                "Pomodoro Focus Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts and sound when focus or break sessions complete"
                enableVibration(true)
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Study Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled reminders for upcoming study assignments and exams"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(pomodoroChannel)
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }

    companion object {
        const val CHANNEL_POMODORO = "chronostudy_pomodoro_channel"
        const val CHANNEL_REMINDERS = "chronostudy_reminders_channel"

        lateinit var instance: ChronoStudyApplication
            private set
    }
}

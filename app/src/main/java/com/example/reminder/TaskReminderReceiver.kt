package com.example.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ChronoStudyApplication
import com.example.MainActivity
import com.example.R

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Study Task"
        val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Assignment"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", "tasks")
            putExtra("EXTRA_TASK_ID", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, ChronoStudyApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.chronostudy_launcher_icon_1794023474724)
            .setContentTitle("Task Due: $taskTitle")
            .setContentText("Subject: $subjectName. Time to tackle this goal! 🎯")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(taskId.toInt() + 3000, notification)
        } catch (_: SecurityException) {
            // Permission check
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
    }
}

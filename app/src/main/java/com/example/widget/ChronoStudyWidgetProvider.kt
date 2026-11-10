package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.ChronoStudyApplication
import com.example.MainActivity
import com.example.R
import com.example.timer.PomodoroMode
import com.example.timer.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChronoStudyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val engine = try {
            ChronoStudyApplication.instance.pomodoroEngine
        } catch (_: Exception) {
            null
        }

        when (intent.action) {
            ACTION_TOGGLE_TIMER -> {
                engine?.toggle()
                updateAllWidgets(context)
            }
            ACTION_RESET_TIMER -> {
                engine?.reset()
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_TIMER = "com.example.ACTION_TOGGLE_TIMER"
        const val ACTION_RESET_TIMER = "com.example.ACTION_RESET_TIMER"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, ChronoStudyWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_chronostudy)

            val engine = try {
                ChronoStudyApplication.instance.pomodoroEngine
            } catch (_: Exception) {
                null
            }

            val state = engine?.state?.value

            val remainingSeconds = state?.remainingSeconds ?: (25 * 60)
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            val timerFormatted = String.format("%02d:%02d", minutes, seconds)

            val mode = state?.mode ?: PomodoroMode.WORK
            val modeLabel = mode.label.uppercase()
            val subjectName = state?.activeSubjectName ?: "General Focus"
            val isRunning = state?.status == TimerStatus.RUNNING

            views.setTextViewText(R.id.widget_timer_text, timerFormatted)
            views.setTextViewText(R.id.widget_mode_badge, modeLabel)
            views.setTextViewText(R.id.widget_subject_text, subjectName)
            views.setTextViewText(R.id.widget_btn_toggle, if (isRunning) "Pause" else "Start")

            // Intent to open MainActivity on widget tap
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Intent to toggle play/pause
            val toggleIntent = Intent(context, ChronoStudyWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TIMER
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_toggle, togglePendingIntent)

            // Fetch upcoming task asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = ChronoStudyApplication.instance.repository
                    val upcoming = repository.getUpcomingTasksSync()
                    val taskText = if (upcoming.isNotEmpty()) {
                        "Next: ${upcoming.first().title}"
                    } else {
                        "All tasks completed! 🌟"
                    }
                    views.setTextViewText(R.id.widget_next_task_text, taskText)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (_: Exception) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

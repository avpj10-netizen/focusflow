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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class ChronoStudyScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateScheduleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, ChronoStudyScheduleWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateScheduleWidget(context, appWidgetManager, widgetId)
            }
        }

        private fun updateScheduleWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_chronostudy_schedule)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                202,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_schedule_root, openAppPendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = ChronoStudyApplication.instance.repository
                    val upcomingTasks = repository.getUpcomingTasksSync()

                    if (upcomingTasks.isNotEmpty()) {
                        views.setTextViewText(R.id.widget_task_item_1, "• ${upcomingTasks[0].title}")
                        if (upcomingTasks.size > 1) {
                            views.setTextViewText(R.id.widget_task_item_2, "• ${upcomingTasks[1].title}")
                        } else {
                            views.setTextViewText(R.id.widget_task_item_2, "• No other pending tasks")
                        }
                    } else {
                        views.setTextViewText(R.id.widget_task_item_1, "• All tasks finished! 🎉")
                        views.setTextViewText(R.id.widget_task_item_2, "• Enjoy your study break")
                    }

                    // Get today's slots
                    val currentDayIndex = run {
                        val cal = Calendar.getInstance()
                        val dow = cal.get(Calendar.DAY_OF_WEEK)
                        if (dow == Calendar.SUNDAY) 7 else dow - 1
                    }
                    val allSlots = repository.timetableSlots.first()
                    val todaySlots = allSlots.filter { it.dayOfWeek == currentDayIndex }.sortedBy { it.startMinuteOfDay }
                    val subjects = repository.subjects.first()

                    if (todaySlots.isNotEmpty()) {
                        val first = todaySlots.first()
                        val subj = subjects.find { it.id == first.subjectId }
                        val startH = first.startMinuteOfDay / 60
                        val startM = first.startMinuteOfDay % 60
                        val endH = first.endMinuteOfDay / 60
                        val endM = first.endMinuteOfDay % 60
                        val timeStr = String.format("%d:%02d - %d:%02d", startH, startM, endH, endM)
                        views.setTextViewText(R.id.widget_next_slot_time, timeStr)
                        views.setTextViewText(R.id.widget_next_slot_title, "${subj?.name ?: "Class"} (${first.roomOrNote})")
                    } else {
                        views.setTextViewText(R.id.widget_next_slot_time, "No classes today")
                        views.setTextViewText(R.id.widget_next_slot_title, "Self-study session")
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (_: Exception) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

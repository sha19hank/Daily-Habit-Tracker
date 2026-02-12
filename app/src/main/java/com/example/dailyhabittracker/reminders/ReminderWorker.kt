package com.example.dailyhabittracker.reminders

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dailyhabittracker.R
import com.example.dailyhabittracker.data.HabitDatabase
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = HabitDatabase.build(applicationContext)
        val dao = db.habitDao()
        val today = LocalDate.now()
        val now = LocalDateTime.now()

        val habits = dao.getHabitsOnce()
        habits.filter { it.reminderEnabled && it.reminderTime != null && !it.paused }
            .filter { habit ->
                val scheduled = habit.scheduledDays
                scheduled.isEmpty() || scheduled.contains(today.dayOfWeek.value)
            }
            .forEach { habit ->
                val reminderTime = habit.reminderTime ?: return@forEach
                val nowTime = now.toLocalTime()
                val windowStart = nowTime.minusMinutes(5)
                val due = !reminderTime.isAfter(nowTime) && !reminderTime.isBefore(windowStart)
                if (!due) return@forEach

                val notification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(habit.name)
                    .setContentText(applicationContext.getString(R.string.reminder_body))
                    .setAutoCancel(true)
                    .build()

                NotificationManagerCompat.from(applicationContext)
                    .notify(habit.id.toInt(), notification)
            }

        ReminderWorkScheduler(applicationContext, dao).scheduleNext()
        return Result.success()
    }

    companion object {
        const val REMINDER_CHANNEL = "habit_reminders"
    }
}

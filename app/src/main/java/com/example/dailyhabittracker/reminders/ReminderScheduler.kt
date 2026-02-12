package com.example.dailyhabittracker.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.dailyhabittracker.data.HabitDao
import com.example.dailyhabittracker.data.HabitEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderScheduler(
    private val context: Context,
    private val habitDao: HabitDao
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleAll() {
        val habits = habitDao.getHabitsOnce()
        habits.forEach { scheduleHabit(it) }
    }

    fun scheduleHabit(habit: HabitEntity) {
        if (!habit.reminderEnabled || habit.reminderTime == null || habit.paused) {
            cancelHabit(habit.id)
            return
        }

        val nextTime = nextOccurrence(habit, LocalDate.now(), habit.reminderTime)
        val triggerAtMillis = nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habit.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    fun cancelHabit(habitId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun nextOccurrence(habit: HabitEntity, today: LocalDate, time: LocalTime): LocalDateTime {
        val scheduled = habit.scheduledDays
        var date = today
        var candidate = LocalDateTime.of(date, time)
        if (candidate.isBefore(LocalDateTime.now())) {
            date = date.plusDays(1)
            candidate = LocalDateTime.of(date, time)
        }

        if (scheduled.isEmpty()) return candidate

        repeat(7) {
            val dayValue = candidate.toLocalDate().dayOfWeek.value
            if (scheduled.contains(dayValue)) return candidate
            candidate = candidate.plusDays(1)
        }
        return candidate
    }
}

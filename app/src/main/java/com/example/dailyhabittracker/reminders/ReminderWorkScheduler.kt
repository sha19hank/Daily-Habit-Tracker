package com.example.dailyhabittracker.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dailyhabittracker.data.HabitDao
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderWorkScheduler(
    private val context: Context,
    private val habitDao: HabitDao
) {
    suspend fun scheduleNext() {
        val next = nextReminderTime()
        if (next == null) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
            return
        }
        val delayMillis = Duration.between(LocalDateTime.now(), next).toMillis().coerceAtLeast(0)

        val workRequest: androidx.work.OneTimeWorkRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    suspend fun nextReminderTime(): LocalDateTime? {
        val habits = habitDao.getHabitsOnce()
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        val candidates = habits
            .filter { it.reminderEnabled && it.reminderTime != null && !it.paused }
            .mapNotNull { habit ->
                val time = habit.reminderTime ?: return@mapNotNull null
                val next = nextOccurrence(habit.scheduledDays, today, time)
                if (next.isBefore(now)) nextOccurrence(habit.scheduledDays, today.plusDays(1), time) else next
            }

        return candidates.minOrNull()
    }

    private fun nextOccurrence(days: List<Int>, date: LocalDate, time: LocalTime): LocalDateTime {
        if (days.isEmpty()) return LocalDateTime.of(date, time)
        var candidate = LocalDateTime.of(date, time)
        repeat(7) {
            if (days.contains(candidate.toLocalDate().dayOfWeek.value)) return candidate
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    companion object {
        const val UNIQUE_WORK = "habit_reminder_work"
        const val WORK_TAG = "habit_reminder"
    }
}

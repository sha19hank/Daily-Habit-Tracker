package com.example.dailyhabittracker.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class HabitRepository(
    private val database: HabitDatabase,
    private val dao: HabitDao,
    private val settings: SettingsRepository
) {
    fun getHabits(): Flow<List<HabitEntity>> = dao.getHabits()

    suspend fun addHabit(
        name: String,
        description: String?,
        category: String?,
        color: Int,
        scheduledDays: List<Int>,
        reminderEnabled: Boolean,
        reminderTime: LocalTime?,
        paused: Boolean,
        stepEnabled: Boolean,
        stepGoal: Int?
    ) {
        val habit = HabitEntity(
            name = name.trim(),
            description = description?.trim(),
            category = category?.trim()?.ifBlank { null },
            color = color,
            scheduledDays = scheduledDays,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime,
            paused = paused,
            stepEnabled = stepEnabled,
            stepGoal = stepGoal
        )
        dao.insertHabit(habit)
    }

    suspend fun markCompleted(habit: HabitEntity, today: LocalDate) {
        if (habit.paused) return
        if (!isScheduledForDay(habit, today)) return
        if (habit.lastCompletedDate == today) return
        if (habit.lastCompletedDate != null && habit.lastCompletedDate.isAfter(today)) return

        val completedToday = dao.hasCompletionForDate(habit.id, today.toString()) > 0
        if (completedToday) return

        val lastCompleted = habit.lastCompletedDate
        val previousScheduled = previousScheduledDate(habit, today)
        val newStreak = if (lastCompleted != null && previousScheduled != null && lastCompleted == previousScheduled) {
            habit.currentStreak + 1
        } else {
            1
        }
        val longest = maxOf(habit.longestStreak, newStreak)

        database.withTransaction {
            dao.updateHabit(
                habit.copy(
                    lastCompletedDate = today,
                    currentStreak = newStreak,
                    longestStreak = longest
                )
            )
            dao.insertCompletion(HabitCompletionEntity(habitId = habit.id, completionDate = today))
        }
        settings.clearPreviousStreak(habit.id)
    }

    suspend fun refreshBrokenStreaks(today: LocalDate) {
        val habits = dao.getHabits().first()
        habits.forEach { habit ->
            val lastCompleted = habit.lastCompletedDate
            if (habit.paused) return@forEach
            if (lastCompleted != null && habit.currentStreak > 0 && missedScheduledDay(habit, lastCompleted, today)) {
                settings.setPreviousStreak(habit.id, habit.currentStreak)
                dao.updateHabit(habit.copy(currentStreak = 0))
            }
        }
    }

    suspend fun restoreStreakIfAllowed(habit: HabitEntity, today: LocalDate): Boolean {
        val previousScheduled = previousScheduledDate(habit, today)
        if (habit.lastCompletedDate == today) return false
        if (habit.currentStreak > 0) return false
        if (habit.lastCompletedDate == null || previousScheduled == null) return false
        if (!habit.lastCompletedDate.isBefore(previousScheduled)) return false

        val previousStreak = settings.getPreviousStreak(habit.id) ?: return false
        val restored = habit.copy(
            currentStreak = previousStreak,
            lastCompletedDate = previousScheduled
        )

        val token = dao.getTokenOnce() ?: TokenEntity()
        if (token.count <= 0) return false

        database.withTransaction {
            dao.updateHabit(restored)
            dao.upsertToken(token.copy(count = token.count - 1))
        }
        settings.clearPreviousStreak(habit.id)
        return true
    }

    suspend fun updateHabit(habit: HabitEntity) {
        dao.updateHabit(habit)
    }

    suspend fun pauseHabit(habit: HabitEntity, paused: Boolean) {
        if (paused) {
            settings.setPausedStreak(habit.id, habit.currentStreak)
            dao.updateHabit(habit.copy(paused = true))
        } else {
            val previous = settings.getPausedStreak(habit.id) ?: habit.currentStreak
            dao.updateHabit(habit.copy(paused = false, currentStreak = previous))
            settings.clearPausedStreak(habit.id)
        }
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        dao.deleteHabit(habit)
    }

    suspend fun getHabitsOnce(): List<HabitEntity> = dao.getHabitsOnce()

    suspend fun weeklySummary(start: LocalDate, end: LocalDate): List<DateCount> {
        return dao.getCompletionCounts(start.toString(), end.toString())
    }

    suspend fun monthlyCompletionCount(start: LocalDate, end: LocalDate): Int {
        return dao.getCompletionCountInRange(start.toString(), end.toString())
    }

    suspend fun completionCountForHabit(start: LocalDate, end: LocalDate, habitId: Long): Int {
        return dao.getCompletionCountForHabitInRange(habitId, start.toString(), end.toString())
    }

    fun tokenFlow(): Flow<TokenEntity?> = dao.getTokenFlow()

    suspend fun syncWeeklyTokens(today: LocalDate, maxTokens: Int = 2) {
        val token = dao.getTokenOnce() ?: TokenEntity()
        val lastEarned = token.lastEarnedDate?.let { LocalDate.parse(it) }
        val weeks = if (lastEarned == null) 1 else (ChronoUnit.DAYS.between(lastEarned, today) / 7).toInt()
        if (weeks <= 0) return

        val newCount = (token.count + weeks).coerceAtMost(maxTokens)
        dao.upsertToken(token.copy(count = newCount, lastEarnedDate = today.toString()))
    }

    suspend fun addToken(maxTokens: Int = 2) {
        val token = dao.getTokenOnce() ?: TokenEntity()
        val newCount = (token.count + 1).coerceAtMost(maxTokens)
        dao.upsertToken(token.copy(count = newCount))
    }

    fun isScheduledForDay(habit: HabitEntity, date: LocalDate): Boolean {
        val scheduled = habit.scheduledDays
        if (scheduled.isEmpty()) return true
        return scheduled.contains(date.dayOfWeek.value)
    }

    private fun previousScheduledDate(habit: HabitEntity, today: LocalDate): LocalDate? {
        val scheduled = habit.scheduledDays
        if (scheduled.isEmpty()) return today.minusDays(1)
        var date = today.minusDays(1)
        repeat(7) {
            if (scheduled.contains(date.dayOfWeek.value)) return date
            date = date.minusDays(1)
        }
        return null
    }

    private fun missedScheduledDay(habit: HabitEntity, lastCompleted: LocalDate, today: LocalDate): Boolean {
        val scheduled = habit.scheduledDays
        val yesterday = today.minusDays(1)
        if (lastCompleted.isAfter(yesterday) || lastCompleted == yesterday) return false
        if (scheduled.isEmpty()) return true

        val daysBetween = ChronoUnit.DAYS.between(lastCompleted, yesterday).toInt()
        for (i in 1..daysBetween) {
            val date = lastCompleted.plusDays(i.toLong())
            if (scheduled.contains(date.dayOfWeek.value)) return true
        }
        return false
    }
}

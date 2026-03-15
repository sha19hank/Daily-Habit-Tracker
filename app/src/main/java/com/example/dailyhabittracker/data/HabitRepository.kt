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
    private val goalDao: GoalDao,
    private val journalDao: JournalDao,
    private val settings: SettingsRepository
) {
    fun getHabits(): Flow<List<HabitEntity>> = dao.getHabits()

    fun getGoals(): Flow<List<GoalEntity>> = goalDao.getGoals()

    suspend fun getGoalsOnce(): List<GoalEntity> = goalDao.getGoalsOnce()

    suspend fun addGoal(
        title: String,
        description: String?,
        startDate: LocalDate,
        deadline: LocalDate?
    ) {
        val goal = GoalEntity(
            title = title.trim(),
            description = description?.trim()?.ifBlank { null },
            startDate = startDate,
            deadline = deadline
        )
        goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoal(goal)
    }

    fun getJournalEntries(): Flow<List<JournalEntryEntity>> = journalDao.getEntries()

    suspend fun getJournalEntryByDate(date: LocalDate): JournalEntryEntity? {
        return journalDao.getEntryByDate(date.toString())
    }

    suspend fun upsertJournalEntry(
        title: String,
        body: String,
        date: LocalDate,
        mood: String?
    ) {
        val entry = JournalEntryEntity(
            title = title.trim(),
            body = body.trim(),
            date = date,
            mood = mood?.trim()?.ifBlank { null }
        )
        journalDao.upsertEntry(entry)
    }

    suspend fun updateJournalEntry(entry: JournalEntryEntity) {
        journalDao.updateEntry(entry)
    }

    suspend fun deleteJournalEntry(entry: JournalEntryEntity) {
        journalDao.deleteEntry(entry)
    }

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
        stepGoal: Int?,
        goalId: Long?
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
            stepGoal = stepGoal,
            goalId = goalId
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
        val tokenAward = milestoneTokenAward(newStreak)
        val token = if (tokenAward > 0) dao.getTokenOnce() ?: TokenEntity() else null

        database.withTransaction {
            dao.updateHabit(
                habit.copy(
                    lastCompletedDate = today,
                    currentStreak = newStreak,
                    longestStreak = longest
                )
            )
            dao.insertCompletion(HabitCompletionEntity(habitId = habit.id, completionDate = today))
            if (tokenAward > 0 && token != null) {
                dao.upsertToken(token.copy(count = token.count + tokenAward))
            }
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

    suspend fun completedHabitIdsForDate(date: LocalDate): List<Long> {
        return dao.getCompletedHabitIdsForDate(date.toString())
    }

    fun tokenFlow(): Flow<TokenEntity?> = dao.getTokenFlow()

    private fun milestoneTokenAward(streak: Int): Int {
        return when (streak) {
            7 -> 1
            30 -> 3
            100 -> 10
            else -> 0
        }
    }

    fun isScheduledForDay(habit: HabitEntity, date: LocalDate): Boolean {
        val scheduled = habit.scheduledDays
        if (scheduled.isEmpty()) return true
        return scheduled.contains(date.dayOfWeek.value)
    }

    suspend fun goalProgressPercent(goal: GoalEntity, today: LocalDate): Int {
        val habits = dao.getHabitsByGoalId(goal.goalId)
        if (habits.isEmpty()) return 0

        val startDate = goal.startDate
        val totalPercent = habits.sumOf { habit ->
            val totalOccurrences = scheduledOccurrencesInRange(habit, startDate, today)
            if (totalOccurrences == 0) return@sumOf 0.0
            val completed = dao.getCompletionCountForHabitInRange(
                habit.id,
                startDate.toString(),
                today.toString()
            )
            completed.toDouble() / totalOccurrences.toDouble()
        }

        val average = (totalPercent / habits.size.toDouble()).coerceIn(0.0, 1.0)
        return (average * 100).toInt()
    }

    private fun scheduledOccurrencesInRange(
        habit: HabitEntity,
        start: LocalDate,
        end: LocalDate
    ): Int {
        if (end.isBefore(start)) return 0
        val scheduled = habit.scheduledDays
        val daysBetween = ChronoUnit.DAYS.between(start, end).toInt()
        if (scheduled.isEmpty()) return daysBetween + 1

        var count = 0
        var date = start
        repeat(daysBetween + 1) {
            if (scheduled.contains(date.dayOfWeek.value)) count += 1
            date = date.plusDays(1)
        }
        return count
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

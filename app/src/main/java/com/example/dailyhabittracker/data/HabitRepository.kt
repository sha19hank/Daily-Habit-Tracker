package com.example.dailyhabittracker.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    fun getAllCompletionsFlow(): Flow<List<HabitCompletionEntity>> = dao.getAllCompletionsFlow()

    suspend fun getGoalsOnce(): List<GoalEntity> = goalDao.getGoalsOnce()

    suspend fun addGoal(
        title: String,
        description: String?,
        startDate: LocalDate,
        deadline: LocalDate?
    ): Long {
        val goal = GoalEntity(
            title = title.trim(),
            description = description?.trim()?.ifBlank { null },
            startDate = startDate,
            deadline = deadline
        )
        return goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        database.withTransaction {
            dao.clearGoalIdForHabits(goal.goalId)
            goalDao.deleteGoal(goal)
        }
    }

    fun getJournalEntries(): Flow<List<JournalEntryEntity>> = journalDao.getEntries()

    suspend fun getJournalEntriesByDate(date: LocalDate): List<JournalEntryEntity> {
        return journalDao.getEntriesByDate(date.toString())
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

    suspend fun markUncompleted(habit: HabitEntity, today: LocalDate) {
        val completedToday = dao.hasCompletionForDate(habit.id, today.toString()) > 0
        if (!completedToday) return

        database.withTransaction {
            dao.deleteCompletionForDate(habit.id, today.toString())
            
            val completions = dao.getCompletionsForHabitDesc(habit.id)
            val previousCompletedDate = completions.firstOrNull { it.completionDate.isBefore(today) }?.completionDate
            
            val newStreak = maxOf(0, habit.currentStreak - 1)
            
            dao.updateHabit(
                habit.copy(
                    lastCompletedDate = previousCompletedDate,
                    currentStreak = newStreak
                )
            )
        }
    }

    suspend fun refreshBrokenStreaks(today: LocalDate) {
        val habits = dao.getHabits().first()
        habits.forEach { habit ->
            val lastCompleted = habit.lastCompletedDate
            if (habit.paused) return@forEach
            if (lastCompleted != null && habit.currentStreak > 0 && missedScheduledDay(habit, lastCompleted, today)) {
                if (settings.isStreakFrozen(habit.id)) {
                    settings.setStreakFrozen(habit.id, false)
                    val missedDay = previousScheduledDate(habit, today) ?: today.minusDays(1)
                    dao.updateHabit(habit.copy(lastCompletedDate = missedDay))
                } else {
                    settings.setPreviousStreak(habit.id, habit.currentStreak)
                    dao.updateHabit(habit.copy(currentStreak = 0))
                }
            }
        }
    }

    suspend fun buyStreakFreeze(habitId: Long): Boolean {
        if (settings.isStreakFrozen(habitId)) return false
        
        val token = dao.getTokenOnce() ?: TokenEntity()
        if (token.count <= 0) return false

        database.withTransaction {
            dao.upsertToken(token.copy(count = token.count - 1))
        }
        settings.setStreakFrozen(habitId, true)
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
        val details = goalProgressDetails(goal, today)
        return details.overallPercent
    }

    suspend fun goalProgressDetails(goal: GoalEntity, today: LocalDate): GoalProgressDetails {
        val habits = dao.getHabitsByGoalId(goal.goalId)
        if (habits.isEmpty()) return GoalProgressDetails(0, emptyList())

        val startDate = goal.startDate
        val habitProgresses = habits.map { habit ->
            val totalOccurrences = scheduledOccurrencesInRange(habit, startDate, goal.deadline ?: today)
            val completed = if (totalOccurrences > 0) {
                dao.getCompletionCountForHabitInRange(
                    habit.id,
                    startDate.toString(),
                    today.toString()
                )
            } else 0
            
            val percent = if (totalOccurrences > 0) {
                (completed.toDouble() / totalOccurrences.toDouble() * 100).toInt().coerceIn(0, 100)
            } else 0
            
            HabitProgress(habit, completed, totalOccurrences, percent)
        }

        val totalPercent = habitProgresses.sumOf { it.percent.toDouble() / 100.0 }
        val average = (totalPercent / habits.size.toDouble()).coerceIn(0.0, 1.0)
        return GoalProgressDetails((average * 100).toInt(), habitProgresses)
    }

    fun getGoalProgressMapFlow(today: LocalDate = LocalDate.now()): Flow<Map<Long, GoalProgressDetails>> {
        return combine(
            goalDao.getGoals(),
            dao.getHabits(),
            dao.getAllCompletionsFlow()
        ) { goals, habits, completions ->
            goals.associate { goal ->
                val goalHabits = habits.filter { it.goalId == goal.goalId }
                goal.goalId to calculateGoalProgress(goal, goalHabits, completions, today)
            }
        }.distinctUntilChanged()
    }

    private fun calculateGoalProgress(
        goal: GoalEntity,
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        today: LocalDate
    ): GoalProgressDetails {
        if (habits.isEmpty()) return GoalProgressDetails(0, emptyList())

        val startDate = goal.startDate
        val endDate = goal.deadline ?: today
        
        val habitProgresses = habits.map { habit ->
            val totalOccurrences = scheduledOccurrencesInRange(habit, startDate, endDate)
            
            val completed = if (totalOccurrences > 0) {
                completions.count {
                    it.habitId == habit.id && 
                    !it.completionDate.isBefore(startDate) &&
                    !it.completionDate.isAfter(today)
                }
            } else 0
            
            val percent = if (totalOccurrences > 0) {
                (completed.toDouble() / totalOccurrences.toDouble() * 100).toInt().coerceIn(0, 100)
            } else 0
            
            HabitProgress(habit, completed, totalOccurrences, percent)
        }

        val totalPercent = habitProgresses.sumOf { it.percent.toDouble() / 100.0 }
        val average = (totalPercent / habits.size.toDouble()).coerceIn(0.0, 1.0)
        return GoalProgressDetails((average * 100).toInt(), habitProgresses)
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

data class HabitProgress(
    val habit: HabitEntity,
    val completed: Int,
    val expected: Int,
    val percent: Int
)

data class GoalProgressDetails(
    val overallPercent: Int,
    val habitProgresses: List<HabitProgress>
)

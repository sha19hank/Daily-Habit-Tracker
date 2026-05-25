package com.mlue.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mlue.app.DailyHabitTrackerApp
import com.mlue.app.data.DateCount
import com.mlue.app.data.GoalEntity
import com.mlue.app.data.GoalProgressDetails
import com.mlue.app.data.HabitEntity
import com.mlue.app.data.HabitRepository
import com.mlue.app.data.JournalEntryEntity
import com.mlue.app.data.SettingsRepository
import com.mlue.app.reminders.GoalDeadlineScheduler
import com.mlue.app.reminders.ReminderScheduler
import com.mlue.app.sensors.StepState
import com.mlue.app.sensors.StepTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as DailyHabitTrackerApp).container
    private val repository: HabitRepository = container.habitRepository
    private val settings: SettingsRepository = container.settings
    private val reminderScheduler: ReminderScheduler = container.reminderScheduler
    private val goalDeadlineScheduler: GoalDeadlineScheduler = container.goalDeadlineScheduler
    private val stepTracker: StepTracker = container.stepTracker

    init {
        Log.d("MlueStartup", "HabitViewModel: init started")
    }

    private val habitsFlow = repository.getHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val goalsFlow = repository.getGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goals: StateFlow<List<GoalEntity>> = goalsFlow

    val journalEntries: StateFlow<List<JournalEntryEntity>> = repository.getJournalEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _sortOption = MutableStateFlow(SortOption.NAME)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    val habits: StateFlow<List<HabitEntity>> = combine(habitsFlow, sortOption) { habits, option ->
        when (option) {
            SortOption.NAME -> habits.sortedBy { it.name.lowercase() }
            SortOption.STREAK -> habits.sortedByDescending { it.currentStreak }
            SortOption.LAST_COMPLETED -> habits.sortedByDescending { it.lastCompletedDate ?: LocalDate.MIN }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklySummary: StateFlow<List<DateCount>> = repository.getAllCompletionsFlow()
        .map { completions ->
            val today = LocalDate.now()
            val startOfWeek = today.minusDays(6)

            val counts = mutableMapOf<String, Int>()
            var date = startOfWeek
            repeat(7) {
                counts[date.toString()] = 0
                date = date.plusDays(1)
            }

            completions.forEach { c ->
                val dateStr = c.completionDate.toString()
                if (counts.containsKey(dateStr)) {
                    counts[dateStr] = counts[dateStr]!! + 1
                }
            }

            counts.map { DateCount(it.key, it.value) }.sortedBy { it.date }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Per-day stats for the last 7 days: completed count + scheduled count.
     * Used by WeeklyBars for true percentage-based rendering.
     * Days with 0 scheduled habits are represented with scheduledCount=0.
     */
    val weeklyStats: StateFlow<List<DailyStats>> = combine(
        repository.getAllCompletionsFlow(),
        habitsFlow
    ) { completions, habits ->
        val today = LocalDate.now()
        (0..6).map { offset ->
            val day = today.minusDays((6 - offset).toLong())
            val completed = completions.count { it.completionDate == day }
            val scheduled = habits.count { habit ->
                !habit.paused &&
                (habit.scheduledDays.isEmpty() || habit.scheduledDays.contains(day.dayOfWeek.value))
            }
            DailyStats(date = day, completed = completed, scheduled = scheduled)
        }
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    val monthlyCount: StateFlow<Int> = repository.getAllCompletionsFlow()
        .map { completions ->
            val today = LocalDate.now()
            val month = YearMonth.from(today)
            val startOfMonth = month.atDay(1)
            val endOfMonth = month.atEndOfMonth()
            
            completions.count { !it.completionDate.isBefore(startOfMonth) && !it.completionDate.isAfter(endOfMonth) }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val insights: StateFlow<List<String>> = combine(habitsFlow, weeklyStats) { habits, stats ->
        buildInsights(habits, stats)
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _habitHistory = MutableStateFlow<Map<Long, HabitHistory>>(emptyMap())
    val habitHistory: StateFlow<Map<Long, HabitHistory>> = _habitHistory.asStateFlow()

    // 30-day Consistency Score (Task 3)
    val consistencyScore: StateFlow<Int> = combine(
        repository.getAllCompletionsFlow(),
        habitsFlow
    ) { completions, habits ->
        val today = LocalDate.now()
        val startDay = today.minusDays(29) // Last 30 days including today
        var totalScheduled = 0
        var totalCompleted = 0

        val days = (0..29).map { startDay.plusDays(it.toLong()) }
        
        for (day in days) {
            val completedToday = completions.count { it.completionDate == day }
            val scheduledToday = habits.count { habit ->
                !habit.paused &&
                (habit.createdDate.isBefore(day) || habit.createdDate.isEqual(day)) &&
                (habit.scheduledDays.isEmpty() || habit.scheduledDays.contains(day.dayOfWeek.value))
            }
            totalCompleted += completedToday
            totalScheduled += scheduledToday
        }

        if (totalScheduled == 0) 0 else ((totalCompleted.toFloat() / totalScheduled) * 100).toInt().coerceIn(0, 100)
    }
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val goalProgress: StateFlow<Map<Long, GoalProgressDetails>> = repository.getGoalProgressMapFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val activeGoal: StateFlow<ActiveGoalState?> = combine(goalsFlow, goalProgress) { goals, progress ->
        val active = selectActiveGoal(goals, LocalDate.now())
        if (active == null) {
            null
        } else {
            ActiveGoalState(active, progress[active.goalId] ?: GoalProgressDetails(0, emptyList()))
        }
    }.stateIn<ActiveGoalState?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _selectedDayCompletedHabitIds = MutableStateFlow<List<Long>>(emptyList())
    val selectedDayCompletedHabitIds: StateFlow<List<Long>> = _selectedDayCompletedHabitIds.asStateFlow()

    private val _selectedDayJournalEntries = MutableStateFlow<List<JournalEntryEntity>>(emptyList())
    val selectedDayJournalEntries: StateFlow<List<JournalEntryEntity>> = _selectedDayJournalEntries.asStateFlow()


    val darkModeEnabled: Flow<Boolean> = settings.darkModeEnabled()
    
    fun getCachedTheme(): Boolean? = settings.getCachedTheme()

    val soundsEnabled: StateFlow<Boolean> = settings.soundsEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val focusModeEnabled: StateFlow<Boolean> = settings.focusModeEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val hapticsEnabled: StateFlow<Boolean> = settings.hapticsEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val animationsEnabled: StateFlow<Boolean> = settings.animationsEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _milestoneEvents = Channel<MilestoneEvent>(Channel.BUFFERED)
    val milestoneEvents = _milestoneEvents.receiveAsFlow()

    private val _goalCompletionEvents = Channel<GoalCompletionEvent>(Channel.BUFFERED)
    val goalCompletionEvents = _goalCompletionEvents.receiveAsFlow()

    val tokenCount: StateFlow<Int> = repository.tokenFlow()
        .map { it?.count ?: 0 }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val stepState: StateFlow<StepState> = stepTracker.stepState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StepState(false, 0))

    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    private val _calendarDays = MutableStateFlow<List<CalendarDayState>>(emptyList())
    val calendarDays: StateFlow<List<CalendarDayState>> = _calendarDays.asStateFlow()

    private var calendarDirty = true

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.refreshBrokenStreaks(LocalDate.now())
            // Restore all alarms on startup — ensures reminders survive process death
            reminderScheduler.scheduleAll()
            goalDeadlineScheduler.scheduleAll()
            loadCalendar(YearMonth.now())
        }


    }

    fun addHabit(
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
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.addHabit(
                name = name,
                description = description,
                category = category,
                color = color,
                scheduledDays = scheduledDays,
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime,
                paused = paused,
                stepEnabled = stepEnabled,
                stepGoal = stepGoal,
                goalId = goalId
            )
            // Schedule alarm using the returned ID + known parameters.
            // We construct a minimal HabitEntity matching what was just inserted
            // to avoid an extra DB round-trip or needing getHabitsOnce() on the repo.
            if (reminderEnabled && reminderTime != null && !paused) {
                reminderScheduler.scheduleHabit(
                    HabitEntity(
                        id = newId,
                        name = name,
                        description = description,
                        category = category,
                        color = color,
                        scheduledDays = scheduledDays,
                        reminderEnabled = true,
                        reminderTime = reminderTime,
                        paused = false,
                        stepEnabled = stepEnabled,
                        stepGoal = stepGoal,
                        goalId = goalId
                    )
                )
            }
        }
    }

    fun updateHabitFull(
        habitId: Long,
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
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getHabitsOnce().firstOrNull { it.id == habitId }
            if (existing != null) {
                val updated = existing.copy(
                    name = name,
                    description = description,
                    category = category,
                    color = color,
                    scheduledDays = scheduledDays,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    paused = paused,
                    stepEnabled = stepEnabled,
                    stepGoal = stepGoal,
                    goalId = goalId
                )
                repository.updateHabit(updated)
                // scheduleHabit handles both schedule and cancel based on reminderEnabled/paused
                reminderScheduler.scheduleHabit(updated)
            }
        }
    }

    fun addGoal(
        title: String,
        description: String?,
        startDate: LocalDate,
        deadline: LocalDate?,
        onSuccess: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.addGoal(title, description, startDate, deadline)
            goalDeadlineScheduler.scheduleAll()
            withContext(Dispatchers.Main) { onSuccess?.invoke(id) }
        }
    }

    fun updateGoal(goal: GoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateGoal(goal)
            goalDeadlineScheduler.scheduleAll()
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGoal(goal)
            goalDeadlineScheduler.scheduleAll()
        }
    }

    fun completeGoal(goal: GoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markGoalCompleted(goal.goalId, true, LocalDate.now())
            _goalCompletionEvents.send(GoalCompletionEvent(goal.title))
            goalDeadlineScheduler.scheduleAll()
        }
    }

    fun upsertJournalEntry(
        title: String,
        body: String,
        date: LocalDate,
        mood: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertJournalEntry(title, body, date, mood)
        }
    }

    fun updateJournalEntry(entry: JournalEntryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateJournalEntry(entry)
        }
    }

    fun deleteJournalEntry(entry: JournalEntryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteJournalEntry(entry)
        }
    }

    fun markCompleted(habit: HabitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val milestoneTriggered = repository.markCompleted(habit, LocalDate.now())
            if (milestoneTriggered != null) {
                _milestoneEvents.send(MilestoneEvent(habit.name, milestoneTriggered, habit.id))
            }
            calendarDirty = true
            loadCalendar(_calendarMonth.value)
            // Reschedule next occurrence for this specific habit
            reminderScheduler.scheduleHabit(habit)
        }
    }

    fun toggleHabitCompletion(habit: HabitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val completedToday = habit.lastCompletedDate == today
            if (completedToday) {
                repository.markUncompleted(habit, today)
            } else {
                val milestoneTriggered = repository.markCompleted(habit, today)
                if (milestoneTriggered != null) {
                    _milestoneEvents.send(MilestoneEvent(habit.name, milestoneTriggered, habit.id))
                }
            }
            calendarDirty = true
            loadCalendar(_calendarMonth.value)
            reminderScheduler.scheduleHabit(habit)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Cancel alarm BEFORE deleting so we have the habit ID to cancel against
            reminderScheduler.cancelHabit(habit.id)
            repository.deleteHabit(habit)
        }
    }



    fun tryBuyStreakFreeze(habit: HabitEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val frozen = repository.buyStreakFreeze(habit.id)
            withContext(Dispatchers.Main) { onResult(frozen) }
        }
    }

    fun updateSort(option: SortOption) {
        _sortOption.value = option
    }

    fun togglePaused(habit: HabitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val nowPaused = !habit.paused
            repository.pauseHabit(habit, nowPaused)
            if (nowPaused) {
                // Immediately cancel the alarm when pausing
                reminderScheduler.cancelHabit(habit.id)
            } else {
                // Restore alarm when unpausing — re-read from DB for fresh state
                val restored = repository.getHabitsOnce().firstOrNull { it.id == habit.id }
                if (restored != null) reminderScheduler.scheduleHabit(restored)
            }
        }
    }

    fun updateReminder(habit: HabitEntity, enabled: Boolean, time: LocalTime?) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = habit.copy(reminderEnabled = enabled, reminderTime = time)
            repository.updateHabit(updated)
            // scheduleHabit handles both enable (schedule) and disable (cancel)
            reminderScheduler.scheduleHabit(updated)
        }
    }

    fun loadCalendar(month: YearMonth) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!calendarDirty && month == _calendarMonth.value) return@launch
            val habits = repository.getHabitsOnce()
            val start = month.atDay(1)
            val end = month.atEndOfMonth()
            val completions = repository.weeklySummary(start, end)
            val completionMap = completions.associateBy { it.date }
            val allJournals = repository.getJournalEntries().first()
            val journalMap = allJournals.groupBy { it.date }

            val days = (1..month.lengthOfMonth()).map { day ->
                val date = month.atDay(day)
                val scheduledCount = habits.count {
                    !it.paused && repository.isScheduledForDay(it, date)
                }
                val completedCount = completionMap[date.toString()]?.count ?: 0
                val journalCount = journalMap[date]?.size ?: 0
                CalendarDayState(date, completedCount, scheduledCount, journalCount)
            }
            _calendarMonth.value = month
            _calendarDays.value = days
            calendarDirty = false
        }
    }

    fun loadDayDetail(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val completed = repository.completedHabitIdsForDate(date)
            val entries = repository.getJournalEntriesByDate(date)
            _selectedDayCompletedHabitIds.value = completed
            _selectedDayJournalEntries.value = entries
        }
    }


    fun loadHabitHistory(habitId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val startOfMonth = YearMonth.from(today).atDay(1)
            val monthCount = repository.completionCountForHabit(startOfMonth, today, habitId)
            val weekStart = today.minusDays(6)
            val weekCount = repository.completionCountForHabit(weekStart, today, habitId)

            val updated = _habitHistory.value.toMutableMap()
            updated[habitId] = HabitHistory(weekCount = weekCount, monthCount = monthCount)
            _habitHistory.value = updated
        }
    }

    fun isScheduledToday(habit: HabitEntity): Boolean {
        return repository.isScheduledForDay(habit, LocalDate.now())
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settings.setDarkMode(enabled) }
    }

    fun setSounds(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settings.setSounds(enabled) }
    }

    fun setFocusMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settings.setFocusMode(enabled) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { settings.setHaptics(enabled) }
    }
}

@androidx.compose.runtime.Immutable
data class ActiveGoalState(
    val goal: GoalEntity,
    val progressDetails: GoalProgressDetails
)

data class MilestoneEvent(val habitName: String, val streak: Int, val habitId: Long)
data class GoalCompletionEvent(val goalTitle: String)

/**
 * Per-day completion stats for the weekly summary chart.
 * [completed] = habits actually completed that day.
 * [scheduled] = habits that were due/scheduled that day (not paused).
 * [completionRate] = 0.0..1.0, safe against division-by-zero.
 */
data class DailyStats(
    val date: LocalDate,
    val completed: Int,
    val scheduled: Int
) {
    val completionRate: Float
        get() = if (scheduled == 0) 0f else (completed.toFloat() / scheduled.toFloat()).coerceIn(0f, 1f)
}

enum class SortOption { NAME, STREAK, LAST_COMPLETED }

@androidx.compose.runtime.Immutable
data class CalendarDayState(
    val date: LocalDate,
    val completedCount: Int,
    val scheduledCount: Int,
    val journalCount: Int = 0
) {
    val missedScheduled: Int
        get() = (scheduledCount - completedCount).coerceAtLeast(0)
}

@androidx.compose.runtime.Immutable
data class HabitHistory(
    val weekCount: Int,
    val monthCount: Int
)

private fun buildInsights(
    habits: List<HabitEntity>,
    weeklyStats: List<DailyStats>
): List<String> {
    if (habits.isEmpty() || weeklyStats.isEmpty()) return emptyList()

    val activeHabits = habits.filter { !it.paused }
    val insights = mutableListOf<String>()

    // --- Weekly completion rate ---
    val totalScheduled = weeklyStats.sumOf { it.scheduled }
    val totalCompleted = weeklyStats.sumOf { it.completed }
    if (totalScheduled > 0) {
        val rate = (totalCompleted * 100) / totalScheduled
        val rateLabel = when {
            rate >= 90 -> "Outstanding"
            rate >= 70 -> "Strong"
            rate >= 50 -> "Moderate"
            rate >= 25 -> "Low"
            else -> "Very low"
        }
        insights.add("$rateLabel week — $rate% of scheduled habits completed.")
    }

    // --- Best and worst performing days ---
    val daysWithScheduled = weeklyStats.filter { it.scheduled > 0 }
    if (daysWithScheduled.size >= 2) {
        val bestDay = daysWithScheduled.maxByOrNull { it.completionRate }
        val worstDay = daysWithScheduled.minByOrNull { it.completionRate }
        if (bestDay != null && bestDay.completed > 0) {
            val dayName = bestDay.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            val pct = (bestDay.completionRate * 100).toInt()
            insights.add("$dayName was your strongest day this week ($pct% complete).")
        }
        if (worstDay != null && worstDay != bestDay && worstDay.completed < worstDay.scheduled) {
            val dayName = worstDay.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            insights.add("$dayName had the most missed habits — consider adjusting that day's schedule.")
        }
    }

    // --- Perfect days ---
    val perfectDays = weeklyStats.count { it.scheduled > 0 && it.completed >= it.scheduled }
    if (perfectDays > 0) {
        insights.add("$perfectDays perfect ${if (perfectDays == 1) "day" else "days"} this week — every scheduled habit completed.")
    }

    // --- Active streaks ---
    val streaking = activeHabits.filter { it.currentStreak >= 3 }
    if (streaking.isNotEmpty()) {
        val top = streaking.maxByOrNull { it.currentStreak }!!
        insights.add("${top.name} is on a ${top.currentStreak}-day streak. Keep it going!")
    }

    // --- Missed habit pattern ---
    val missedDays = weeklyStats.count { it.scheduled > 0 && it.completed == 0 }
    if (missedDays >= 3) {
        insights.add("$missedDays days this week had no completions. A shorter habit list might help consistency.")
    }

    return insights.take(5)
}

private fun selectActiveGoal(goals: List<GoalEntity>, today: LocalDate): GoalEntity? {
    if (goals.isEmpty()) return null
    val withDeadline = goals.filter { it.deadline != null }
    val upcoming = withDeadline.filter { it.deadline?.isBefore(today) == false }
    val deadlineSorted = upcoming.ifEmpty { withDeadline }

    if (deadlineSorted.isNotEmpty()) {
        return deadlineSorted
            .sortedWith(
                compareBy<GoalEntity> { it.deadline }
                    .thenByDescending { it.goalId }
            )
            .first()
    }

    return goals.maxByOrNull { it.goalId }
}

package com.example.dailyhabittracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyhabittracker.DailyHabitTrackerApp
import com.example.dailyhabittracker.data.DateCount
import com.example.dailyhabittracker.data.GoalEntity
import com.example.dailyhabittracker.data.HabitEntity
import com.example.dailyhabittracker.data.HabitRepository
import com.example.dailyhabittracker.data.JournalEntryEntity
import com.example.dailyhabittracker.data.SettingsRepository
import com.example.dailyhabittracker.reminders.GoalDeadlineScheduler
import com.example.dailyhabittracker.reminders.ReminderWorkScheduler
import com.example.dailyhabittracker.sensors.StepState
import com.example.dailyhabittracker.sensors.StepTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as DailyHabitTrackerApp).container
    private val repository: HabitRepository = container.habitRepository
    private val settings: SettingsRepository = container.settings
    private val reminderScheduler: ReminderWorkScheduler = container.reminderScheduler
    private val goalDeadlineScheduler: GoalDeadlineScheduler = container.goalDeadlineScheduler
    private val stepTracker: StepTracker = container.stepTracker

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

    private val _weeklySummary = MutableStateFlow<List<DateCount>>(emptyList())
    val weeklySummary: StateFlow<List<DateCount>> = _weeklySummary.asStateFlow()

    private val _monthlyCount = MutableStateFlow(0)
    val monthlyCount: StateFlow<Int> = _monthlyCount.asStateFlow()

    private val _insights = MutableStateFlow<List<String>>(emptyList())
    val insights: StateFlow<List<String>> = _insights.asStateFlow()

    private val _habitHistory = MutableStateFlow<Map<Long, HabitHistory>>(emptyMap())
    val habitHistory: StateFlow<Map<Long, HabitHistory>> = _habitHistory.asStateFlow()

    private val _activeGoal = MutableStateFlow<ActiveGoalState?>(null)
    val activeGoal: StateFlow<ActiveGoalState?> = _activeGoal.asStateFlow()

    private val _selectedDayCompletedHabitIds = MutableStateFlow<List<Long>>(emptyList())
    val selectedDayCompletedHabitIds: StateFlow<List<Long>> = _selectedDayCompletedHabitIds.asStateFlow()

    private val _selectedDayJournalEntry = MutableStateFlow<JournalEntryEntity?>(null)
    val selectedDayJournalEntry: StateFlow<JournalEntryEntity?> = _selectedDayJournalEntry.asStateFlow()

    val darkModeEnabled: StateFlow<Boolean> = settings.darkModeEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val soundsEnabled: StateFlow<Boolean> = settings.soundsEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val focusModeEnabled: StateFlow<Boolean> = settings.focusModeEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val hapticsEnabled: StateFlow<Boolean> = settings.hapticsEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val tokenCount: StateFlow<Int> = repository.tokenFlow()
        .map { it?.count ?: 0 }
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
            reminderScheduler.scheduleNext()
            goalDeadlineScheduler.scheduleAll()
            refreshStats()
            loadCalendar(YearMonth.now())
        }

        viewModelScope.launch(Dispatchers.IO) {
            goalsFlow.collect { goals ->
                val today = LocalDate.now()
                val active = selectActiveGoal(goals, today)
                if (active == null) {
                    _activeGoal.value = null
                } else {
                    val progress = repository.goalProgressPercent(active, today)
                    _activeGoal.value = ActiveGoalState(active, progress)
                }
            }
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
            repository.addHabit(
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
            reminderScheduler.scheduleNext()
        }
    }

    fun addGoal(
        title: String,
        description: String?,
        startDate: LocalDate,
        deadline: LocalDate?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addGoal(title, description, startDate, deadline)
            goalDeadlineScheduler.scheduleAll()
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
            repository.markCompleted(habit, LocalDate.now())
            refreshStats()
            calendarDirty = true
            loadCalendar(_calendarMonth.value)
            reminderScheduler.scheduleNext()
        }
    }

    fun refreshStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val startOfWeek = today.minusDays(6)
            _weeklySummary.value = repository.weeklySummary(startOfWeek, today)

            val month = YearMonth.from(today)
            val startOfMonth = month.atDay(1)
            val endOfMonth = month.atEndOfMonth()
            _monthlyCount.value = repository.monthlyCompletionCount(startOfMonth, endOfMonth)

            val habits = repository.getHabitsOnce()
            _insights.value = buildInsights(habits, _weeklySummary.value, startOfWeek, today)
        }
    }

    fun tryRestoreStreak(habit: HabitEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val restored = repository.restoreStreakIfAllowed(habit, LocalDate.now())
            withContext(Dispatchers.Main) { onResult(restored) }
        }
    }

    fun updateSort(option: SortOption) {
        _sortOption.value = option
    }

    fun togglePaused(habit: HabitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.pauseHabit(habit, !habit.paused)
            reminderScheduler.scheduleNext()
        }
    }

    fun updateReminder(habit: HabitEntity, enabled: Boolean, time: LocalTime?) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = habit.copy(reminderEnabled = enabled, reminderTime = time)
            repository.updateHabit(updated)
            reminderScheduler.scheduleNext()
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

            val days = (1..month.lengthOfMonth()).map { day ->
                val date = month.atDay(day)
                val scheduledCount = habits.count {
                    !it.paused && repository.isScheduledForDay(it, date)
                }
                val completedCount = completionMap[date.toString()]?.count ?: 0
                CalendarDayState(date, completedCount, scheduledCount)
            }
            _calendarMonth.value = month
            _calendarDays.value = days
            calendarDirty = false
        }
    }

    fun loadDayDetail(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val completed = repository.completedHabitIdsForDate(date)
            val entry = repository.getJournalEntryByDate(date)
            _selectedDayCompletedHabitIds.value = completed
            _selectedDayJournalEntry.value = entry
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
    val progressPercent: Int
)

enum class SortOption { NAME, STREAK, LAST_COMPLETED }

@androidx.compose.runtime.Immutable
data class CalendarDayState(
    val date: LocalDate,
    val completedCount: Int,
    val scheduledCount: Int
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
    weeklySummary: List<DateCount>,
    startOfWeek: LocalDate,
    today: LocalDate
): List<String> {
    if (habits.isEmpty()) return emptyList()
    val completions = weeklySummary.sumOf { it.count }

    val scheduledOpportunities = habits.sumOf { habit ->
        if (habit.paused) return@sumOf 0
        (0..6).count { offset ->
            val date = startOfWeek.plusDays(offset.toLong())
            habit.scheduledDays.isEmpty() || habit.scheduledDays.contains(date.dayOfWeek.value)
        }
    }
    val rate = if (scheduledOpportunities == 0) 0 else (completions * 100) / scheduledOpportunities

    val bestDay = weeklySummary.maxByOrNull { it.count }?.date
    val bestDayLabel = bestDay?.let { LocalDate.parse(it).dayOfWeek.name.lowercase().replaceFirstChar { c -> c.uppercase() } }

    val insights = mutableListOf<String>()
    if (bestDayLabel != null) {
        insights.add("You are most consistent on $bestDayLabel.")
    }
    if (scheduledOpportunities > 0) {
        insights.add("You completed $rate% of scheduled habits this week.")
    }
    return insights.take(2)
}

private fun selectActiveGoal(goals: List<GoalEntity>, today: LocalDate): GoalEntity? {
    if (goals.isEmpty()) return null
    val withDeadline = goals.filter { it.deadline != null }
    val upcoming = withDeadline.filter { it.deadline != null && !it.deadline!!.isBefore(today) }
    val deadlineSorted = upcoming.ifEmpty { withDeadline }

    if (deadlineSorted.isNotEmpty()) {
        return deadlineSorted
            .sortedWith(
                compareBy<GoalEntity> { it.deadline }
                    .thenByDescending { it.startDate }
            )
            .first()
    }

    return goals.maxByOrNull { it.startDate }
}

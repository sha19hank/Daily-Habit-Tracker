package com.mlue.app

import android.content.Context
import com.mlue.app.data.HabitDatabase
import com.mlue.app.data.HabitRepository
import com.mlue.app.data.SettingsRepository
import com.mlue.app.reminders.GoalDeadlineScheduler
import com.mlue.app.reminders.ReminderScheduler
import com.mlue.app.sensors.StepTracker

class AppContainer(context: Context) {
    private val database = HabitDatabase.build(context)
    private val settingsRepository = SettingsRepository(context)

    val habitRepository: HabitRepository = HabitRepository(
        database,
        database.habitDao(),
        database.goalDao(),
        database.journalDao(),
        settingsRepository
    )
    val settings: SettingsRepository = settingsRepository

    // Single source of truth for habit reminder scheduling (AlarmManager-based)
    val reminderScheduler: ReminderScheduler = ReminderScheduler(context, database.habitDao())

    // Goal deadline notifications still use WorkManager (appropriate for non-exact deferred work)
    val goalDeadlineScheduler: GoalDeadlineScheduler = GoalDeadlineScheduler(context, database.goalDao())

    val stepTracker: StepTracker = StepTracker(context, settingsRepository)
}

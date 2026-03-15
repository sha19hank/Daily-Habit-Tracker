package com.example.dailyhabittracker

import android.content.Context
import com.example.dailyhabittracker.ads.AdManager
import com.example.dailyhabittracker.data.HabitDatabase
import com.example.dailyhabittracker.data.HabitRepository
import com.example.dailyhabittracker.data.SettingsRepository
import com.example.dailyhabittracker.reminders.GoalDeadlineScheduler
import com.example.dailyhabittracker.reminders.ReminderWorkScheduler
import com.example.dailyhabittracker.sensors.StepTracker

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
    val adManager: AdManager = AdManager(context)
    val settings: SettingsRepository = settingsRepository
    val reminderScheduler: ReminderWorkScheduler = ReminderWorkScheduler(context, database.habitDao())
    val goalDeadlineScheduler: GoalDeadlineScheduler = GoalDeadlineScheduler(context, database.goalDao())
    val stepTracker: StepTracker = StepTracker(context, settingsRepository)
}

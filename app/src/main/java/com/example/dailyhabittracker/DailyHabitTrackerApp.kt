package com.example.dailyhabittracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.dailyhabittracker.reminders.GoalDeadlineWorker
import com.example.dailyhabittracker.reminders.ReminderWorker

class DailyHabitTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ReminderWorker.REMINDER_CHANNEL,
            "Habit Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val goalChannel = NotificationChannel(
            GoalDeadlineWorker.GOAL_CHANNEL,
            "Goal Deadlines",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
        manager?.createNotificationChannel(goalChannel)
    }
}

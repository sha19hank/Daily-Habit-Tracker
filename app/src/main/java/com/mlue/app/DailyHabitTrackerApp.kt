package com.mlue.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.WorkManager
import com.mlue.app.reminders.GoalDeadlineWorker
import com.mlue.app.reminders.ReminderReceiver

class DailyHabitTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        cancelLegacyReminderWork()
        createNotificationChannels()
    }

    /**
     * One-time migration: cancel any WorkManager reminder jobs scheduled by the old
     * ReminderWorkScheduler / ReminderWorker system. Without this, ghost WorkManager
     * jobs could continue firing alongside the new AlarmManager system, causing
     * duplicate notifications.
     *
     * The string constants are inlined here because the source files are deleted.
     */
    private fun cancelLegacyReminderWork() {
        val wm = WorkManager.getInstance(this)
        wm.cancelAllWorkByTag("habit_reminder")          // ReminderWorkScheduler.WORK_TAG
        wm.cancelUniqueWork("habit_reminder_work")       // ReminderWorkScheduler.UNIQUE_WORK
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java) ?: return

        // Habit reminders: IMPORTANCE_HIGH for reliable delivery through Doze and OEM
        // battery savers. The channel handles actual heads-up/sound/vibration behavior —
        // IMPORTANCE_HIGH does NOT mean noisy: it enables delivery, user controls the rest.
        val reminderChannel = NotificationChannel(
            ReminderReceiver.CHANNEL_ID,
            "Habit Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily habit reminder notifications"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 150, 100, 150) // Subtle double-tap
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        // Goal deadlines: default importance (deferred, informational)
        val goalChannel = NotificationChannel(
            GoalDeadlineWorker.GOAL_CHANNEL,
            "Goal Deadlines",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Upcoming goal deadline reminders"
        }

        manager.createNotificationChannel(reminderChannel)
        manager.createNotificationChannel(goalChannel)
    }
}

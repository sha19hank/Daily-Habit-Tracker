package com.example.dailyhabittracker.reminders

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dailyhabittracker.R

class GoalDeadlineWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Goal deadline"
        val daysRemaining = inputData.getInt(KEY_DAYS_REMAINING, 0)
        val deadline = inputData.getString(KEY_DEADLINE)

        val message = when (daysRemaining) {
            3 -> "Deadline in 3 days"
            1 -> "Deadline tomorrow"
            0 -> "Goal deadline today"
            else -> "Goal deadline approaching"
        }

        val text = if (deadline.isNullOrBlank()) message else "$message • $deadline"

        val notification = NotificationCompat.Builder(applicationContext, GOAL_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify((title.hashCode() + daysRemaining).coerceAtLeast(0), notification)

        return Result.success()
    }

    companion object {
        const val GOAL_CHANNEL = "goal_deadline_reminders"
        const val KEY_TITLE = "goal_title"
        const val KEY_DAYS_REMAINING = "goal_days_remaining"
        const val KEY_DEADLINE = "goal_deadline"
    }
}

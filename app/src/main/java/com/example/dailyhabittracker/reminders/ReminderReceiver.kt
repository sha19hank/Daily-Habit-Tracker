package com.example.dailyhabittracker.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dailyhabittracker.R
import com.example.dailyhabittracker.data.HabitDatabase
import kotlinx.coroutines.runBlocking

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId <= 0L) return

        runBlocking {
            val dao = HabitDatabase.build(context).habitDao()
            val habit = dao.getHabitById(habitId) ?: return@runBlocking
            if (!habit.reminderEnabled || habit.paused) return@runBlocking

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(habit.name)
                .setContentText(context.getString(R.string.reminder_body))
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(habitId.toInt(), notification)

            ReminderScheduler(context, dao).scheduleHabit(habit)
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val CHANNEL_ID = "habit_reminders"
    }
}

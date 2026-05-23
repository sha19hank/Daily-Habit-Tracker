package com.mlue.app.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mlue.app.MainActivity
import com.mlue.app.R
import com.mlue.app.data.HabitDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: return
        if (habitId <= 0L) return

        // goAsync() keeps the BroadcastReceiver alive past onReceive() return
        // while we do IO work, avoiding main-thread blocking / ANR risk
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = HabitDatabase.build(context).habitDao()
                val habit = dao.getHabitById(habitId)

                // Double-check: don't notify if habit was deleted, paused, or disabled
                if (habit == null || !habit.reminderEnabled || habit.paused) {
                    pendingResult.finish()
                    return@launch
                }

                // Build tap-to-open action — opens app on notification tap
                val tapIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val tapPendingIntent = PendingIntent.getActivity(
                    context,
                    habitId.toInt(),
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(habit.name)
                    .setContentText(context.getString(R.string.reminder_body))
                    .setAutoCancel(true)
                    .setContentIntent(tapPendingIntent)
                    // PRIORITY_HIGH ensures delivery through Doze and OEM battery savers
                    // without being "loud" — channel importance controls the actual UX
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVibrate(longArrayOf(0, 150, 100, 150)) // Subtle double-tap vibration
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .build()

                NotificationManagerCompat.from(context).notify(habitId.toInt(), notification)

                // Reschedule the next occurrence for this habit
                ReminderScheduler(context, dao).scheduleHabit(habit)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        // Explicit action ensures only intentional broadcasts trigger this receiver
        const val ACTION_REMINDER = "com.mlue.app.ACTION_HABIT_REMINDER"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
        const val CHANNEL_ID = "habit_reminders"
    }
}

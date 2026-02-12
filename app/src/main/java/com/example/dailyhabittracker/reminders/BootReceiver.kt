package com.example.dailyhabittracker.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dailyhabittracker.data.HabitDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val dao = HabitDatabase.build(context).habitDao()
        val scheduler = ReminderWorkScheduler(context, dao)
        CoroutineScope(Dispatchers.IO).launch {
            scheduler.scheduleNext()
        }
    }
}

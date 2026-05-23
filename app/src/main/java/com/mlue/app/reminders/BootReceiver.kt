package com.mlue.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mlue.app.data.HabitDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Handle both standard Android boot AND Xiaomi/HTC quick-boot
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Restore ALL habit reminders from Room — single lightweight pass
                val dao = HabitDatabase.build(context).habitDao()
                ReminderScheduler(context, dao).scheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

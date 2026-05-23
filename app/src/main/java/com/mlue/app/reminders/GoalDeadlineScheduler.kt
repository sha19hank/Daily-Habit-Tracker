package com.mlue.app.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mlue.app.data.GoalDao
import com.mlue.app.data.GoalEntity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class GoalDeadlineScheduler(
    private val context: Context,
    private val goalDao: GoalDao
) {
    suspend fun scheduleAll() {
        val goals = goalDao.getGoalsOnce()
        val today = LocalDate.now()
        goals.forEach { goal ->
            val deadline = goal.deadline ?: return@forEach
            scheduleForGoal(goal, deadline, today)
        }
    }

    private fun scheduleForGoal(goal: GoalEntity, deadline: LocalDate, today: LocalDate) {
        val offsets = listOf(3L, 1L, 0L)
        offsets.forEach { offset ->
            val notifyDate = deadline.minusDays(offset)
            if (notifyDate.isBefore(today)) return@forEach

            val notifyTime = LocalTime.of(9, 0)
            val notifyDateTime = LocalDateTime.of(notifyDate, notifyTime)
            val delayMillis = Duration.between(LocalDateTime.now(), notifyDateTime)
                .toMillis()
                .coerceAtLeast(0)

            val workRequest = OneTimeWorkRequestBuilder<GoalDeadlineWorker>()
                .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        GoalDeadlineWorker.KEY_TITLE to goal.title,
                        GoalDeadlineWorker.KEY_DAYS_REMAINING to offset.toInt(),
                        GoalDeadlineWorker.KEY_DEADLINE to deadline.toString()
                    )
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(goal.goalId, offset),
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    private fun workName(goalId: Long, offset: Long): String {
        return "goal_deadline_${goalId}_$offset"
    }
}

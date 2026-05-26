package com.mlue.app.viewmodel

import com.mlue.app.data.HabitCompletionEntity
import com.mlue.app.data.HabitEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure function to compute a [HabitMomentum] state for a single habit.
 *
 * Priority order (first match wins):
 *  1. STRONG    — streak >= 7 AND completed within last 2 days
 *  2. BUILDING  — streak >= 3 AND completed within last 3 days
 *  3. RECOVERING— streak broke (== 0) BUT returned after a 4+ day gap within last 5 days
 *  4. DORMANT   — habit established (>= 7 days old) AND no completions in last 14 days
 *  5. STEADY    — everything else (default, no visual indicator shown)
 *
 * No IO, no suspend. Runs inside combine() on background dispatchers.
 * Sprint 3B: Adaptive Intelligence Layer
 */
internal fun computeMomentum(
    habit: HabitEntity,
    completions: List<HabitCompletionEntity>,
    today: LocalDate
): HabitMomentum {
    val habitCompletions = completions
        .filter { it.habitId == habit.id }
        .map { it.completionDate }

    val completedToday = habit.lastCompletedDate == today
    val completedRecently2 = habitCompletions.any { !it.isBefore(today.minusDays(1)) }
    val completedRecently3 = habitCompletions.any { !it.isBefore(today.minusDays(2)) }
    val completedRecently5 = habitCompletions.any { !it.isBefore(today.minusDays(4)) }
    val completedRecently14 = habitCompletions.any { !it.isBefore(today.minusDays(13)) }

    // STRONG: sustained, peak rhythm
    if (habit.currentStreak >= 7 && (completedToday || completedRecently2)) {
        return HabitMomentum.STRONG
    }

    // BUILDING: growing consistency
    if (habit.currentStreak >= 3 && (completedToday || completedRecently3)) {
        return HabitMomentum.BUILDING
    }

    // RECOVERING: streak is broken but user came back after a real gap
    if (habit.currentStreak == 0 && completedRecently5) {
        // Verify there was actually a gap (≥ 4 days) before the recent completion
        val sorted = habitCompletions.sorted()
        val recentReturn = sorted.lastOrNull { !it.isBefore(today.minusDays(4)) }
        val beforeReturn = sorted.lastOrNull { it.isBefore(today.minusDays(4)) }
        if (recentReturn != null && beforeReturn != null) {
            val gap = ChronoUnit.DAYS.between(beforeReturn, recentReturn)
            if (gap >= 4) return HabitMomentum.RECOVERING
        }
    }

    // DORMANT: established habit gone quiet — not a failure, just a gentle observation
    val habitAgedays = ChronoUnit.DAYS.between(habit.createdDate, today)
    if (habitAgedays >= 7 && !completedRecently14) {
        return HabitMomentum.DORMANT
    }

    // STEADY: normal active state — no dot shown
    return HabitMomentum.STEADY
}

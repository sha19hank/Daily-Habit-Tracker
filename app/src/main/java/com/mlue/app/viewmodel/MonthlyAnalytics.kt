package com.mlue.app.viewmodel

/**
 * Monthly behavioral analytics — derived entirely in-memory from existing Room data.
 * No DB migration required. Computed off the main thread inside combine() flow lambdas.
 *
 * Sprint 3A: Behavioral Intelligence Layer
 */
data class MonthlyAnalytics(
    /** Completion rate for the current month to date (0–100). */
    val completionPercent: Int,

    /** Total habit completions recorded so far this month. */
    val totalCompleted: Int,

    /** Total habit-days scheduled this month based on active habits and their day schedules. */
    val totalScheduled: Int,

    /** Name of the habit with the most completions this month, or null if no completions exist. */
    val strongestHabitName: String?,

    /**
     * Day of the week (e.g. "Wednesday") where scheduled habits were most often not completed.
     * Null if no meaningful pattern found or data is insufficient.
     */
    val mostMissedWeekday: String?,

    /**
     * Best current streak across all habits — used as a proxy for peak monthly consistency.
     * Not the true "longest streak this month" (which would require per-day history queries).
     */
    val bestActiveStreak: Int,

    /**
     * Delta from previous month's completion %, positive = improvement, negative = decline.
     * Zero if previous month had no scheduled habits.
     */
    val trendVsPreviousMonth: Int,

    /**
     * Calm, human-toned one-sentence reflection on the month's pattern.
     * Chosen from a curated set — never AI-chatbot phrasing.
     */
    val reflectionSummary: String
)

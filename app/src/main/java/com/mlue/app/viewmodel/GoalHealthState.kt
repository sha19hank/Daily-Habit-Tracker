package com.mlue.app.viewmodel

/**
 * Goal momentum category — drives the subtle health chip shown below each active goal's
 * progress bar. Computed from completion % relative to time elapsed toward the deadline.
 *
 * Color guidance (Sprint 3A design rules):
 *  STRONG         → soft green
 *  ON_TRACK       → primary (warm amber / maroon)
 *  SLOW           → muted / onSurfaceVariant
 *  NEEDS_ATTENTION → warm amber — NOT red, never alarming
 *
 * Sprint 3A: Goal Intelligence Layer
 */
enum class GoalMomentum {
    /** Ahead of pace or ≥ 80% complete relative to time elapsed. */
    STRONG,

    /** Within ~15% of the expected pace. */
    ON_TRACK,

    /** Behind expected pace but still recoverable. */
    SLOW,

    /** Significantly behind OR deadline is very close with low progress. */
    NEEDS_ATTENTION
}

/**
 * Derived goal health snapshot — displayed as a single subtle chip below the goal progress bar.
 * Computed from existing [GoalProgressDetails] and [GoalEntity] — no new DB queries.
 */
data class GoalHealthState(
    val goalId: Long,

    val momentum: GoalMomentum,

    /**
     * Soft-language completion likelihood statement.
     * e.g. "Likely to finish on time", "Current pace may need a nudge"
     */
    val completionLikelihood: String,

    /**
     * Short label for the chip pill.
     * e.g. "Strong momentum", "Steady pace", "Slow progress", "Needs attention"
     */
    val velocityLabel: String
)

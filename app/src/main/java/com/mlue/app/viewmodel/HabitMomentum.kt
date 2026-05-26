package com.mlue.app.viewmodel

/**
 * Per-habit momentum state — derived from streak + recent completions.
 * Used to render a subtle 5dp dot indicator on HabitCard.
 *
 * Visual mapping:
 *  STRONG    → primary color dot (habit with strong active streak)
 *  BUILDING  → primaryContainer tint dot (short but growing streak)
 *  RECOVERING→ tertiary tint dot (returned after a gap)
 *  DORMANT   → very faint dot (active habit, no recent completions)
 *  STEADY    → no dot rendered (normal state — no visual noise)
 *
 * Tone principle: momentum is a quiet observation, not a badge or reward.
 * DORMANT does not mean failure. STRONG does not mean superiority.
 *
 * Sprint 3B: Adaptive Intelligence Layer
 */
enum class HabitMomentum {
    /** Streak >= 7 AND completed within last 2 days. Peak sustained rhythm. */
    STRONG,

    /** Streak >= 3 AND completed within last 3 days. Emerging consistency. */
    BUILDING,

    /** Streak == 0 AND completed within last 5 days after a gap of 4+ days. Returning. */
    RECOVERING,

    /** No completions in last 14 days AND habit older than 7 days. Quiet, not failing. */
    DORMANT,

    /** Everything else. Default — no visual indicator shown. */
    STEADY
}

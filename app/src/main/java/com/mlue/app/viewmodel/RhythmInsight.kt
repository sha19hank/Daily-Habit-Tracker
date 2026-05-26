package com.mlue.app.viewmodel

/**
 * A lightweight rhythm observation — derived from behavioral history, never AI-generated.
 *
 * Tone contract (Sprint 3B):
 *  ✓ Observational, soft: "You seem most consistent midweek."
 *  ✗ Predictive, scientific: "Your optimal performance window is Wednesday."
 *  ✗ Prescriptive: "You should perform habits on Tuesday."
 *
 * Max 2 observations per compute cycle to avoid information overload.
 * Minimum data threshold enforced in RhythmAnalyzer to prevent false patterns.
 *
 * Sprint 3B: Adaptive Intelligence Layer
 */
data class RhythmInsight(
    /**
     * Soft, human-toned observations about the user's natural patterns.
     * Each string is standalone — no ordering dependency.
     * Empty list = insufficient data, nothing displayed.
     */
    val observations: List<String>
)

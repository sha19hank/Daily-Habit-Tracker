package com.mlue.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * Centralized motion constants for Mlue.
 *
 * ALL animation specs across the app should reference this object.
 * Do NOT define inline tweens or springs elsewhere after Phase 5.
 *
 * Design principle: calm, editorial, physically believable.
 * No elastic bounce, no cartoon overswing.
 */
object AppMotion {

    // ── Durations ────────────────────────────────────────────────────────────
    const val durationMicro  = 120   // instant tap acknowledgment (scale pop, alpha nudge)
    const val durationShort  = 160   // micro interactions (checkbox, icon tint)
    const val durationMedium = 220   // card color/scale transitions
    const val durationLong   = 280   // screen enter transitions

    const val exitDuration   = 200   // screen exit (slightly faster — feels snappy)

    // ── Tween specs ─────────────────────────────────────────────────────────
    /** General color transition — used for card backgrounds, icon tints */
    fun colorTween() = tween<Color>(durationMillis = durationMedium, easing = FastOutSlowInEasing)

    /** Float transitions — opacity, progress bars, FAB alpha */
    fun floatTween(duration: Int = durationMedium) =
        tween<Float>(durationMillis = duration, easing = FastOutSlowInEasing)

    /** Dp transitions — elevation changes */
    fun dpTween() = tween<Dp>(durationMillis = durationShort, easing = FastOutSlowInEasing)

    /** Tween-based scale press — deterministic, no spring overshoot */
    fun pressTween() = tween<Float>(durationMillis = durationMicro, easing = FastOutSlowInEasing)

    // ── Spring specs ─────────────────────────────────────────────────────────
    /**
     * Tactile press response — used for card/button scale.
     * Slightly bouncy but controlled. NOT rubbery.
     */
    fun pressBounce() = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = 380f
    )

    /**
     * Checkbox pop — faster, tiny. Felt more than seen.
     */
    fun checkboxPop() = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Content size change — expand/collapse of HabitCard actions.
     */
    fun contentSpring() = spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = 0.85f,
        stiffness = 220f
    )

    /**
     * Panel slide spring — used for slideInVertically / slideOutVertically.
     * Must return SpringSpec<IntOffset> to match the animationSpec type.
     */
    fun panelSlideSpring() = spring<IntOffset>(
        dampingRatio = 0.82f,
        stiffness = 210f
    )
}

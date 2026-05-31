package com.mlue.app.ui.theme

import androidx.compose.ui.graphics.Color

// Cinematic Dark Mode Colors
val DarkBackground = Color(0xFF0A0C10) // Deep Charcoal/Navy
val DarkSurface = Color(0xFF13171F) // Muted Indigo layered
val DarkSurfaceVariant = Color(0xFF1C212B) // Slightly elevated
val DarkOnBackground = Color(0xFFF1F5F9)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF2D3748)

// Warm Editorial Light Mode Colors — 3-Layer Tonal System
// Layer 0 (Background): #F1ECE4 — warm linen, visibly below card surfaces
// Layer 1 (Cards):      #FFFDF9 — soft ivory (bright, tactile)
// Layer 2 (Elevated):   #FFFEFC — barely-warmer-than-pure-white (interactive surfaces)
val LightBackground = Color(0xFFF1ECE4)   // Deeper warm linen — clear depth vs cards
val LightSurface = Color(0xFFFFFDF9)
val LightSurfaceElevated = Color(0xFFFFFEFC) // Layer 2: progress card, quick journal, settings toggles, calendar container
val LightSurfaceVariant = Color(0xFFF7F2EC)
val LightOnBackground = Color(0xFF1F1A17)
val LightOnSurfaceVariant = Color(0xFF6B625C)
val LightOutline = Color(0xFFE6DED5)
val LightOutlineVariant = Color(0xFFEFE7DE)

val DarkOutlineVariant = Color(0xFF334155) // Hairline border color

// Amber/Orange Primary Accent (Used sparingly)
val PrimaryAmber = Color(0xFFE6A117)
val OnPrimaryAmber = Color(0xFFFFFFFF)
val PrimaryAmberContainerDark = Color(0xFF78350F)
val OnPrimaryAmberContainerDark = Color(0xFFFDE68A)

// Warm Editorial Accents (Light Mode)
val LightPrimaryMaroon = Color(0xFF7A3E2B)
val LightSecondaryAmber = Color(0xFFA46B3C)
val LightSuccessGreen = Color(0xFF4C7A57)
val LightDangerRed = Color(0xFFB14E48)

// Light Theme Hero / Goal Card — immutable editorial tokens
// Slightly softened maroon (editorial, not luxury-wine)
val LightHeroCardBackground = Color(0xFF8B4A35)  // Softened editorial maroon
val LightHeroCardOnSurface = Color(0xFFFFFDF9)   // Warm off-white text
val LightHeroCardTrack = Color(0xFF9E5840)        // Muted warm maroon for progress track
val LightHeroCardFill = Color(0xFFA46B3C)         // Burnt amber progress fill
val LightHeroCardSubtext = Color(0xFFEFE7DE)      // Warm muted label text on dark bg

// Light Theme Secondary Surface (no alpha, solid) — replaces secondaryContainer
val LightSolidPanel = Color(0xFFF7F2EC)           // Same as SurfaceVariant — solid, no fog

// Muted Violet/Indigo Secondary Accent
val SecondaryViolet = Color(0xFF5B4FCF)
val OnSecondaryViolet = Color(0xFFFFFFFF)
val SecondaryVioletContainerDark = Color(0xFF312E81)
val OnSecondaryVioletContainerDark = Color(0xFFE0E7FF)
val SecondaryVioletContainerLight = Color(0xFFEEF2FF)
val OnSecondaryVioletContainerLight = Color(0xFF5B4FCF)

val ErrorRed = Color(0xFFE5484D)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFFE5484D)

val SuccessGreen = Color(0xFF2FA36B)

// ── Sprint 3C: Topology Border Tokens ─────────────────────────────────────────
// Borders SUPPORT tonal separation — they do NOT become the topology.
// Light mode uses more visible hairlines (surfaces blend in bright light).
// Dark mode uses near-invisible lines — tonal shift IS the separator.

// Light mode hairline tokens (warm, editorial — not gray)
val LightTopologyBorder = Color(0xFFE8E0D4)        // Warm hairline — card/container edges
val LightTopologyBorderStrong = Color(0xFFDDD4C6)  // Slightly more defined — form fields, analytics

// Dark mode hairline tokens (~40–50% softer than light equivalents)
// Many surfaces will use no border at all — these are for cases where
// a hairline genuinely helps separate adjacent dark surfaces.
val DarkTopologyBorder = Color(0xFF1E2330)          // Almost invisible — very subtle depth
val DarkTopologyBorderStrong = Color(0xFF232A38)    // Slightly readable — use sparingly

// ── Sprint 3C: Active Goal Card — Dark Mode Upgrade ────────────────────────────
// Transforms the goal card from a blended slate into a cinematic bronze focus object.
// Same amber family as FAB and nav capsule — but deeper, richer, more editorial.
val DarkGoalCardBg = Color(0xFF2A1F0E)        // Deep burnt amber — cinematic, not bright
val DarkGoalCardText = Color(0xFFF8F7F4)      // Near-white — clean, premium contrast on dark amber
val DarkGoalCardSubtext = Color(0xFFBFA880)   // Muted warm gold — subdued secondary context
val DarkGoalCardTrack = Color(0xFF4A3520)     // Deep amber trough — progress bar track

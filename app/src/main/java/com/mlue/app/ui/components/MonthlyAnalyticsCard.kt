package com.mlue.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mlue.app.ui.theme.DarkSurface
import com.mlue.app.ui.theme.DarkSurfaceVariant
import com.mlue.app.ui.theme.LightSurface
import com.mlue.app.ui.theme.LightSurfaceVariant
import com.mlue.app.ui.theme.SuccessGreen
import com.mlue.app.viewmodel.MonthlyAnalytics
import kotlin.math.abs

/**
 * Monthly Analytics card for the Insights screen.
 *
 * Design principles (Sprint 3A):
 *  - One primary focus: completion %
 *  - Trend chip: secondary, only shown when delta >= 5%
 *  - Strongest habit + missed weekday: tertiary, compact
 *  - Reflection summary: the emotional anchor — calm, human-toned
 *  - No charts, no enterprise metrics, generous whitespace
 */
@Composable
fun MonthlyAnalyticsCard(
    analytics: MonthlyAnalytics,
    modifier: Modifier = Modifier
) {
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f

    Surface(
        color = if (isLightMode) LightSurface else DarkSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Text(
                text = "This Month",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Primary metric: completion % + trend chip ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${analytics.completionPercent}%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (analytics.totalScheduled > 0)
                            "${analytics.totalCompleted} of ${analytics.totalScheduled} completed"
                        else
                            "No habits scheduled yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Trend chip — only visible when delta is meaningful (>= 5%)
                if (abs(analytics.trendVsPreviousMonth) >= 5) {
                    val trendPositive = analytics.trendVsPreviousMonth > 0
                    val trendText = if (trendPositive)
                        "↑ +${analytics.trendVsPreviousMonth}%"
                    else
                        "↓ ${analytics.trendVsPreviousMonth}%"
                    val trendColor = if (trendPositive)
                        SuccessGreen
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                    val chipBg = if (isLightMode) LightSurfaceVariant else DarkSurfaceVariant

                    Surface(
                        color = chipBg,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = trendText,
                            style = MaterialTheme.typography.labelSmall,
                            color = trendColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ── Divider + secondary metrics (only with substantive data) ──────
            if (analytics.strongestHabitName != null || analytics.mostMissedWeekday != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Strongest habit
                    analytics.strongestHabitName?.let { habitName ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Strongest",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(68.dp)
                            )
                            Text(
                                text = habitName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Most missed weekday — phrased gently as "Quieter on"
                    analytics.mostMissedWeekday?.let { day ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quieter on",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(68.dp)
                            )
                            Text(
                                text = "${day}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // ── Reflection summary — the emotional anchor ─────────────────────
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = analytics.reflectionSummary,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

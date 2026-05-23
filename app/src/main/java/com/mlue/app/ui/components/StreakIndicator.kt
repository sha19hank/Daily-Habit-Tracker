package com.mlue.app.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StreakIndicator(current: Int, longest: Int) {
    val animatedCurrent by animateIntAsState(targetValue = current, label = "currentStreak")
    val animatedLongest by animateIntAsState(targetValue = longest, label = "longestStreak")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Current: $animatedCurrent",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Longest: $animatedLongest",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

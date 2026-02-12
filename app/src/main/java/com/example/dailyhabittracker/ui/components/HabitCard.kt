package com.example.dailyhabittracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dailyhabittracker.data.HabitEntity
import java.time.LocalDate

@Composable
fun HabitCard(
    habit: HabitEntity,
    today: LocalDate,
    isScheduledToday: Boolean,
    canRestore: Boolean,
    stepSupported: Boolean,
    stepsToday: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onCompleted: () -> Unit,
    onRestore: () -> Unit,
    onTogglePause: () -> Unit,
    onEnableReminder: () -> Unit,
    onDisableReminder: () -> Unit
) {
    val isCompletedToday = habit.lastCompletedDate == today
    val isBroken = habit.lastCompletedDate != null && habit.lastCompletedDate.isBefore(today.minusDays(1))
    val isPaused = habit.paused

    val baseTint = if (habit.color != 0) Color(habit.color) else MaterialTheme.colorScheme.surface
    val containerColor by animateColorAsState(
        targetValue = when {
            isCompletedToday -> baseTint.copy(alpha = 0.12f)
            habit.color != 0 -> baseTint.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 200),
        label = "cardColor"
    )
    val interactionSource = MutableInteractionSource()
    val pressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = when {
            pressed -> 4.dp
            isCompletedToday -> 6.dp
            else -> 2.dp
        },
        animationSpec = tween(durationMillis = 180),
        label = "cardElevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isCompletedToday) 1.01f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .animateContentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box {
            if (habit.color != 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .align(Alignment.CenterStart)
                        .padding(vertical = 12.dp)
                        .background(Color(habit.color))
                )
            }
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!habit.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = habit.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!habit.category.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = habit.category!!,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (isPaused) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text("Paused") }
                            )
                        }
                    }
                    Checkbox(
                        checked = isCompletedToday,
                        enabled = !isPaused && isScheduledToday,
                        onCheckedChange = { if (!isCompletedToday) onCompleted() },
                        modifier = Modifier.semantics {
                            contentDescription = if (isCompletedToday) "Completed" else "Mark complete"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                StreakIndicator(current = habit.currentStreak, longest = habit.longestStreak)

                AnimatedVisibility(
                    visible = !isScheduledToday && !isPaused,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Not scheduled today",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onTogglePause) {
                        Text(text = if (isPaused) "Resume" else "Pause")
                    }
                    if (habit.reminderEnabled) {
                        OutlinedButton(onClick = onDisableReminder) {
                            Text(text = "Disable reminder")
                        }
                    } else {
                        OutlinedButton(onClick = onEnableReminder) {
                            Text(text = "Enable reminder")
                        }
                    }
                }

                AnimatedVisibility(
                    visible = habit.reminderEnabled && habit.reminderTime != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reminder: ${habit.reminderTime}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (habit.stepEnabled && habit.stepGoal != null) {
                Spacer(modifier = Modifier.height(16.dp))
                if (!stepSupported) {
                    Text(
                        text = "Step tracking not supported on this device",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val progress by animateFloatAsState(
                        targetValue = (stepsToday.toFloat() / habit.stepGoal).coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 180),
                        label = "stepProgress"
                    )
                    Text(
                        text = "Steps: $stepsToday / ${habit.stepGoal}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxSize()
                        ) {}
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(8.dp)
                        ) {}
                    }
                }
            }

            if (isBroken) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRestore, enabled = canRestore) {
                        Text(text = "Restore Streak")
                    }
                    if (!isCompletedToday) {
                        Button(onClick = onCompleted) {
                            Text(text = "Complete Today")
                        }
                    }
                }
            }
            }
        }
    }
}

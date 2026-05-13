package com.example.dailyhabittracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    canFreeze: Boolean,
    stepSupported: Boolean,
    stepsToday: Int,
    modifier: Modifier = Modifier,
    goalTitle: String? = null,
    onEdit: () -> Unit,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit,
    onFreeze: () -> Unit,
    onTogglePause: () -> Unit,
    onEnableReminder: () -> Unit,
    onDisableReminder: () -> Unit
) {
    val isCompletedToday = habit.lastCompletedDate == today
    val isPaused = habit.paused
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
    val interactionSource = remember { MutableInteractionSource() }
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
        targetValue = when {
            pressed -> 0.96f
            else -> 1f
        },
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.6f, 
            stiffness = 300f
        ),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 200f))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material.ripple.rememberRipple(),
                onClick = { expanded = !expanded }
            ),
        shape = MaterialTheme.shapes.large,
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
            Column(modifier = Modifier.padding(20.dp)) {
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
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Streak: ${habit.currentStreak}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (goalTitle != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.SuggestionChip(
                                onClick = { },
                                label = { Text("🎯 $goalTitle", style = MaterialTheme.typography.labelSmall) },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                ),
                                colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = if (pressed) 0.9f else 1f
                                scaleY = if (pressed) 0.9f else 1f
                            }
                    ) {
                        Checkbox(
                        checked = isCompletedToday,
                        enabled = !isPaused && isScheduledToday,
                        onCheckedChange = { onToggleCompletion() },
                        modifier = Modifier.semantics {
                            contentDescription = if (isCompletedToday) "Completed" else "Mark complete"
                        }
                    )
                    }
                }

                if (expanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Longest streak: ${habit.longestStreak}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onTogglePause) {
                            Text(text = if (isPaused) "Resume" else "Pause")
                        }
                        Button(onClick = onFreeze, enabled = canFreeze) {
                            Text(text = "Freeze")
                        }
                        OutlinedButton(onClick = onEdit) {
                            Text(text = "Edit")
                        }
                        androidx.compose.material3.IconButton(onClick = { showDeleteDialog = true }) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Habit",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (habit.reminderEnabled) {
                        Text(
                            text = "Reminder: ${habit.reminderTime ?: "Not set"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onDisableReminder) {
                            Text(text = "Disable reminder")
                        }
                    } else {
                        OutlinedButton(onClick = onEnableReminder) {
                            Text(text = "Enable reminder")
                        }
                    }

                    if (habit.stepEnabled && habit.stepGoal != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!stepSupported) {
                            Text(
                                text = "Step tracking not supported",
                                style = MaterialTheme.typography.bodySmall,
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
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
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
                                        .height(6.dp)
                                ) {}
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Habit?") },
            text = { Text("Are you sure you want to delete '${habit.name}'? All history and completions will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

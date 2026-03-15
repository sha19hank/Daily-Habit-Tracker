package com.example.dailyhabittracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dailyhabittracker.data.HabitEntity
import com.example.dailyhabittracker.viewmodel.HabitHistory
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailSheet(
    habit: HabitEntity,
    history: HabitHistory?,
    stepsToday: Int,
    stepSupported: Boolean,
    canRestore: Boolean,
    onDismiss: () -> Unit,
    onTogglePause: () -> Unit,
    onRestore: () -> Unit,
    onEnableReminder: () -> Unit,
    onDisableReminder: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = habit.name, style = MaterialTheme.typography.titleLarge)

            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Current streak: ${habit.currentStreak}", color = MaterialTheme.colorScheme.secondary)
                    Text(text = "Longest streak: ${habit.longestStreak}")
                    Text(
                        text = "Last completed: ${habit.lastCompletedDate?.format(DateTimeFormatter.ISO_DATE) ?: "Not yet"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Completion history", style = MaterialTheme.typography.titleMedium)
                    Text(text = "This week: ${history?.weekCount ?: 0} completions")
                    Text(text = "This month: ${history?.monthCount ?: 0} completions")
                }
            }

            if (habit.stepEnabled && habit.stepGoal != null) {
                Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Step progress", style = MaterialTheme.typography.titleMedium)
                        if (!stepSupported) {
                            Text(
                                text = "Step tracking not supported on this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(text = "Today: $stepsToday / ${habit.stepGoal}")
                        }
                    }
                }
            }

            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (habit.paused) "Paused" else "Active")
                    Switch(checked = habit.paused, onCheckedChange = { onTogglePause() })
                }
            }

            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Streak recovery", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (canRestore) "Use a token to restore your streak." else "No tokens available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onRestore, enabled = canRestore) {
                        Text("Restore streak")
                    }
                }
            }

            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Reminder", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = habit.reminderTime?.toString() ?: "Not set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (habit.reminderEnabled) {
                        OutlinedButton(onClick = onDisableReminder) {
                            Text("Disable reminder")
                        }
                    } else {
                        OutlinedButton(onClick = onEnableReminder) {
                            Text("Enable reminder")
                        }
                    }
                }
            }

            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Reflection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = reflectionMessage(habit.currentStreak),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun reflectionMessage(streak: Int): String {
    return when {
        streak >= 14 -> "Your consistency is quietly becoming a strength."
        streak >= 7 -> "A steady week builds calm momentum."
        streak >= 1 -> "Small steps, repeated, become meaningful."
        else -> "Today is a gentle place to begin again."
    }
}

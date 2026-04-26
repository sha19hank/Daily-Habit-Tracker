package com.example.dailyhabittracker.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dailyhabittracker.R
import com.example.dailyhabittracker.ui.components.HabitCard
import com.example.dailyhabittracker.viewmodel.HabitViewModel
import com.example.dailyhabittracker.viewmodel.SortOption
import java.time.LocalDate
import java.time.LocalTime

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    navController: NavController,
    viewModel: HabitViewModel
) {
    val habits by viewModel.habits.collectAsState()
    val tokenCount by viewModel.tokenCount.collectAsState()
    val stepState by viewModel.stepState.collectAsState()
    val focusMode by viewModel.focusModeEnabled.collectAsState()
    val haptics by viewModel.hapticsEnabled.collectAsState()
    val activeGoal by viewModel.activeGoal.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val today = LocalDate.now()
    var sortExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var quickNote by rememberSaveable { mutableStateOf("") }

    val displayHabits by remember(habits, focusMode) {
        derivedStateOf {
            if (!focusMode) habits else habits.filter { viewModel.isScheduledToday(it) }
        }
    }
    val scheduledTodayCount by remember(displayHabits, today) {
        derivedStateOf { displayHabits.count { !it.paused && viewModel.isScheduledToday(it) } }
    }
    val completedTodayCount by remember(displayHabits, today) {
        derivedStateOf { displayHabits.count { it.lastCompletedDate == today && viewModel.isScheduledToday(it) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Habit Tracker") },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = focusMode,
                            onClick = { viewModel.setFocusMode(!focusMode) },
                            label = { Text("Focus") }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Tokens"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = tokenCount.toString())
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = listState,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "loaded") {
                Text(
                    text = "App Loaded",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "progress") {
                Surface(tonalElevation = 1.dp, shape = androidx.compose.material3.MaterialTheme.shapes.medium) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Daily progress",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$completedTodayCount / $scheduledTodayCount habits completed",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(contentAlignment = Alignment.Center) {
                            val progress = if (scheduledTodayCount == 0) 0f
                            else completedTodayCount.toFloat() / scheduledTodayCount.toFloat()
                            CircularProgressIndicator(progress = { progress }, strokeWidth = 6.dp)
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }



            activeGoal?.let { active ->
                item(key = "activeGoal") {
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🎯 Active Goal",
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = active.goal.title,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Deadline: ${active.goal.deadline ?: "None"}",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${active.progressDetails.overallPercent}%",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { active.progressDetails.overallPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                trackColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
            }

            item(key = "habitsHeader") {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's habits",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                    if (!focusMode) {
                        IconButton(onClick = { sortExpanded = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("By name") },
                                onClick = {
                                    viewModel.updateSort(SortOption.NAME)
                                    sortExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("By streak") },
                                onClick = {
                                    viewModel.updateSort(SortOption.STREAK)
                                    sortExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("By last completed") },
                                onClick = {
                                    viewModel.updateSort(SortOption.LAST_COMPLETED)
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (displayHabits.isEmpty()) {
                item(key = "empty") {
                    Surface(tonalElevation = 0.dp) {
                        Text(
                            text = "Consistency begins with one small action.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(displayHabits, key = { it.id }, contentType = { "habit" }) { habit ->
                val goalTitle = remember(habit.goalId, goals) {
                    goals.firstOrNull { it.goalId == habit.goalId }?.title
                }
                HabitCard(
                    habit = habit,
                    today = today,
                    isScheduledToday = viewModel.isScheduledToday(habit),
                    canFreeze = tokenCount > 0 && habit.currentStreak > 0,
                    stepSupported = stepState.supported,
                    stepsToday = stepState.stepsToday,
                    goalTitle = goalTitle,
                    onEdit = {
                        navController.navigate("add?habitId=${habit.id}")
                    },
                    onCompleted = {
                        if (haptics) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        viewModel.markCompleted(habit)
                    },
                    onFreeze = {
                        viewModel.tryBuyStreakFreeze(habit) { frozen ->
                            if (!frozen) {
                                scope.launch { snackbarHostState.showSnackbar("Could not freeze streak") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Streak frozen for 1 missed day!") }
                            }
                        }
                    },
                    onTogglePause = { viewModel.togglePaused(habit) },
                    onEnableReminder = {
                        val initial = habit.reminderTime ?: LocalTime.of(9, 0)
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                viewModel.updateReminder(habit, true, LocalTime.of(hour, minute))
                            },
                            initial.hour,
                            initial.minute,
                            false
                        ).show()
                    },
                    onDisableReminder = { viewModel.updateReminder(habit, false, null) }
                )
            }

            if (!focusMode) {
                item(key = "reflection") {
                    val line = remember(today) {
                        reflectionLines[today.dayOfYear % reflectionLines.size]
                    }
                    Text(
                        text = line,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                item(key = "quickJournal") {
                    Surface(tonalElevation = 1.dp, shape = androidx.compose.material3.MaterialTheme.shapes.medium) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Quick journal entry",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )
                            OutlinedTextField(
                                value = quickNote,
                                onValueChange = { quickNote = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("What stood out today?") },
                                minLines = 3
                            )
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        val text = quickNote.trim()
                                        if (text.isNotEmpty()) {
                                            viewModel.upsertJournalEntry(
                                                title = "Today",
                                                body = text,
                                                date = today,
                                                mood = null
                                            )
                                            quickNote = ""
                                        }
                                    }
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

private val reflectionLines = listOf(
    "Consistency begins quietly.",
    "Today is a fresh opportunity.",
    "Small steps compound.",
    "Progress is built daily."
)

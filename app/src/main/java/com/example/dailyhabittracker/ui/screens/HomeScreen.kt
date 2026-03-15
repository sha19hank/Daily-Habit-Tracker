package com.example.dailyhabittracker.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dailyhabittracker.R
import com.example.dailyhabittracker.ads.AdManager
import com.example.dailyhabittracker.ui.components.HabitCard
import com.example.dailyhabittracker.ui.components.Wordmark
import com.example.dailyhabittracker.ui.screens.HabitDetailSheet
import com.example.dailyhabittracker.viewmodel.HabitViewModel
import com.example.dailyhabittracker.viewmodel.SortOption
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import java.time.LocalDate
import java.time.LocalTime

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    navController: NavController,
    viewModel: HabitViewModel,
    adManager: AdManager
) {
    val habits by viewModel.habits.collectAsState()
    val tokenCount by viewModel.tokenCount.collectAsState()
    val stepState by viewModel.stepState.collectAsState()
    val focusMode by viewModel.focusModeEnabled.collectAsState()
    val haptics by viewModel.hapticsEnabled.collectAsState()
    val activeGoal by viewModel.activeGoal.collectAsState()
    val habitHistory by viewModel.habitHistory.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val today = LocalDate.now()
    var sortExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var selectedHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
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
                title = { Wordmark() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add") }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!focusMode) {
                BannerAd(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    adManager = adManager,
                    onFailed = { message ->
                        scope.launch { snackbarHostState.showSnackbar("Ad failed: $message") }
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = listState,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                            CircularProgressIndicator(progress = progress, strokeWidth = 6.dp)
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            if (!focusMode) {
                item(key = "tokens") {
                    Surface(tonalElevation = 1.dp, shape = androidx.compose.material3.MaterialTheme.shapes.medium) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "Recovery tokens")
                            Text(
                                text = tokenCount.toString(),
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Earned at 7, 30, and 100-day streaks.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
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
                            Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort")
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
                HabitCard(
                    habit = habit,
                    today = today,
                    isScheduledToday = viewModel.isScheduledToday(habit),
                    canRestore = tokenCount > 0,
                    stepSupported = stepState.supported,
                    stepsToday = stepState.stepsToday,
                    onClick = {
                        selectedHabitId = habit.id
                        viewModel.loadHabitHistory(habit.id)
                    },
                    onCompleted = {
                        if (haptics) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        viewModel.markCompleted(habit)
                    },
                    onRestore = {
                        viewModel.tryRestoreStreak(habit) { restored ->
                            if (!restored) {
                                scope.launch { snackbarHostState.showSnackbar("No tokens available") }
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

                if (activeGoal != null) {
                    item(key = "activeGoal") {
                        Surface(tonalElevation = 1.dp, shape = androidx.compose.material3.MaterialTheme.shapes.medium) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Active goal",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = activeGoal!!.goal.title,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                )
                                if (!activeGoal!!.goal.description.isNullOrBlank()) {
                                    Text(
                                        text = activeGoal!!.goal.description!!,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Progress: ${activeGoal!!.progressPercent}%",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                                )
                                if (activeGoal!!.goal.deadline != null) {
                                    Text(
                                        text = "Deadline: ${activeGoal!!.goal.deadline}",
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
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

    val selectedHabit = displayHabits.firstOrNull { it.id == selectedHabitId }
    if (selectedHabit != null) {
        HabitDetailSheet(
            habit = selectedHabit,
            history = habitHistory[selectedHabit.id],
            stepsToday = stepState.stepsToday,
            stepSupported = stepState.supported,
            onDismiss = { selectedHabitId = null },
            onTogglePause = { viewModel.togglePaused(selectedHabit) }
        )
    }
}

@Composable
private fun BannerAd(
    modifier: Modifier,
    adManager: AdManager,
    onFailed: (String) -> Unit
) {
    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = context.getString(R.string.admob_banner_id)
            adManager.loadBanner(this, onFailed)
        }
    }

    DisposableEffect(Unit) {
        onDispose { adView.destroy() }
    }
    Surface(
        tonalElevation = 0.dp,
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        shape = androidx.compose.material3.MaterialTheme.shapes.medium
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = modifier,
            factory = { adView }
        )
    }
}

private val reflectionLines = listOf(
    "Consistency begins quietly.",
    "Today is a fresh opportunity.",
    "Small steps compound.",
    "Progress is built daily."
)

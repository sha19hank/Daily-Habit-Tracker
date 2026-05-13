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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
    val goalProgress by viewModel.goalProgress.collectAsState()
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

    val carouselGoals by remember(goals, goalProgress) {
        derivedStateOf {
            val active = goals.filter { goal ->
                val progress = goalProgress[goal.goalId]?.overallPercent ?: 0
                progress < 100
            }
            val completed = goals.filter { goal ->
                val progress = goalProgress[goal.goalId]?.overallPercent ?: 0
                progress == 100
            }
            active + completed
        }
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
                // Cinematic Hero Card
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth().clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = androidx.compose.material.ripple.rememberRipple(),
                        onClick = { }
                    ),
                    shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your Progress",
                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (scheduledTodayCount == 0) "No habits today" else if (completedTodayCount == scheduledTodayCount) "Perfect Day!" else "Keep Going",
                                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "$completedTodayCount of $scheduledTodayCount completed",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(contentAlignment = Alignment.Center) {
                            val progress = if (scheduledTodayCount == 0) 0f
                            else completedTodayCount.toFloat() / scheduledTodayCount.toFloat()
                            val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = progress,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                ),
                                label = "progress"
                            )
                            
                            // Ambient Glow behind progress
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.size(80.dp),
                                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 8.dp,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(80.dp),
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                strokeWidth = 8.dp,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }



            if (carouselGoals.isNotEmpty()) {
                item(key = "activeGoalPager") {
                    val pagerState = rememberPagerState(pageCount = { carouselGoals.size })
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pageSpacing = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val goal = carouselGoals[page]
                        val progressDetails = goalProgress[goal.goalId] ?: com.example.dailyhabittracker.data.GoalProgressDetails(0, emptyList())
                        
                        // Calculate parallax scale
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val scale = 1f - (kotlin.math.abs(pageOffset) * 0.1f).coerceIn(0f, 0.1f)
                        val alpha = 1f - (kotlin.math.abs(pageOffset) * 0.4f).coerceIn(0f, 0.4f)
                        
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                                .clickable { navController.navigate("insights?goalId=${goal.goalId}") },
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ),
                            shape = androidx.compose.material3.MaterialTheme.shapes.large,
                            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = if (progressDetails.overallPercent == 100) "🏆 Completed Goal" else "🎯 Active Goal",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = goal.title,
                                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Deadline: ${goal.deadline ?: "None"}",
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "${progressDetails.overallPercent}%",
                                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val animatedGoalProgress by androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = progressDetails.overallPercent / 100f,
                                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 100f),
                                    label = "goalProgress"
                                )
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { animatedGoalProgress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                                    trackColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            } else {
                item(key = "activeGoalEmpty") {
                    Surface(
                        color = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = androidx.compose.material3.MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No active goals yet",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Button(onClick = { navController.navigate("insights") }) {
                                Text("Create Goal")
                            }
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
                    onToggleCompletion = {
                        if (haptics) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        viewModel.toggleHabitCompletion(habit)
                    },
                    onDelete = {
                        viewModel.deleteHabit(habit)
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

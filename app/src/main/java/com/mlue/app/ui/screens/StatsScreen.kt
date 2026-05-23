package com.mlue.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mlue.app.viewmodel.HabitViewModel
import com.mlue.app.viewmodel.DailyStats
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.navigationBarsPadding
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: HabitViewModel,
    highlightGoalId: Long? = null,
    openDialogRequest: Boolean = false,
    onDialogRequestConsumed: () -> Unit = {}
) {
    val weeklySummary by viewModel.weeklySummary.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val monthlyCount by viewModel.monthlyCount.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val focusMode by viewModel.focusModeEnabled.collectAsState()
    val activeGoal by viewModel.activeGoal.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val goalProgress by viewModel.goalProgress.collectAsState()

    val context = LocalContext.current
    var showGoalEditor by remember { mutableStateOf(false) }
    var editingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var goalTitle by rememberSaveable { mutableStateOf("") }
    var goalDescription by rememberSaveable { mutableStateOf("") }
    var goalStartDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var goalDeadline by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var goalToDelete by remember { mutableStateOf<com.mlue.app.data.GoalEntity?>(null) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(highlightGoalId, goals) {
        if (highlightGoalId != null && goals.any { it.goalId == highlightGoalId }) {
            kotlinx.coroutines.delay(300)
            bringIntoViewRequester.bringIntoView()
        }
    }


    LaunchedEffect(openDialogRequest) {
        if (openDialogRequest) {
            editingGoalId = null
            goalTitle = ""
            goalDescription = ""
            goalStartDate = LocalDate.now()
            goalDeadline = null
            showGoalEditor = true
            onDialogRequestConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights") }
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isLightStats = MaterialTheme.colorScheme.background.luminance() > 0.5f
            // Explicit static token — tonalElevation alone triggers Material You tint on OEMs
            Surface(
                color = if (isLightStats) com.mlue.app.ui.theme.LightSurfaceVariant
                        else com.mlue.app.ui.theme.DarkSurface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Goals", style = MaterialTheme.typography.titleMedium)
                    if (goals.isEmpty()) {
                        Text(
                            text = "Patterns appear with time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                editingGoalId = null
                                goalTitle = ""
                                goalDescription = ""
                                goalStartDate = LocalDate.now()
                                goalDeadline = null
                                showGoalEditor = true
                            }
                        ) {
                            Text("Create Goal")
                        }
                    } else {
                        goals.forEach { goal ->
                            val isHighlighted = highlightGoalId == goal.goalId
                            val isLightStats2 = MaterialTheme.colorScheme.background.luminance() > 0.5f
                            Surface(
                                // Highlighted: primaryContainer; normal: static surface token
                                color = if (isHighlighted)
                                    MaterialTheme.colorScheme.primaryContainer
                                else if (isLightStats2)
                                    com.mlue.app.ui.theme.LightSurface
                                else
                                    com.mlue.app.ui.theme.DarkSurface,
                                shape = MaterialTheme.shapes.small,
                                border = if (isHighlighted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .then(if (isHighlighted) Modifier.bringIntoViewRequester(bringIntoViewRequester) else Modifier)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = goal.title, style = MaterialTheme.typography.titleMedium)
                                            val deadline = goal.deadline?.toString() ?: "No deadline"
                                            Text(
                                                text = "Deadline: $deadline",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row {
                                            OutlinedButton(
                                                onClick = {
                                                    editingGoalId = goal.goalId
                                                    goalTitle = goal.title
                                                    goalDescription = goal.description ?: ""
                                                    goalStartDate = goal.startDate
                                                    goalDeadline = goal.deadline
                                                    showGoalEditor = true
                                                }
                                            ) {
                                                Text("Edit")
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(onClick = { goalToDelete = goal }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Goal",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    val progressDetails = goalProgress[goal.goalId]
                                    val overallPercent = progressDetails?.overallPercent ?: 0
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Overall Progress",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "$overallPercent%",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    val animatedProgress by animateFloatAsState(targetValue = overallPercent / 100f, label = "goalProgress")
                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp)
                                    )

                                    if (progressDetails != null && progressDetails.habitProgresses.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Linked Habits",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            progressDetails.habitProgresses.forEach { hp ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = hp.habit.name,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    Text(
                                                        text = "${hp.completed}/${hp.expected} (${hp.percent}%)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        val isLightStatsMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
                                        // Explicit static tokens — removes banned secondaryContainer + alpha bleed
                                        Surface(
                                            color = if (isLightStatsMode)
                                                com.mlue.app.ui.theme.LightSurfaceVariant
                                            else
                                                com.mlue.app.ui.theme.DarkSurfaceVariant,
                                            shape = MaterialTheme.shapes.small,
                                            border = if (isLightStatsMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            Text(
                                                text = "No habits linked. Edit a habit on the Home screen to connect it to this goal.",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(8.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }



            if (weeklySummary.isEmpty() && monthlyCount == 0) {
                Text(
                    text = "Patterns emerge with consistency.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val isLightStats = MaterialTheme.colorScheme.background.luminance() > 0.5f
                Text(text = "Weekly Summary", style = MaterialTheme.typography.titleLarge)
                // Explicit containerColor — tonalElevation alone can bleed Material You tint on OEMs
                Surface(
                    color = if (isLightStats) com.mlue.app.ui.theme.LightSurfaceVariant
                            else com.mlue.app.ui.theme.DarkSurface,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        WeeklyBars(weeklyStats = weeklyStats)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (isLightStats) com.mlue.app.ui.theme.LightSurfaceVariant
                            else com.mlue.app.ui.theme.DarkSurface,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Monthly completions")
                        Text(text = monthlyCount.toString(), style = MaterialTheme.typography.titleLarge)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (isLightStats) com.mlue.app.ui.theme.LightSurfaceVariant
                            else com.mlue.app.ui.theme.DarkSurface,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val totalScheduled = weeklyStats.sumOf { it.scheduled }
                        val totalCompleted = weeklyStats.sumOf { it.completed }
                        val weekRate = if (totalScheduled > 0)
                            (totalCompleted * 100) / totalScheduled else 0
                        Text(text = "Weekly review", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "$totalCompleted of $totalScheduled scheduled habits completed ($weekRate%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Monthly completions to date: $monthlyCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!focusMode && insights.isNotEmpty()) {
                Surface(
                    color = if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
                        com.mlue.app.ui.theme.LightSurfaceVariant
                    else
                        com.mlue.app.ui.theme.DarkSurface,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Insights", style = MaterialTheme.typography.titleMedium)
                        insights.forEach { insight ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "·",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = insight,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGoalEditor) {
        AlertDialog(
            onDismissRequest = { showGoalEditor = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (goalTitle.isNotBlank()) {
                            if (editingGoalId == null) {
                                viewModel.addGoal(
                                    title = goalTitle,
                                    description = goalDescription.ifBlank { null },
                                    startDate = goalStartDate,
                                    deadline = goalDeadline
                                ) { newGoalId ->
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Goal created successfully!",
                                            actionLabel = "Add Habit",
                                            duration = androidx.compose.material3.SnackbarDuration.Long
                                        )
                                        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            navController.navigate("add?goalId=$newGoalId") {
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                }
                            } else {
                                val existingGoal = goals.firstOrNull { it.goalId == editingGoalId }
                                if (existingGoal != null) {
                                    viewModel.updateGoal(
                                        existingGoal.copy(
                                            title = goalTitle,
                                            description = goalDescription.ifBlank { null },
                                            startDate = goalStartDate,
                                            deadline = goalDeadline
                                        )
                                    )
                                }
                            }
                            showGoalEditor = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalEditor = false }) { Text("Cancel") }
            },
            title = { Text(if (editingGoalId == null) "New goal" else "Edit goal") },
            text = {
                val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
                // Static opaque tokens — surfaceVariant.copy(alpha) is a documented OEM regression trigger
                val fieldFillColor = if (isLightMode)
                    Color(0xFFFBF8F4)
                else
                    com.mlue.app.ui.theme.DarkSurfaceVariant
                val fieldFocusedFillColor = if (isLightMode)
                    Color(0xFFFBF8F4)
                else
                    com.mlue.app.ui.theme.DarkSurface
                val fieldShape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)

                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = fieldFillColor,
                    focusedContainerColor = fieldFocusedFillColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        shape = fieldShape
                    )
                    OutlinedTextField(
                        value = goalDescription,
                        onValueChange = { goalDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        shape = fieldShape
                    )
                    TextButton(
                        onClick = {
                            val current = goalStartDate
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    goalStartDate = LocalDate.of(year, month + 1, dayOfMonth)
                                },
                                current.year,
                                current.monthValue - 1,
                                current.dayOfMonth
                            ).show()
                        }
                    ) {
                        Text("Start: $goalStartDate")
                    }
                    TextButton(
                        onClick = {
                            val current = goalDeadline ?: LocalDate.now()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    goalDeadline = LocalDate.of(year, month + 1, dayOfMonth)
                                },
                                current.year,
                                current.monthValue - 1,
                                current.dayOfMonth
                            ).show()
                        }
                    ) {
                        Text("Deadline: ${goalDeadline ?: "None"}")
                    }
                    if (goalDeadline != null) {
                        TextButton(onClick = { goalDeadline = null }) {
                            Text("Clear deadline")
                        }
                    }
                }
            }
        )
    }

    if (goalToDelete != null) {
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Delete Goal?") },
            text = { Text("Are you sure you want to delete '${goalToDelete!!.title}'? Your habits will remain, but they will be disconnected from this goal.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goalToDelete!!)
                        goalToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WeeklyBars(weeklyStats: List<DailyStats>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        weeklyStats.forEach { stats ->
            val fraction = stats.completionRate  // 0.0..1.0, division-by-zero safe
            val isToday = stats.date == LocalDate.now()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Day label
                Text(
                    text = stats.date.dayOfWeek.name.take(3),
                    modifier = Modifier.width(40.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Progress bar — always renders at a minimum 2% so empty days are visible
                val animatedFraction by animateFloatAsState(
                    targetValue = if (stats.scheduled == 0) 0f
                                  else fraction.coerceAtLeast(0.02f),
                    label = "weekBar_${stats.date}"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    if (stats.scheduled > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedFraction)
                                .height(14.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = if (fraction >= 1f)
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary
                                            )
                                        else
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                MaterialTheme.colorScheme.primary
                                            )
                                    ),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                // Completion fraction label
                Text(
                    text = if (stats.scheduled == 0) "—"
                           else "${stats.completed}/${stats.scheduled}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (fraction >= 1f) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}

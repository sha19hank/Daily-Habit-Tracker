package com.example.dailyhabittracker.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import com.example.dailyhabittracker.viewmodel.HabitViewModel
import androidx.compose.foundation.clickable
import java.time.LocalDate

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StatsScreen(
    navController: NavController,
    viewModel: HabitViewModel,
    openDialogRequest: Boolean = false,
    onDialogRequestConsumed: () -> Unit = {}
) {
    val weeklySummary by viewModel.weeklySummary.collectAsState()
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
    var goalToDelete by remember { mutableStateOf<com.example.dailyhabittracker.data.GoalEntity?>(null) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Goals", style = MaterialTheme.typography.titleMedium)
                    if (goals.isEmpty()) {
                        Text(
                            text = "No goals yet.",
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
                            Surface(
                                tonalElevation = 2.dp,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
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
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            Text(
                                                text = "No habits linked. Edit a habit on the Home screen to connect it to this goal.",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(8.dp),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
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
                    text = "Small habits compound.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(text = "Weekly Summary", style = MaterialTheme.typography.titleLarge)
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        WeeklyBars(weeklySummary = weeklySummary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    tonalElevation = 1.dp,
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
                Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val weeklyTotal = weeklySummary.sumOf { it.count }
                        Text(text = "Weekly review", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Total completions this week: $weeklyTotal",
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
                Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "Insights", style = MaterialTheme.typography.titleMedium)
                        insights.take(2).forEach { insight ->
                            Text(text = insight, style = MaterialTheme.typography.bodySmall)
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
                                            navController.navigate("add?goalId=$newGoalId")
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = goalDescription,
                        onValueChange = { goalDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
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
private fun WeeklyBars(weeklySummary: List<com.example.dailyhabittracker.data.DateCount>) {
    val today = LocalDate.now()
    val dates = remember(today) { (0..6).map { today.minusDays((6 - it).toLong()) } }
    val max = (weeklySummary.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        dates.forEach { date ->
            val count = weeklySummary.firstOrNull { it.date == date.toString() }?.count ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = date.dayOfWeek.name.take(3), modifier = Modifier.width(40.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .height(10.dp)
                        .fillMaxWidth(fraction = (count.toFloat() / max).coerceIn(0f, 1f))
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = count.toString())
            }
        }
    }
}

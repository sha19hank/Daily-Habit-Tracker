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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dailyhabittracker.viewmodel.HabitViewModel
import java.time.LocalDate

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StatsScreen(navController: NavController, viewModel: HabitViewModel) {
    val weeklySummary by viewModel.weeklySummary.collectAsState()
    val monthlyCount by viewModel.monthlyCount.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val focusMode by viewModel.focusModeEnabled.collectAsState()
    val activeGoal by viewModel.activeGoal.collectAsState()
    val goals by viewModel.goals.collectAsState()

    val context = LocalContext.current
    var showGoalEditor by remember { mutableStateOf(false) }
    var goalTitle by rememberSaveable { mutableStateOf("") }
    var goalDescription by rememberSaveable { mutableStateOf("") }
    var goalStartDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var goalDeadline by rememberSaveable { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                goalTitle = ""
                goalDescription = ""
                goalStartDate = LocalDate.now()
                goalDeadline = null
                showGoalEditor = true
            }) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add goal"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    } else {
                        goals.forEach { goal ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = goal.title, style = MaterialTheme.typography.bodyMedium)
                                val deadline = goal.deadline?.toString() ?: "No deadline"
                                Text(
                                    text = deadline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (activeGoal != null) {
                Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "Goal progress", style = MaterialTheme.typography.titleMedium)
                        Text(text = activeGoal!!.goal.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${activeGoal!!.progressPercent}% complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            viewModel.addGoal(
                                title = goalTitle,
                                description = goalDescription.ifBlank { null },
                                startDate = goalStartDate,
                                deadline = goalDeadline
                            )
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
            title = { Text("New goal") },
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

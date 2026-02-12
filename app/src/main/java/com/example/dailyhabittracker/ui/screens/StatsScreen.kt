package com.example.dailyhabittracker.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

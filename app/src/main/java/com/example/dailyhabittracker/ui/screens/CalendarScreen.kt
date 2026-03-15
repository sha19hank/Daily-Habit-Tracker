package com.example.dailyhabittracker.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dailyhabittracker.viewmodel.CalendarDayState
import com.example.dailyhabittracker.viewmodel.HabitViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController, viewModel: HabitViewModel) {
    val month by viewModel.calendarMonth.collectAsState()
    val days by viewModel.calendarDays.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val completedIds by viewModel.selectedDayCompletedHabitIds.collectAsState()
    val journalEntry by viewModel.selectedDayJournalEntry.collectAsState()
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    if (displayedMonth != month) {
        viewModel.loadCalendar(displayedMonth)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "${displayedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${displayedMonth.year}")
                    }
                },
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous month")
                }
                IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next month")
                }
            }
            Crossfade(
                targetState = displayedMonth,
                animationSpec = tween(durationMillis = 200),
                label = "monthFade"
            )
            { targetMonth ->
                CalendarMonthGrid(
                    month = targetMonth,
                    days = if (targetMonth == month) days else emptyList(),
                    onDaySelected = { date ->
                        selectedDate = date
                        viewModel.loadDayDetail(date)
                    }
                )
            }

            if (selectedDate != null) {
                val dateLabel = selectedDate.toString()
                val completedHabits = habits.filter { completedIds.contains(it.id) }
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Day details", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = "Completed habits", style = MaterialTheme.typography.labelMedium)
                        if (completedHabits.isEmpty()) {
                            Text(
                                text = "No completed habits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            completedHabits.forEach { habit ->
                                Text(text = habit.name, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(text = "Journal entry", style = MaterialTheme.typography.labelMedium)
                        if (journalEntry == null) {
                            Text(
                                text = "No journal entry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(text = journalEntry!!.title, style = MaterialTheme.typography.bodySmall)
                            if (journalEntry!!.body.isNotBlank()) {
                                Text(
                                    text = journalEntry!!.body,
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
}

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    days: List<CalendarDayState>,
    onDaySelected: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val startOffset = ((firstDay.dayOfWeek.value + 6) % 7)
    val totalDays = month.lengthOfMonth()
    val stateMap = days.associateBy { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (days.all { it.completedCount == 0 && it.scheduledCount == 0 }) {
            Text(
                text = "Today is a fresh start.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DayOfWeek.values().forEach { day ->
                Text(
                    text = day.name.take(3),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        val rows = (startOffset + totalDays + 6) / 7
        var index = -startOffset
        repeat(rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) {
                    index++
                    if (index in 1..totalDays) {
                        val date = month.atDay(index)
                        val state = stateMap[date]
                        CalendarDayCell(
                            date = date,
                            state = state,
                            modifier = Modifier.weight(1f),
                            onClick = { onDaySelected(date) }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(44.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    state: CalendarDayState?,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val completed = state?.completedCount ?: 0
    val missed = state?.missedScheduled ?: 0
    val background = when {
        completed > 0 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        missed > 0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    Surface(
        modifier = modifier
            .height(44.dp)
            .padding(2.dp)
            .clickable { onClick() },
        color = background,
        tonalElevation = 0.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall)
            if (completed > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(6.dp),
                    shape = MaterialTheme.shapes.small
                ) {}
            } else if (missed > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(6.dp),
                    shape = MaterialTheme.shapes.small
                ) {}
            }
        }
    }
}

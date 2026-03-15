package com.example.dailyhabittracker.ui.screens

import android.app.TimePickerDialog
import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dailyhabittracker.viewmodel.HabitViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.time.LocalTime

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
fun AddHabitScreen(navController: NavController, viewModel: HabitViewModel) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var selectedColor by remember { mutableStateOf(DEFAULT_COLORS.first()) }
    var stepEnabled by remember { mutableStateOf(false) }
    var stepGoal by remember { mutableStateOf("5000") }
    var showError by remember { mutableStateOf(false) }
    var goalExpanded by remember { mutableStateOf(false) }
    var selectedGoalId by remember { mutableStateOf<Long?>(null) }
    val goals by viewModel.goals.collectAsState()
    val context = LocalContext.current
    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        stepEnabled = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Habit") },
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
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (showError) showError = false
                },
                label = { Text("Habit name") },
                isError = showError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Goal (optional)", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            Button(onClick = { goalExpanded = true }) {
                val selectedLabel = goals.firstOrNull { it.goalId == selectedGoalId }?.title ?: "No goal"
                Text(text = selectedLabel)
            }
            DropdownMenu(expanded = goalExpanded, onDismissRequest = { goalExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("No goal") },
                    onClick = {
                        selectedGoalId = null
                        goalExpanded = false
                    }
                )
                goals.forEach { goal ->
                    DropdownMenuItem(
                        text = { Text(goal.title) },
                        onClick = {
                            selectedGoalId = goal.goalId
                            goalExpanded = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Schedule", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DAYS.forEach { (label, value) ->
                    val selected = selectedDays.contains(value)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedDays = if (selected) selectedDays - value else selectedDays + value
                        },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Color", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DEFAULT_COLORS.forEach { colorInt ->
                    val selected = selectedColor == colorInt
                    Surface(
                        color = Color(colorInt),
                        shape = androidx.compose.material3.MaterialTheme.shapes.small,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(2.dp)
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = androidx.compose.material3.MaterialTheme.shapes.small
                            )
                            .clickable { selectedColor = colorInt }
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Reminder")
                Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
            }
            if (reminderEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val dialog = TimePickerDialog(
                        context,
                        { _, hour, minute -> reminderTime = LocalTime.of(hour, minute) },
                        reminderTime.hour,
                        reminderTime.minute,
                        false
                    )
                    dialog.show()
                }) {
                    Text(text = "Reminder time: ${reminderTime}")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Step goal")
                Switch(checked = stepEnabled, onCheckedChange = { enabled ->
                    if (!enabled) {
                        stepEnabled = false
                        return@Switch
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val permission = Manifest.permission.ACTIVITY_RECOGNITION
                        val granted = ContextCompat.checkSelfPermission(context, permission) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            stepEnabled = true
                        } else {
                            activityRecognitionLauncher.launch(permission)
                        }
                    } else {
                        stepEnabled = true
                    }
                })
            }
            if (stepEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = stepGoal,
                    onValueChange = { stepGoal = it.filter { char -> char.isDigit() } },
                    label = { Text("Daily steps") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                if (name.isBlank()) {
                    showError = true
                } else {
                    viewModel.addHabit(
                        name = name,
                        description = description.ifBlank { null },
                        category = category.ifBlank { null },
                        color = selectedColor,
                        scheduledDays = selectedDays.toList().sorted(),
                        reminderEnabled = reminderEnabled,
                        reminderTime = if (reminderEnabled) reminderTime else null,
                        paused = false,
                        stepEnabled = stepEnabled,
                        stepGoal = stepGoal.toIntOrNull(),
                        goalId = selectedGoalId
                    )
                    navController.popBackStack()
                }
            }) {
                Text("Save Habit")
            }
        }
    }
}

private val DAYS = listOf(
    "Mon" to 1,
    "Tue" to 2,
    "Wed" to 3,
    "Thu" to 4,
    "Fri" to 5,
    "Sat" to 6,
    "Sun" to 7
)

private val DEFAULT_COLORS = listOf(
    0xFF3B82F6.toInt(),
    0xFF10B981.toInt(),
    0xFFF59E0B.toInt(),
    0xFFEF4444.toInt(),
    0xFF8B5CF6.toInt(),
    0xFF14B8A6.toInt()
)

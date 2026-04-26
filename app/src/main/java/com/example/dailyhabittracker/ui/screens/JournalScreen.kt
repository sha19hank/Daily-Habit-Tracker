package com.example.dailyhabittracker.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyhabittracker.viewmodel.HabitViewModel
import java.time.LocalDate

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun JournalScreen(
    viewModel: HabitViewModel,
    openDialogRequest: Boolean,
    onDialogRequestConsumed: () -> Unit
) {
    val entries by viewModel.journalEntries.collectAsState()
    val context = LocalContext.current

    var showDialog by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var mood by rememberSaveable { mutableStateOf("") }
    var entryDate by rememberSaveable { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(openDialogRequest) {
        if (openDialogRequest) {
            showDialog = true
            onDialogRequestConsumed()
        }
    }

    LaunchedEffect(showDialog) {
        if (showDialog) {
            title = ""
            body = ""
            mood = ""
            entryDate = LocalDate.now()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Journal") }) }
    ) { padding ->
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Capture a note for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
                            val timeString = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(entry.timestamp))
                            Text(
                                text = "${entry.date} at $timeString",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (entry.body.isNotBlank()) {
                                Text(text = entry.body, style = MaterialTheme.typography.bodySmall)
                            }
                            if (!entry.mood.isNullOrBlank()) {
                                val moodEmoji = when (entry.mood) {
                                    "RAD" -> "😁"
                                    "GOOD" -> "🙂"
                                    "MEH" -> "😐"
                                    "BAD" -> "🙁"
                                    "AWFUL" -> "😢"
                                    else -> entry.mood
                                }
                                Text(
                                    text = "Mood: $moodEmoji",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (body.isNotBlank()) {
                            viewModel.upsertJournalEntry(
                                title = if (title.isBlank()) "Journal" else title,
                                body = body,
                                date = entryDate,
                                mood = mood.ifBlank { null }
                            )
                            showDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
            title = { Text("New entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Body") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                    Text("Mood", style = MaterialTheme.typography.labelMedium)
                    val predefinedMoods = listOf("RAD" to "😁", "GOOD" to "🙂", "MEH" to "😐", "BAD" to "🙁", "AWFUL" to "😢")
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(predefinedMoods) { (moodName, emoji) ->
                            val isSelected = mood == moodName
                            FilterChip(
                                selected = isSelected,
                                onClick = { mood = if (isSelected) "" else moodName },
                                label = { Text(emoji, fontSize = 20.sp) }
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            val current = entryDate
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    entryDate = LocalDate.of(year, month + 1, dayOfMonth)
                                },
                                current.year,
                                current.monthValue - 1,
                                current.dayOfMonth
                            ).show()
                        }
                    ) {
                        Text("Date: ${entryDate}")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        )
    }
}

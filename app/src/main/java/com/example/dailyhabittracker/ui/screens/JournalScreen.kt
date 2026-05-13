package com.example.dailyhabittracker.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.example.dailyhabittracker.data.JournalEntryEntity
import com.example.dailyhabittracker.viewmodel.HabitViewModel
import java.time.LocalDate
import java.time.LocalTime

fun generateTitle(body: String, time: LocalTime): String {
    val firstSentence = body.split(Regex("[.!?\n]")).firstOrNull { it.isNotBlank() }?.trim()
    if (firstSentence != null) {
        return if (firstSentence.length > 30) firstSentence.take(27) + "..." else firstSentence
    }
    return when (time.hour) {
        in 5..11 -> "Morning Reflection"
        in 12..16 -> "Afternoon Thoughts"
        in 17..21 -> "Evening Reflection"
        else -> "Late Night Thoughts"
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun JournalScreen(
    viewModel: HabitViewModel,
    openDialogRequest: Boolean,
    onDialogRequestConsumed: () -> Unit
) {
    val entries by viewModel.journalEntries.collectAsState()
    
    var isSheetOpen by rememberSaveable { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<JournalEntryEntity?>(null) }
    var entryToDelete by remember { mutableStateOf<JournalEntryEntity?>(null) }

    LaunchedEffect(openDialogRequest) {
        if (openDialogRequest) {
            editingEntry = null
            isSheetOpen = true
            onDialogRequestConsumed()
        }
    }

    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val groupedEntries = remember(entries) {
        entries.groupBy { entry ->
            val date = entry.date
            when (date) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> "Older"
            }
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
                    text = "No reflections yet.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Capture a calm thought or a moment from your day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    editingEntry = null
                    isSheetOpen = true
                }) {
                    Text("Write your first entry")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val keys = listOf("Today", "Yesterday", "Older").filter { groupedEntries.containsKey(it) }
                keys.forEach { key ->
                    item(key = key) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(groupedEntries[key]!!, key = { it.id }) { entry ->
                        JournalCard(
                            entry = entry,
                            onClick = {
                                editingEntry = entry
                                isSheetOpen = true
                            },
                            onDeleteRequest = { entryToDelete = entry }
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = isSheetOpen,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 200f)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 200f))
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            JournalEditor(
                initialEntry = editingEntry,
                onSave = { title, body, date, mood ->
                    val finalTitle = title.ifBlank { generateTitle(body, LocalTime.now()) }
                    if (editingEntry != null) {
                        viewModel.updateJournalEntry(
                            editingEntry!!.copy(
                                title = finalTitle,
                                body = body,
                                date = date,
                                mood = mood
                            )
                        )
                    } else {
                        viewModel.upsertJournalEntry(finalTitle, body, date, mood)
                    }
                    isSheetOpen = false
                },
                onCancel = { isSheetOpen = false }
            )
        }
    }

    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Entry?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteJournalEntry(entryToDelete!!)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun JournalCard(
    entry: JournalEntryEntity,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material.ripple.rememberRipple(),
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
                    val timeString = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(entry.timestamp))
                    Text(
                        text = "${entry.date} at $timeString",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuExpanded = false
                                onDeleteRequest()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
            if (entry.body.isNotBlank()) {
                Text(
                    text = entry.body,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditor(
    initialEntry: JournalEntryEntity?,
    onSave: (String, String, LocalDate, String?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(initialEntry?.title ?: "") }
    var body by rememberSaveable { mutableStateOf(initialEntry?.body ?: "") }
    var mood by rememberSaveable { mutableStateOf(initialEntry?.mood ?: "") }
    var entryDate by remember { mutableStateOf(initialEntry?.date ?: LocalDate.now()) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Close") }
                },
                title = { Text(if (initialEntry != null) "Edit Entry" else "Reflect", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    TextButton(
                        onClick = {
                            if (body.isNotBlank()) {
                                onSave(title, body, entryDate, mood.ifBlank { null })
                            }
                        },
                        enabled = body.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title (Optional)", style = MaterialTheme.typography.titleLarge) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            
            TextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text("What's on your mind?", style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Mood", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp))
            val predefinedMoods = listOf("RAD" to "😁", "GOOD" to "🙂", "MEH" to "😐", "BAD" to "🙁", "AWFUL" to "😢")
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(predefinedMoods) { (moodName, emoji) ->
                    val isSelected = mood == moodName
                    FilterChip(
                        selected = isSelected,
                        onClick = { mood = if (isSelected) "" else moodName },
                        label = { Text(emoji, fontSize = 20.sp) },
                        border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = "Date: $entryDate", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

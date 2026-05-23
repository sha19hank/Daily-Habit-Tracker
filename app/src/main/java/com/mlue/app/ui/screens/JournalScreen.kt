package com.mlue.app.ui.screens

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import com.mlue.app.ui.theme.AppMotion
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.mlue.app.data.JournalEntryEntity
import com.mlue.app.viewmodel.HabitViewModel
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

    // Use remember (NOT rememberSaveable) so that editor state never leaks
    // across config changes with a stale entry reference.
    var isSheetOpen by remember { mutableStateOf(false) }
    // Snapshot the selected entry at open time — isolates state from live list
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
            when (entry.date) {
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
                    text = "Nothing captured yet.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to write your first reflection.",
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
                // 104dp bottom padding: dock(68) + dock-bottom-margin(20) + breathing room(16)
                contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val keys = listOf("Today", "Yesterday", "Older").filter { groupedEntries.containsKey(it) }
                keys.forEach { sectionKey ->
                    item(key = "header_$sectionKey") {
                        Text(
                            text = sectionKey,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(groupedEntries[sectionKey]!!, key = { it.id }) { entry ->
                        JournalCard(
                            entry = entry,
                            onClick = {
                                // Snapshot: copy entry data at click time to prevent live reference mutation
                                editingEntry = entry.copy()
                                isSheetOpen = true
                            },
                            onDeleteRequest = { entryToDelete = entry.copy() }
                        )
                    }
                }
            }
        }
    }

    // Editor — keyed on the editing entry's ID so state is fully reset per entry
    // Transition: fade + slide together for a layered-paper feel
    AnimatedVisibility(
        visible = isSheetOpen,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = AppMotion.panelSlideSpring()
        ) + fadeIn(animationSpec = AppMotion.floatTween(AppMotion.durationLong)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = AppMotion.panelSlideSpring()
        ) + fadeOut(animationSpec = AppMotion.floatTween(AppMotion.exitDuration))
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // key() forces full recomposition + state reset when editingEntry changes
            key(editingEntry?.id) {
                JournalEditor(
                    initialEntry = editingEntry,
                    onSave = { title, body, date, mood ->
                        val finalTitle = title.ifBlank { generateTitle(body, LocalTime.now()) }
                        val current = editingEntry
                        if (current != null) {
                            viewModel.updateJournalEntry(
                                current.copy(
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
                TextButton(onClick = { entryToDelete = null }) { Text("Cancel") }
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
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = AppMotion.pressBounce(),
        label = "scale_${entry.id}"
    )
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val moodEmoji = when (entry.mood) {
        "RAD" -> "😁"
        "GOOD" -> "🙂"
        "MEH" -> "😐"
        "BAD" -> "🙁"
        "AWFUL" -> "😢"
        else -> null
    }

    Surface(
        color = if (isLightMode) Color(0xFFFFFDF9) else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isLightMode) 0.dp else 1.dp,
        tonalElevation = if (isLightMode) 0.dp else 1.dp,
        shape = MaterialTheme.shapes.medium,
        border = if (isLightMode) androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        ) else null,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isLightMode) Modifier.shadow(
                    elevation = 2.dp,
                    shape = MaterialTheme.shapes.medium,
                    spotColor = Color(0xFF2A140A).copy(alpha = 0.08f),
                    ambientColor = Color(0xFF2A140A).copy(alpha = 0.04f)
                ) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material.ripple.rememberRipple(),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: title + timestamp + preview snippet
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Title row — title + mood emoji inline
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (moodEmoji != null) {
                        Text(text = moodEmoji, fontSize = 14.sp)
                    }
                }
                // Timestamp
                val timeString = java.text.SimpleDateFormat(
                    "hh:mm a",
                    java.util.Locale.getDefault()
                ).format(java.util.Date(entry.timestamp))
                Text(
                    text = "${ entry.date }  ·  $timeString",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Body preview — 1 line max
                if (entry.body.isNotBlank()) {
                    Text(
                        text = entry.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: overflow menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
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

    // Use plain remember (not rememberSaveable) + no explicit key here because
    // the parent key(editingEntry?.id) block already forces full recomposition
    // when entry identity changes — preventing any state bleed between entries.
    var title by remember { mutableStateOf(initialEntry?.title ?: "") }
    var body by remember { mutableStateOf(initialEntry?.body ?: "") }
    var mood by remember { mutableStateOf(initialEntry?.mood ?: "") }
    var entryDate by remember { mutableStateOf(initialEntry?.date ?: LocalDate.now()) }

    // Static opaque token for focused container — no alpha copy of Material-derived surfaceVariant
    val editorFocusedBg = if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
        com.mlue.app.ui.theme.LightSurfaceVariant
    else
        com.mlue.app.ui.theme.DarkSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                title = {
                    Text(
                        if (initialEntry != null) "Edit Entry" else "Reflect",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
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
                placeholder = {
                    Text("Title (Optional)", style = MaterialTheme.typography.titleLarge)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = editorFocusedBg,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    disabledIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            TextField(
                value = body,
                onValueChange = { body = it },
                placeholder = {
                    Text("What's on your mind?", style = MaterialTheme.typography.bodyLarge)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = editorFocusedBg,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Mood",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp)
            )
            val predefinedMoods = listOf(
                "RAD" to "😁", "GOOD" to "🙂", "MEH" to "😐", "BAD" to "🙁", "AWFUL" to "😢"
            )
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            Spacer(modifier = Modifier.height(8.dp))
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

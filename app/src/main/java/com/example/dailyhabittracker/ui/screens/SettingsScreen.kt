package com.example.dailyhabittracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dailyhabittracker.viewmodel.HabitViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(navController: NavController, viewModel: HabitViewModel) {
    val darkMode by viewModel.darkModeEnabled.collectAsState()
    val sounds by viewModel.soundsEnabled.collectAsState()
    val focusMode by viewModel.focusModeEnabled.collectAsState()
    val haptics by viewModel.hapticsEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            ListItem(
                headlineContent = { Text("Dark mode") },
                supportingContent = { Text("Use dark theme across the app") },
                trailingContent = {
                    Switch(checked = darkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                }
            )
            ListItem(
                headlineContent = { Text("Sounds") },
                supportingContent = { Text("Enable confirmation sounds") },
                trailingContent = {
                    Switch(checked = sounds, onCheckedChange = { viewModel.setSounds(it) })
                }
            )
            ListItem(
                headlineContent = { Text("Haptic feedback") },
                supportingContent = { Text("Subtle vibration on completion") },
                trailingContent = {
                    Switch(checked = haptics, onCheckedChange = { viewModel.setHaptics(it) })
                }
            )
            ListItem(
                headlineContent = { Text("Focus Mode") },
                supportingContent = { Text("Hide extras and show only today's habits") },
                trailingContent = {
                    Switch(checked = focusMode, onCheckedChange = { viewModel.setFocusMode(it) })
                }
            )
        }
    }
}

package com.mlue.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowMlueWorksScreen(
    navController: NavController,
    darkMode: Boolean
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How Mlue Works") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            
            EditorialSection(
                title = "Why Mlue exists",
                body = "Mlue is designed to help you build consistency gently over time. There are no harsh penalties or aggressive streaks. It simply reflects your effort."
            )

            EditorialSection(
                title = "Habits",
                body = "Habits form the core of your routine. You can create them, set optional reminders, and mark them complete each day. Consistency is tracked over time to reveal patterns."
            )

            EditorialSection(
                title = "Goals",
                body = "Goals group habits into larger outcomes.\n\nFor example:\nGoal: Sleep Better\nHabits:\n• Sleep before 12\n• No caffeine after 8\n• Wake up at 7"
            )

            EditorialSection(
                title = "Insights",
                body = "Insights become more meaningful as your routines develop. They reveal patterns, trends, and how consistently you show up for your habits."
            )

            EditorialSection(
                title = "Journal",
                body = "Journaling is entirely optional. It provides a soft space for reflection, helping to add emotional context to your daily habits."
            )

            EditorialSection(
                title = "Focus Mode",
                body = "Focus Mode prioritizes your most important habits by hiding background noise and gently sorting your daily view."
            )

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun EditorialSection(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 26.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}

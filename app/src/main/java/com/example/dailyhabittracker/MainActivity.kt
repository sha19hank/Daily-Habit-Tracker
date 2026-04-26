package com.example.dailyhabittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.dailyhabittracker.ui.screens.AddHabitScreen
import com.example.dailyhabittracker.ui.screens.CalendarScreen
import com.example.dailyhabittracker.ui.screens.HomeScreen
import com.example.dailyhabittracker.ui.screens.JournalScreen
import com.example.dailyhabittracker.ui.screens.SettingsScreen
import com.example.dailyhabittracker.ui.screens.StatsScreen
import com.example.dailyhabittracker.ui.theme.DailyHabitTrackerTheme
import com.example.dailyhabittracker.viewmodel.HabitViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy

class MainActivity : ComponentActivity() {
    private val viewModel: HabitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkMode by viewModel.darkModeEnabled.collectAsState()
            DailyHabitTrackerTheme(darkTheme = darkMode) {
                AppNavHost(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AppNavHost(viewModel: HabitViewModel) {
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarRoutes = remember { setOf("today", "calendar", "journal", "insights", "settings") }
    val showBottomBar = currentRoute in bottomBarRoutes
    val showFab = currentRoute == "today" || currentRoute == "journal" || currentRoute == "insights"
    val journalDialogRequest = rememberSaveable { mutableStateOf(false) }
    val goalDialogRequest = rememberSaveable { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (showFab) {
                val isJournal = currentRoute == "journal"
                val isInsights = currentRoute == "insights"
                FloatingActionButton(onClick = {
                    if (isJournal) {
                        journalDialogRequest.value = true
                    } else if (isInsights) {
                        goalDialogRequest.value = true
                    } else {
                        navController.navigate("add")
                    }
                }) {
                    Icon(
                        imageVector = if (isJournal) Icons.Filled.EditNote else Icons.Filled.Add,
                        contentDescription = if (isJournal) "New journal entry" else if (isInsights) "Create goal" else "Add habit"
                    )
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier.padding(padding)
        ) {
            composable("today") { HomeScreen(navController = navController, viewModel = viewModel) }
            composable("calendar") { CalendarScreen(navController = navController, viewModel = viewModel) }
            composable("journal") {
                JournalScreen(
                    viewModel = viewModel,
                    openDialogRequest = journalDialogRequest.value,
                    onDialogRequestConsumed = { journalDialogRequest.value = false }
                )
            }
            composable("insights") { 
                StatsScreen(
                    navController = navController, 
                    viewModel = viewModel,
                    openDialogRequest = goalDialogRequest.value,
                    onDialogRequestConsumed = { goalDialogRequest.value = false }
                ) 
            }
            composable("settings") { SettingsScreen(navController = navController, viewModel = viewModel) }
            composable(
                route = "add?habitId={habitId}&goalId={goalId}",
                arguments = listOf(
                    navArgument("habitId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("goalId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getLong("habitId").takeIf { it != -1L }
                val goalId = backStackEntry.arguments?.getLong("goalId").takeIf { it != -1L }
                AddHabitScreen(navController = navController, viewModel = viewModel, habitId = habitId, prefilledGoalId = goalId)
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("today", "Today", Icons.Filled.CheckCircle),
        BottomNavItem("calendar", "Calendar", Icons.Filled.DateRange),
        BottomNavItem("journal", "Journal", Icons.Filled.EditNote),
        BottomNavItem("insights", "Insights", Icons.AutoMirrored.Filled.ShowChart),
        BottomNavItem("settings", "Settings", Icons.Filled.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { androidx.compose.material3.Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

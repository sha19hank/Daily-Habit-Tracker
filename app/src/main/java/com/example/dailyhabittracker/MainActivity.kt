package com.example.dailyhabittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dailyhabittracker.ui.screens.AddHabitScreen
import com.example.dailyhabittracker.ui.screens.CalendarScreen
import com.example.dailyhabittracker.ui.screens.HomeScreen
import com.example.dailyhabittracker.ui.screens.SettingsScreen
import com.example.dailyhabittracker.ui.screens.StatsScreen
import com.example.dailyhabittracker.ui.theme.DailyHabitTrackerTheme
import com.example.dailyhabittracker.viewmodel.HabitViewModel

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
    val adManager = (context.applicationContext as DailyHabitTrackerApp).container.adManager

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController = navController, viewModel = viewModel, adManager = adManager) }
        composable("add") { AddHabitScreen(navController = navController, viewModel = viewModel) }
        composable("stats") { StatsScreen(navController = navController, viewModel = viewModel) }
        composable("calendar") { CalendarScreen(navController = navController, viewModel = viewModel) }
        composable("settings") { SettingsScreen(navController = navController, viewModel = viewModel) }
    }
}

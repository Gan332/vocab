package com.vocabapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vocabapp.ui.banks.BanksScreen
import com.vocabapp.ui.learn.LearnScreen
import com.vocabapp.ui.stats.StatsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Banks : Screen("banks", "词库", Icons.Default.Book)
    data object Learn : Screen("learn", "学习", Icons.Default.Psychology)
    data object Stats : Screen("stats", "统计", Icons.Default.BarChart)
}

val screens = listOf(Screen.Banks, Screen.Learn, Screen.Stats)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentScreen = screens.find { it.route == currentDestination?.route }

    Scaffold(
        topBar = {
            if (currentScreen != null) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "背单词",
                            style = MaterialTheme.typography.displayLarge
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = androidx.compose.ui.unit.dp.times(0)
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.route == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Banks.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Banks.route) {
                BanksScreen(
                    onStartLearn = { bankName ->
                        navController.navigate(Screen.Learn.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Learn.route) {
                LearnScreen()
            }
            composable(Screen.Stats.route) {
                StatsScreen()
            }
        }
    }
}

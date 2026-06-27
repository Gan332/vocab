package com.vocabapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vocabapp.ui.banks.BanksScreen
import com.vocabapp.ui.learn.LearnScreen
import com.vocabapp.ui.stats.StatsScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Banks : Screen("banks", "词库", Icons.Default.Book)
    data object Learn : Screen("learn?bankName={bankName}", "学习", Icons.Default.Psychology)
    data object Stats : Screen("stats", "统计", Icons.Default.BarChart)
}

val screens = listOf(Screen.Banks, Screen.Learn, Screen.Stats)

@Composable
fun VocabNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentScreen = screens.find { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                screens.forEach { screen ->
                    val selected = currentDestination?.route?.substringBefore("?") == screen.route.substringBefore("?")
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route.split('?')[0]) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Banks.route,
            modifier = Modifier
                        .padding(innerPadding)
                        .statusBarsPadding()
        ) {
            composable(Screen.Banks.route) {
                BanksScreen(
                    onStartLearn = { bankName ->
                        val encoded = URLEncoder.encode(bankName, StandardCharsets.UTF_8.toString())
                        navController.navigate("learn?bankName=$encoded") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = Screen.Learn.route,
                arguments = listOf(navArgument("bankName") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { backStackEntry ->
                val bankName = backStackEntry.arguments?.getString("bankName")?.let {
                    URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                } ?: ""
                LearnScreen(initialBankName = bankName)
            }
            composable(Screen.Stats.route) {
                StatsScreen()
            }
        }
    }
}

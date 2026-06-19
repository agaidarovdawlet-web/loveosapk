package com.example.loveosapk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.loveosapk.ui.MainViewModel
import com.example.loveosapk.ui.navigation.Screen
import com.example.loveosapk.ui.theme.LoveOsApkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import com.example.loveosapk.ui.components.AnimationConstants

import androidx.compose.ui.platform.LocalContext
import com.example.loveosapk.ui.features.cycle.CycleViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val appState by viewModel.appState.collectAsState()
            
            LoveOsApkTheme(appThemeSetting = appState.theme) {
                MainApp(viewModel, appState)
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainApp(viewModel: MainViewModel, appState: com.example.loveosapk.data.AppState) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(appState.setup) {
        if (appState.setup && navController.currentDestination?.route == Screen.Setup.route) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Setup.route) { inclusive = true }
            }
        }
    }

    LaunchedEffect(currentRoute) {
        android.util.Log.d("NAV_DIAGNOSTIC", "Current Route: $currentRoute")
    }

    Scaffold(
        bottomBar = {
            if (appState.setup && currentRoute != Screen.Setup.route) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            Triple(Screen.Home, Icons.Default.Home, "Главная"),
                            Triple(Screen.Cycle, Icons.Default.CalendarMonth, "Цикл"),
                            Triple(Screen.Chat, Icons.AutoMirrored.Filled.Chat, "Чат"),
                            Triple(Screen.Stuff, Icons.Default.Favorite, "Желания"),
                            Triple(Screen.More, Icons.Default.MoreHoriz, "Ещё")
                        )
                        
                        tabs.forEach { (screen, icon, label) ->
                            val selected = currentRoute == screen.route
                            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                            
                            Box(
                                modifier = Modifier
                                    .size(width = 56.dp, height = 44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        android.util.Log.d("NAV_DIAGNOSTIC", "Tab clicked: ${screen.route}")
                                        if (!selected) {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            try {
                                                navController.navigate(screen.route) {
                                                    // Pop up to the start destination of the graph to
                                                    // avoid building up a large stack of destinations
                                                    // on the back stack as users select items
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    // Avoid multiple copies of the same destination when
                                                    // reselecting the same item
                                                    launchSingleTop = true
                                                    // Restore state when reselecting a previously selected item
                                                    restoreState = true
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("NAV_DIAGNOSTIC", "Navigation failed to ${screen.route}", e)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val scale by animateFloatAsState(
                                    targetValue = if (selected) 1.08f else 1.0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "tabScale"
                                )
                                
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    modifier = Modifier.size(26.dp).graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = if (appState.setup) Screen.Home.route else Screen.Setup.route,
                modifier = Modifier.padding(innerPadding),
                // Using default transitions to isolate the issue
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                popExitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                composable(Screen.Setup.route) { 
                    com.example.loveosapk.ui.screens.SetupScreen(viewModel) 
                }
                composable(Screen.Home.route) { 
                    com.example.loveosapk.ui.screens.HomeScreen(appState, viewModel) 
                }
                composable(Screen.Cycle.route) { 
                    val cycleViewModel: CycleViewModel = viewModel(
                        factory = CycleViewModel.Factory(viewModel)
                    )
                    com.example.loveosapk.ui.features.cycle.CycleScreen(cycleViewModel)
                }
                composable(Screen.Chat.route) { 
                    com.example.loveosapk.ui.screens.ChatScreen(viewModel) 
                }
                composable(Screen.Stuff.route) { 
                    com.example.loveosapk.ui.screens.StuffScreen(viewModel) 
                }
                composable(Screen.Notes.route) { 
                    com.example.loveosapk.ui.screens.NotesScreen(viewModel) 
                }
                composable(Screen.More.route) { 
                    com.example.loveosapk.ui.screens.MoreScreen(viewModel, navController, this@SharedTransitionLayout, this@composable) 
                }
                composable(Screen.Games.route) { 
                    com.example.loveosapk.ui.screens.GamesScreen(viewModel) 
                }
                composable(Screen.Profile.route) { 
                    com.example.loveosapk.ui.screens.ProfileScreen(viewModel, onBack = { navController.popBackStack() }, this@SharedTransitionLayout, this@composable) 
                }
            }
        }
    }
}

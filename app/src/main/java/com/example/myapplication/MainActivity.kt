package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.myapplication.ui.EditorScreen
import com.example.myapplication.ui.TimelineScreen
import com.example.myapplication.ui.CalendarScreen
import com.example.myapplication.ui.SummarizeScreen
import com.example.myapplication.ui.MeScreen
import com.example.myapplication.ui.SettingsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import android.net.Uri

@OptIn(ExperimentalSharedTransitionApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContent {
            var backgroundUri by remember { mutableStateOf<Uri?>(null) }
            var backgroundOpacity by remember { mutableFloatStateOf(0.6f) }
            MyApplicationTheme {
                MainScreen(
                    backgroundUri = backgroundUri,
                    backgroundOpacity = backgroundOpacity,
                    onBackgroundChanged = { backgroundUri = it },
                    onOpacityChanged = { backgroundOpacity = it }
                )
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Timeline : Screen("timeline", "我的日记", Icons.Default.EditNote)
    object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    object Summarize : Screen("summarize", "总结", Icons.Default.AutoAwesome)
    object Me : Screen("me", "我的", Icons.Default.Person)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    backgroundUri: Uri?,
    backgroundOpacity: Float,
    onBackgroundChanged: (Uri?) -> Unit,
    onOpacityChanged: (Float) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val items = listOf(Screen.Timeline, Screen.Calendar, Screen.Summarize, Screen.Me, Screen.Settings)

    val isMainScreen = items.any { it.route == currentDestination?.route }

    SharedTransitionLayout {
        Scaffold(
            // 关键：不要在这里设置 contentWindowInsets = WindowInsets(0)，
            // 否则会阻止 IME Insets 向下传递。
            bottomBar = {
                AnimatedVisibility(
                    visible = isMainScreen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NavigationBar {
                        items.forEach { screen ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = null) },
                                label = { Text(screen.label) },
                                selected = isSelected,
                                onClick = {
                                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Timeline.route,
                modifier = Modifier.fillMaxSize(),
                // 默认动画：淡入淡出 (适用于底部栏标签切换)
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                composable(
                    Screen.Timeline.route,
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it / 3 },
                            animationSpec = tween(280)
                        ) + fadeIn(animationSpec = tween(280))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it / 3 },
                            animationSpec = tween(280)
                        ) + fadeOut(animationSpec = tween(280))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it / 3 },
                            animationSpec = tween(280)
                        ) + fadeIn(animationSpec = tween(280))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = tween(280)
                        ) + fadeOut(animationSpec = tween(280))
                    }
                ) {
                    TimelineScreen(
                        onEntryClick = { entryId -> navController.navigate("editor?entryId=$entryId") },
                        onAddEntryClick = { navController.navigate("editor") },
                        backgroundUri = backgroundUri,
                        backgroundOpacity = backgroundOpacity,
                        modifier = Modifier.padding(innerPadding),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
                composable(
                    Screen.Calendar.route,
                    enterTransition = {
                        val from = initialState.destination.route
                        when (from) {
                            Screen.Timeline.route -> slideInHorizontally(
                                initialOffsetX = { it / 3 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                            Screen.Summarize.route,
                            Screen.Me.route,
                            Screen.Settings.route -> slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                            else -> fadeIn(animationSpec = tween(280))
                        }
                    },
                    exitTransition = {
                        val to = targetState.destination.route
                        when (to) {
                            Screen.Timeline.route -> slideOutHorizontally(
                                targetOffsetX = { it / 3 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                            Screen.Summarize.route,
                            Screen.Me.route,
                            Screen.Settings.route -> slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                            else -> fadeOut(animationSpec = tween(280))
                        }
                    },
                    popEnterTransition = {
                        val from = initialState.destination.route
                        when (from) {
                            Screen.Timeline.route -> slideInHorizontally(
                                initialOffsetX = { it / 3 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                            Screen.Summarize.route,
                            Screen.Me.route,
                            Screen.Settings.route -> slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                            else -> fadeIn(animationSpec = tween(280))
                        }
                    },
                    popExitTransition = {
                        val to = targetState.destination.route
                        when (to) {
                            Screen.Timeline.route -> slideOutHorizontally(
                                targetOffsetX = { it / 3 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                            Screen.Summarize.route,
                            Screen.Me.route,
                            Screen.Settings.route -> slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                            else -> fadeOut(animationSpec = tween(280))
                        }
                    }
                ) {
                    CalendarScreen(
                        onEntryClick = { entryId -> navController.navigate("editor?entryId=$entryId") },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                composable(
                    Screen.Summarize.route,
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it / 2 },
                            animationSpec = tween(280)
                        ) + fadeIn(animationSpec = tween(280))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 2 },
                            animationSpec = tween(280)
                        ) + fadeOut(animationSpec = tween(280))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it / 2 },
                            animationSpec = tween(280)
                        ) + fadeIn(animationSpec = tween(280))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it / 2 },
                            animationSpec = tween(280)
                        ) + fadeOut(animationSpec = tween(280))
                    }
                ) {
                    SummarizeScreen(Modifier.padding(innerPadding))
                }
                composable(
                    Screen.Me.route,
                    enterTransition = {
                        val from = initialState.destination.route
                        when (from) {
                            Screen.Settings.route -> slideInHorizontally(
                                initialOffsetX = { -it / 2 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                            else -> slideInHorizontally(
                                initialOffsetX = { it / 2 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                        }
                    },
                    exitTransition = {
                        val to = targetState.destination.route
                        when (to) {
                            Screen.Settings.route -> slideOutHorizontally(
                                targetOffsetX = { -it / 2 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                            else -> slideOutHorizontally(
                                targetOffsetX = { it / 2 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                        }
                    },
                    popEnterTransition = {
                        val from = initialState.destination.route
                        when (from) {
                            Screen.Settings.route -> slideInHorizontally(
                                initialOffsetX = { -it / 2 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                            else -> slideInHorizontally(
                                initialOffsetX = { it / 2 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                        }
                    },
                    popExitTransition = {
                        val to = targetState.destination.route
                        when (to) {
                            Screen.Settings.route -> slideOutHorizontally(
                                targetOffsetX = { -it / 2 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                            else -> slideOutHorizontally(
                                targetOffsetX = { it / 2 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                        }
                    }
                ) {
                    MeScreen(modifier = Modifier.padding(innerPadding))
                }
                composable(
                    Screen.Settings.route,
                    enterTransition = {
                        val from = initialState.destination.route
                        when (from) {
                            Screen.Me.route -> slideInHorizontally(
                                initialOffsetX = { it / 2 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                            else -> slideInHorizontally(
                                initialOffsetX = { -it / 2 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                        }
                    },
                    exitTransition = {
                        val to = targetState.destination.route
                        when (to) {
                            Screen.Me.route -> slideOutHorizontally(
                                targetOffsetX = { it / 2 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                            else -> slideOutHorizontally(
                                targetOffsetX = { -it / 2 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                        }
                    },
                    popEnterTransition = {
                        val from = initialState.destination.route
                        when (from) {
                            Screen.Me.route -> slideInHorizontally(
                                initialOffsetX = { it / 2 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                            else -> slideInHorizontally(
                                initialOffsetX = { -it / 2 },
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))
                        }
                    },
                    popExitTransition = {
                        val to = targetState.destination.route
                        when (to) {
                            Screen.Me.route -> slideOutHorizontally(
                                targetOffsetX = { it / 2 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                            else -> slideOutHorizontally(
                                targetOffsetX = { -it / 2 },
                                animationSpec = tween(280)
                            ) + fadeOut(animationSpec = tween(280))
                        }
                    }
                ) {
                    SettingsScreen(
                        onBackgroundSelected = onBackgroundChanged,
                        backgroundOpacity = backgroundOpacity,
                        onOpacityChanged = onOpacityChanged,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                composable(
                    route = "editor?entryId={entryId}",
                    arguments = listOf(navArgument("entryId") { type = NavType.LongType; defaultValue = -1L }),
                    // 编辑页面使用左右滑动动画
                    enterTransition = {
                        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
                    },
                    exitTransition = {
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                    },
                    popEnterTransition = {
                        slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
                    },
                    popExitTransition = {
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                    }
                ) { backStackEntry ->
                    val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
                    EditorScreen(
                        entryId = entryId,
                        onNavigateBack = { navController.popBackStack() },
                        backgroundUri = backgroundUri,
                        backgroundOpacity = backgroundOpacity,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Text(text = "$name Screen coming soon!", modifier = Modifier.padding(16.dp))
    }
}

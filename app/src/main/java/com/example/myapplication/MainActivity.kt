package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.myapplication.ui.LanDiscoveryScreen
import com.example.myapplication.ui.MeScreen
import com.example.myapplication.ui.PeerTweetScreen
import com.example.myapplication.ui.SettingsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import com.example.myapplication.ui.smoothPageEnterTransition
import com.example.myapplication.ui.smoothPageExitTransition
import com.example.myapplication.ui.smoothPopEnterTransition
import com.example.myapplication.ui.smoothPopExitTransition
import com.example.myapplication.ui.sharedAxisEnterTransition
import com.example.myapplication.ui.sharedAxisExitTransition

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

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isMainScreen,
                enter = slideInVertically(
                    animationSpec = tween(
                        durationMillis = 280,
                        easing = FastOutSlowInEasing
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = FastOutSlowInEasing
                    )
                ),
                exit = slideOutVertically(
                    animationSpec = tween(
                        durationMillis = 240,
                        easing = FastOutSlowInEasing
                    ),
                    targetOffsetY = { it }
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 180,
                        easing = FastOutSlowInEasing
                    )
                )
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
                                        popUpTo(Screen.Timeline.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
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
            enterTransition = { sharedAxisEnterTransition() },
            exitTransition = { sharedAxisExitTransition() },
            popEnterTransition = { smoothPopEnterTransition() },
            popExitTransition = { smoothPopExitTransition() }
        ) {
            composable(
                Screen.Timeline.route,
                enterTransition = { sharedAxisEnterTransition() },
                exitTransition = { sharedAxisExitTransition() },
                popEnterTransition = { smoothPopEnterTransition() },
                popExitTransition = { smoothPopExitTransition() }
            ) {
                TimelineScreen(
                    onEntryClick = { entryId -> navController.navigate("editor?entryId=$entryId") },
                    onAddEntryClick = { navController.navigate("editor") },
                    backgroundUri = backgroundUri,
                    backgroundOpacity = backgroundOpacity,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            composable(
                Screen.Calendar.route,
                enterTransition = { sharedAxisEnterTransition() },
                exitTransition = { sharedAxisExitTransition() },
                popEnterTransition = { smoothPopEnterTransition() },
                popExitTransition = { smoothPopExitTransition() }
            ) {
                CalendarScreen(
                    onEntryClick = { entryId -> navController.navigate("editor?entryId=$entryId") },
                    backgroundUri = backgroundUri,
                    backgroundOpacity = backgroundOpacity,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            composable(
                Screen.Summarize.route,
                enterTransition = { sharedAxisEnterTransition() },
                exitTransition = { sharedAxisExitTransition() },
                popEnterTransition = { smoothPopEnterTransition() },
                popExitTransition = { smoothPopExitTransition() }
            ) {
                SummarizeScreen(Modifier.padding(innerPadding))
            }
            composable(
                Screen.Me.route,
                enterTransition = { sharedAxisEnterTransition() },
                exitTransition = { sharedAxisExitTransition() },
                popEnterTransition = { smoothPopEnterTransition() },
                popExitTransition = { smoothPopExitTransition() }
            ) {
                MeScreen(
                    onNavigateToLanDiscovery = { navController.navigate("lan_discovery") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            composable(
                Screen.Settings.route,
                enterTransition = { sharedAxisEnterTransition() },
                exitTransition = { sharedAxisExitTransition() },
                popEnterTransition = { smoothPopEnterTransition() },
                popExitTransition = { smoothPopExitTransition() }
            ) {
                SettingsScreen(
                    onBackgroundSelected = onBackgroundChanged,
                    backgroundOpacity = backgroundOpacity,
                    onOpacityChanged = onOpacityChanged,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            composable(
                route = "lan_discovery",
                enterTransition = { smoothPageEnterTransition() },
                exitTransition = { smoothPageExitTransition() },
                popEnterTransition = { smoothPopEnterTransition() },
                popExitTransition = { smoothPopExitTransition() }
            ) {
                LanDiscoveryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPeer = { ip, port, deviceId ->
                        navController.navigate("peer_tweets?ip=$ip&port=$port&deviceId=$deviceId")
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            composable(
                route = "peer_tweets?ip={ip}&port={port}&deviceId={deviceId}",
                arguments = listOf(
                    navArgument("ip") { type = NavType.StringType },
                    navArgument("port") { type = NavType.IntType },
                    navArgument("deviceId") { type = NavType.StringType }
                ),
                enterTransition = { smoothPageEnterTransition() },
                exitTransition = { smoothPageExitTransition() },
                popEnterTransition = { smoothPopEnterTransition() },
                popExitTransition = { smoothPopExitTransition() }
            ) { backStackEntry ->
                val ip = backStackEntry.arguments?.getString("ip") ?: ""
                val port = backStackEntry.arguments?.getInt("port") ?: 8765
                val peerDeviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                PeerTweetScreen(
                    ip = ip,
                    port = port,
                    deviceId = peerDeviceId,
                    onNavigateBack = { navController.popBackStack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            composable(
                route = "editor?entryId={entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.LongType; defaultValue = -1L }),
                enterTransition = { smoothPageEnterTransition() },
                exitTransition = { smoothPageExitTransition() },
                popEnterTransition = { smoothPopEnterTransition() },
                popExitTransition = { smoothPopExitTransition() }
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

@Composable
fun PlaceholderScreen(name: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Text(text = "$name Screen coming soon!", modifier = Modifier.padding(16.dp))
    }
}
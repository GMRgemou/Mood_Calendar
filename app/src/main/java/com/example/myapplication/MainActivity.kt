package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.myapplication.ui.CustomBackgroundContainer
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.example.myapplication.ui.androidActivityEnterTransition
import com.example.myapplication.ui.androidActivityExitTransition
import com.example.myapplication.ui.androidActivityPopEnterTransition
import com.example.myapplication.ui.androidActivityPopExitTransition
import com.example.myapplication.ui.smoothPageEnterTransition
import com.example.myapplication.ui.smoothPageExitTransition
import com.example.myapplication.ui.smoothPopEnterTransition
import com.example.myapplication.ui.smoothPopExitTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val APPEARANCE_PREFS = "appearance"
private const val PREF_BACKGROUND_URI = "background_uri"
private const val PREF_BACKGROUND_OPACITY = "background_opacity"
private const val DEFAULT_BACKGROUND_OPACITY = 0.6f
private const val PREF_AVATAR_URI = "avatar_uri"

private fun deleteStoredBackground(context: android.content.Context) {
    File(context.filesDir, "backgrounds").deleteRecursively()
}

private fun copyBackgroundToPrivateStorage(
    context: android.content.Context,
    sourceUri: Uri
): Uri? {
    return runCatching {
        val backgroundDir = File(context.filesDir, "backgrounds").apply { mkdirs() }
        val tempFile = File(backgroundDir, "custom_background.tmp")
        val targetFile = File(backgroundDir, "custom_background")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        if (targetFile.exists()) targetFile.delete()
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }

        Uri.fromFile(targetFile)
    }.getOrNull()
}

private fun copyAvatarToPrivateStorage(
    context: android.content.Context,
    sourceUri: Uri
): Uri? {
    return runCatching {
        val avatarDir = File(context.filesDir, "avatars").apply { mkdirs() }
        val tempFile = File(avatarDir, "custom_avatar.tmp")
        val targetFile = File(avatarDir, "custom_avatar")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        if (targetFile.exists()) targetFile.delete()
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }

        Uri.fromFile(targetFile)
    }.getOrNull()
}

private fun deleteStoredAvatar(context: android.content.Context) {
    File(context.filesDir, "avatars").deleteRecursively()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContent {
            val context = LocalContext.current
            val appearancePrefs = remember {
                context.getSharedPreferences(APPEARANCE_PREFS, MODE_PRIVATE)
            }
            val scope = rememberCoroutineScope()
            var backgroundUri by remember {
                mutableStateOf(
                    appearancePrefs.getString(PREF_BACKGROUND_URI, null)?.let(Uri::parse)
                )
            }
            var backgroundOpacity by remember {
                mutableFloatStateOf(
                    appearancePrefs.getFloat(PREF_BACKGROUND_OPACITY, DEFAULT_BACKGROUND_OPACITY)
                        .coerceIn(0f, 1f)
                )
            }
            var avatarUri by remember {
                mutableStateOf(
                    appearancePrefs.getString(PREF_AVATAR_URI, null)?.let(Uri::parse)
                )
            }
            MyApplicationTheme {
                MainScreen(
                    backgroundUri = backgroundUri,
                    backgroundOpacity = backgroundOpacity,
                    avatarUri = avatarUri,
                    onBackgroundChanged = { selectedUri ->
                        if (selectedUri == null) {
                            deleteStoredBackground(context)
                            backgroundUri = null
                            appearancePrefs.edit().remove(PREF_BACKGROUND_URI).apply()
                        } else {
                            scope.launch {
                                val storedUri = withContext(Dispatchers.IO) {
                                    copyBackgroundToPrivateStorage(context, selectedUri)
                                }
                                if (storedUri != null) {
                                    backgroundUri = storedUri
                                    appearancePrefs.edit()
                                        .putString(PREF_BACKGROUND_URI, storedUri.toString())
                                        .apply()
                                }
                            }
                        }
                    },
                    onOpacityChanged = { opacity ->
                        val safeOpacity = opacity.coerceIn(0f, 1f)
                        backgroundOpacity = safeOpacity
                        appearancePrefs.edit()
                            .putFloat(PREF_BACKGROUND_OPACITY, safeOpacity)
                            .apply()
                    },
                    onAvatarChanged = { selectedUri ->
                        if (selectedUri == null) {
                            deleteStoredAvatar(context)
                            avatarUri = null
                            appearancePrefs.edit().remove(PREF_AVATAR_URI).apply()
                        } else {
                            scope.launch {
                                val storedUri = withContext(Dispatchers.IO) {
                                    copyAvatarToPrivateStorage(context, selectedUri)
                                }
                                if (storedUri != null) {
                                    avatarUri = storedUri
                                    appearancePrefs.edit()
                                        .putString(PREF_AVATAR_URI, storedUri.toString())
                                        .apply()
                                }
                            }
                        }
                    }
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
@OptIn(ExperimentalFoundationApi::class)
fun MainScreen(
    backgroundUri: Uri?,
    backgroundOpacity: Float,
    avatarUri: Uri?,
    onBackgroundChanged: (Uri?) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onAvatarChanged: (Uri?) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val items = listOf(Screen.Timeline, Screen.Calendar, Screen.Summarize, Screen.Me, Screen.Settings)
    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    val isMainScreen = currentDestination?.route == "main"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    items.forEachIndexed { index, screen ->
                        val isSelected = pagerState.currentPage == index

                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = isSelected,
                            onClick = {
                                if (pagerState.currentPage != index) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        CustomBackgroundContainer(
            backgroundUri = backgroundUri,
            overlayAlpha = backgroundOpacity,
            modifier = Modifier
        ) {
            NavHost(
                navController = navController,
                startDestination = "main",
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(180)) },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { smoothPopEnterTransition() },
                popExitTransition = { smoothPopExitTransition() }
            ) {
                composable("main") {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { page ->
                        when (items[page]) {
                            Screen.Timeline -> TimelineScreen(
                                onEntryClick = { entryId -> navController.navigate("editor?entryId=$entryId") },
                                onAddEntryClick = { navController.navigate("editor") },
                                modifier = Modifier.padding(innerPadding),
                                avatarUri = avatarUri
                            )

                            Screen.Calendar -> CalendarScreen(
                                onEntryClick = { entryId -> navController.navigate("editor?entryId=$entryId") },
                                modifier = Modifier.padding(innerPadding)
                            )

                            Screen.Summarize -> SummarizeScreen(Modifier.padding(innerPadding))

                            Screen.Me -> MeScreen(
                                avatarUri = avatarUri,
                                onNavigateToLanDiscovery = { navController.navigate("lan_discovery") },
                                modifier = Modifier.padding(innerPadding)
                            )

                            Screen.Settings -> SettingsScreen(
                                onBackgroundSelected = onBackgroundChanged,
                                backgroundOpacity = backgroundOpacity,
                                onOpacityChanged = onOpacityChanged,
                                onAvatarSelected = onAvatarChanged,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
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
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) { backStackEntry ->
                    val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
                    EditorScreen(
                        entryId = entryId,
                        onNavigateBack = { navController.popBackStack() },
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
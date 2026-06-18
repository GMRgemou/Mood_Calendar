package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.example.myapplication.ui.diaryCardEnterTransition
import com.example.myapplication.ui.diaryCardExitTransition
import com.example.myapplication.ui.diaryCardPopExitTransition
import com.example.myapplication.ui.smoothPageEnterTransition
import com.example.myapplication.ui.smoothPageExitTransition
import com.example.myapplication.ui.smoothPopEnterTransition
import com.example.myapplication.ui.smoothPopExitTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val APPEARANCE_PREFS = "appearance"
private const val PREF_BACKGROUND_URI = "background_uri"
private const val PREF_BACKGROUND_OPACITY = "background_opacity"
private const val DEFAULT_BACKGROUND_OPACITY = 0.6f
private const val PREF_AVATAR_URI = "avatar_uri"
private const val PREF_DIARY_TITLE_ENABLED = "diary_title_enabled"

private val BackgroundMedia = StoredMedia(
    subDir = "backgrounds",
    tempFileName = "custom_background.tmp",
    targetFileName = "custom_background"
)

private val AvatarMedia = StoredMedia(
    subDir = "avatars",
    tempFileName = "custom_avatar.tmp",
    targetFileName = "custom_avatar"
)

private data class StoredMedia(
    val subDir: String,
    val tempFileName: String,
    val targetFileName: String
)

private fun deleteStoredMedia(context: Context, media: StoredMedia) {
    File(context.filesDir, media.subDir).deleteRecursively()
}

private fun copyMediaToPrivateStorage(
    context: Context,
    sourceUri: Uri,
    media: StoredMedia
): Uri? = runCatching {
    val mediaDir = File(context.filesDir, media.subDir).apply { mkdirs() }
    val tempFile = File(mediaDir, media.tempFileName)
    val targetFile = File(mediaDir, media.targetFileName)

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

private fun SharedPreferences.putNullableString(key: String, value: String?) {
    val editor = edit()
    if (value == null) editor.remove(key) else editor.putString(key, value)
    editor.apply()
}

private fun updateStoredMedia(
    context: Context,
    prefs: SharedPreferences,
    scope: CoroutineScope,
    selectedUri: Uri?,
    media: StoredMedia,
    prefKey: String,
    onStoredUriChanged: (Uri?) -> Unit
) {
    if (selectedUri == null) {
        deleteStoredMedia(context, media)
        onStoredUriChanged(null)
        prefs.putNullableString(prefKey, null)
        return
    }

    scope.launch {
        val storedUri = withContext(Dispatchers.IO) {
            copyMediaToPrivateStorage(context, selectedUri, media)
        }
        if (storedUri != null) {
            onStoredUriChanged(storedUri)
            prefs.putNullableString(prefKey, storedUri.toString())
        }
    }
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
            var diaryTitleEnabled by remember {
                mutableStateOf(appearancePrefs.getBoolean(PREF_DIARY_TITLE_ENABLED, true))
            }
            MyApplicationTheme {
                MainScreen(
                    backgroundUri = backgroundUri,
                    backgroundOpacity = backgroundOpacity,
                    avatarUri = avatarUri,
                    diaryTitleEnabled = diaryTitleEnabled,
                    onBackgroundChanged = { selectedUri ->
                        updateStoredMedia(
                            context = context,
                            prefs = appearancePrefs,
                            scope = scope,
                            selectedUri = selectedUri,
                            media = BackgroundMedia,
                            prefKey = PREF_BACKGROUND_URI,
                            onStoredUriChanged = { backgroundUri = it }
                        )
                    },
                    onOpacityChanged = { opacity ->
                        val safeOpacity = opacity.coerceIn(0f, 1f)
                        backgroundOpacity = safeOpacity
                        appearancePrefs.edit()
                            .putFloat(PREF_BACKGROUND_OPACITY, safeOpacity)
                            .apply()
                    },
                    onAvatarChanged = { selectedUri ->
                        updateStoredMedia(
                            context = context,
                            prefs = appearancePrefs,
                            scope = scope,
                            selectedUri = selectedUri,
                            media = AvatarMedia,
                            prefKey = PREF_AVATAR_URI,
                            onStoredUriChanged = { avatarUri = it }
                        )
                    },
                    onDiaryTitleEnabledChanged = { enabled ->
                        diaryTitleEnabled = enabled
                        appearancePrefs.edit()
                            .putBoolean(PREF_DIARY_TITLE_ENABLED, enabled)
                            .apply()
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
    diaryTitleEnabled: Boolean,
    onBackgroundChanged: (Uri?) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onAvatarChanged: (Uri?) -> Unit,
    onDiaryTitleEnabledChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(Screen.Timeline, Screen.Calendar, Screen.Summarize, Screen.Me, Screen.Settings)
    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                composable(
                    route = "main",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
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
                    ) { mainInnerPadding ->
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            when (items[page]) {
                                Screen.Timeline -> TimelineScreen(
                                    onEntryClick = { entryId -> navController.navigate("editor?entryId=$entryId") },
                                    onAddEntryClick = { navController.navigate("editor") },
                                    onLanShareClick = { navController.navigate("lan_discovery") },
                                    modifier = Modifier.padding(mainInnerPadding),
                                    avatarUri = avatarUri
                                )

                                Screen.Calendar -> CalendarScreen(
                                    onEntryClick = { entryId -> navController.navigate("editor?entryId=$entryId") },
                                    modifier = Modifier.padding(mainInnerPadding)
                                )

                                Screen.Summarize -> SummarizeScreen(Modifier.padding(mainInnerPadding))

                                Screen.Me -> MeScreen(
                                    avatarUri = avatarUri,
                                    onNavigateToLanDiscovery = { navController.navigate("lan_discovery") },
                                    modifier = Modifier.padding(mainInnerPadding)
                                )

                                Screen.Settings -> SettingsScreen(
                                    onBackgroundSelected = onBackgroundChanged,
                                    backgroundOpacity = backgroundOpacity,
                                    onOpacityChanged = onOpacityChanged,
                                    onAvatarSelected = onAvatarChanged,
                                    diaryTitleEnabled = diaryTitleEnabled,
                                    onDiaryTitleEnabledChanged = onDiaryTitleEnabledChanged,
                                    modifier = Modifier.padding(mainInnerPadding)
                                )
                            }
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
                    enterTransition = { diaryCardEnterTransition() },
                    exitTransition = { diaryCardExitTransition() },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { diaryCardPopExitTransition() }
                ) { backStackEntry ->
                    val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
                    EditorScreen(
                        entryId = entryId,
                        onNavigateBack = { navController.popBackStack() },
                        diaryTitleEnabled = diaryTitleEnabled,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

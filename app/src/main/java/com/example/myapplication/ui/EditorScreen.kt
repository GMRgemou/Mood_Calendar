package com.example.myapplication.ui

import com.example.myapplication.R
import androidx.activity.compose.BackHandler
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import org.json.JSONObject
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.*

private fun defaultAudioName(uri: Uri): String {
    return "语音记录 ${uri.lastPathSegment?.takeLast(10).orEmpty()}".trim()
}

private fun splitStoredAudioMetadata(value: String): List<String> {
    return if (value.isEmpty()) emptyList() else value.split("|")
}

@Composable
fun AudioPropertiesDialog(
    initialName: String,
    initialVisibility: Boolean,
    initialTranscription: String,
    onConfirm: (String, Boolean, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var visibility by remember { mutableStateOf(initialVisibility) }
    var transcription by remember { mutableStateOf(initialTranscription) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑录音属性") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("录音名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("显示语音转文字内容")
                    Switch(
                        checked = visibility,
                        onCheckedChange = { visibility = it }
                    )
                }
                OutlinedTextField(
                    value = transcription,
                    onValueChange = { transcription = it },
                    label = { Text("语音转文字内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, visibility, transcription) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun MoodSelectionDialog(
    onMoodSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val moods = listOf("😊", "🥰", "😎", "🤔", "😴", "😢", "😠", "🥳", "🌈", "☕", "🍕", "🎮")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Mood") },
        text = {
            Column {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(moods.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(48.dp)
                                .clickable { onMoodSelected(moods[index]) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = moods[index], style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun LocationInputDialog(
    currentLocation: String,
    onLocationConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentLocation) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.location_dialog_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onLocationConfirmed(text) }) { Text(stringResource(R.string.location_dialog_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.location_dialog_cancel)) }
        }
    )
}

@Composable
fun PhotoViewerDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. 照片展示
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clickable { onDismiss() },
                    contentScale = ContentScale.Fit
                )

                // 2. 顶部返回按钮
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // 3. 底部功能栏
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 保存到本地
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSave() }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Save", tint = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("保存", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }

                    // 分享
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onShare() }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("分享", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }

                    // 删除
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onDelete() }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("删除", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorToolbar(
    onAddImage: () -> Unit,
    onAddAttachment: () -> Unit,
    onVoiceInput: () -> Unit,
    onAddLocation: () -> Unit,
    onAddLocationManual: () -> Unit,
    onAddMood: () -> Unit,
    onOpenCamera: () -> Unit,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val onboardingPrefs = remember { context.getSharedPreferences("onboarding", 0) }
    var hasSeenLocationTip by remember { mutableStateOf(onboardingPrefs.getBoolean("seen_location_tip", false)) }
    val locationTipState = rememberTooltipState(isPersistent = true)
    LaunchedEffect(Unit) {
        if (!hasSeenLocationTip) {
            hasSeenLocationTip = true
            onboardingPrefs.edit().putBoolean("seen_location_tip", true).apply()
            locationTipState.show()
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        tonalElevation = 3.dp,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onAddImage) {
                    Icon(Icons.Default.Image, contentDescription = "照片", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onOpenCamera) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "拍摄", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onAddAttachment) {
                    Icon(Icons.Default.AttachFile, contentDescription = "附件", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onVoiceInput,
                    colors = if (isRecording) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer) else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "录音",
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(stringResource(R.string.tip_long_press_location)) } },
                    state = locationTipState,
                    focusable = true
                ) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .combinedClickable(
                                onClick = {
                                    if (!hasSeenLocationTip) {
                                        hasSeenLocationTip = true
                                        onboardingPrefs.edit().putBoolean("seen_location_tip", true).apply()
                                    }
                                    onAddLocation()
                                },
                                onLongClick = {
                                    if (!hasSeenLocationTip) {
                                        hasSeenLocationTip = true
                                        onboardingPrefs.edit().putBoolean("seen_location_tip", true).apply()
                                    }
                                    onAddLocationManual()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = stringResource(R.string.location_content_description), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onAddMood) {
                    Icon(Icons.Default.SentimentSatisfied, contentDescription = "心情", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    entryId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = viewModel()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var attachmentUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var audioUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var audioTranscriptions by remember { mutableStateOf<Map<Uri, String>>(emptyMap()) }
    var audioNames by remember { mutableStateOf<Map<Uri, String>>(emptyMap()) }
    var audioTranscriptionsVisibility by remember { mutableStateOf<Map<Uri, Boolean>>(emptyMap()) }
    var mood by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var isRecording by remember { mutableStateOf(false) }
    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }
    var recordingJob by remember { mutableStateOf<Job?>(null) }

    var voskModel by remember { mutableStateOf<Model?>(null) }
    var voskRecognizer by remember { mutableStateOf<Recognizer?>(null) }
    var isVoskReady by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // 辅助函数：寻找模型实际所在的目录 (含有 am 文件夹的目录)
    fun findModelDir(root: File): File? {
        if (File(root, "am").exists() && File(root, "conf").exists()) {
            return root
        }
        root.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val found = findModelDir(file)
                if (found != null) return found
            }
        }
        return null
    }

    fun updateWavHeader(file: File, totalAudioLen: Long) {
        fun ascii(char: Char) = char.code.toByte()
        val totalDataLen = totalAudioLen + 36
        val sampleRate = 16000L
        val channels = 1
        val byteRate = 16 * sampleRate * channels / 8
        val header = ByteArray(44)
        
        header[0] = ascii('R') // RIFF/WAVE header
        header[1] = ascii('I')
        header[2] = ascii('F')
        header[3] = ascii('F')
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
        header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
        header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
        header[8] = ascii('W')
        header[9] = ascii('A')
        header[10] = ascii('V')
        header[11] = ascii('E')
        header[12] = ascii('f') // 'fmt ' chunk
        header[13] = ascii('m')
        header[14] = ascii('t')
        header[15] = ascii(' ')
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xffL).toByte()
        header[25] = ((sampleRate shr 8) and 0xffL).toByte()
        header[26] = ((sampleRate shr 16) and 0xffL).toByte()
        header[27] = ((sampleRate shr 24) and 0xffL).toByte()
        header[28] = (byteRate and 0xffL).toByte()
        header[29] = ((byteRate shr 8) and 0xffL).toByte()
        header[30] = ((byteRate shr 16) and 0xffL).toByte()
        header[31] = ((byteRate shr 24) and 0xffL).toByte()
        header[32] = (1 * 16 / 8).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = ascii('d')
        header[37] = ascii('a')
        header[38] = ascii('t')
        header[39] = ascii('a')
        header[40] = (totalAudioLen and 0xffL).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xffL).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xffL).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xffL).toByte()
        
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.write(header)
        }
    }

    LaunchedEffect(Unit) {
        // 1. 加载现有日记内容 (如果是编辑模式)
        if (entryId != -1L) {
            val entry = viewModel.getEntryById(entryId)
            entry?.let {
                title = it.title
                content = it.content
                selectedDate = it.date
                mood = it.mood
                location = it.location
                if (it.imageUris.isNotEmpty()) {
                    imageUris = it.imageUris.split(",").map { uriStr -> Uri.parse(uriStr) }
                }
                if (it.audioUris.isNotEmpty()) {
                    val uris = it.audioUris.split(",").filter { s -> s.isNotEmpty() }.map { s -> Uri.parse(s) }
                    val transcriptions = splitStoredAudioMetadata(it.audioTranscriptions)
                    val names = splitStoredAudioMetadata(it.audioNames)
                    val visibilities = splitStoredAudioMetadata(it.audioTranscriptionsVisibility)
                    
                    audioUris = uris
                    audioTranscriptions = uris.zip(transcriptions).toMap()
                    audioNames = if (names.size == uris.size) {
                        uris.zip(names)
                            .mapNotNull { (uri, name) -> name.takeIf { savedName -> savedName.isNotBlank() }?.let { uri to it } }
                            .toMap()
                    } else {
                        emptyMap()
                    }
                    audioTranscriptionsVisibility = if (visibilities.size == uris.size) {
                        uris.zip(visibilities.map { v -> v == "1" }).toMap()
                    } else {
                        uris.associateWith { true }
                    }
                }
            }
        }

        // 2. 初始化 Vosk（离线中文语音识别）
        withContext(Dispatchers.IO) {
            try {
                // 尝试解压或加载已存在的模型
                StorageService.unpack(context, "model-cn", "model", 
                    object : StorageService.Callback<Model> {
                        override fun onComplete(model: Model) {
                            // 使用 unpack 成功提供的 model
                            voskModel = model
                            try {
                                voskRecognizer = Recognizer(model, 16000f)
                                scope.launch(Dispatchers.Main) { isVoskReady = true }
                                Log.d("EditorScreen", "Vosk model loaded successfully via unpack callback")
                            } catch (e: Exception) {
                                Log.e("EditorScreen", "Vosk recognizer init failed with provided model", e)
                                // 如果直接使用失败，再尝试搜索子目录（以防结构嵌套）
                                scope.launch(Dispatchers.IO) {
                                    val targetDir = File(context.filesDir, "model")
                                    val actualModelDir = findModelDir(targetDir)
                                    if (actualModelDir != null && actualModelDir.absolutePath != targetDir.absolutePath) {
                                        try {
                                            val nestedModel = Model(actualModelDir.absolutePath)
                                            voskModel = nestedModel
                                            voskRecognizer = Recognizer(nestedModel, 16000f)
                                            scope.launch(Dispatchers.Main) { isVoskReady = true }
                                            Log.d("EditorScreen", "Vosk model recovered from nested dir: ${actualModelDir.absolutePath}")
                                            return@launch
                                        } catch (ex: Exception) {
                                            Log.e("EditorScreen", "Vosk nested recovery failed", ex)
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "语音识别初始化失败", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    object : StorageService.Callback<java.io.IOException> {
                        override fun onComplete(e: java.io.IOException) {
                            Log.e("EditorScreen", "Vosk unpack error (sync failed)", e)
                            scope.launch(Dispatchers.IO) {
                                // 即使同步报错，也尝试寻找是否已有解压好的模型
                                val targetDir = File(context.filesDir, "model")
                                val actualModelDir = findModelDir(targetDir)
                                if (actualModelDir != null) {
                                    try {
                                        val model = Model(actualModelDir.absolutePath)
                                        voskModel = model
                                        voskRecognizer = Recognizer(model, 16000f)
                                        scope.launch(Dispatchers.Main) { isVoskReady = true }
                                        Log.d("EditorScreen", "Vosk model recovered from existing storage: ${actualModelDir.absolutePath}")
                                        return@launch
                                    } catch (ex: Exception) {
                                        Log.e("EditorScreen", "Vosk recovery from existing failed", ex)
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "语音模型准备失败: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("EditorScreen", "Vosk StorageService critical failure", e)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voskRecognizer?.close()
            voskModel?.close()
        }
    }

    var selectedViewerUri by remember { mutableStateOf<Uri?>(null) }
    var showMoodDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showAudioPropertiesDialog by remember { mutableStateOf(false) }
    var editingAudioUri by remember { mutableStateOf<Uri?>(null) }
    var editableLocationText by remember { mutableStateOf("") }
    var editorVisible by remember { mutableStateOf(false) }
    var isNavigatingBack by remember { mutableStateOf(false) }
    val slideDistancePx = with(LocalDensity.current) { 56.dp.toPx() }
    val editorAnimationProgress by animateFloatAsState(
        targetValue = if (editorVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 260,
            easing = FastOutSlowInEasing
        ),
        label = "editorOpenCloseProgress",
        finishedListener = { progress ->
            if (progress == 0f && isNavigatingBack) {
                onNavigateBack()
            }
        }
    )

    LaunchedEffect(Unit) {
        editorVisible = true
    }

    fun closeEditor() {
        if (isNavigatingBack) return
        isNavigatingBack = true
        editorVisible = false
    }

    BackHandler(
        enabled = !showDatePicker && selectedViewerUri == null && !showMoodDialog && !showLocationDialog && !showAudioPropertiesDialog
    ) {
        closeEditor()
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    // 相机权限状态
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    // 录音权限状态
    val audioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    // 位置权限状态 (使用多权限请求)
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun fetchCurrentLocation() {
        if (locationPermissionsState.allPermissionsGranted || locationPermissionsState.revokedPermissions.size < locationPermissionsState.permissions.size) {
            try {
                fun resolveAndSetAddress(loc: android.location.Location) {
                    scope.launch {
                        try {
                            if (!android.location.Geocoder.isPresent()) {
                                location = "${loc.latitude}, ${loc.longitude}"
                                return@launch
                            }
                            val geocoder = android.location.Geocoder(context, Locale.getDefault())
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                geocoder.getFromLocation(loc.latitude, loc.longitude, 1) { addresses ->
                                    scope.launch(Dispatchers.Main) {
                                        if (addresses.isNotEmpty()) {
                                            location = addresses[0].getAddressLine(0) ?: "${loc.latitude}, ${loc.longitude}"
                                        } else {
                                            location = "${loc.latitude}, ${loc.longitude}"
                                        }
                                    }
                                }
                            } else {
                                val address = withContext(Dispatchers.IO) {
                                    @Suppress("DEPRECATION")
                                    val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                    if (!addresses.isNullOrEmpty()) {
                                        addresses[0].getAddressLine(0) ?: "${loc.latitude}, ${loc.longitude}"
                                    } else {
                                        "${loc.latitude}, ${loc.longitude}"
                                    }
                                }
                                location = address
                            }
                        } catch (e: Exception) {
                            Log.e("EditorScreen", "Geocoder failed", e)
                            location = "${loc.latitude}, ${loc.longitude}"
                        }
                    }
                }

                fun fetchWithLastLocation() {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { last ->
                            if (last != null) {
                                resolveAndSetAddress(last)
                            } else {
                                Toast.makeText(context, context.getString(R.string.location_fetch_failed_service_disabled), Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("EditorScreen", "LastLocation request failed", e)
                            Toast.makeText(context, context.getString(R.string.location_fetch_failed_generic, e.message ?: "Unknown error"), Toast.LENGTH_SHORT).show()
                        }
                }

                fun fetchWithBalanced() {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                        .addOnSuccessListener { loc ->
                            if (loc != null) {
                                resolveAndSetAddress(loc)
                            } else {
                                fetchWithLastLocation()
                            }
                        }
                        .addOnFailureListener {
                            fetchWithLastLocation()
                        }
                }

                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            resolveAndSetAddress(loc)
                        } else {
                            fetchWithBalanced()
                        }
                    }
                    .addOnFailureListener {
                        fetchWithBalanced()
                    }
            } catch (e: SecurityException) {
                Toast.makeText(context, context.getString(R.string.location_permission_disabled), Toast.LENGTH_SHORT).show()
            }
        } else {
            if (locationPermissionsState.shouldShowRationale) {
                Toast.makeText(context, context.getString(R.string.location_permission_rationale), Toast.LENGTH_SHORT).show()
            }
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    // 相册选择
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris -> if (uris.isNotEmpty()) imageUris = imageUris + uris }
    )

    // 拍照
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempPhotoUri != null) {
                imageUris = imageUris + tempPhotoUri!!
            }
        }
    )

    fun openCamera() {
        val file = File(context.filesDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        tempPhotoUri = uri
        cameraLauncher.launch(uri)
    }

    // 保存到本地相册逻辑
    fun saveToGallery(uri: Uri) {
        try {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "Diary_${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyApplication")
                }
            }
            
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let { destUri ->
                resolver.openInputStream(uri)?.use { input ->
                    resolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 分享逻辑
    fun shareImage(uri: Uri) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "分享图片"))
        } catch (e: Exception) {
            Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 附件选择
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris -> if (uris.isNotEmpty()) attachmentUris = attachmentUris + uris }
    )

    fun startRecording() {
        try {
            if (isRecording) return
            val audioDir = File(context.filesDir, "audio").apply { if (!exists()) mkdirs() }
            val file = File(audioDir, "recording_${System.currentTimeMillis()}.wav")
            currentAudioFile = file
            
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Toast.makeText(context, "当前设备不支持录音参数", Toast.LENGTH_SHORT).show()
                return
            }
            
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                Toast.makeText(context, "录音初始化失败", Toast.LENGTH_SHORT).show()
                return
            }
            
            audioRecord = recorder
            isRecording = true

            recordingJob = scope.launch(Dispatchers.IO) {
                recorder.startRecording()
                val fos = FileOutputStream(file)
                fos.use { output ->
                    // 写入 44 字节的 WAV 头部占位符
                    output.write(ByteArray(44))
                    
                    val buffer = ByteArray(bufferSize)
                    var totalAudioLen = 0L
                    while (isRecording) {
                        val read = recorder.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            output.write(buffer, 0, read)
                            totalAudioLen += read
                        }
                    }
                    
                    // 录音结束后更新 WAV 头部
                    updateWavHeader(file, totalAudioLen)
                }
                runCatching { recorder.stop() }
                recorder.release()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "录音失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        try {
            isRecording = false
            audioRecord = null
            
            currentAudioFile?.let { file ->
                scope.launch(Dispatchers.IO) {
                    recordingJob?.join()
                    // 读取 PCM 数据进行语音识别 (跳过 44 字节头部)
                    val pcmData = FileInputStream(file).use { fis ->
                        fis.skip(44)
                        fis.readBytes()
                    }
                    var transcription = if (isVoskReady) "正在处理语音..." else "语音记录 (Vosk 未就绪)"
                    
                    if (isVoskReady) {
                        voskRecognizer?.let { recognizer ->
                            recognizer.reset()
                            recognizer.acceptWaveForm(pcmData, pcmData.size)
                            val jsonResult = recognizer.finalResult
                            try {
                                val json = JSONObject(jsonResult)
                                val text = json.optString("text", "")
                                transcription = if (text.isNotEmpty()) text else "语音记录 (无文字内容)"
                            } catch (e: Exception) {
                                Log.e("EditorScreen", "Vosk result parse failed", e)
                                transcription = "语音记录 (解析失败)"
                            }
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        audioUris = audioUris + uri
                        audioTranscriptions = audioTranscriptions + (uri to transcription)
                        audioNames = audioNames + (uri to defaultAudioName(uri))
                        audioTranscriptionsVisibility = audioTranscriptionsVisibility + (uri to true)
                        recordingJob = null
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "停止录音失败", Toast.LENGTH_SHORT).show()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: selectedDate
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Box(modifier = modifier) {
        Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                topBar = {
                    UnifiedTopBar(
                        title = if (entryId == -1L) "新建日记" else "编辑日记",
                        navigationIcon = {
                            IconButton(onClick = { closeEditor() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    viewModel.saveEntry(
                                        id = entryId,
                                        title = title,
                                        content = content,
                                        date = selectedDate,
                                        imageUris = imageUris.joinToString(","),
                                        attachmentUris = attachmentUris.joinToString(","),
                                        audioUris = audioUris.joinToString(","),
                                        audioTranscriptions = audioUris.map { audioTranscriptions[it] ?: "" }.joinToString("|"),
                                        audioNames = audioUris.map { audioNames[it].takeUnless { name -> name.isNullOrBlank() } ?: defaultAudioName(it) }.joinToString("|"),
                                        audioTranscriptionsVisibility = audioUris.map { if (audioTranscriptionsVisibility[it] != false) "1" else "0" }.joinToString("|"),
                                        mood = mood,
                                        location = location
                                    )
                                    closeEditor()
                                }
                            ) { Icon(Icons.Default.Done, contentDescription = "Save") }
                        }
                    )
                }
            ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .graphicsLayer {
                        translationX = (1f - editorAnimationProgress) * slideDistancePx
                        alpha = 0.72f + 0.28f * editorAnimationProgress
                        scaleX = 0.985f + 0.015f * editorAnimationProgress
                        scaleY = 0.985f + 0.015f * editorAnimationProgress
                    }
            ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                val dateString = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate))
                Surface(
                    onClick = { showDatePicker = true },
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(text = dateString, style = MaterialTheme.typography.bodyLarge)
                        
                        if (mood.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Text(
                                    text = mood,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                if (location.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { showLocationDialog = true }
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = location,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 图片展示
                if (imageUris.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        imageUris.forEach { uri ->
                            Box {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { selectedViewerUri = uri }, // 点击放大
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(16.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .clickable { imageUris = imageUris - uri },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(10.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                    // 全屏查看对话框
                    selectedViewerUri?.let { uri ->
                        PhotoViewerDialog(
                            uri = uri,
                            onDismiss = { selectedViewerUri = null },
                            onDelete = {
                                imageUris = imageUris - uri
                                selectedViewerUri = null
                            },
                            onSave = { saveToGallery(uri) },
                            onShare = { shareImage(uri) }
                        )
                    }

                    // 录音展示
                    if (audioUris.isNotEmpty()) {
                        Text("录音", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        audioUris.forEach { uri ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val audioName = audioNames[uri].takeUnless { it.isNullOrBlank() } ?: defaultAudioName(uri)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .combinedClickable(
                                            onClick = {
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                        val mimeType = context.contentResolver.getType(uri) ?: "audio/wav"
                                                        setDataAndType(uri, mimeType)
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "无法播放音频: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onLongClick = {
                                                editingAudioUri = uri
                                                showAudioPropertiesDialog = true
                                            }
                                        ),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Audiotrack, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(audioName, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { 
                                            audioUris = audioUris - uri
                                            audioTranscriptions = audioTranscriptions - uri
                                            audioNames = audioNames - uri
                                            audioTranscriptionsVisibility = audioTranscriptionsVisibility - uri
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                                        }
                                    }
                                }
                                // 显示转换后的文字 (如果可见)
                                val transcription = audioTranscriptions[uri] ?: ""
                                val isVisible = audioTranscriptionsVisibility[uri] != false
                                if (transcription.isNotEmpty() && isVisible) {
                                    Text(
                                        text = transcription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (transcription.startsWith("Error:")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 附件展示
                    if (attachmentUris.isNotEmpty()) {
                        Text("附件", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        attachmentUris.forEach { uri ->
                            val fileName = remember(uri) {
                                var name = "Unknown File"
                                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (nameIndex != -1 && cursor.moveToFirst()) {
                                        name = cursor.getString(nameIndex)
                                    }
                                }
                                name
                            }
                            Card(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, context.contentResolver.getType(uri))
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "无法打开文件: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(fileName, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    IconButton(onClick = { attachmentUris = attachmentUris - uri }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Thoughts...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                EditorToolbar(
                    onAddImage = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onAddAttachment = {
                        attachmentLauncher.launch("*/*")
                    },
                    onVoiceInput = {
                        if (audioPermissionState.status.isGranted) {
                            if (isRecording) {
                                stopRecording()
                            } else {
                                if (isVoskReady) {
                                    startRecording()
                                } else {
                                    Toast.makeText(context, "语音识别模型正在初始化，请稍后...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            audioPermissionState.launchPermissionRequest()
                        }
                    },
                    onAddLocation = { fetchCurrentLocation() },
                    onAddLocationManual = { showLocationDialog = true },
                    onAddMood = { showMoodDialog = true },
                    onOpenCamera = {
                        if (cameraPermissionState.status.isGranted) {
                            openCamera()
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    isRecording = isRecording
                )
            }

            if (showMoodDialog) {
                MoodSelectionDialog(
                    onMoodSelected = {
                        mood = it
                        showMoodDialog = false
                    },
                    onDismiss = { showMoodDialog = false }
                )
            }

            if (showLocationDialog) {
                LocationInputDialog(
                    currentLocation = location,
                    onLocationConfirmed = {
                        location = it
                        showLocationDialog = false
                    },
                    onDismiss = { showLocationDialog = false }
                )
            }

            if (showAudioPropertiesDialog && editingAudioUri != null) {
                val uri = editingAudioUri!!
                val defaultAudioName = audioNames[uri].takeUnless { it.isNullOrBlank() } ?: defaultAudioName(uri)
                AudioPropertiesDialog(
                    initialName = defaultAudioName,
                    initialVisibility = audioTranscriptionsVisibility[uri] ?: true,
                    initialTranscription = audioTranscriptions[uri] ?: "",
                    onConfirm = { name, visibility, transcription ->
                        audioNames = audioNames + (uri to name)
                        audioTranscriptionsVisibility = audioTranscriptionsVisibility + (uri to visibility)
                        audioTranscriptions = audioTranscriptions + (uri to transcription)
                        showAudioPropertiesDialog = false
                        editingAudioUri = null
                    },
                    onDismiss = {
                        showAudioPropertiesDialog = false
                        editingAudioUri = null
                    }
                )
            }
            }
    }
}

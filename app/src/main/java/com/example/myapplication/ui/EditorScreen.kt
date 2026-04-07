package com.example.myapplication.ui

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.whisper.java.WhisperLib
import com.whisper.java.WhisperUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

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
        title = { Text("Enter Location") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Where are you?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onLocationConfirmed(text) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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

@OptIn(ExperimentalFoundationApi::class)
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
                        contentDescription = "语音转文字",
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                // 定位按钮 (点击自动获取，长按手动输入)
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .combinedClickable(
                            onClick = onAddLocation,
                            onLongClick = onAddLocationManual
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "定位", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onAddMood) {
                    Icon(Icons.Default.SentimentSatisfied, contentDescription = "心情", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EditorScreen(
    entryId: Long,
    onNavigateBack: () -> Unit,
    backgroundUri: Uri? = null,
    backgroundOpacity: Float = 0.6f,
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
    var mood by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var isRecording by remember { mutableStateOf(false) }
    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }
    var currentTranscription by remember { mutableStateOf("") }
    
    // Whisper TFLite (whisper_java) 相关
    var whisperLib by remember { mutableStateOf<WhisperLib?>(null) }
    val modelName = "whisper-tiny.tflite"

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
                    val uris = it.audioUris.split(",").map { uriStr -> Uri.parse(uriStr) }
                    val transcriptions = it.audioTranscriptions.split("|")
                    audioUris = uris
                    audioTranscriptions = uris.zip(transcriptions).toMap()
                }
            }
        }

        // 2. 初始化 WhisperLib
        withContext(Dispatchers.IO) {
            try {
                whisperLib = WhisperLib.init(context.assets, "models/$modelName")
                if (whisperLib == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Whisper 模型加载失败，请检查 assets/models 目录", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Throwable) {
                Log.e("EditorScreen", "WhisperLib init failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            whisperLib?.release()
        }
    }

    var selectedViewerUri by remember { mutableStateOf<Uri?>(null) }
    var showMoodDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    // 相机权限状态
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    // 录音权限状态
    val audioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    // 位置权限状态
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()

    fun fetchCurrentLocation() {
        if (locationPermissionState.status.isGranted) {
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            scope.launch {
                                try {
                                    val geocoder = android.location.Geocoder(context, Locale.getDefault())
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        geocoder.getFromLocation(loc.latitude, loc.longitude, 1) { addresses ->
                                            if (addresses.isNotEmpty()) {
                                                location = addresses[0].getAddressLine(0) ?: "${loc.latitude}, ${loc.longitude}"
                                            }
                                        }
                                    } else {
                                        // 在 IO 线程执行耗时的反向地理编码
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
                                    location = "${loc.latitude}, ${loc.longitude}"
                                }
                            }
                        } else {
                            Toast.makeText(context, "无法获取当前位置", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (e: SecurityException) {
                Toast.makeText(context, "权限不足", Toast.LENGTH_SHORT).show()
            }
        } else {
            locationPermissionState.launchPermissionRequest()
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

    LaunchedEffect(entryId) {
        if (entryId != -1L) {
            val entry = viewModel.getEntryById(entryId)
            entry?.let {
                title = it.title
                content = it.content
                selectedDate = it.date
                val loadedAudioUris = it.audioUris.split(",").filter { s -> s.isNotEmpty() }.map { s -> Uri.parse(s) }
                // 这里不能用 filter { s -> s.isNotEmpty() }，否则会破坏索引对应关系
                val loadedTranscriptions = it.audioTranscriptions.split("|")
                audioUris = loadedAudioUris
                // 如果长度不匹配，只取匹配的部分
                audioTranscriptions = loadedAudioUris.zip(loadedTranscriptions).toMap()
                imageUris = it.imageUris.split(",").filter { s -> s.isNotEmpty() }.map { s -> Uri.parse(s) }
                attachmentUris = it.attachmentUris.split(",").filter { s -> s.isNotEmpty() }.map { s -> Uri.parse(s) }
                mood = it.mood
                location = it.location
            }
        }
    }

    fun startRecording() {
        try {
            val audioDir = File(context.filesDir, "audio").apply { if (!exists()) mkdirs() }
            val file = File(audioDir, "recording_${System.currentTimeMillis()}.pcm")
            currentAudioFile = file
            
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            
            val recorder = AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            audioRecord = recorder
            isRecording = true
            currentTranscription = "正在识别中..."

            scope.launch(Dispatchers.IO) {
                recorder.startRecording()
                FileOutputStream(file).use { output ->
                    // --- WAV 录制辅助函数 (已迁移至 WhisperUtil) ---
                    // 1. 预留 44 字节的 WAV 头空间
                    WhisperUtil.writeWavHeader(output, 0)

                    val buffer = ByteArray(bufferSize)
                    while (isRecording) {
                        val read = recorder.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            output.write(buffer, 0, read)
                        }
                    }
                }
                recorder.stop()
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
                scope.launch(Dispatchers.Default) {
                    // 1. 更新 WAV 头
                    val totalAudioLen = file.length() - 44
                    if (totalAudioLen > 0) {
                        val raf = java.io.RandomAccessFile(file, "rw")
                        val bos = FileOutputStream(raf.fd)
                        WhisperUtil.writeWavHeader(bos, totalAudioLen)
                        bos.close()
                        raf.close()
                    }

                    // 2. 解码并转录
                    val audioData = WhisperUtil.decodeAudioFile(file)
                    
                    // 调用 WhisperLib 转文字
                    val result = if (whisperLib != null) {
                        WhisperLib.transcribe(whisperLib!!, audioData)
                    } else {
                        null
                    }
                    val transcription = result?.text ?: "识别失败 (Whisper 未就绪)"
                    
                    withContext(Dispatchers.Main) {
                        currentTranscription = transcription
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        audioUris = audioUris + uri
                        audioTranscriptions = audioTranscriptions + (uri to transcription)
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

    Box(modifier = modifier.fillMaxSize()) {
        // 自定义背景图片
        if (backgroundUri != null) {
            AsyncImage(
                model = backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // 半透明遮罩层 (无论是否有自定义背景都存在)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = backgroundOpacity))
        )

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (entryId == -1L) "New Entry" else "Edit Entry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
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
                                    mood = mood,
                                    location = location
                                )
                                onNavigateBack()
                            }
                        ) { Icon(Icons.Default.Done, contentDescription = "Save") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .imePadding()
                    .navigationBarsPadding()
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
                                Card(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, context.contentResolver.getType(uri) ?: "audio/*")
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "无法播放音频: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Audiotrack, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("语音记录 ${uri.lastPathSegment?.takeLast(10) ?: ""}", modifier = Modifier.weight(1f))
                                        IconButton(onClick = { 
                                            audioUris = audioUris - uri
                                            audioTranscriptions = audioTranscriptions - uri
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                                        }
                                    }
                                }
                                // 显示转换后的文字
                                val transcription = audioTranscriptions[uri] ?: ""
                                if (transcription.isNotEmpty()) {
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
                            if (isRecording) stopRecording() else startRecording()
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
        }
    }
}

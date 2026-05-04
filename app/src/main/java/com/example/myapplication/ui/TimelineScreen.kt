package com.example.myapplication.ui

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.DiaryEntry
import com.example.myapplication.util.DeviceIdManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import com.example.myapplication.ui.staggeredItemAnimation
import androidx.compose.animation.AnimatedVisibility

private val ScreenTitleFontSize = 30.sp

// 定义滑动状态
enum class SwipeState { Settled, Open }

// 生成二维码 Bitmap（在后台线程执行）
private suspend fun generateQrCodeBitmap(text: String, size: Int): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}

// 个人识别码弹窗：上部显示二维码，下部显示设备 ID 并可点击复制
@Composable
fun DeviceIdDialog(
    deviceId: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var copyFeedback by remember { mutableStateOf(false) }

    // 异步生成二维码
    LaunchedEffect(deviceId) {
        qrBitmap = generateQrCodeBitmap(deviceId, 512)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "个人识别码",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 二维码区域
                Card(
                    modifier = Modifier.size(220.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                painter = BitmapPainter(qrBitmap!!),
                                contentDescription = "个人识别码二维码",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 设备 ID 文本，点击复制
                Text(
                    text = "点击下方识别码复制",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(deviceId))
                            copyFeedback = true
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = deviceId,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (copyFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            modifier = Modifier.size(18.dp),
                            tint = if (copyFeedback) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // 复制反馈提示
                AnimatedVisibility(visible = copyFeedback) {
                    Text(
                        text = "已复制到剪贴板",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 关闭按钮
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    }
}

// 时间线主页面：展示日记列表、背景图层与新增入口
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onEntryClick: (Long) -> Unit,
    onAddEntryClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = viewModel(),
    avatarUri: Uri? = null
) {
    // 订阅 ViewModel 中的日记流，驱动列表实时刷新
    val entries by viewModel.entries.collectAsState()
    // 追踪当前打开了菜单的日记 ID
    var openEntryId by remember { mutableStateOf<Long?>(null) }
    // 控制个人识别码弹窗
    var showDeviceIdDialog by remember { mutableStateOf(false) }
    // 获取设备 ID
    val context = LocalContext.current
    val deviceId = remember { DeviceIdManager.getDeviceId(context) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            UnifiedTopBar(
                title = "我的日记",
                actions = {
                    // 头像按钮：右边距留白，避免紧贴屏幕边缘
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { showDeviceIdDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUri != null) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = "个人识别码",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // 默认头像占位
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "个人识别码",
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val fabScale = remember { Animatable(1f) }
            val scope = rememberCoroutineScope()
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        // Scale down with a quick spring
                        fabScale.animateTo(
                            targetValue = 0.75f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                        // Navigate to editor
                        onAddEntryClick()
                        // Spring back to original size
                        fabScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.scale(fabScale.value)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { padding ->
        // 个人识别码弹窗
        if (showDeviceIdDialog) {
            DeviceIdDialog(
                deviceId = deviceId,
                onDismiss = { showDeviceIdDialog = false }
            )
        }

        // 背景点击监听：点击空白区域关闭所有已打开的二级菜单
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures { openEntryId = null }
                }
        ) {
            if (entries.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = entries,
                        key = { it.id }
                    ) { entry ->
                        val itemIndex = entries.indexOf(entry)
                        AnimatedVisibility(
                            visible = true,
                            enter = staggeredItemAnimation(delayMs = itemIndex * 50)
                        ) {
                            SwipeableDiaryItem(
                                entry = entry,
                                isOpen = openEntryId == entry.id,
                                onOpened = { openEntryId = entry.id },
                                onClosed = { if (openEntryId == entry.id) openEntryId = null },
                                onClick = { onEntryClick(entry.id) },
                                onDelete = {
                                    viewModel.deleteEntry(entry)
                                    openEntryId = null
                                },
                                onPin = {
                                    viewModel.togglePin(entry)
                                    openEntryId = null
                                },
                                modifier = Modifier
                            )
                        }
                    }
                }
            }
        }
    }
}

// 可侧滑的日记卡片：左滑显示置顶/删除操作
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableDiaryItem(
    entry: DiaryEntry,
    isOpen: Boolean,
    onOpened: () -> Unit,
    onClosed: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 用于 dp 与 px 互转，保证手势阈值和位移计算准确
    val density = LocalDensity.current
    // 控制删除状态
    var isDeleting by remember { mutableStateOf(false) }
    
    // 设置露出的按钮宽度
    val actionWidth = 140.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    
    // 定义吸附动画规格：无回弹
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // 使用 AnchoredDraggableState 管理滑动状态
    val state = remember {
        AnchoredDraggableState(
            initialValue = SwipeState.Settled,
            positionalThreshold = { distance: Float -> distance * 0.3f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = springSpec,
            decayAnimationSpec = exponentialDecay()
        ).apply {
            updateAnchors(
                DraggableAnchors {
                    SwipeState.Settled at 0f
                    SwipeState.Open at -actionWidthPx
                }
            )
        }
    }

    // 监听内部滑动状态的变化，并通知外部
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeState.Open) {
            onOpened()
        } else if (state.currentValue == SwipeState.Settled) {
            onClosed()
        }
    }

    // 响应外部关闭指令（如点击了背景或其他卡片）
    LaunchedEffect(isOpen) {
        if (!isOpen && state.currentValue == SwipeState.Open) {
            state.animateTo(SwipeState.Settled)
        }
    }

    // 删除时直接从列表中移除
    if (!isDeleting) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            // 背景按钮容器：跟着卡片滑出来
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight()
                    .offset {
                        // 将按钮初始位置偏移到屏幕外，随着滑动偏移量增加而滑入
                        val offset = if (state.offset.isNaN()) 0f else state.offset
                        IntOffset((actionWidthPx + offset).roundToInt(), 0)
                    }
                    .graphicsLayer {
                        // 只有当有滑动偏移时才显示背景按钮，防止由于透明度导致的意外显示
                        val offset = if (state.offset.isNaN()) 0f else state.offset
                        alpha = if (offset == 0f) 0f else 1f
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledIconButton(
                    onClick = onPin,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (entry.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pin")
                }
                
                FilledIconButton(
                    onClick = {
                        isDeleting = true
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            // 前景卡片层：承载可拖拽的日记内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { 
                        val offset = if (state.offset.isNaN()) 0f else state.offset
                        IntOffset(offset.roundToInt(), 0) 
                    }
                    .anchoredDraggable(state, Orientation.Horizontal)
            ) {
                DiaryEntryItem(
                    entry = entry, 
                    onClick = onClick
                )
            }
        }
    }

    // 触发真正的删除操作
    if (isDeleting) {
        LaunchedEffect(Unit) {
            // 立即执行删除，不使用延迟等待动画
            onDelete()
        }
    }
}

// 空状态视图：当没有日记时给出引导提示
@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your diary is empty",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = "Tap the + button to write your first entry.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
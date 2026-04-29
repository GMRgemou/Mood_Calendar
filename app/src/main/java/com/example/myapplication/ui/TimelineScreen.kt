package com.example.myapplication.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.DiaryEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import android.net.Uri
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.myapplication.ui.staggeredItemAnimation
import androidx.compose.animation.AnimatedVisibility

private val ScreenTitleFontSize = 30.sp

// 定义滑动状态
enum class SwipeState { Settled, Open }

// 时间线主页面：展示日记列表、背景图层与新增入口
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onEntryClick: (Long) -> Unit,
    onAddEntryClick: () -> Unit,
    backgroundUri: Uri? = null,
    backgroundOpacity: Float = 0.6f,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = viewModel()
) {
    // 订阅 ViewModel 中的日记流，驱动列表实时刷新
    val entries by viewModel.entries.collectAsState()
    // 追踪当前打开了菜单的日记 ID
    var openEntryId by remember { mutableStateOf<Long?>(null) }

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
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                UnifiedTopBar(title = "我的日记")
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
                                    onClick = {
                                        if (openEntryId != null) {
                                            onEntryClick(entry.id)
                                        } else {
                                            onEntryClick(entry.id)
                                        }
                                    },
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
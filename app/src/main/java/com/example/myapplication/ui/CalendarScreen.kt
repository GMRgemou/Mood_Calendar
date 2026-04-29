package com.example.myapplication.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.data.DiaryEntry
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
// ── Constants ──────────────────────────────────────────────
private const val CALENDAR_COLUMNS = 7
private const val CALENDAR_CELLS = 42   // 6 weeks × 7 days

// ── Main Screen ────────────────────────────────────────────

@Composable
fun CalendarScreen(
    onEntryClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    backgroundUri: Uri? = null,
    backgroundOpacity: Float = 0.6f,
    viewModel: DiaryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.of(today.year, today.month)) }
    var selectedDate by remember { mutableStateOf(today) }
    val zoneId = remember { ZoneId.systemDefault() }

    // Toggle visibility to re-trigger card entrance animations on each page visit
    var cardsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        cardsVisible = true
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

        // 半透明遮罩层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = backgroundOpacity))
        )

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            topBar = {
                UnifiedTopBar(title = "日历")
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                CalendarHeader(
                    month = month,
                    onPrevMonth = { month = month.minusMonths(1) },
                    onNextMonth = { month = month.plusMonths(1) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Pre-compute all derived data in a single memoized block
                val (totalCount, streakDays, entryDates, monthEntries) = remember(entries, month, zoneId) {
                    val dates = entries.map { it.toLocalDate(zoneId) }
                    val dateSet = dates.toSet()
                    val filtered = entries.filterIndexed { index, _ ->
                        YearMonth.from(dates[index]) == month
                    }
                    val count = entries.size
                    val streak = calculateStreakDays(entries, zoneId)
                    Result4(count, streak, dateSet, filtered)
                }
                val monthCount = remember(monthEntries) { monthEntries.size }

                AnimatedVisibility(
                    visible = cardsVisible,
                    enter = cardEntranceAnimation(delayMs = 100)
                ) {
                    StatsCard(streakDays = streakDays, totalCount = totalCount, monthCount = monthCount)
                }
                Spacer(modifier = Modifier.height(14.dp))

                AnimatedVisibility(
                    visible = cardsVisible,
                    enter = cardEntranceAnimation(delayMs = 200)
                ) {
                    CalendarGridCard(
                        month = month,
                        selectedDate = selectedDate,
                        entryDates = entryDates,
                        onDateClick = { day -> selectedDate = day }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                val selectedEntries = remember(entries, selectedDate, zoneId) {
                    entries.filter {
                        it.toLocalDate(zoneId) == selectedDate
                    }.sortedByDescending {
                        it.date
                    }
                }
                AnimatedVisibility(
                    visible = cardsVisible,
                    enter = cardEntranceAnimation(delayMs = 300)
                ) {
                    DailyEntryCard(
                        date = selectedDate,
                        entries = selectedEntries,
                        onEntryClick = onEntryClick
                    )
                }
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────

@Composable
private fun CalendarHeader(
    month: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val textSub = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = month.format(DateTimeFormatter.ofPattern("yyyy年MM月", Locale.CHINA)),
            style = MaterialTheme.typography.titleMedium,
            color = textSub,
            fontWeight = FontWeight.SemiBold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上一月", tint = textSub)
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "下一月", tint = textSub)
            }
        }
    }
}

@Composable
private fun StatsCard(streakDays: Int, totalCount: Int, monthCount: Int) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.primaryContainer)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "连续记录", color = colors.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$streakDays 天",
                    color = colors.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            StatRow(label = "总日记数", value = totalCount.toString())
            Spacer(modifier = Modifier.height(8.dp))
            StatRow(label = "本月日记数", value = monthCount.toString())

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = colors.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "保持当下的节奏，每一天都算数。",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.onSurfaceVariant)
        Text(text = value, color = colors.onSurface, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalendarGridCard(
    month: YearMonth,
    selectedDate: LocalDate,
    entryDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val weeks = remember(month) {
        buildCalendarCells(month).chunked(CALENDAR_COLUMNS)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            WeekHeader()
            Spacer(modifier = Modifier.height(8.dp))

            weeks.forEachIndexed { weekIndex, week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            selectedDate = selectedDate,
                            hasEntry = date != null && date in entryDates,
                            onDateClick = onDateClick
                        )
                    }
                }
                if (weekIndex < weeks.lastIndex) Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun WeekHeader() {
    val textSub = MaterialTheme.colorScheme.onSurfaceVariant
    val names = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        names.forEach {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Text(text = it, color = textSub, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    selectedDate: LocalDate,
    hasEntry: Boolean,
    onDateClick: (LocalDate) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isSelected = date == selectedDate
    val isToday = date == LocalDate.now()
    val entryDotColor = when {
        isSelected -> colors.onPrimary
        hasEntry -> colors.primary
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    date == null -> Color.Transparent
                    isSelected -> colors.primary
                    hasEntry -> colors.primaryContainer
                    isToday -> colors.secondaryContainer
                    else -> Color.Transparent
                }
            )
            .then(
                if (date != null) Modifier.clickable { onDateClick(date) }
                else Modifier
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date?.dayOfMonth?.toString() ?: "",
            color = when {
                date == null -> Color.Transparent
                isSelected -> colors.onPrimary
                hasEntry -> colors.onPrimaryContainer
                isToday -> colors.onSecondaryContainer
                else -> colors.onSurface
            },
            fontWeight = if (isSelected || isToday || hasEntry) FontWeight.Bold else FontWeight.Medium
        )

        // Keep the diary marker visible even when the day is today or currently selected.
        // Previously, a diary on today's pre-selected cell was hidden by the selected-day
        // background, making it look like today's entry was not shown on the calendar.
        if (date != null && hasEntry) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(entryDotColor)
            )
        }
    }
}

@Composable
private fun DailyEntryCard(
    date: LocalDate,
    entries: List<DiaryEntry>,
    onEntryClick: (Long) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = "当日日记",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (entries.isEmpty()) {
                Text(
                    text = "当天暂无日记记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    lineHeight = 22.sp
                )
            } else {
                Text(
                    text = "共 ${entries.size} 篇日记，点击可查看或编辑。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                entries.forEachIndexed { index, entry ->
                    DiaryEntryItem(
                        entry = entry,
                        onClick = { onEntryClick(entry.id) }
                    )
                    if (index < entries.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────

private fun buildCalendarCells(month: YearMonth): List<LocalDate?> {
    val firstDayOffset = month.atDay(1).dayOfWeek.ordinal
    val cells = MutableList<LocalDate?>(CALENDAR_CELLS) { null }
    for (day in 1..month.lengthOfMonth()) {
        cells[firstDayOffset + day - 1] = month.atDay(day)
    }
    return cells
}

private fun calculateStreakDays(
    entries: List<DiaryEntry>,
    zoneId: ZoneId
): Int {
    if (entries.isEmpty()) return 0

    val distinctDays = entries
        .map { it.toLocalDate(zoneId) }
        .toSet()

    var cursor = LocalDate.now(zoneId)
    var streak = 0
    while (cursor in distinctDays) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun DiaryEntry.toLocalDate(zoneId: ZoneId): LocalDate {
    return Instant.ofEpochMilli(date).atZone(zoneId).toLocalDate()
}

// ── Multi-result holder to batch computations ──────────────

private data class Result4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
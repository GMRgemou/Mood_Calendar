package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PageBg = Color(0xFFF6F8FF)
private val CardBg = Color(0xFFFFFFFF)
private val Accent = Color(0xFF6C63FF)
private val AccentSoft = Color(0xFFEAE8FF)
private val TextMain = Color(0xFF1D2233)
private val TextSub = Color(0xFF7B8294)
private val Success = Color(0xFF2EBD85)
private val TodayBg = Color(0xFFEEF1FF)

@Composable
fun CalendarScreen(
    onEntryClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.of(today.year, today.month)) }
    var selectedDate by remember { mutableStateOf(today) }
    val zoneId = remember { ZoneId.systemDefault() }

    Scaffold(
        modifier = modifier,
        containerColor = PageBg,
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

            val monthEntries = remember(entries, month, zoneId) {
                entries.filter { entry ->
                    val date = Instant.ofEpochMilli(entry.date).atZone(zoneId).toLocalDate()
                    YearMonth.from(date) == month
                }
            }
            val totalCount = remember(entries) { entries.size }
            val streakDays = remember(entries, zoneId) { calculateStreakDays(entries, zoneId) }
            val monthCount = remember(monthEntries) { monthEntries.size }

            StatsCard(streakDays = streakDays, totalCount = totalCount, monthCount = monthCount)
            Spacer(modifier = Modifier.height(14.dp))

            val entryDates = remember(entries, zoneId) {
                entries
                    .map { Instant.ofEpochMilli(it.date).atZone(zoneId).toLocalDate() }
                    .toSet()
            }

            CalendarGridCard(
                month = month,
                selectedDate = selectedDate,
                entryDates = entryDates,
                onDateClick = { day -> selectedDate = day }
            )
            Spacer(modifier = Modifier.height(14.dp))

            val selectedEntry = remember(entries, selectedDate, zoneId) {
                entries.firstOrNull {
                    Instant.ofEpochMilli(it.date).atZone(zoneId).toLocalDate() == selectedDate
                }
            }
            DailyEntryCard(date = selectedDate, entry = selectedEntry)
        }
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = month.format(DateTimeFormatter.ofPattern("yyyy年MM月", Locale.CHINA)),
            style = MaterialTheme.typography.titleMedium,
            color = TextSub,
            fontWeight = FontWeight.SemiBold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上一月", tint = TextSub)
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "下一月", tint = TextSub)
            }
        }
    }
}

@Composable
private fun StatsCard(streakDays: Int, totalCount: Int, monthCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AccentSoft)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "连续记录", color = TextSub, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$streakDays 天",
                    color = TextMain,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF8F9FE))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("总日记数", color = TextSub)
                Text(
                    text = totalCount.toString(),
                    color = TextMain,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF8F9FE))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("本月日记数", color = TextSub)
                Text(
                    text = monthCount.toString(),
                    color = TextMain,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "保持当下的节奏，每一天都算数。",
                    color = TextSub,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CalendarGridCard(
    month: YearMonth,
    selectedDate: LocalDate,
    entryDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit
) {
    val days = remember(month) { buildCalendarCells(month) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            WeekHeader()
            Spacer(modifier = Modifier.height(8.dp))

            for (weekIndex in 0 until 6) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (dayIndex in 0 until 7) {
                        val date = days[weekIndex * 7 + dayIndex]
                        DayCell(
                            date = date,
                            selectedDate = selectedDate,
                            hasEntry = date != null && date in entryDates,
                            onDateClick = onDateClick
                        )
                    }
                }
                if (weekIndex != 5) Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun WeekHeader() {
    val names = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        names.forEach {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Text(text = it, color = TextSub, style = MaterialTheme.typography.labelMedium)
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
    val isSelected = date == selectedDate
    val isToday = date == LocalDate.now()

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    date == null -> Color.Transparent
                    isSelected -> Accent
                    hasEntry -> AccentSoft
                    isToday -> TodayBg
                    else -> Color.Transparent
                }
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date?.dayOfMonth?.toString() ?: "",
            color = when {
                date == null -> Color.Transparent
                isSelected -> Color.White
                hasEntry -> Accent
                else -> TextMain
            },
            fontWeight = if (isSelected || isToday || hasEntry) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Transparent)
                .padding(2.dp)
        )

        if (date != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(Color.Transparent)
            ) {
                IconButton(
                    onClick = { onDateClick(date) },
                    modifier = Modifier.matchParentSize()
                ) {
                    Spacer(modifier = Modifier.size(0.dp))
                }
            }
        }
    }
}

@Composable
private fun DailyEntryCard(date: LocalDate, entry: com.example.myapplication.data.DiaryEntry?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = "当日日记",
                style = MaterialTheme.typography.titleMedium,
                color = TextMain,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)),
                style = MaterialTheme.typography.bodySmall,
                color = TextSub
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = entry?.content?.takeIf { it.isNotBlank() } ?: "当天暂无日记记录。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMain,
                lineHeight = 22.sp
            )
        }
    }
}

private fun buildCalendarCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val firstDayOffset = when (firstDay.dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }

    val cells = MutableList<LocalDate?>(42) { null }
    for (day in 1..month.lengthOfMonth()) {
        cells[firstDayOffset + day - 1] = month.atDay(day)
    }
    return cells
}

private fun calculateStreakDays(
    entries: List<com.example.myapplication.data.DiaryEntry>,
    zoneId: ZoneId
): Int {
    if (entries.isEmpty()) return 0

    val distinctDays = entries
        .map { Instant.ofEpochMilli(it.date).atZone(zoneId).toLocalDate() }
        .toSet()

    var cursor = LocalDate.now(zoneId)
    var streak = 0
    while (cursor in distinctDays) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

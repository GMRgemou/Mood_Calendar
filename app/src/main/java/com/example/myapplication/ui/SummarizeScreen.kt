package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.DiaryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarizeScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "日记总结",
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SummaryCard(
                    title = "总计日记",
                    value = entries.size.toString(),
                    icon = Icons.Default.StickyNote2,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }

            if (entries.isNotEmpty()) {
                val mostCommonMood = entries
                    .filter { it.mood.isNotEmpty() }
                    .groupBy { it.mood }
                    .maxByOrNull { it.value.size }
                    ?.key ?: "暂无数据"

                item {
                    SummaryCard(
                        title = "最常出现的心情",
                        value = mostCommonMood,
                        icon = Icons.Default.Mood,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    )
                }

                val currentMonth = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                }.timeInMillis

                val entriesThisMonth = entries.count { it.date >= currentMonth }

                item {
                    SummaryCard(
                        title = "本月日记数",
                        value = entriesThisMonth.toString(),
                        icon = Icons.Default.BarChart,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "快去写日记吧，写完之后这里会有统计哦！",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

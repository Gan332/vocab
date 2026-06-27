package com.vocabapp.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vocabapp.data.db.entity.HistoryEntity
import com.vocabapp.ui.theme.*
import com.vocabapp.util.TimeUtils
import com.vocabapp.ui.learn.IosCard

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "统计",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary row
            item {
                IosCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryValue("学习次数", uiState.sessionCount.toString())
                        SummaryValue("平均正确率", uiState.averageAccuracy)
                        SummaryValue("总用时", uiState.totalDuration)
                    }
                }
            }

            // Daily goal section
            item {
                DailyGoalSection(uiState, viewModel)
            }

            // Weekly chart
            if (uiState.weeklyData.isNotEmpty()) {
                item {
                    WeeklyChartSection(uiState.weeklyData)
                }
            }

            // History header
            item {
                Text(
                    "历史记录",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            if (uiState.history.isEmpty()) {
                item {
                    IosCard {
                        Text(
                            "暂无学习记录",
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(uiState.history, key = { it.id }) { history ->
                HistoryItem(history)
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeeklyChartSection(weeklyData: List<WeeklyBar>) {
    val maxCount = weeklyData.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    IosCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "📊 本周学习",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.forEach { bar ->
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (bar.count > 0) {
                            Text(
                                bar.count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 4.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val barHeight = if (bar.count > 0) {
                                (bar.count.toFloat() / maxCount).coerceAtLeast(0.03f)
                            } else 0.03f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(barHeight)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (bar.count > 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                        Text(
                            bar.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (bar.isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyGoalSection(
    uiState: StatsUiState,
    viewModel: StatsViewModel
) {
    val goal = uiState.dailyGoal
    val todayCount = uiState.todayCount
    val pct = if (goal > 0) (todayCount.toFloat() / goal).coerceAtMost(1f) else 0f
    val streak = uiState.streak

    IosCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📅 每日目标", style = MaterialTheme.typography.headlineLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    var goalText by remember { mutableStateOf(goal.toString()) }
                    LaunchedEffect(goal) {
                        if (goalText.toIntOrNull() != goal) goalText = goal.toString()
                    }
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = {
                            goalText = it
                            it.toIntOrNull()?.let { v -> viewModel.updateDailyGoal(v) }
                        },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text("词/天", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = pct,
                modifier = Modifier.fillMaxWidth().height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = iosGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$todayCount / $goal", style = MaterialTheme.typography.bodyMedium,
                    color = iosGreen, fontWeight = FontWeight.SemiBold)
                Text("🔥 $streak 天连续打卡", style = MaterialTheme.typography.bodyMedium,
                    color = iosOrange, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            // Calendar grid (28 days)
            val checkinMap = remember(uiState.checkins) {
                uiState.checkins.associate { it.date to it.count }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                        Text(day, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(4.dp))
                for (week in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (dayOfWeek in 0 until 7) {
                            val dayOffset = (28 - 1) - (week * 7 + (6 - dayOfWeek))
                            val date = TimeUtils.getDateString(-dayOffset)
                            val count = checkinMap[date] ?: 0
                            val isToday = dayOffset == 0
                            val isDone = count >= goal
                            val isPartial = count > 0

                            val bgColor = when {
                                isDone -> iosGreen
                                isPartial -> iosGreenLight
                                else -> Color.Transparent
                            }
                            val textColor = when {
                                isDone -> Color.White
                                isPartial -> iosGreen
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier.size(28.dp)
                                    .clip(CircleShape).background(bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                val dayNum = date.substringAfterLast("-").removePrefix("0").toIntOrNull() ?: 0
                                Text("$dayNum", style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(history: HistoryEntity) {
    val modeLabels = mapOf(
        "flashcard" to "闪卡", "quiz" to "答题",
        "typing" to "打字", "srs" to "间隔重复"
    )

    IosCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(history.bankName, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("${history.total} 题 · ${history.remembered} 正确",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(modeLabels[history.mode] ?: "闪卡",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp))
                    if (history.interrupted) {
                        Text("中断", style = MaterialTheme.typography.labelSmall,
                            color = iosOrange,
                            modifier = Modifier
                                .background(iosOrangeLight, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${history.accuracy}%", style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, color = iosGreen)
                Text("${TimeUtils.formatDuration(history.duration)} · ${
                    java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(history.date))
                }", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

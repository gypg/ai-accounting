package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.components.charts.PieChart
import com.example.aiaccounting.ui.components.charts.TrendChart
import com.example.aiaccounting.ui.components.charts.BarChart
import com.example.aiaccounting.ui.components.charts.MonthlyData
import com.example.aiaccounting.ui.viewmodel.StatisticsViewModel
import com.example.aiaccounting.ui.viewmodel.StatisticsData
import com.example.aiaccounting.ui.viewmodel.CategoryStat
import com.example.aiaccounting.ui.theme.HorseTheme2026Colors
import com.example.aiaccounting.utils.NumberUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorseStatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    var showDatePickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HorseTheme2026Colors.Background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "📊 统计分析",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HorseTheme2026Colors.GoldenText
                )
            },
            actions = {
                IconButton(onClick = { showDatePickerDialog = true }) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "筛选",
                        tint = HorseTheme2026Colors.GoldenText
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = HorseTheme2026Colors.Primary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            val timeDisplayText = getHorseTimeDisplayText(uiState.timeFilter)
            if (timeDisplayText.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = HorseTheme2026Colors.Secondary
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 统计时间：$timeDisplayText",
                            fontSize = 14.sp,
                            color = HorseTheme2026Colors.GoldenText
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = HorseTheme2026Colors.GoldenText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (showDatePickerDialog) {
                HorseDatePickerDialog(
                    timeFilter = uiState.timeFilter,
                    onTimeFilterSelected = {
                        viewModel.setTimeFilter(it)
                        showDatePickerDialog = false
                    },
                    onDismiss = { showDatePickerDialog = false }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorseStatCard(
                    title = "💰 总收入",
                    amount = statistics.totalIncome,
                    color = HorseTheme2026Colors.Income,
                    modifier = Modifier.weight(1f)
                )
                HorseStatCard(
                    title = "💸 总支出",
                    amount = statistics.totalExpense,
                    color = HorseTheme2026Colors.Expense,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                HorseTabButton(
                    text = "收入",
                    selected = uiState.selectedTab == "income",
                    onClick = { viewModel.setSelectedTab("income") },
                    modifier = Modifier.weight(1f)
                )
                HorseTabButton(
                    text = "支出",
                    selected = uiState.selectedTab == "expense",
                    onClick = { viewModel.setSelectedTab("expense") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (statistics.categoryStats.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📈 分类统计",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = HorseTheme2026Colors.GoldenText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PieChart(
                            data = statistics.categoryStats,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (statistics.monthlyTrend.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📊 月度趋势",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = HorseTheme2026Colors.GoldenText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TrendChart(
                            data = statistics.monthlyTrend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (statistics.categoryStats.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📋 分类详情",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = HorseTheme2026Colors.GoldenText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(
                            modifier = Modifier.height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(statistics.categoryStats) { stat ->
                                CategoryStatItem(stat = stat)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun HorseStatCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = NumberUtils.formatMoney(amount),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun HorseTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) HorseTheme2026Colors.Primary else Color.LightGray
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) HorseTheme2026Colors.GoldenText else Color.DarkGray
        )
    }
}

@Composable
fun CategoryStatItem(stat: CategoryStat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(stat.color))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stat.name,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }
        Text(
            text = "${NumberUtils.formatMoney(stat.amount)} (${(stat.percentage * 100).toInt()}%)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
    }
}

@Composable
fun HorseDatePickerDialog(
    timeFilter: String,
    onTimeFilterSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "📅 选择统计时间",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HorseTheme2026Colors.GoldenText
                )
                Spacer(modifier = Modifier.height(24.dp))

                val options = listOf(
                    "本月" to "current",
                    "上月" to "last",
                    "今年" to "year-${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}"
                )

                options.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTimeFilterSelected(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = timeFilter == value,
                            onClick = { onTimeFilterSelected(value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = HorseTheme2026Colors.Primary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HorseTheme2026Colors.Primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("确定", color = HorseTheme2026Colors.GoldenText)
                }
            }
        }
    }
}

private fun getHorseTimeDisplayText(timeFilter: String): String {
    return when {
        timeFilter == "current" -> {
            val calendar = java.util.Calendar.getInstance()
            "${calendar.get(java.util.Calendar.YEAR)}年${calendar.get(java.util.Calendar.MONTH) + 1}月"
        }
        timeFilter == "last" -> {
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.MONTH, -1)
            "${calendar.get(java.util.Calendar.YEAR)}年${calendar.get(java.util.Calendar.MONTH) + 1}月"
        }
        timeFilter.startsWith("year-") -> {
            val year = timeFilter.substringAfter("year-")
            "${year}年"
        }
        timeFilter.matches(Regex("\\d{4}-\\d{2}")) -> {
            val parts = timeFilter.split("-")
            "${parts[0]}年${parts[1]}月"
        }
        else -> ""
    }
}

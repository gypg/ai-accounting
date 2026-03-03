package com.example.aiaccounting.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * 月度数据点
 */
data class MonthlyData(
    val month: String,
    val income: Double,
    val expense: Double
)

/**
 * 趋势图组件 - 显示收支趋势
 */
@Composable
fun TrendChart(
    data: List<MonthlyData>,
    modifier: Modifier = Modifier,
    showIncome: Boolean = true,
    showExpense: Boolean = true
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)

    Column(
        modifier = modifier
    ) {
        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIncome) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(incomeColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "收入",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            if (showExpense) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(expenseColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "支出",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 图表区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val padding = 40f

                val chartWidth = canvasWidth - padding * 2
                val chartHeight = canvasHeight - padding * 2

                // 计算最大值
                val maxIncome = data.maxOfOrNull { it.income } ?: 0.0
                val maxExpense = data.maxOfOrNull { it.expense } ?: 0.0
                val maxValue = max(maxIncome, maxExpense) * 1.1

                if (maxValue <= 0) return@Canvas

                val xStep = chartWidth / (data.size - 1).coerceAtLeast(1)

                // 绘制网格线
                val gridLines = 5
                for (i in 0..gridLines) {
                    val y = padding + chartHeight * i / gridLines
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(padding, y),
                        end = Offset(canvasWidth - padding, y),
                        strokeWidth = 1f
                    )
                }

                // 绘制收入折线
                if (showIncome) {
                    val incomePath = Path()
                    data.forEachIndexed { index, monthlyData ->
                        val x = padding + index * xStep
                        val y = if (maxValue > 0) {
                            padding + chartHeight * (1 - monthlyData.income / maxValue).toFloat()
                        } else {
                            padding + chartHeight
                        }

                        if (index == 0) {
                            incomePath.moveTo(x, y * animationProgress.value + (padding + chartHeight) * (1 - animationProgress.value))
                        } else {
                            incomePath.lineTo(x, y * animationProgress.value + (padding + chartHeight) * (1 - animationProgress.value))
                        }
                    }

                    drawPath(
                        path = incomePath,
                        color = incomeColor,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )

                    // 绘制收入数据点
                    data.forEachIndexed { index, monthlyData ->
                        val x = padding + index * xStep
                        val y = if (maxValue > 0) {
                            padding + chartHeight * (1 - monthlyData.income / maxValue).toFloat()
                        } else {
                            padding + chartHeight
                        }

                        drawCircle(
                            color = incomeColor,
                            radius = 4f,
                            center = Offset(x, y * animationProgress.value + (padding + chartHeight) * (1 - animationProgress.value))
                        )
                    }
                }

                // 绘制支出折线
                if (showExpense) {
                    val expensePath = Path()
                    data.forEachIndexed { index, monthlyData ->
                        val x = padding + index * xStep
                        val y = if (maxValue > 0) {
                            padding + chartHeight * (1 - monthlyData.expense / maxValue).toFloat()
                        } else {
                            padding + chartHeight
                        }

                        if (index == 0) {
                            expensePath.moveTo(x, y * animationProgress.value + (padding + chartHeight) * (1 - animationProgress.value))
                        } else {
                            expensePath.lineTo(x, y * animationProgress.value + (padding + chartHeight) * (1 - animationProgress.value))
                        }
                    }

                    drawPath(
                        path = expensePath,
                        color = expenseColor,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )

                    // 绘制支出数据点
                    data.forEachIndexed { index, monthlyData ->
                        val x = padding + index * xStep
                        val y = if (maxValue > 0) {
                            padding + chartHeight * (1 - monthlyData.expense / maxValue).toFloat()
                        } else {
                            padding + chartHeight
                        }

                        drawCircle(
                            color = expenseColor,
                            radius = 4f,
                            center = Offset(x, y * animationProgress.value + (padding + chartHeight) * (1 - animationProgress.value))
                        )
                    }
                }

                // 绘制X轴标签（月份）
                data.forEachIndexed { index, monthlyData ->
                    val x = padding + index * xStep
                    // 只显示部分标签避免重叠
                    if (data.size <= 6 || index % (data.size / 6 + 1) == 0) {
                        // 这里简化处理，实际应该使用drawText
                    }
                }
            }
        }

        // X轴标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEachIndexed { index, monthlyData ->
                if (data.size <= 6 || index % (data.size / 6 + 1) == 0) {
                    Text(
                        text = monthlyData.month,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 柱状图组件 - 显示月度对比
 */
@Composable
fun BarChart(
    data: List<MonthlyData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)

    Column(
        modifier = modifier
    ) {
        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(incomeColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "收入",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(expenseColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "支出",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 图表
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val padding = 32f

                val chartWidth = canvasWidth - padding * 2
                val chartHeight = canvasHeight - padding * 2

                val maxValue = max(
                    data.maxOfOrNull { it.income } ?: 0.0,
                    data.maxOfOrNull { it.expense } ?: 0.0
                ) * 1.1

                if (maxValue <= 0) return@Canvas

                val barGroupWidth = chartWidth / data.size
                val barWidth = barGroupWidth * 0.35f
                val spacing = barGroupWidth * 0.1f

                data.forEachIndexed { index, monthlyData ->
                    val groupX = padding + index * barGroupWidth

                    // 收入柱
                    val incomeHeight = if (maxValue > 0) {
                        (monthlyData.income / maxValue * chartHeight).toFloat() * animationProgress.value
                    } else 0f

                    drawRect(
                        color = incomeColor,
                        topLeft = Offset(
                            groupX + spacing,
                            padding + chartHeight - incomeHeight
                        ),
                        size = androidx.compose.ui.geometry.Size(barWidth, incomeHeight)
                    )

                    // 支出柱
                    val expenseHeight = if (maxValue > 0) {
                        (monthlyData.expense / maxValue * chartHeight).toFloat() * animationProgress.value
                    } else 0f

                    drawRect(
                        color = expenseColor,
                        topLeft = Offset(
                            groupX + spacing + barWidth + 4f,
                            padding + chartHeight - expenseHeight
                        ),
                        size = androidx.compose.ui.geometry.Size(barWidth, expenseHeight)
                    )
                }

                // 基线
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, padding + chartHeight),
                    end = Offset(canvasWidth - padding, padding + chartHeight),
                    strokeWidth = 1f
                )
            }
        }

        // X轴标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { monthlyData ->
                Text(
                    text = monthlyData.month,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

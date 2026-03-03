package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.ui.components.charts.MonthlyData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    val statistics = combine(
        transactionRepository.getAllTransactions(),
        categoryRepository.getAllCategories(),
        _uiState
    ) { transactions, categories, state ->
        calculateStatistics(transactions, categories, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsData()
    )

    private fun calculateStatistics(
        transactions: List<com.example.aiaccounting.data.local.entity.Transaction>,
        categories: List<com.example.aiaccounting.data.local.entity.Category>,
        state: StatisticsUiState
    ): StatisticsData {
        val calendar = Calendar.getInstance()

        // 根据时间筛选过滤交易
        val filteredTransactions = when {
            // 处理年月日格式筛选 (如 "2024-03-15")
            state.timeFilter.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                val parts = state.timeFilter.split("-")
                val filterYear = parts[0].toInt()
                val filterMonth = parts[1].toInt() - 1 // Calendar月份从0开始
                val filterDay = parts[2].toInt()
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == filterYear &&
                    calendar.get(Calendar.MONTH) == filterMonth &&
                    calendar.get(Calendar.DAY_OF_MONTH) == filterDay
                }
            }
            // 处理年月格式筛选 (如 "2024-03")
            state.timeFilter.matches(Regex("\\d{4}-\\d{2}")) -> {
                val parts = state.timeFilter.split("-")
                val filterYear = parts[0].toInt()
                val filterMonth = parts[1].toInt() - 1 // Calendar月份从0开始
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == filterYear &&
                    calendar.get(Calendar.MONTH) == filterMonth
                }
            }
            // 处理年份筛选 (如 "2024")
            state.timeFilter.matches(Regex("\\d{4}")) -> {
                val filterYear = state.timeFilter.toInt()
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == filterYear
                }
            }
            // 处理年份筛选 (如 "year-2024")
            state.timeFilter.startsWith("year-") -> {
                val filterYear = state.timeFilter.substringAfter("year-").toInt()
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.YEAR) == filterYear
                }
            }
            state.timeFilter == "current" -> {
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.MONTH) == currentMonth &&
                    calendar.get(Calendar.YEAR) == currentYear
                }
            }
            state.timeFilter == "last" -> {
                calendar.add(Calendar.MONTH, -1)
                val lastMonth = calendar.get(Calendar.MONTH)
                val lastYear = calendar.get(Calendar.YEAR)
                transactions.filter {
                    calendar.timeInMillis = it.date
                    calendar.get(Calendar.MONTH) == lastMonth &&
                    calendar.get(Calendar.YEAR) == lastYear
                }
            }
            state.timeFilter == "3months" -> {
                val threeMonthsAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -3)
                }.timeInMillis
                transactions.filter { it.date >= threeMonthsAgo }
            }
            state.timeFilter == "6months" -> {
                val sixMonthsAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -6)
                }.timeInMillis
                transactions.filter { it.date >= sixMonthsAgo }
            }
            state.timeFilter == "1year" -> {
                val oneYearAgo = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -1)
                }.timeInMillis
                transactions.filter { it.date >= oneYearAgo }
            }
            state.timeFilter == "all" -> transactions
            else -> transactions
        }

        // 根据选中的标签过滤
        val typeFiltered = when (state.selectedTab) {
            "income" -> filteredTransactions.filter { it.type == TransactionType.INCOME }
            "expense" -> filteredTransactions.filter { it.type == TransactionType.EXPENSE }
            else -> filteredTransactions
        }

        val totalIncome = filteredTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val totalExpense = filteredTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        // 创建分类ID到名称和颜色的映射
        val categoryMap = categories.associateBy { it.id }

        // 分类统计
        val categoryStats = typeFiltered
            .groupBy { it.categoryId }
            .map { (categoryId, transList) ->
                val amount = transList.sumOf { it.amount }
                val total = if (state.selectedTab == "income") totalIncome else totalExpense
                val category = categoryMap[categoryId]
                CategoryStat(
                    name = category?.name ?: "未分类",
                    amount = amount,
                    percentage = if (total > 0) (amount / total).toFloat() else 0f,
                    color = category?.color ?: "#2196F3"
                )
            }
            .sortedByDescending { it.amount }

        // 计算月度趋势数据
        val monthlyData = calculateMonthlyTrend(transactions, state.timeFilter)

        return StatisticsData(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            categoryStats = categoryStats,
            monthlyTrend = monthlyData
        )
    }

    /**
     * 计算月度趋势数据
     */
    private fun calculateMonthlyTrend(
        transactions: List<com.example.aiaccounting.data.local.entity.Transaction>,
        timeFilter: String
    ): List<MonthlyData> {
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MM月", Locale.getDefault())

        // 确定时间范围
        val (startTime, endTime) = when {
            // 处理年月日格式筛选 (如 "2024-03-15")
            timeFilter.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                val parts = timeFilter.split("-")
                val filterYear = parts[0].toInt()
                val filterMonth = parts[1].toInt() - 1
                val filterDay = parts[2].toInt()
                calendar.set(filterYear, filterMonth, filterDay, 0, 0, 0)
                val start = calendar.timeInMillis
                calendar.set(filterYear, filterMonth, filterDay, 23, 59, 59)
                Pair(start, calendar.timeInMillis)
            }
            // 处理年月格式筛选 (如 "2024-03")
            timeFilter.matches(Regex("\\d{4}-\\d{2}")) -> {
                val parts = timeFilter.split("-")
                val filterYear = parts[0].toInt()
                val filterMonth = parts[1].toInt() - 1
                calendar.set(filterYear, filterMonth, 1, 0, 0, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                Pair(start, calendar.timeInMillis)
            }
            // 处理年份筛选 (如 "2024")
            timeFilter.matches(Regex("\\d{4}")) -> {
                val filterYear = timeFilter.toInt()
                calendar.set(filterYear, 0, 1, 0, 0, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                Pair(start, calendar.timeInMillis)
            }
            // 处理年份筛选 (如 "year-2024")
            timeFilter.startsWith("year-") -> {
                val filterYear = timeFilter.substringAfter("year-").toInt()
                calendar.set(filterYear, 0, 1, 0, 0, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                Pair(start, calendar.timeInMillis)
            }
            timeFilter == "current" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                Pair(start, calendar.timeInMillis)
            }
            timeFilter == "last" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                Pair(start, calendar.timeInMillis)
            }
            timeFilter == "3months" -> {
                val end = calendar.timeInMillis
                calendar.add(Calendar.MONTH, -3)
                Pair(calendar.timeInMillis, end)
            }
            timeFilter == "6months" -> {
                val end = calendar.timeInMillis
                calendar.add(Calendar.MONTH, -6)
                Pair(calendar.timeInMillis, end)
            }
            timeFilter == "1year" -> {
                val end = calendar.timeInMillis
                calendar.add(Calendar.YEAR, -1)
                Pair(calendar.timeInMillis, end)
            }
            timeFilter == "all" -> {
                // 显示所有数据，从最早的记录开始
                val earliestTransaction = transactions.minByOrNull { it.date }
                val start = earliestTransaction?.date ?: calendar.timeInMillis
                val end = System.currentTimeMillis()
                Pair(start, end)
            }
            else -> {
                // 默认显示最近6个月
                val end = calendar.timeInMillis
                calendar.add(Calendar.MONTH, -6)
                Pair(calendar.timeInMillis, end)
            }
        }

        // 过滤时间范围内的交易
        val filteredTransactions = transactions.filter {
            it.date in startTime..endTime
        }

        // 按月份分组统计
        val monthlyGroups = filteredTransactions.groupBy { transaction ->
            monthFormat.format(Date(transaction.date))
        }

        // 生成所有月份（包括没有数据的月份）
        val result = mutableListOf<MonthlyData>()
        calendar.timeInMillis = startTime

        while (calendar.timeInMillis < endTime) {
            val monthKey = monthFormat.format(calendar.time)
            val monthTransactions = monthlyGroups[monthKey] ?: emptyList()

            val income = monthTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
            val expense = monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            result.add(
                MonthlyData(
                    month = displayFormat.format(calendar.time),
                    income = income,
                    expense = expense
                )
            )

            calendar.add(Calendar.MONTH, 1)
        }

        return result
    }

    fun setSelectedTab(tab: String) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setTimeFilter(filter: String) {
        _uiState.update { it.copy(timeFilter = filter) }
    }

    fun setStartDate(date: String) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun setEndDate(date: String) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun setMergeSubCategories(merge: Boolean) {
        _uiState.update { it.copy(mergeSubCategories = merge) }
    }

    fun setShowAllCategories(show: Boolean) {
        _uiState.update { it.copy(showAllCategories = show) }
    }
}

data class StatisticsUiState(
    val selectedTab: String = "expense",
    val timeFilter: String = "current",
    val startDate: String? = null,
    val endDate: String? = null,
    val mergeSubCategories: Boolean = false,
    val showAllCategories: Boolean = true
)

data class StatisticsData(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categoryStats: List<CategoryStat> = emptyList(),
    val monthlyTrend: List<MonthlyData> = emptyList()
)

data class CategoryStat(
    val name: String,
    val amount: Double,
    val percentage: Float,
    val color: String
)

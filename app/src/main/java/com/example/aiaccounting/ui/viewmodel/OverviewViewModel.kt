package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.ui.components.charts.MonthlyData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    // 共享的交易数据流，避免多次查询
    private val allTransactions = transactionRepository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val accounts = accountRepository.getAllAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentTransactions = transactionRepository.getRecentTransactions(10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 从共享的allTransactions派生所有统计数据
    val monthlyStats = allTransactions
        .map { transactions ->
            calculateMonthlyStats(transactions)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthlyStats()
        )

    // 年度趋势数据
    val yearlyTrendData = allTransactions
        .map { transactions ->
            calculateYearlyTrend(transactions)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 当天统计
    val todayStats = allTransactions
        .map { transactions ->
            calculateTodayStats(transactions)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DayStats()
        )

    // 当周统计
    val weekStats = allTransactions
        .map { transactions ->
            calculateWeekStats(transactions)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DayStats()
        )

    fun toggleBalanceVisibility() {
        _uiState.update { it.copy(isBalanceVisible = !it.isBalanceVisible) }
    }

    private fun calculateMonthlyStats(transactions: List<Transaction>): MonthlyStats {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // 年度统计
        val yearStart = Calendar.getInstance().apply {
            set(currentYear, 0, 1, 0, 0, 0)
        }.timeInMillis

        val yearEnd = Calendar.getInstance().apply {
            set(currentYear + 1, 0, 1, 0, 0, 0)
        }.timeInMillis

        val yearlyTransactions = transactions.filter { it.date in yearStart until yearEnd }
        val totalIncome = yearlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = yearlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // 月度统计
        val monthStart = Calendar.getInstance().apply {
            set(currentYear, currentMonth, 1, 0, 0, 0)
        }.timeInMillis

        val monthEnd = Calendar.getInstance().apply {
            set(currentYear, currentMonth + 1, 1, 0, 0, 0)
        }.timeInMillis

        val monthlyTransactions = transactions.filter { it.date in monthStart until monthEnd }
        val monthlyIncome = monthlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val monthlyExpense = monthlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        return MonthlyStats(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense
        )
    }

    /**
     * 计算年度趋势数据（按月）
     */
    private fun calculateYearlyTrend(transactions: List<Transaction>): List<MonthlyData> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        val monthlyData = mutableListOf<MonthlyData>()

        for (month in 0..11) {
            val monthStart = Calendar.getInstance().apply {
                set(currentYear, month, 1, 0, 0, 0)
            }.timeInMillis

            val monthEnd = Calendar.getInstance().apply {
                set(currentYear, month + 1, 1, 0, 0, 0)
            }.timeInMillis

            val monthTransactions = transactions.filter { it.date in monthStart until monthEnd }
            val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            monthlyData.add(
                MonthlyData(
                    month = "${month + 1}月",
                    income = income,
                    expense = expense
                )
            )
        }

        return monthlyData
    }

    /**
     * 计算当天统计
     */
    private fun calculateTodayStats(transactions: List<Transaction>): DayStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val todayEnd = calendar.timeInMillis

        val todayTransactions = transactions.filter { it.date in todayStart until todayEnd }
        val income = todayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = todayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        return DayStats(
            income = income,
            expense = expense,
            count = todayTransactions.size
        )
    }

    /**
     * 计算当周统计
     */
    private fun calculateWeekStats(transactions: List<Transaction>): DayStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis

        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val weekEnd = calendar.timeInMillis

        val weekTransactions = transactions.filter { it.date in weekStart until weekEnd }
        val income = weekTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = weekTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        return DayStats(
            income = income,
            expense = expense,
            count = weekTransactions.size
        )
    }
}

data class OverviewUiState(
    val isBalanceVisible: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class MonthlyStats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0
)

data class DayStats(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val count: Int = 0
)

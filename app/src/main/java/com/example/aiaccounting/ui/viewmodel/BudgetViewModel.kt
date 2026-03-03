package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.Budget
import com.example.aiaccounting.data.local.entity.BudgetPeriod
import com.example.aiaccounting.data.repository.BudgetRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Budget management
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    val budgets = budgetRepository.getAllBudgets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val budgetsWithDetails = budgetRepository.getAllBudgets()
        .map { budgetList ->
            budgetList.map { budget ->
                val category = categoryRepository.getCategoryById(budget.categoryId)
                val spent = calculateSpent(budget)
                BudgetWithDetails(
                    budget = budget,
                    categoryName = category?.name ?: "未分类",
                    spent = spent
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private suspend fun calculateSpent(budget: Budget): Double {
        // 简化计算，实际应该根据预算周期计算
        return 0.0
    }

    /**
     * Add a new budget
     */
    fun addBudget(categoryId: Long, amount: Double, period: BudgetPeriod) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val budget = Budget(
                    categoryId = categoryId,
                    amount = amount,
                    period = period
                )
                
                budgetRepository.insertBudget(budget)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Edit an existing budget
     */
    fun editBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                budgetRepository.updateBudget(budget)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Delete a budget
     */
    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                budgetRepository.deleteBudgetById(budgetId)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Get budgets in date range
     */
    fun getBudgetsInRange(startDate: Long, endDate: Long): Flow<List<Budget>> {
        return budgetRepository.getBudgetsInRange(startDate, endDate)
    }

    /**
     * Get budget by category
     */
    fun getBudgetByCategory(categoryId: Long): Flow<Budget?> {
        return budgetRepository.getBudgetByCategory(categoryId)
    }

    /**
     * Check if category has budget
     */
    fun hasBudgetForCategory(categoryId: Long): Flow<Boolean> {
        return budgetRepository.hasBudgetForCategory(categoryId)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for Budget screen
 */
data class BudgetUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val editingBudget: Budget? = null
)

/**
 * Budget with additional details for UI
 */
data class BudgetWithDetails(
    val budget: Budget,
    val categoryName: String,
    val spent: Double
)

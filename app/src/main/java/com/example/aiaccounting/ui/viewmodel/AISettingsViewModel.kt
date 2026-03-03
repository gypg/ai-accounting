package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.repository.AIUsageStats
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.RemoteModel
import com.example.aiaccounting.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AISettingsViewModel @Inject constructor(
    private val aiConfigRepository: AIConfigRepository,
    private val aiService: AIService,
    private val aiUsageRepository: AIUsageRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(AISettingsUiState())
    val uiState: StateFlow<AISettingsUiState> = _uiState.asStateFlow()

    init {
        loadAIConfig()
        loadUsageStats()
        checkNetworkStatus()
    }

    private fun loadAIConfig() {
        viewModelScope.launch {
            aiConfigRepository.getAIConfig().collect { config ->
                _uiState.value = _uiState.value.copy(
                    config = config,
                    isLoading = false
                )
            }
        }
    }

    private fun loadUsageStats() {
        viewModelScope.launch {
            aiUsageRepository.getUsageStats().collect { stats ->
                _uiState.value = _uiState.value.copy(
                    usageStats = stats
                )
            }
        }
    }

    private fun checkNetworkStatus() {
        viewModelScope.launch {
            val isAvailable = networkUtils.isNetworkAvailable()
            _uiState.update { it.copy(isNetworkAvailable = isAvailable) }
        }
    }

    fun refreshNetworkStatus() {
        checkNetworkStatus()
    }

    fun updateProvider(provider: AIProvider) {
        val defaultConfig = AIConfig.defaultFor(provider)
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(
                provider = provider,
                apiUrl = defaultConfig.apiUrl,
                model = defaultConfig.model
            )
        )
    }

    fun updateApiKey(apiKey: String) {
        // 清理API密钥中的换行符和空格
        val cleanedKey = apiKey.trim().replace("\n", "").replace("\r", "").replace(" ", "")
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(apiKey = cleanedKey)
        )
    }

    fun updateApiUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(apiUrl = url)
        )
    }

    fun updateModel(model: String) {
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(model = model)
        )
    }

    fun updateEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(isEnabled = enabled)
        )
    }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            aiConfigRepository.saveAIConfig(_uiState.value.config)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = true
            )
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)

            // 先检查网络
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = TestResult.Error("网络不可用，请检查网络连接")
                )
                return@launch
            }

            // 使用AI服务进行真正的连接测试
            val errorMessage = aiService.testConnection(_uiState.value.config)

            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = if (errorMessage == null) {
                    TestResult.Success
                } else {
                    TestResult.Error(errorMessage)
                }
            )
        }
    }

    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(testResult = null)
    }

    /**
     * 获取远程模型列表
     */
    fun fetchRemoteModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingModels = true)

            // 先检查网络
            if (!networkUtils.isNetworkAvailable()) {
                _uiState.value = _uiState.value.copy(
                    isFetchingModels = false,
                    testResult = TestResult.Error("网络不可用，无法获取模型列表")
                )
                return@launch
            }

            // 获取模型列表
            val models = aiService.fetchModels(_uiState.value.config)

            _uiState.value = _uiState.value.copy(
                isFetchingModels = false,
                remoteModels = models,
                testResult = if (models.isEmpty()) {
                    TestResult.Error("无法获取模型列表，请检查API配置")
                } else {
                    null
                }
            )
        }
    }

    /**
     * 重置使用统计
     */
    fun resetUsageStats() {
        viewModelScope.launch {
            aiUsageRepository.resetStats()
        }
    }
}

data class AISettingsUiState(
    val config: AIConfig = AIConfig(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: TestResult? = null,
    val usageStats: AIUsageStats = AIUsageStats(),
    val isNetworkAvailable: Boolean = true,
    val isFetchingModels: Boolean = false,
    val remoteModels: List<RemoteModel> = emptyList()
)

sealed class TestResult {
    object Success : TestResult()
    data class Error(val message: String) : TestResult()
}

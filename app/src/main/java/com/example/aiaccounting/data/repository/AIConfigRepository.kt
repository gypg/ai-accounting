package com.example.aiaccounting.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_config")

/**
 * AI配置仓库
 */
@Singleton
class AIConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.aiConfigDataStore

    /**
     * 获取AI配置流
     */
    fun getAIConfig(): Flow<AIConfig> {
        return dataStore.data.map { preferences ->
            val useBuiltin = preferences[booleanPreferencesKey(AIConfig.KEY_USE_BUILTIN)] ?: false
            val userConfig = AIConfig(
                provider = AIProvider.fromString(
                    preferences[stringPreferencesKey(AIConfig.KEY_PROVIDER)] ?: AIProvider.QWEN.name
                ),
                apiKey = preferences[stringPreferencesKey(AIConfig.KEY_API_KEY)] ?: "",
                apiUrl = preferences[stringPreferencesKey(AIConfig.KEY_API_URL)] ?: "",
                model = preferences[stringPreferencesKey(AIConfig.KEY_MODEL)] ?: "",
                isEnabled = preferences[booleanPreferencesKey(AIConfig.KEY_ENABLED)] ?: false
            )
            // 如果使用内置配置且内置配置已设置，则返回内置配置
            AIConfig.getEffectiveConfig(userConfig, useBuiltin)
        }
    }

    /**
     * 获取是否使用内置配置
     */
    fun getUseBuiltin(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(AIConfig.KEY_USE_BUILTIN)] ?: false
        }
    }

    /**
     * 设置是否使用内置配置
     */
    suspend fun setUseBuiltin(useBuiltin: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(AIConfig.KEY_USE_BUILTIN)] = useBuiltin
            // 如果使用内置配置，自动启用AI
            if (useBuiltin) {
                preferences[booleanPreferencesKey(AIConfig.KEY_ENABLED)] = true
            }
        }
    }

    /**
     * 保存AI配置
     */
    suspend fun saveAIConfig(config: AIConfig) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_PROVIDER)] = config.provider.name
            preferences[stringPreferencesKey(AIConfig.KEY_API_KEY)] = config.apiKey
            preferences[stringPreferencesKey(AIConfig.KEY_API_URL)] = config.apiUrl
            preferences[stringPreferencesKey(AIConfig.KEY_MODEL)] = config.model
            preferences[booleanPreferencesKey(AIConfig.KEY_ENABLED)] = config.isEnabled
        }
    }

    /**
     * 更新API密钥
     */
    suspend fun updateApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_API_KEY)] = apiKey
        }
    }

    /**
     * 更新API提供商
     */
    suspend fun updateProvider(provider: AIProvider) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(AIConfig.KEY_PROVIDER)] = provider.name
            // 更新为默认配置
            val defaultConfig = AIConfig.defaultFor(provider)
            preferences[stringPreferencesKey(AIConfig.KEY_API_URL)] = defaultConfig.apiUrl
            preferences[stringPreferencesKey(AIConfig.KEY_MODEL)] = defaultConfig.model
        }
    }

    /**
     * 更新启用状态
     */
    suspend fun updateEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(AIConfig.KEY_ENABLED)] = enabled
        }
    }

    /**
     * 清除配置
     */
    suspend fun clearConfig() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

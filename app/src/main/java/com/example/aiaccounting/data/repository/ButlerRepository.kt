package com.example.aiaccounting.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ButlerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管家仓库
 * 管理当前选中的管家和管家偏好设置
 */
@Singleton
class ButlerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 当前选中的管家ID Flow
    private val _currentButlerId = MutableStateFlow(getSelectedButlerId())
    val currentButlerId: Flow<String> = _currentButlerId.asStateFlow()
    
    /**
     * 获取当前选中的管家
     */
    fun getCurrentButler(): Butler {
        val butlerId = getSelectedButlerId()
        return ButlerManager.getButlerById(butlerId) ?: ButlerManager.getDefaultButler()
    }
    
    /**
     * 获取当前管家的系统提示词
     */
    fun getCurrentButlerSystemPrompt(): String {
        return getCurrentButler().systemPrompt
    }
    
    /**
     * 获取选中的管家ID
     */
    fun getSelectedButlerId(): String {
        return prefs.getString(KEY_SELECTED_BUTLER, ButlerManager.BUTLER_TAOTAO) 
            ?: ButlerManager.BUTLER_TAOTAO
    }
    
    /**
     * 设置选中的管家
     */
    fun setSelectedButler(butlerId: String) {
        prefs.edit().putString(KEY_SELECTED_BUTLER, butlerId).apply()
        _currentButlerId.value = butlerId
    }
    
    /**
     * 获取所有可用管家列表
     */
    fun getAllButlers(): List<Butler> {
        return ButlerManager.getAllButlers()
    }
    
    /**
     * 根据ID获取管家
     */
    fun getButlerById(id: String): Butler? {
        return ButlerManager.getButlerById(id)
    }
    
    companion object {
        private const val PREFS_NAME = "butler_preferences"
        private const val KEY_SELECTED_BUTLER = "selected_butler_id"
    }
}

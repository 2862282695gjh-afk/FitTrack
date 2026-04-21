package com.fittrack.ui.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.data.api.QwenMessage
import com.fittrack.data.api.QwenRepository
import com.fittrack.data.api.QwenResult
import com.fittrack.data.storage.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置页面的 ViewModel
 * 管理 API Key 配置和应用设置
 */
class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    // 是否已完成配置（有 API Key）
    private val _isConfigured = MutableStateFlow(settingsManager.hasApiKey)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    // API Key 输入
    private val _apiKeyInput = MutableStateFlow("")
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    // API Base URL 输入
    private val _apiBaseUrlInput = MutableStateFlow(settingsManager.apiBaseUrl)
    val apiBaseUrlInput: StateFlow<String> = _apiBaseUrlInput.asStateFlow()

    // 是否显示 API Key（明文）
    private val _showApiKey = MutableStateFlow(false)
    val showApiKey: StateFlow<Boolean> = _showApiKey.asStateFlow()

    // 保存状态
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // 用户档案是否完成
    private val _isProfileComplete = MutableStateFlow(settingsManager.isProfileComplete)
    val isProfileComplete: StateFlow<Boolean> = _isProfileComplete.asStateFlow()

    // 已保存的 API Key（遮罩显示）
    val maskedApiKey: String?
        get() = settingsManager.apiKey?.let { key ->
            if (key.length > 8) {
                "${key.take(4)}****${key.takeLast(4)}"
            } else {
                "****"
            }
        }

    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    // 更新 API Key 输入
    fun updateApiKeyInput(input: String) {
        _apiKeyInput.value = input
    }

    // 更新 API Base URL 输入
    fun updateApiBaseUrlInput(input: String) {
        _apiBaseUrlInput.value = input
    }

    // 切换 API Key 显示状态
    fun toggleShowApiKey() {
        _showApiKey.value = !_showApiKey.value
    }

    // 验证 API Key 格式
    fun validateApiKey(apiKey: String): Boolean {
        // 百炼平台 API Key 格式：sk-xxxxxx
        return apiKey.isNotBlank() && apiKey.length >= 8
    }

    // 验证 URL 格式
    fun validateUrl(url: String): Boolean {
        return url.isNotBlank() && Patterns.WEB_URL.matcher(url).matches()
    }

    // 保存 API Key（带验证）
    fun saveApiKey(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val apiKey = _apiKeyInput.value.trim()
            var baseUrl = _apiBaseUrlInput.value.trim()

            // 验证 API Key 格式
            if (!validateApiKey(apiKey)) {
                _saveState.value = SaveState.Error("API Key 格式不正确")
                return@launch
            }

            // 验证 URL 格式
            if (!validateUrl(baseUrl)) {
                _saveState.value = SaveState.Error("API 地址格式不正确")
                return@launch
            }

            // 确保 baseUrl 以 / 结尾（Retrofit 要求）
            if (!baseUrl.endsWith("/")) {
                baseUrl = "$baseUrl/"
            }

            _saveState.value = SaveState.Saving

            try {
                // 直接保存 API Key，不进行网络验证
                // 网络验证可能导致某些设备上的崩溃
                Log.d("SettingsViewModel", "开始保存 API Key")
                settingsManager.apiKey = apiKey
                settingsManager.apiBaseUrl = baseUrl
                settingsManager.hasApiKey = true

                // 重置 QwenRepository 单例
                QwenRepository.resetInstance()

                Log.d("SettingsViewModel", "API Key 保存成功")
                _isConfigured.value = true
                _saveState.value = SaveState.Success
                _apiKeyInput.value = "" // 清空输入
                onSuccess()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "保存失败", e)
                _saveState.value = SaveState.Error("保存失败: ${e.message}")
            }
        }
    }

    // 清除 API Key
    fun clearApiKey() {
        viewModelScope.launch {
            settingsManager.apiKey = null
            settingsManager.hasApiKey = false
            settingsManager.isProfileComplete = false
            _isConfigured.value = false
            _isProfileComplete.value = false
        }
    }

    // 重置保存状态
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    // 更新档案完成状态
    fun setProfileComplete(complete: Boolean) {
        settingsManager.isProfileComplete = complete
        _isProfileComplete.value = complete
    }

    class Factory(private val settingsManager: SettingsManager) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(settingsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

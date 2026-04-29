package com.fittrack.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 安全存储管理器
 * 使用 EncryptedSharedPreferences 安全存储敏感信息（如 API Key）
 * 如果加密存储不可用，则回退到普通 SharedPreferences
 */
class SettingsManager(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                "fittrack_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SettingsManager", "EncryptedSharedPreferences 创建失败，使用普通存储", e)
            _isUsingSecureStorage = false
            // 回退到普通 SharedPreferences
            appContext.getSharedPreferences("fittrack_prefs", Context.MODE_PRIVATE)
        }
    }

    /** 是否正在使用加密存储（false 表示已回退到普通存储） */
    @Volatile
    private var _isUsingSecureStorage = true
    val isUsingSecureStorage: Boolean get() = _isUsingSecureStorage

    // API Key 相关
    var apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)
        set(value) {
            prefs.edit().putString(KEY_API_KEY, value).apply()
        }

    var hasApiKey: Boolean
        get() = prefs.getBoolean(KEY_HAS_API_KEY, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HAS_API_KEY, value).apply()
        }

    // 百炼平台 API 基础 URL
    var apiBaseUrl: String
        get() = prefs.getString(KEY_API_BASE_URL, DEFAULT_BAILIAN_URL) ?: DEFAULT_BAILIAN_URL
        set(value) {
            prefs.edit().putString(KEY_API_BASE_URL, value).apply()
        }

    // 用户档案完成状态
    var isProfileComplete: Boolean
        get() = prefs.getBoolean(KEY_PROFILE_COMPLETE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PROFILE_COMPLETE, value).apply()
        }

    // 清除所有设置
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_HAS_API_KEY = "has_api_key"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_PROFILE_COMPLETE = "profile_complete"

        // 阿里云百炼平台 API 地址（必须以 / 结尾）
        const val DEFAULT_BAILIAN_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

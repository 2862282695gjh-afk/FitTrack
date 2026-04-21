package com.fittrack.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.data.api.ParsedBodyAnalysis
import com.fittrack.data.api.QwenRepository
import com.fittrack.data.api.QwenResult
import com.fittrack.data.api.UserBodyStats
import com.fittrack.data.entity.BodyMeasurement
import com.fittrack.data.entity.UserProfile
import com.fittrack.data.repository.UserProfileRepository
import com.fittrack.data.storage.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * 用户档案 ViewModel
 */
class UserProfileViewModel(
    private val repository: UserProfileRepository,
    private val settingsManager: SettingsManager,
    private val context: Context
) : ViewModel() {

    // 用户档案
    val profile: StateFlow<UserProfile?> = repository.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // 表单输入状态
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _gender = MutableStateFlow("")
    val gender: StateFlow<String> = _gender.asStateFlow()

    private val _age = MutableStateFlow("")
    val age: StateFlow<String> = _age.asStateFlow()

    private val _heightCm = MutableStateFlow("")
    val heightCm: StateFlow<String> = _heightCm.asStateFlow()

    private val _weightKg = MutableStateFlow("")
    val weightKg: StateFlow<String> = _weightKg.asStateFlow()

    private val _targetWeightKg = MutableStateFlow("")
    val targetWeightKg: StateFlow<String> = _targetWeightKg.asStateFlow()

    private val _fitnessGoal = MutableStateFlow("")
    val fitnessGoal: StateFlow<String> = _fitnessGoal.asStateFlow()

    private val _experienceLevel = MutableStateFlow("")
    val experienceLevel: StateFlow<String> = _experienceLevel.asStateFlow()

    private val _weeklyMinutes = MutableStateFlow("")
    val weeklyMinutes: StateFlow<String> = _weeklyMinutes.asStateFlow()

    private val _healthIssues = MutableStateFlow("")
    val healthIssues: StateFlow<String> = _healthIssues.asStateFlow()

    // 照片状态
    private val _frontPhotoUri = MutableStateFlow<Uri?>(null)
    val frontPhotoUri: StateFlow<Uri?> = _frontPhotoUri.asStateFlow()

    private val _sidePhotoUri = MutableStateFlow<Uri?>(null)
    val sidePhotoUri: StateFlow<Uri?> = _sidePhotoUri.asStateFlow()

    private val _backPhotoUri = MutableStateFlow<Uri?>(null)
    val backPhotoUri: StateFlow<Uri?> = _backPhotoUri.asStateFlow()

    // 保存状态
    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // AI 分析状态
    sealed class AnalysisState {
        object Idle : AnalysisState()
        object Analyzing : AnalysisState()
        data class Success(val result: ParsedBodyAnalysis) : AnalysisState()
        data class Error(val message: String) : AnalysisState()
    }

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    // 体重记录
    val weightRecords = repository.getRecentWeightRecords(30)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 身体测量记录
    val measurements = repository.getRecentMeasurements(10)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 是否是新建档案
    private val _isNewProfile = MutableStateFlow(true)
    val isNewProfile: StateFlow<Boolean> = _isNewProfile.asStateFlow()

    init {
        loadProfile()
    }

    /**
     * 加载已有档案到表单
     */
    private fun loadProfile() {
        viewModelScope.launch {
            val existingProfile = repository.getProfile()
            if (existingProfile != null) {
                _isNewProfile.value = false
                _name.value = existingProfile.name
                _gender.value = existingProfile.gender
                _age.value = if (existingProfile.age > 0) existingProfile.age.toString() else ""
                _heightCm.value = if (existingProfile.heightCm > 0) existingProfile.heightCm.toString() else ""
                _weightKg.value = if (existingProfile.weightKg > 0) existingProfile.weightKg.toString() else ""
                _targetWeightKg.value = if (existingProfile.targetWeightKg > 0) existingProfile.targetWeightKg.toString() else ""
                _fitnessGoal.value = existingProfile.fitnessGoal
                _experienceLevel.value = existingProfile.experienceLevel
                _weeklyMinutes.value = if (existingProfile.weeklyAvailableMinutes > 0) existingProfile.weeklyAvailableMinutes.toString() else ""
                _healthIssues.value = existingProfile.healthIssues

                // 加载照片
                if (existingProfile.frontPhotoPath.isNotEmpty()) {
                    _frontPhotoUri.value = Uri.fromFile(File(existingProfile.frontPhotoPath))
                }
                if (existingProfile.sidePhotoPath.isNotEmpty()) {
                    _sidePhotoUri.value = Uri.fromFile(File(existingProfile.sidePhotoPath))
                }
                if (existingProfile.backPhotoPath.isNotEmpty()) {
                    _backPhotoUri.value = Uri.fromFile(File(existingProfile.backPhotoPath))
                }
            }
        }
    }

    // ========== 表单更新方法 ==========

    fun updateName(value: String) { _name.value = value }
    fun updateGender(value: String) { _gender.value = value }
    fun updateAge(value: String) { _age.value = value }
    fun updateHeight(value: String) { _heightCm.value = value }
    fun updateWeight(value: String) { _weightKg.value = value }
    fun updateTargetWeight(value: String) { _targetWeightKg.value = value }
    fun updateFitnessGoal(value: String) { _fitnessGoal.value = value }
    fun updateExperienceLevel(value: String) { _experienceLevel.value = value }
    fun updateWeeklyMinutes(value: String) { _weeklyMinutes.value = value }
    fun updateHealthIssues(value: String) { _healthIssues.value = value }

    fun updateFrontPhoto(uri: Uri?) { _frontPhotoUri.value = uri }
    fun updateSidePhoto(uri: Uri?) { _sidePhotoUri.value = uri }
    fun updateBackPhoto(uri: Uri?) { _backPhotoUri.value = uri }

    // ========== 保存档案 ==========

    /**
     * 保存用户档案
     */
    fun saveProfile(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving

            try {
                // 验证必填项
                val height = _heightCm.value.toDoubleOrNull()
                val weight = _weightKg.value.toDoubleOrNull()

                if (height == null || height <= 0) {
                    _saveState.value = SaveState.Error("请输入有效的身高")
                    return@launch
                }
                if (weight == null || weight <= 0) {
                    _saveState.value = SaveState.Error("请输入有效的体重")
                    return@launch
                }

                // 保存照片到本地
                val frontPath = savePhotoToLocal(_frontPhotoUri.value, "front")
                val sidePath = savePhotoToLocal(_sidePhotoUri.value, "side")
                val backPath = savePhotoToLocal(_backPhotoUri.value, "back")

                // 获取现有档案或创建新的
                val existingProfile = repository.getProfile()
                val profile = UserProfile(
                    id = existingProfile?.id ?: 0,
                    name = _name.value,
                    gender = _gender.value,
                    age = _age.value.toIntOrNull() ?: 0,
                    heightCm = height,
                    weightKg = weight,
                    targetWeightKg = _targetWeightKg.value.toDoubleOrNull() ?: weight,
                    fitnessGoal = _fitnessGoal.value,
                    experienceLevel = _experienceLevel.value,
                    weeklyAvailableMinutes = _weeklyMinutes.value.toIntOrNull() ?: 0,
                    healthIssues = _healthIssues.value,
                    frontPhotoPath = frontPath ?: existingProfile?.frontPhotoPath ?: "",
                    sidePhotoPath = sidePath ?: existingProfile?.sidePhotoPath ?: "",
                    backPhotoPath = backPath ?: existingProfile?.backPhotoPath ?: "",
                    bodyAnalysisJson = existingProfile?.bodyAnalysisJson ?: "",
                    createdAt = existingProfile?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                repository.saveProfile(profile)
                _saveState.value = SaveState.Success
                onComplete?.invoke()
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("保存失败: ${e.message}")
            }
        }
    }

    /**
     * 保存照片到本地存储
     */
    private fun savePhotoToLocal(uri: Uri?, type: String): String? {
        if (uri == null) return null

        return try {
            val photosDir = File(context.filesDir, "body_photos")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }

            val fileName = "${type}_${System.currentTimeMillis()}.jpg"
            val outputFile = File(photosDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
                }
            }

            outputFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // ========== AI 体态分析 ==========

    /**
     * 执行 AI 体态分析
     */
    fun analyzeBody() {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Analyzing

            try {
                val qwenRepository = QwenRepository.getInstance(settingsManager)
                val result = repository.analyzeBody(qwenRepository)

                when (result) {
                    is QwenResult.Success -> {
                        _analysisState.value = AnalysisState.Success(result.data)
                    }
                    is QwenResult.Error -> {
                        _analysisState.value = AnalysisState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _analysisState.value = AnalysisState.Error("分析失败: ${e.message}")
            }
        }
    }

    /**
     * 重置分析状态
     */
    fun resetAnalysisState() {
        _analysisState.value = AnalysisState.Idle
    }

    // ========== 体重记录 ==========

    /**
     * 添加体重记录
     */
    fun addWeightRecord(weightKg: Double, note: String = "") {
        viewModelScope.launch {
            repository.addWeightRecord(weightKg, note)
        }
    }

    // ========== 身体测量 ==========

    /**
     * 添加身体测量记录
     */
    fun addMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.addMeasurement(measurement)
        }
    }

    // ========== 工厂 ==========

    class Factory(
        private val repository: UserProfileRepository,
        private val settingsManager: SettingsManager,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
                return UserProfileViewModel(repository, settingsManager, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    /**
     * 重置保存状态
     */
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}

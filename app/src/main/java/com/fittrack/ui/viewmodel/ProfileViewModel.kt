package com.fittrack.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrack.data.api.QwenRepository
import com.fittrack.data.api.QwenResult
import com.fittrack.data.api.ParsedBodyAnalysis
import com.fittrack.data.api.UserBodyStats
import com.fittrack.data.entity.BodyMeasurement
import com.fittrack.data.entity.UserProfile
import com.fittrack.data.entity.WeightRecord
import com.fittrack.data.repository.UserProfileRepository
import com.fittrack.data.storage.SettingsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 用户档案编辑状态
 */
data class ProfileEditState(
    val name: String = "",
    val gender: String = "",
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val targetWeightKg: String = "",
    val fitnessGoal: String = "",
    val experienceLevel: String = "",
    val weeklyMinutes: String = "",
    val healthIssues: String = ""
) {
    companion object {
        fun fromProfile(profile: UserProfile): ProfileEditState {
            return ProfileEditState(
                name = profile.name,
                gender = profile.gender,
                age = if (profile.age > 0) profile.age.toString() else "",
                heightCm = if (profile.heightCm > 0) profile.heightCm.toString() else "",
                weightKg = if (profile.weightKg > 0) profile.weightKg.toString() else "",
                targetWeightKg = if (profile.targetWeightKg > 0) profile.targetWeightKg.toString() else "",
                fitnessGoal = profile.fitnessGoal,
                experienceLevel = profile.experienceLevel,
                weeklyMinutes = if (profile.weeklyAvailableMinutes > 0) profile.weeklyAvailableMinutes.toString() else "",
                healthIssues = profile.healthIssues
            )
        }

        fun toProfile(state: ProfileEditState, existingProfile: UserProfile? = null): UserProfile {
            return UserProfile(
                id = existingProfile?.id ?: 0,
                name = state.name,
                gender = state.gender,
                age = state.age.toIntOrNull() ?: 0,
                heightCm = state.heightCm.toDoubleOrNull() ?: 0.0,
                weightKg = state.weightKg.toDoubleOrNull() ?: 0.0,
                targetWeightKg = state.targetWeightKg.toDoubleOrNull() ?: 0.0,
                fitnessGoal = state.fitnessGoal,
                experienceLevel = state.experienceLevel,
                weeklyAvailableMinutes = state.weeklyMinutes.toIntOrNull() ?: 0,
                healthIssues = state.healthIssues,
                frontPhotoPath = existingProfile?.frontPhotoPath ?: "",
                sidePhotoPath = existingProfile?.sidePhotoPath ?: "",
                backPhotoPath = existingProfile?.backPhotoPath ?: "",
                bodyAnalysisJson = existingProfile?.bodyAnalysisJson ?: "",
                createdAt = existingProfile?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}

/**
 * UI 状态
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isGeneratingPlan: Boolean = false,
    val editState: ProfileEditState = ProfileEditState(),
    val profile: UserProfile? = null,
    val frontPhotoUri: Uri? = null,
    val sidePhotoUri: Uri? = null,
    val backPhotoUri: Uri? = null,
    val bodyAnalysis: ParsedBodyAnalysis? = null,
    val generatedPlanJson: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val recentWeights: List<WeightRecord> = emptyList(),
    val recentMeasurements: List<BodyMeasurement> = emptyList()
)

class ProfileViewModel(
    private val repository: UserProfileRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // 用户档案
    val profile: StateFlow<UserProfile?> = repository.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // 最近体重记录
    val recentWeights: StateFlow<List<WeightRecord>> = repository.getRecentWeightRecords(30)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 最近测量记录
    val recentMeasurements: StateFlow<List<BodyMeasurement>> = repository.getRecentMeasurements(10)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadProfile()
    }

    /**
     * 加载用户档案
     */
    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profile = repository.getProfile()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    profile = profile,
                    editState = profile?.let { p -> ProfileEditState.fromProfile(p) } ?: ProfileEditState()
                )
            }
        }
    }

    /**
     * 更新编辑状态
     */
    fun updateEditState(newState: ProfileEditState) {
        _uiState.update { it.copy(editState = newState) }
    }

    /**
     * 保存用户档案
     */
    fun saveProfile(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val currentState = _uiState.value
                val profile = ProfileEditState.toProfile(currentState.editState, currentState.profile)
                repository.saveProfile(profile)

                // 如果有新照片，保存照片路径
                val savedProfile = repository.getProfile()
                if (savedProfile != null) {
                    val frontPath = currentState.frontPhotoUri?.let { savePhoto(it, "front_${savedProfile.id}") }
                        ?: savedProfile.frontPhotoPath
                    val sidePath = currentState.sidePhotoUri?.let { savePhoto(it, "side_${savedProfile.id}") }
                        ?: savedProfile.sidePhotoPath
                    val backPath = currentState.backPhotoUri?.let { savePhoto(it, "back_${savedProfile.id}") }
                        ?: savedProfile.backPhotoPath

                    if (frontPath != savedProfile.frontPhotoPath ||
                        sidePath != savedProfile.sidePhotoPath ||
                        backPath != savedProfile.backPhotoPath
                    ) {
                        repository.updatePhotoPaths(frontPath, sidePath, backPath)
                    }
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "档案保存成功！"
                    )
                }
                onComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "保存失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 设置照片 URI
     */
    fun setPhotoUri(type: String, uri: Uri?) {
        _uiState.update { state ->
            when (type) {
                "front" -> state.copy(frontPhotoUri = uri)
                "side" -> state.copy(sidePhotoUri = uri)
                "back" -> state.copy(backPhotoUri = uri)
                else -> state
            }
        }
    }

    /**
     * 保存照片到本地
     */
    private fun savePhoto(uri: Uri, prefix: String): String {
        val photosDir = File(context.filesDir, "body_photos")
        if (!photosDir.exists()) photosDir.mkdirs()

        val fileName = "${prefix}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        val outputFile = File(photosDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }

        return outputFile.absolutePath
    }

    /**
     * 执行 AI 体态分析（支持多面照片）
     */
    fun analyzeBodyPhoto() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }

            val currentState = _uiState.value
            val profile = currentState.profile

            // 使用一个临时 ID，如果没有保存过的用户
            val profileId = profile?.id ?: 0

            // 收集所有可用的照片
            val photos = mutableListOf<Pair<String, File>>()

            // 正面照片（必需）
            val frontPath = when {
                currentState.frontPhotoUri != null -> savePhoto(currentState.frontPhotoUri!!, "front_$profileId")
                profile != null && profile.frontPhotoPath.isNotEmpty() -> profile.frontPhotoPath
                else -> null
            }

            if (frontPath == null) {
                _uiState.update { it.copy(isAnalyzing = false, errorMessage = "请至少上传正面照片") }
                return@launch
            }

            val frontFile = File(frontPath)
            if (!frontFile.exists()) {
                _uiState.update { it.copy(isAnalyzing = false, errorMessage = "正面照片文件不存在") }
                return@launch
            }
            photos.add("正面" to frontFile)

            // 侧面照片（可选）
            val sidePath = when {
                currentState.sidePhotoUri != null -> savePhoto(currentState.sidePhotoUri!!, "side_$profileId")
                profile != null && profile.sidePhotoPath.isNotEmpty() -> profile.sidePhotoPath
                else -> null
            }
            sidePath?.let { path ->
                val sideFile = File(path)
                if (sideFile.exists()) {
                    photos.add("侧面" to sideFile)
                }
            }

            // 背面照片（可选）
            val backPath = when {
                currentState.backPhotoUri != null -> savePhoto(currentState.backPhotoUri!!, "back_$profileId")
                profile != null && profile.backPhotoPath.isNotEmpty() -> profile.backPhotoPath
                else -> null
            }
            backPath?.let { path ->
                val backFile = File(path)
                if (backFile.exists()) {
                    photos.add("背面" to backFile)
                }
            }

            // 调用 AI 分析
            val settingsManager = SettingsManager.getInstance(context)
            val qwenRepository = QwenRepository.getInstance(settingsManager)

            // 构建用户身体数据 - 如果没有保存的 profile，使用编辑状态中的数据或默认值
            val editState = currentState.editState
            val userStats = UserBodyStats(
                height = profile?.heightCm ?: editState.heightCm.toDoubleOrNull() ?: 0.0,
                weight = profile?.weightKg ?: editState.weightKg.toDoubleOrNull() ?: 0.0,
                age = profile?.age ?: editState.age.toIntOrNull() ?: 0,
                gender = profile?.gender ?: editState.gender,
                goal = profile?.fitnessGoal ?: editState.fitnessGoal,
                experience = profile?.experienceLevel ?: editState.experienceLevel
            )

            // 使用多图片分析方法
            val result = if (photos.size == 1) {
                // 只有一张正面照片，使用单图分析
                qwenRepository.analyzeBodyImage(photos.first().second, userStats)
            } else {
                // 多张照片，使用多图分析
                qwenRepository.analyzeBodyImages(photos, userStats)
            }

            when (result) {
                is QwenResult.Success -> {
                    val analysis = result.data
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            bodyAnalysis = analysis,
                            successMessage = "体态分析完成！共分析 ${photos.size} 张照片"
                        )
                    }

                    // 保存分析结果到数据库（只有保存过 profile 才保存）
                    if (profile != null) {
                        repository.updateBodyAnalysis(analysis.rawJson)
                    }
                }
                is QwenResult.Error -> {
                    _uiState.update {
                        it.copy(isAnalyzing = false, errorMessage = "分析失败: ${result.message}")
                    }
                }
            }
        }
    }

    /**
     * 添加体重记录
     */
    fun addWeightRecord(weightKg: Double, note: String = "") {
        viewModelScope.launch {
            repository.addWeightRecord(weightKg, note)

            _uiState.update { it.copy(successMessage = "体重记录已添加") }
        }
    }

    /**
     * 添加身体测量记录
     */
    fun addMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.addMeasurement(measurement)
            _uiState.update { it.copy(successMessage = "测量记录已添加") }
        }
    }

    /**
     * 生成智能训练计划
     */
    fun generateWorkoutPlan(onPlanGenerated: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPlan = true, errorMessage = null) }

            val currentState = _uiState.value
            val profile = currentState.profile

            if (profile == null) {
                _uiState.update { it.copy(isGeneratingPlan = false, errorMessage = "请先保存基本信息") }
                return@launch
            }

            val settingsManager = SettingsManager.getInstance(context)
            val qwenRepository = QwenRepository.getInstance(settingsManager)

            // 构建体态分析结果
            val bodyAnalysis = currentState.bodyAnalysis
            val bodyAnalysisResult = if (bodyAnalysis != null) {
                com.fittrack.data.api.BodyAnalysisResult(
                    postureScore = bodyAnalysis.postureScore ?: 0,
                    bodyFatPercentage = bodyAnalysis.bodyFatPercentage ?: 0.0,
                    muscleBalance = bodyAnalysis.muscleBalance ?: "",
                    issues = bodyAnalysis.issues,
                    suggestions = bodyAnalysis.suggestions
                )
            } else {
                // 如果没有分析结果，使用基本信息
                com.fittrack.data.api.BodyAnalysisResult(
                    postureScore = 0,
                    bodyFatPercentage = 0.0,
                    muscleBalance = "未知",
                    issues = emptyList(),
                    suggestions = emptyList()
                )
            }

            // 构建用户身体数据
            val userStats = UserBodyStats(
                height = profile.heightCm,
                weight = profile.weightKg,
                age = profile.age,
                gender = profile.gender,
                goal = profile.fitnessGoal,
                experience = profile.experienceLevel,
                weeklyMinutes = profile.weeklyAvailableMinutes,
                healthIssues = profile.healthIssues
            )

            when (val result = qwenRepository.generateWorkoutPlan(bodyAnalysisResult, userStats)) {
                is QwenResult.Success -> {
                    val plan = result.data
                    _uiState.update {
                        it.copy(
                            isGeneratingPlan = false,
                            generatedPlanJson = plan.rawResponse,
                            successMessage = "训练计划生成成功！"
                        )
                    }
                    onPlanGenerated(plan.rawResponse)
                }
                is QwenResult.Error -> {
                    _uiState.update {
                        it.copy(isGeneratingPlan = false, errorMessage = "生成计划失败: ${result.message}")
                    }
                }
            }
        }
    }

    /**
     * 清除消息
     */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    class Factory(
        private val repository: UserProfileRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

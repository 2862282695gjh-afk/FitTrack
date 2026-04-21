package com.fittrack.data.repository

import com.fittrack.data.api.*
import com.fittrack.data.db.UserProfileDao
import com.fittrack.data.entity.BodyMeasurement
import com.fittrack.data.entity.UserProfile
import com.fittrack.data.entity.WeightRecord
import com.fittrack.data.storage.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 用户档案仓库
 * 管理用户档案、体重记录、身体测量等数据
 */
class UserProfileRepository(
    private val userProfileDao: UserProfileDao,
    private val settingsManager: SettingsManager
) {
    // ========== 用户档案 ==========

    /**
     * 获取用户档案
     */
    suspend fun getProfile(): UserProfile? {
        return userProfileDao.getProfile()
    }

    /**
     * 获取用户档案 (Flow)
     */
    fun getProfileFlow(): Flow<UserProfile?> {
        return userProfileDao.getProfileFlow()
    }

    /**
     * 保存或更新用户档案
     */
    suspend fun saveProfile(profile: UserProfile): Long {
        val id = userProfileDao.insertOrUpdateProfile(profile)
        // 更新设置中的档案完成状态
        settingsManager.isProfileComplete = true
        return id
    }

    /**
     * 创建默认用户档案
     */
    suspend fun createDefaultProfile(): UserProfile {
        val profile = UserProfile()
        userProfileDao.insertOrUpdateProfile(profile)
        return profile
    }

    /**
     * 获取或创建用户档案
     */
    suspend fun getOrCreateProfile(): UserProfile {
        return userProfileDao.getProfile() ?: createDefaultProfile()
    }

    /**
     * 更新身体参数
     */
    suspend fun updateBodyStats(
        heightCm: Double,
        weightKg: Double,
        targetWeightKg: Double
    ) {
        val profile = getOrCreateProfile()
        userProfileDao.updateBodyStats(
            profileId = profile.id,
            heightCm = heightCm,
            weightKg = weightKg,
            targetWeightKg = targetWeightKg
        )
    }

    /**
     * 更新照片路径
     */
    suspend fun updatePhotoPaths(
        frontPath: String,
        sidePath: String,
        backPath: String
    ) {
        val profile = getOrCreateProfile()
        userProfileDao.updatePhotoPaths(
            profileId = profile.id,
            frontPath = frontPath,
            sidePath = sidePath,
            backPath = backPath
        )
    }

    /**
     * 更新 AI 分析结果
     */
    suspend fun updateBodyAnalysis(analysisJson: String) {
        val profile = getOrCreateProfile()
        userProfileDao.updateBodyAnalysis(
            profileId = profile.id,
            analysisJson = analysisJson
        )
    }

    /**
     * 检查档案是否完整
     */
    suspend fun isProfileComplete(): Boolean {
        val profile = userProfileDao.getProfile() ?: return false
        return profile.isBasicInfoComplete()
    }

    // ========== 体重记录 ==========

    /**
     * 添加体重记录
     */
    suspend fun addWeightRecord(weightKg: Double, note: String = ""): Long {
        return userProfileDao.insertWeightRecord(
            WeightRecord(weightKg = weightKg, note = note)
        )
    }

    /**
     * 获取所有体重记录
     */
    fun getAllWeightRecords(): Flow<List<WeightRecord>> {
        return userProfileDao.getAllWeightRecords()
    }

    /**
     * 获取最近 N 条体重记录
     */
    fun getRecentWeightRecords(limit: Int = 30): Flow<List<WeightRecord>> {
        return userProfileDao.getRecentWeightRecords(limit)
    }

    /**
     * 获取最新体重
     */
    suspend fun getLatestWeight(): Double? {
        return userProfileDao.getLatestWeightRecord()?.weightKg
    }

    /**
     * 删除体重记录
     */
    suspend fun deleteWeightRecord(record: WeightRecord) {
        userProfileDao.deleteWeightRecord(record)
    }

    // ========== 身体测量记录 ==========

    /**
     * 添加身体测量记录
     */
    suspend fun addMeasurement(measurement: BodyMeasurement): Long {
        return userProfileDao.insertMeasurement(measurement)
    }

    /**
     * 获取所有测量记录
     */
    fun getAllMeasurements(): Flow<List<BodyMeasurement>> {
        return userProfileDao.getAllMeasurements()
    }

    /**
     * 获取最近 N 条测量记录
     */
    fun getRecentMeasurements(limit: Int = 10): Flow<List<BodyMeasurement>> {
        return userProfileDao.getRecentMeasurements(limit)
    }

    /**
     * 获取最新测量记录
     */
    suspend fun getLatestMeasurement(): BodyMeasurement? {
        return userProfileDao.getLatestMeasurement()
    }

    // ========== AI 体态分析 ==========

    /**
     * 获取用于 API 调用的用户身体数据
     */
    suspend fun getUserBodyStats(): UserBodyStats {
        val profile = getOrCreateProfile()
        return UserBodyStats(
            age = profile.age,
            gender = profile.gender,
            height = profile.heightCm,
            weight = profile.weightKg,
            goal = profile.fitnessGoal,
            experience = profile.experienceLevel,
            healthIssues = profile.healthIssues
        )
    }

    /**
     * 执行 AI 体态分析
     */
    suspend fun analyzeBody(qwenRepository: QwenRepository): QwenResult<ParsedBodyAnalysis> {
        val profile = getOrCreateProfile()

        if (!profile.hasPhotos()) {
            return QwenResult.Error("请先上传照片")
        }

        val userStats = getUserBodyStats()

        // 使用正面照片进行分析
        val photoFile = when {
            profile.frontPhotoPath.isNotEmpty() -> java.io.File(profile.frontPhotoPath)
            profile.sidePhotoPath.isNotEmpty() -> java.io.File(profile.sidePhotoPath)
            else -> null
        }

        if (photoFile == null || !photoFile.exists()) {
            return QwenResult.Error("照片文件不存在")
        }

        return qwenRepository.analyzeBodyImage(photoFile, userStats)
    }

    // ========== 删除数据 ==========

    /**
     * 删除所有用户数据
     */
    suspend fun deleteAllUserData() {
        userProfileDao.deleteProfile()
        userProfileDao.deleteAllWeightRecords()
        userProfileDao.deleteAllMeasurements()
        settingsManager.isProfileComplete = false
    }
}

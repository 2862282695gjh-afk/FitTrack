package com.fittrack.data.db

import androidx.room.*
import com.fittrack.data.entity.BodyMeasurement
import com.fittrack.data.entity.UserProfile
import com.fittrack.data.entity.WeightRecord
import kotlinx.coroutines.flow.Flow

/**
 * 用户档案数据访问对象
 */
@Dao
interface UserProfileDao {

    // ========== 用户档案 ==========

    /**
     * 插入或更新用户档案
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile): Long

    /**
     * 获取用户档案（通常只有一条记录）
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getProfile(): UserProfile?

    /**
     * 获取用户档案 (Flow)
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getProfileFlow(): Flow<UserProfile?>

    /**
     * 更新身体参数
     */
    @Query("""
        UPDATE user_profile
        SET heightCm = :heightCm,
            weightKg = :weightKg,
            targetWeightKg = :targetWeightKg,
            updatedAt = :updatedAt
        WHERE id = :profileId
    """)
    suspend fun updateBodyStats(
        profileId: Long,
        heightCm: Double,
        weightKg: Double,
        targetWeightKg: Double,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * 更新照片路径
     */
    @Query("""
        UPDATE user_profile
        SET frontPhotoPath = :frontPath,
            sidePhotoPath = :sidePath,
            backPhotoPath = :backPath,
            updatedAt = :updatedAt
        WHERE id = :profileId
    """)
    suspend fun updatePhotoPaths(
        profileId: Long,
        frontPath: String,
        sidePath: String,
        backPath: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * 更新 AI 分析结果
     */
    @Query("""
        UPDATE user_profile
        SET bodyAnalysisJson = :analysisJson,
            updatedAt = :updatedAt
        WHERE id = :profileId
    """)
    suspend fun updateBodyAnalysis(
        profileId: Long,
        analysisJson: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * 删除用户档案
     */
    @Query("DELETE FROM user_profile")
    suspend fun deleteProfile()

    // ========== 体重记录 ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightRecord(record: WeightRecord): Long

    @Query("SELECT * FROM weight_record ORDER BY recordedAt DESC")
    fun getAllWeightRecords(): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_record ORDER BY recordedAt DESC LIMIT :limit")
    fun getRecentWeightRecords(limit: Int = 30): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_record WHERE recordedAt BETWEEN :startTime AND :endTime ORDER BY recordedAt ASC")
    suspend fun getWeightRecordsBetween(startTime: Long, endTime: Long): List<WeightRecord>

    @Query("SELECT * FROM weight_record ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestWeightRecord(): WeightRecord?

    @Delete
    suspend fun deleteWeightRecord(record: WeightRecord)

    @Query("DELETE FROM weight_record")
    suspend fun deleteAllWeightRecords()

    // ========== 身体测量记录 ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: BodyMeasurement): Long

    @Query("SELECT * FROM body_measurement ORDER BY measuredAt DESC")
    fun getAllMeasurements(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurement ORDER BY measuredAt DESC LIMIT :limit")
    fun getRecentMeasurements(limit: Int = 10): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurement WHERE measuredAt BETWEEN :startTime AND :endTime ORDER BY measuredAt ASC")
    suspend fun getMeasurementsBetween(startTime: Long, endTime: Long): List<BodyMeasurement>

    @Query("SELECT * FROM body_measurement ORDER BY measuredAt DESC LIMIT 1")
    suspend fun getLatestMeasurement(): BodyMeasurement?

    @Delete
    suspend fun deleteMeasurement(measurement: BodyMeasurement)

    @Query("DELETE FROM body_measurement")
    suspend fun deleteAllMeasurements()
}

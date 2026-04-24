package com.fittrack.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fittrack.data.entity.BodyMeasurement
import com.fittrack.data.entity.ChatMessage
import com.fittrack.data.entity.ChatSession
import com.fittrack.data.entity.Exercise
import com.fittrack.data.entity.ExerciseRecord
import com.fittrack.data.entity.UserProfile
import com.fittrack.data.entity.WeightRecord
import com.fittrack.data.entity.MealRecord
import com.fittrack.data.entity.NutritionAdvice
import com.fittrack.data.entity.WorkoutPlan
import com.fittrack.data.entity.WorkoutRecord
import com.fittrack.data.entity.WorkoutSchedule

@Database(
    entities = [
        WorkoutPlan::class,
        Exercise::class,
        WorkoutRecord::class,
        ExerciseRecord::class,
        UserProfile::class,
        WeightRecord::class,
        BodyMeasurement::class,
        ChatMessage::class,
        ChatSession::class,
        WorkoutSchedule::class,
        MealRecord::class,
        NutritionAdvice::class
    ],
    version = 9,
    exportSchema = false
)
abstract class FitTrackDatabase : RoomDatabase() {

    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutRecordDao(): WorkoutRecordDao
    abstract fun exerciseRecordDao(): ExerciseRecordDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun chatDao(): ChatDao
    abstract fun workoutScheduleDao(): WorkoutScheduleDao
    abstract fun mealRecordDao(): MealRecordDao
    abstract fun nutritionAdviceDao(): NutritionAdviceDao

    companion object {
        @Volatile
        private var INSTANCE: FitTrackDatabase? = null

        /**
         * 数据库迁移：版本 1 -> 2
         * 添加用户档案、体重记录、身体测量记录表
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建用户档案表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        gender TEXT NOT NULL,
                        age INTEGER NOT NULL,
                        heightCm REAL NOT NULL,
                        weightKg REAL NOT NULL,
                        targetWeightKg REAL NOT NULL,
                        fitnessGoal TEXT NOT NULL,
                        experienceLevel TEXT NOT NULL,
                        weeklyAvailableMinutes INTEGER NOT NULL,
                        healthIssues TEXT NOT NULL,
                        frontPhotoPath TEXT NOT NULL,
                        sidePhotoPath TEXT NOT NULL,
                        backPhotoPath TEXT NOT NULL,
                        bodyAnalysisJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)

                // 创建体重记录表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS weight_record (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        weightKg REAL NOT NULL,
                        recordedAt INTEGER NOT NULL,
                        note TEXT NOT NULL
                    )
                """)

                // 创建身体测量记录表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS body_measurement (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        chestCm REAL NOT NULL,
                        waistCm REAL NOT NULL,
                        hipCm REAL NOT NULL,
                        leftArmCm REAL NOT NULL,
                        rightArmCm REAL NOT NULL,
                        leftThighCm REAL NOT NULL,
                        rightThighCm REAL NOT NULL,
                        measuredAt INTEGER NOT NULL,
                        note TEXT NOT NULL
                    )
                """)
            }
        }

        /**
         * 数据库迁移：版本 2 -> 3
         * 添加压力分析相关字段
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    ALTER TABLE workout_records
                    ADD COLUMN sleepQuality INTEGER NOT NULL DEFAULT 3
                """)
                db.execSQL("""
                    ALTER TABLE workout_records
                    ADD COLUMN appetite INTEGER NOT NULL DEFAULT 3
                """)
                db.execSQL("""
                    ALTER TABLE workout_records
                    ADD COLUMN energyLevel INTEGER NOT NULL DEFAULT 3
                """)
                db.execSQL("""
                    ALTER TABLE workout_records
                    ADD COLUMN metabolicPressure INTEGER NOT NULL DEFAULT 0
                """)
                db.execSQL("""
                    ALTER TABLE workout_records
                    ADD COLUMN mentalPressure INTEGER NOT NULL DEFAULT 0
                """)
                db.execSQL("""
                    ALTER TABLE workout_records
                    ADD COLUMN isDeload INTEGER NOT NULL DEFAULT 0
                """)
            }
        }

        /**
         * 数据库迁移：版本 3 -> 4
         * 给 exercises 表添加 dayOfWeek 字段，用于区分训练日
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    ALTER TABLE exercises
                    ADD COLUMN dayOfWeek INTEGER NOT NULL DEFAULT 1
                """)
            }
        }

        /**
         * 数据库迁移：版本 4 -> 5
         * 添加聊天消息和会话表
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建聊天消息表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_message (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        messageType TEXT NOT NULL DEFAULT 'text',
                        relatedPlanId INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """)
                // 创建索引
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_chat_message_createdAt ON chat_message(createdAt)
                """)

                // 创建聊天会话表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_session (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        lastMessageAt INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        /**
         * 数据库迁移：版本 5 -> 6
         * 给 workout_records 表添加 aiSummary 字段
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    ALTER TABLE workout_records
                    ADD COLUMN aiSummary TEXT NOT NULL DEFAULT ''
                """)
            }
        }

        /**
         * 数据库迁移：版本 6 -> 7
         * 给 chat_message 表添加 sessionId 字段
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    ALTER TABLE chat_message
                    ADD COLUMN sessionId INTEGER NOT NULL DEFAULT 0
                """)
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_chat_message_sessionId ON chat_message(sessionId)
                """)
            }
        }

        /**
         * 数据库迁移：版本 7 -> 8
         * 添加训练日程表（workout_schedules）支持日历绑定和逾期处理
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS workout_schedules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        planId INTEGER NOT NULL,
                        scheduledDate TEXT NOT NULL,
                        originalScheduledDate TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'pending',
                        isRescheduled INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)
                // 创建索引
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_workout_schedules_planId ON workout_schedules(planId)
                """)
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_workout_schedules_scheduledDate ON workout_schedules(scheduledDate)
                """)
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_workout_schedules_status ON workout_schedules(status)
                """)
            }
        }

        /**
         * 数据库迁移：版本 8 -> 9
         * 添加饮食记录表（meal_records）和营养推荐表（nutrition_advices）
         *
         * 回滚方案：DROP TABLE meal_records; DROP TABLE nutrition_advices;
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS meal_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        mealType TEXT NOT NULL DEFAULT 'lunch',
                        foodsJson TEXT NOT NULL DEFAULT '[]',
                        totalCalories REAL NOT NULL DEFAULT 0,
                        totalProtein REAL NOT NULL DEFAULT 0,
                        totalCarbs REAL NOT NULL DEFAULT 0,
                        totalFat REAL NOT NULL DEFAULT 0,
                        note TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_meal_records_date ON meal_records(date)
                """)
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_meal_records_date_type ON meal_records(date, mealType)
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS nutrition_advices (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        mealType TEXT NOT NULL DEFAULT 'lunch',
                        goal TEXT NOT NULL DEFAULT '',
                        targetCalories REAL NOT NULL DEFAULT 0,
                        targetProtein REAL NOT NULL DEFAULT 0,
                        targetCarbs REAL NOT NULL DEFAULT 0,
                        targetFat REAL NOT NULL DEFAULT 0,
                        adviceJson TEXT NOT NULL DEFAULT '[]',
                        summary TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_nutrition_advices_date ON nutrition_advices(date)
                """)
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_nutrition_advices_date_type ON nutrition_advices(date, mealType)
                """)
            }
        }

        fun getDatabase(context: Context): FitTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitTrackDatabase::class.java,
                    "fittrack_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

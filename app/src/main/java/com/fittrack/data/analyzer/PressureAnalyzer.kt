package com.fittrack.data.analyzer

import com.fittrack.data.entity.ExerciseRecord
import com.fittrack.data.entity.WorkoutRecord

/**
 * 压力分析结果
 */
data class PressureAnalysis(
    val metabolicPressure: Int,        // 代谢压力 0-100
    val mentalPressure: Int,            // 精神压力 0-100
    val needsDeload: Boolean,          // 是否需要减载
    val deloadReason: String,           // 减载原因
    val recommendations: List<String>   // 建议
)

/**
 * 压力分析器
 * 根据训练记录和用户反馈分析代谢压力和精神压力
 */
class PressureAnalyzer {

    companion object {
        private const val METABOLIC_WEIGHT = 0.6f  // 代谢压力权重
        private const val MENTAL_WEIGHT = 0.4f      // 精神压力权重

        private const val HIGH_PRESSURE_THRESHOLD = 75
        private const val DELOAD_THRESHOLD = 80

        /**
         * 分析压力水平
         * @param currentRecord 当前训练记录
         * @param recentRecords 最近7次训练记录
         * @param exerciseRecords 当前训练的动作记录
         */
        fun analyzePressure(
            currentRecord: WorkoutRecord,
            recentRecords: List<WorkoutRecord>,
            exerciseRecords: List<ExerciseRecord>
        ): PressureAnalysis {
            val metabolicPressure = calculateMetabolicPressure(currentRecord, recentRecords, exerciseRecords)
            val mentalPressure = calculateMentalPressure(currentRecord, recentRecords)

            val overallPressure = (metabolicPressure * METABOLIC_WEIGHT + mentalPressure * MENTAL_WEIGHT).toInt()
            val needsDeload = overallPressure >= DELOAD_THRESHOLD || mentalPressure >= 85

            val deloadReason = if (needsDeload) {
                when {
                    metabolicPressure >= 85 -> "代谢压力过高：训练负荷过大"
                    mentalPressure >= 85 -> "精神压力过高：恢复状态不佳"
                    overallPressure >= DELOAD_THRESHOLD -> "综合压力过高：需要减载恢复"
                    else -> "建议减载：避免过度训练"
                }
            } else ""

            val recommendations = generateRecommendations(metabolicPressure, mentalPressure)

            return PressureAnalysis(
                metabolicPressure = metabolicPressure,
                mentalPressure = mentalPressure,
                needsDeload = needsDeload,
                deloadReason = deloadReason,
                recommendations = recommendations
            )
        }

        /**
         * 计算代谢压力
         * 基于做组容量、重量变化趋势
         */
        private fun calculateMetabolicPressure(
            currentRecord: WorkoutRecord,
            recentRecords: List<WorkoutRecord>,
            exerciseRecords: List<ExerciseRecord>
        ): Int {
            // 1. 基于做组容量的压力
            val volumePressure = calculateVolumePressure(exerciseRecords)

            // 2. 基于感受评分的压力（感受差 = 代谢压力大）
            val feelingPressure = when (currentRecord.feeling) {
                1 -> 90
                2 -> 75
                3 -> 50
                4 -> 25
                5 -> 10
                else -> 50
            }

            // 3. 基于训练频率的压力
            val frequencyPressure = calculateFrequencyPressure(recentRecords)

            // 4. 基于能量水平的压力
            val energyPressure = (5 - currentRecord.energyLevel) * 20

            // 综合计算
            return ((volumePressure * 0.3f +
                    feelingPressure * 0.25f +
                    frequencyPressure * 0.25f +
                    energyPressure * 0.2f).toInt()
                ).coerceIn(0, 100)
        }

        /**
         * 计算做组容量压力
         */
        private fun calculateVolumePressure(exerciseRecords: List<ExerciseRecord>): Int {
            if (exerciseRecords.isEmpty()) return 0

            var totalVolume = 0
            exerciseRecords.forEach { record ->
                val weightList = record.weights.split(",").mapNotNull { it.toDoubleOrNull() }
                val repsList = record.reps.split(",").mapNotNull { it.toIntOrNull() }

                if (weightList.isNotEmpty() && repsList.isNotEmpty()) {
                    val avgWeight = weightList.average()
                    val avgReps = repsList.average()
                    totalVolume += (avgWeight * avgReps * record.sets).toInt()
                }
            }

            // 根据容量评分（假设正常容量为10000）
            return when {
                totalVolume > 15000 -> 80
                totalVolume > 10000 -> 60
                totalVolume > 5000 -> 40
                else -> 20
            }
        }

        /**
         * 计算训练频率压力
         */
        private fun calculateFrequencyPressure(recentRecords: List<WorkoutRecord>): Int {
            val weeklyCount = recentRecords.size
            return when (weeklyCount) {
                0, 1 -> 10
                2, 3 -> 30
                4, 5 -> 60
                6, 7 -> 85
                else -> 100
            }
        }

        /**
         * 计算精神压力
         * 基于睡眠质量、食欲、疲劳感
         */
        private fun calculateMentalPressure(
            currentRecord: WorkoutRecord,
            recentRecords: List<WorkoutRecord>
        ): Int {
            // 1. 睡眠质量压力（睡眠差 = 精神压力大）
            val sleepPressure = when (currentRecord.sleepQuality) {
                1 -> 90
                2 -> 75
                3 -> 50
                4 -> 25
                5 -> 10
                else -> 50
            }

            // 2. 食欲压力（食欲差 = 精神压力大）
            val appetitePressure = when (currentRecord.appetite) {
                1 -> 85
                2 -> 65
                3 -> 45
                4 -> 25
                5 -> 10
                else -> 50
            }

            // 3. 能量水平压力
            val energyPressure = (5 - currentRecord.energyLevel) * 20

            // 4. 近期持续疲劳压力
            val fatiguePressure = calculateFatiguePressure(recentRecords)

            // 综合计算
            return ((sleepPressure * 0.35f +
                    appetitePressure * 0.25f +
                    energyPressure * 0.25f +
                    fatiguePressure * 0.15f).toInt()
                ).coerceIn(0, 100)
        }

        /**
         * 计算持续疲劳压力
         */
        private fun calculateFatiguePressure(recentRecords: List<WorkoutRecord>): Int {
            if (recentRecords.isEmpty()) return 0

            val lowEnergyCount = recentRecords.count { it.energyLevel <= 2 }
            val lowSleepCount = recentRecords.count { it.sleepQuality <= 2 }

            return ((lowEnergyCount * 20 + lowSleepCount * 25) / recentRecords.size).toInt()
                .coerceIn(0, 100)
        }

        /**
         * 生成建议
         */
        private fun generateRecommendations(
            metabolicPressure: Int,
            mentalPressure: Int
        ): List<String> {
            val recommendations = mutableListOf<String>()

            when {
                metabolicPressure >= 85 -> {
                    recommendations.add("建议减少训练强度 40-50%")
                    recommendations.add("减少组数或次数，保持动作质量")
                }
                metabolicPressure >= 70 -> {
                    recommendations.add("适当降低训练重量")
                    recommendations.add("延长组间休息时间")
                }
            }

            when {
                mentalPressure >= 85 -> {
                    recommendations.add("建议休息1-2天，充分恢复")
                    recommendations.add("保证7-8小时优质睡眠")
                }
                mentalPressure >= 70 -> {
                    recommendations.add("关注睡眠质量")
                    recommendations.add("适当补充营养")
                }
                mentalPressure >= 55 -> {
                    recommendations.add("注意劳逸结合")
                }
            }

            if (metabolicPressure >= 70 || mentalPressure >= 70) {
                recommendations.add("可以考虑安排一次减载周")
            }

            return recommendations
        }

        /**
         * 生成减载训练计划提示
         */
        fun generateDeloadPrompt(
            normalPlan: String,
            pressureAnalysis: PressureAnalysis
        ): String {
            return """
                当前训练需要调整为减载训练。

                原因：${pressureAnalysis.deloadReason}

                当前压力状态：
                - 代谢压力：${pressureAnalysis.metabolicPressure}/100
                - 精神压力：${pressureAnalysis.mentalPressure}/100

                建议减载方案：
                1. 训练强度降低至正常的 40-60%
                2. 减少组数（例如：4组 → 2-3组）
                3. 保持动作模式，专注于技术
                4. 每组不要达到力竭
                5. 缩短训练时长

                基于以下正常训练计划，请生成减载版本的训练计划：

                $normalPlan

                减载周期建议：1-2周
            """.trimIndent()
        }
    }
}

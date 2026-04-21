package com.fittrack.ai.tools

import com.fittrack.data.api.QwenRepository
import com.fittrack.data.api.QwenResult

/**
 * 多模态分析工具
 * 支持图片和视频的健身动作分析
 */
class MultimodalAnalysisTool(
    private val qwenRepository: QwenRepository
) {
    /**
     * 分析图片
     * @param imageBase64 图片的 Base64 编码
     * @param userQuestion 用户问题
     * @param systemPrompt 系统提示
     */
    suspend fun analyzeImage(
        imageBase64: String,
        userQuestion: String,
        systemPrompt: String
    ): Result<String> {
        val prompt = buildImagePrompt(userQuestion)
        return when (val result = qwenRepository.chatWithImage(prompt, imageBase64, systemPrompt)) {
            is QwenResult.Success -> Result.success(result.data)
            is QwenResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * 分析视频（多帧）
     * @param videoFrames 视频帧列表（每秒一帧的 Base64）
     * @param userQuestion 用户问题
     * @param systemPrompt 系统提示
     */
    suspend fun analyzeVideo(
        videoFrames: List<String>,
        userQuestion: String,
        systemPrompt: String
    ): Result<String> {
        if (videoFrames.isEmpty()) {
            return Result.failure(Exception("视频帧提取失败"))
        }

        val prompt = buildVideoPrompt(userQuestion, videoFrames.size)

        return when (val result = qwenRepository.chatWithImages(prompt, videoFrames, systemPrompt)) {
            is QwenResult.Success -> Result.success(result.data)
            is QwenResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * 构建图片分析提示词
     */
    private fun buildImagePrompt(userQuestion: String): String {
        return if (userQuestion.isNotBlank()) {
            "用户上传了一张健身相关的图片。用户问题：$userQuestion\n\n" +
                    "请分析这张图片中的健身动作或内容，指出任何可能的问题或改进建议。"
        } else {
            "用户上传了一张健身相关的图片。请分析这张图片中的健身动作或内容，" +
                    "指出任何可能的问题或改进建议。"
        }
    }

    /**
     * 构建视频分析提示词
     */
    private fun buildVideoPrompt(userQuestion: String, frameCount: Int): String {
        return if (userQuestion.isNotBlank()) {
            "用户上传了一个健身视频（共 $frameCount 秒），以下是每秒提取的关键帧。用户问题：$userQuestion\n\n" +
                    "请基于这些视频帧分析用户的健身动作，指出问题并给出改进建议。"
        } else {
            "用户上传了一个健身视频（共 $frameCount 秒），以下是每秒提取的关键帧。" +
                    "请分析这个健身动作，指出任何可能的问题或改进建议。"
        }
    }
}

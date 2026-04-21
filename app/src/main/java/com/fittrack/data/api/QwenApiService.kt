package com.fittrack.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * Qwen API 服务接口
 * 支持阿里云百炼平台的文本和视觉模型
 */
interface QwenApiService {

    /**
     * Chat Completion API
     * 用于文本对话
     */
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: QwenChatRequest
    ): Response<QwenChatResponse>

    /**
     * Chat Completion API (流式)
     * 用于流式文本对话
     */
    @Streaming
    @POST("chat/completions")
    suspend fun chatCompletionStream(
        @Header("Authorization") authorization: String,
        @Body request: QwenChatRequest
    ): Response<ResponseBody>

    /**
     * Chat Completion API (VL 模型)
     * 用于图像分析
     */
    @POST("chat/completions")
    suspend fun vlCompletion(
        @Header("Authorization") authorization: String,
        @Body request: QwenVLRequest
    ): Response<QwenChatResponse>

    companion object {
        const val BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/"

        // 可用模型
        const val MODEL_QWEN_TURBO = "qwen-turbo"
        const val MODEL_QWEN_PLUS = "qwen-plus"
        const val MODEL_QWEN_MAX = "qwen-max"
        const val MODEL_QWEN_VL_PLUS = "qwen-vl-plus"
        const val MODEL_QWEN_VL_MAX = "qwen-vl-max"
    }
}

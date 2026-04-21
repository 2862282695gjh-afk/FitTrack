package com.fittrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * 聊天消息实体
 * 存储与 AI 教练的对话记录
 */
@Entity(
    tableName = "chat_message",
    indices = [Index(value = ["createdAt"]), Index(value = ["sessionId"])]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 所属会话 ID
    val sessionId: Long = 0,

    // 消息角色: "user" 或 "assistant"
    val role: String,

    // 消息内容
    val content: String,

    // 消息类型: "text", "workout_plan", "exercise_tip"
    val messageType: String = "text",

    // 关联的训练计划 ID (如果有)
    val relatedPlanId: Long? = null,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 是否是用户消息
     */
    fun isUser(): Boolean = role == "user"

    /**
     * 是否是 AI 消息
     */
    fun isAssistant(): Boolean = role == "assistant"
}

/**
 * 聊天会话实体
 * 管理聊天会话
 */
@Entity(tableName = "chat_session")
data class ChatSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 会话标题
    val title: String = "",

    // 会话摘要
    val summary: String = "",

    // 最后消息时间
    val lastMessageAt: Long = System.currentTimeMillis(),

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)

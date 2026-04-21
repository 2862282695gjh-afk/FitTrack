package com.fittrack.data.db

import androidx.room.*
import com.fittrack.data.entity.ChatMessage
import com.fittrack.data.entity.ChatSession
import kotlinx.coroutines.flow.Flow

/**
 * 聊天数据访问对象
 */
@Dao
interface ChatDao {

    // ========== 聊天消息 ==========

    /**
     * 插入消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    /**
     * 获取所有消息
     */
    @Query("SELECT * FROM chat_message ORDER BY createdAt ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    /**
     * 获取最近的消息
     */
    @Query("SELECT * FROM chat_message ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentMessages(limit: Int = 50): Flow<List<ChatMessage>>

    /**
     * 获取指定时间范围的消息
     */
    @Query("SELECT * FROM chat_message WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt ASC")
    suspend fun getMessagesBetween(startTime: Long, endTime: Long): List<ChatMessage>

    /**
     * 删除消息
     */
    @Delete
    suspend fun deleteMessage(message: ChatMessage)

    /**
     * 清空所有消息
     */
    @Query("DELETE FROM chat_message")
    suspend fun deleteAllMessages()

    /**
     * 获取消息数量
     */
    @Query("SELECT COUNT(*) FROM chat_message")
    suspend fun getMessageCount(): Int

    // ========== 聊天会话 ==========

    /**
     * 插入会话
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    /**
     * 获取所有会话
     */
    @Query("SELECT * FROM chat_session ORDER BY lastMessageAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    /**
     * 获取会话
     */
    @Query("SELECT * FROM chat_session WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): ChatSession?

    /**
     * 更新会话最后消息时间
     */
    @Query("UPDATE chat_session SET lastMessageAt = :lastMessageAt WHERE id = :sessionId")
    suspend fun updateSessionLastMessage(sessionId: Long, lastMessageAt: Long)

    /**
     * 删除会话
     */
    @Delete
    suspend fun deleteSession(session: ChatSession)

    /**
     * 清空所有会话
     */
    @Query("DELETE FROM chat_session")
    suspend fun deleteAllSessions()

    /**
     * 获取指定会话的消息
     */
    @Query("SELECT * FROM chat_message WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getMessagesBySession(sessionId: Long): Flow<List<com.fittrack.data.entity.ChatMessage>>

    /**
     * 更新会话标题
     */
    @Query("UPDATE chat_session SET title = :title WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: Long, title: String)

    /**
     * 获取会话数量
     */
    @Query("SELECT COUNT(*) FROM chat_session")
    suspend fun getSessionCount(): Int
}

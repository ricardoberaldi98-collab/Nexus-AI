package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val label: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_models")
data class AiModelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val modelId: String,
    val name: String,
    val provider: String,
    val isFree: Boolean = true,
    val isActive: Boolean = false,
    val description: String = ""
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long = 1L,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false,
    val modelUsed: String = "",
    val executionType: String = "REMOTE", // "LOCAL" or "REMOTE"
    val latencyMs: Long = 0L,
    val tokensPerSec: Float = 0f
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "Nova Conversa",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val language: String = "pt-BR",
    val isArchived: Boolean = false
)

@Entity(tableName = "conversation_memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String, // e.g., "user_preference", "topic_car", "user_goal"
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_models")
data class LocalModelEntity(
    @PrimaryKey
    val modelId: String, // e.g. "qwen2.5-0.5b-instruct-q4", "qwen2.5-1.5b-instruct-q4"
    val name: String,
    val family: String = "Qwen",
    val parameters: String = "1.5B",
    val quantization: String = "Q4_K_M",
    val sizeBytes: Long,
    val ramRequiredMb: Int,
    val downloadUrl: String,
    val localFilePath: String? = null,
    val isDownloaded: Boolean = false,
    val isActive: Boolean = false,
    val isRecommended: Boolean = false,
    val downloadProgress: Float = 0f,
    val lastBenchmarkTokensPerSec: Float = 0f,
    val lastBenchmarkTtftMs: Long = 0L
)

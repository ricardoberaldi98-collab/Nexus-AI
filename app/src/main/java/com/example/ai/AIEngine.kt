package com.example.ai

import kotlinx.coroutines.flow.Flow

data class ConversationMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class ConversationContext(
    val systemPrompt: String,
    val messages: List<ConversationMessage>,
    val userMemories: List<String> = emptyList(),
    val isFastResponseMode: Boolean = true,
    val language: String = "pt-BR"
)

data class GenerationOptions(
    val modelId: String,
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val quantization: String = "Q4_K_M"
)

interface AIEngine {
    val engineName: String
    val isLocal: Boolean

    fun isReady(): Boolean

    /**
     * Generates a streaming response token-by-token.
     */
    fun generateStream(context: ConversationContext, options: GenerationOptions): Flow<String>

    /**
     * Cancels active inference or network streaming immediately (for Barge-in).
     */
    suspend fun cancel()
}

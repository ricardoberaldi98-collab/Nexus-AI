package com.example.memory

import com.example.ai.ConversationContext
import com.example.ai.ConversationMessage
import com.example.data.local.ChatMessageDao
import com.example.data.local.ConversationDao
import com.example.data.local.MemoryDao
import com.example.data.model.ChatMessageEntity
import com.example.data.model.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContextManager(
    private val conversationDao: ConversationDao,
    private val chatMessageDao: ChatMessageDao,
    private val memoryDao: MemoryDao
) {
    /**
     * Prepares an optimized ConversationContext for the AI engine.
     * Prunes older context if too large and incorporates active long-term memories.
     */
    suspend fun buildContext(
        conversationId: Long = 1L,
        systemPrompt: String,
        currentUserPrompt: String,
        isFastResponseMode: Boolean = true,
        language: String = "pt-BR"
    ): ConversationContext = withContext(Dispatchers.IO) {
        // Fetch recent messages from database
        val recentEntities = chatMessageDao.getRecentMessages(conversationId, limit = 8).reversed()

        val messages = mutableListOf<ConversationMessage>()
        recentEntities.forEach { entity ->
            messages.add(ConversationMessage(role = entity.role, content = entity.content))
        }

        // Add current user prompt as the final turn
        if (currentUserPrompt.isNotBlank()) {
            messages.add(ConversationMessage(role = "user", content = currentUserPrompt))
        }

        // Fetch user memories (facts, topics)
        val memories = memoryDao.getRecentMemories().map { "${it.key}: ${it.value}" }

        ConversationContext(
            systemPrompt = systemPrompt,
            messages = messages,
            userMemories = memories,
            isFastResponseMode = isFastResponseMode,
            language = language
        )
    }

    /**
     * Analyzes turn to automatically persist key long-term facts.
     */
    suspend fun extractAndStoreMemory(userText: String, aiText: String) = withContext(Dispatchers.IO) {
        val lower = userText.lowercase()
        if (lower.contains("meu nome é") || lower.contains("me chamo")) {
            val name = userText.substringAfter("é", "").substringAfter("chamo", "").trim().take(30)
            if (name.isNotBlank()) {
                memoryDao.insertMemory(MemoryEntity(key = "nome_usuario", value = name))
            }
        } else if (lower.contains("moro em") || lower.contains("sou de")) {
            val location = userText.substringAfter("em", "").substringAfter("de", "").trim().take(30)
            if (location.isNotBlank()) {
                memoryDao.insertMemory(MemoryEntity(key = "localizacao", value = location))
            }
        } else if (lower.contains("gosto de") || lower.contains("meu carro") || lower.contains("minha moto")) {
            memoryDao.insertMemory(MemoryEntity(key = "interesse_usuario", value = userText.take(60)))
        }
    }

    suspend fun saveTurn(
        conversationId: Long,
        userText: String,
        aiText: String,
        modelUsed: String,
        isVoice: Boolean,
        executionType: String,
        latencyMs: Long,
        tokensPerSec: Float
    ) = withContext(Dispatchers.IO) {
        chatMessageDao.insertMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                role = "user",
                content = userText,
                isVoice = isVoice,
                modelUsed = modelUsed,
                executionType = executionType
            )
        )
        chatMessageDao.insertMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                role = "assistant",
                content = aiText,
                isVoice = isVoice,
                modelUsed = modelUsed,
                executionType = executionType,
                latencyMs = latencyMs,
                tokensPerSec = tokensPerSec
            )
        )
        extractAndStoreMemory(userText, aiText)
    }
}

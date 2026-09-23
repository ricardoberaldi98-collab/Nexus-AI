package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AiModelEntity
import com.example.data.model.ApiKeyEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.LocalModelEntity
import com.example.data.model.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY createdAt DESC")
    fun getAllApiKeys(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys WHERE isActive = 1 LIMIT 1")
    fun getActiveApiKey(): Flow<ApiKeyEntity?>

    @Query("SELECT * FROM api_keys WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveApiKeyDirect(): ApiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(key: ApiKeyEntity): Long

    @Update
    suspend fun updateApiKey(key: ApiKeyEntity)

    @Delete
    suspend fun deleteApiKey(key: ApiKeyEntity)

    @Query("UPDATE api_keys SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE api_keys SET isActive = 1 WHERE id = :id")
    suspend fun markActive(id: Long)

    @Transaction
    suspend fun setActiveKey(id: Long) {
        deactivateAll()
        markActive(id)
    }
}

@Dao
interface AiModelDao {
    @Query("SELECT * FROM ai_models ORDER BY isFree DESC, name ASC")
    fun getAllModels(): Flow<List<AiModelEntity>>

    @Query("SELECT * FROM ai_models WHERE isActive = 1 LIMIT 1")
    fun getActiveModel(): Flow<AiModelEntity?>

    @Query("SELECT * FROM ai_models WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveModelDirect(): AiModelEntity?

    @Query("SELECT COUNT(*) FROM ai_models")
    suspend fun countModels(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: AiModelEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(models: List<AiModelEntity>)

    @Update
    suspend fun updateModel(model: AiModelEntity)

    @Delete
    suspend fun deleteModel(model: AiModelEntity)

    @Query("UPDATE ai_models SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE ai_models SET isActive = 1 WHERE id = :id")
    suspend fun markActive(id: Long)

    @Transaction
    suspend fun setActiveModel(id: Long) {
        deactivateAll()
        markActive(id)
    }
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: Long, limit: Int = 10): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesGlobal(limit: Int = 10): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun clearMessagesForConversation(conversationId: Long)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conv: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conv: ConversationEntity)

    @Delete
    suspend fun deleteConversation(conv: ConversationEntity)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM conversation_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM conversation_memories ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentMemories(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Query("DELETE FROM conversation_memories WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM conversation_memories")
    suspend fun clearAll()
}

@Dao
interface LocalModelDao {
    @Query("SELECT * FROM local_models ORDER BY isRecommended DESC, sizeBytes ASC")
    fun getAllLocalModels(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM local_models WHERE isActive = 1 LIMIT 1")
    fun getActiveLocalModel(): Flow<LocalModelEntity?>

    @Query("SELECT * FROM local_models WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveLocalModelDirect(): LocalModelEntity?

    @Query("SELECT * FROM local_models WHERE isDownloaded = 1")
    suspend fun getDownloadedModels(): List<LocalModelEntity>

    @Query("SELECT * FROM local_models WHERE modelId = :modelId LIMIT 1")
    suspend fun getModelById(modelId: String): LocalModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(model: LocalModelEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(models: List<LocalModelEntity>)

    @Query("UPDATE local_models SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE local_models SET isActive = 1 WHERE modelId = :modelId")
    suspend fun markActive(modelId: String)

    @Transaction
    suspend fun setActiveLocalModel(modelId: String) {
        deactivateAll()
        markActive(modelId)
    }

    @Delete
    suspend fun delete(model: LocalModelEntity)
}

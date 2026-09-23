package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AiModelEntity
import com.example.data.model.ApiKeyEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.LocalModelEntity
import com.example.data.model.MemoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ApiKeyEntity::class,
        AiModelEntity::class,
        ChatMessageEntity::class,
        ConversationEntity::class,
        MemoryEntity::class,
        LocalModelEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun aiModelDao(): AiModelDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun localModelDao(): LocalModelDao

    companion object {
        @Volatile
        private var INSTANCE: NexusDatabase? = null

        fun getInstance(context: Context): NexusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexusDatabase::class.java,
                    "nexus_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.aiModelDao().insertAll(getDefaultModels())
                                database.localModelDao().insertAll(getDefaultLocalQwenModels())
                                database.conversationDao().insertConversation(
                                    ConversationEntity(
                                        id = 1L,
                                        title = "Conversa Principal",
                                        language = "pt-BR"
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun getDefaultModels(): List<AiModelEntity> {
            return listOf(
                AiModelEntity(
                    modelId = "google/gemini-2.0-flash-exp:free",
                    name = "Gemini 2.0 Flash (Free)",
                    provider = "Google",
                    isFree = true,
                    isActive = true,
                    description = "Ultra-rápido, baixa latência, excelente para respostas conversacionais."
                ),
                AiModelEntity(
                    modelId = "meta-llama/llama-3.3-70b-instruct:free",
                    name = "Llama 3.3 70B (Free)",
                    provider = "Meta",
                    isFree = true,
                    isActive = false,
                    description = "Raciocínio avançado e grande profundidade de conhecimento."
                ),
                AiModelEntity(
                    modelId = "deepseek/deepseek-r1:free",
                    name = "DeepSeek R1 (Free)",
                    provider = "DeepSeek",
                    isFree = true,
                    isActive = false,
                    description = "Especialista em raciocínio analítico e lógica complexa."
                ),
                AiModelEntity(
                    modelId = "qwen/qwen-2.5-72b-instruct:free",
                    name = "Qwen 2.5 72B (Free)",
                    provider = "Alibaba / Qwen",
                    isFree = true,
                    isActive = false,
                    description = "Excelente fluência em português do Brasil e instruções de voz."
                ),
                AiModelEntity(
                    modelId = "mistralai/mistral-7b-instruct:free",
                    name = "Mistral 7B (Free)",
                    provider = "Mistral AI",
                    isFree = true,
                    isActive = false,
                    description = "Respostas concisas, rápidas e precisas."
                ),
                AiModelEntity(
                    modelId = "openrouter/auto",
                    name = "OpenRouter Auto",
                    provider = "OpenRouter",
                    isFree = true,
                    isActive = false,
                    description = "Roteador inteligente para selecionar a melhor IA disponível."
                )
            )
        }

        fun getDefaultLocalQwenModels(): List<LocalModelEntity> {
            return listOf(
                LocalModelEntity(
                    modelId = "qwen2.5-0.5b-instruct-q4",
                    name = "Qwen 2.5 0.5B Instruct (Ultra-Rápido)",
                    family = "Qwen",
                    parameters = "0.5B",
                    quantization = "Q4_K_M",
                    sizeBytes = 390_000_000L, // ~390 MB
                    ramRequiredMb = 650,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
                    isDownloaded = false,
                    isActive = false,
                    isRecommended = false,
                    downloadProgress = 0f
                ),
                LocalModelEntity(
                    modelId = "qwen2.5-1.5b-instruct-q4",
                    name = "Qwen 2.5 1.5B Instruct (Equilibrado Poco X5)",
                    family = "Qwen",
                    parameters = "1.5B",
                    quantization = "Q4_K_M",
                    sizeBytes = 980_000_000L, // ~980 MB
                    ramRequiredMb = 1400,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
                    isDownloaded = false,
                    isActive = true,
                    isRecommended = true,
                    downloadProgress = 0f
                ),
                LocalModelEntity(
                    modelId = "qwen2.5-3b-instruct-q4",
                    name = "Qwen 2.5 3B Instruct (Alta Precisão)",
                    family = "Qwen",
                    parameters = "3B",
                    quantization = "Q4_K_M",
                    sizeBytes = 1_950_000_000L, // ~1.95 GB
                    ramRequiredMb = 2600,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf",
                    isDownloaded = false,
                    isActive = false,
                    isRecommended = false,
                    downloadProgress = 0f
                ),
                LocalModelEntity(
                    modelId = "qwen3-mobile-preview-q4",
                    name = "Qwen 3 Mobile Preview (Próxima Geração)",
                    family = "Qwen",
                    parameters = "1.8B",
                    quantization = "Q4_K_M",
                    sizeBytes = 1_150_000_000L, // ~1.15 GB
                    ramRequiredMb = 1600,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen3-Mobile-Preview-GGUF/resolve/main/qwen3-mobile-q4_k_m.gguf",
                    isDownloaded = false,
                    isActive = false,
                    isRecommended = false,
                    downloadProgress = 0f
                )
            )
        }
    }
}

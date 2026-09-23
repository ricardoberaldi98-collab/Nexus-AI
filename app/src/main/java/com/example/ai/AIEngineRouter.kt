package com.example.ai

import android.util.Log
import com.example.ai.local.LocalAIEngine
import com.example.ai.remote.OpenRouterAIEngine
import com.example.data.local.AIExecutionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class AIEngineRouter(
    private val localEngine: LocalAIEngine,
    private val remoteEngine: OpenRouterAIEngine
) {
    private var currentActiveEngine: AIEngine? = null

    /**
     * Executes generation respecting the chosen AIExecutionMode, with automatic fallback if configured.
     */
    fun routeGeneration(
        mode: AIExecutionMode,
        context: ConversationContext,
        options: GenerationOptions,
        hasDownloadedLocalModel: Boolean
    ): Flow<String> = flow {
        when (mode) {
            AIExecutionMode.LOCAL_ONLY -> {
                if (!hasDownloadedLocalModel) {
                    emit("Aviso: Nenhum modelo local Qwen está baixado no momento. Acesse Configurações -> IA Local para instalar um modelo compatível.")
                    return@flow
                }
                currentActiveEngine = localEngine
                localEngine.generateStream(context, options).collect { emit(it) }
            }

            AIExecutionMode.REMOTE_ONLY -> {
                currentActiveEngine = remoteEngine
                remoteEngine.generateStream(context, options).collect { emit(it) }
            }

            AIExecutionMode.AUTOMATIC -> {
                // Try local first if downloaded; otherwise seamlessly fallback to remote
                if (hasDownloadedLocalModel) {
                    currentActiveEngine = localEngine
                    var emittedAny = false
                    try {
                        localEngine.generateStream(context, options)
                            .catch { err ->
                                Log.w("AIEngineRouter", "Local engine error: ${err.message}. Falling back to OpenRouter API.")
                                currentActiveEngine = remoteEngine
                                remoteEngine.generateStream(context, options).collect { emit(it) }
                            }
                            .collect {
                                emittedAny = true
                                emit(it)
                            }
                    } catch (e: Exception) {
                        if (!emittedAny) {
                            Log.w("AIEngineRouter", "Local engine exception. Falling back to Remote.")
                            currentActiveEngine = remoteEngine
                            remoteEngine.generateStream(context, options).collect { emit(it) }
                        } else {
                            throw e
                        }
                    }
                } else {
                    currentActiveEngine = remoteEngine
                    remoteEngine.generateStream(context, options).collect { emit(it) }
                }
            }
        }
    }

    suspend fun cancelCurrentGeneration() {
        try {
            currentActiveEngine?.cancel()
            localEngine.cancel()
            remoteEngine.cancel()
            Log.d("AIEngineRouter", "All active engine streams cancelled.")
        } catch (e: Exception) {
            Log.e("AIEngineRouter", "Error cancelling engines", e)
        }
    }
}

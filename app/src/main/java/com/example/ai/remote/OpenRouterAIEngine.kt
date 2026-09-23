package com.example.ai.remote

import android.util.Log
import com.example.ai.AIEngine
import com.example.ai.ConversationContext
import com.example.ai.GenerationOptions
import com.example.data.api.OpenRouterClient
import com.example.hardware.DiagnosticsTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class OpenRouterAIEngine(
    private var apiKeyProvider: () -> String
) : AIEngine {

    override val engineName: String = "OpenRouter Remote"
    override val isLocal: Boolean = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var activeCall: Call? = null

    override fun isReady(): Boolean {
        return apiKeyProvider().isNotBlank()
    }

    override fun generateStream(
        context: ConversationContext,
        options: GenerationOptions
    ): Flow<String> = flow {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isEmpty()) {
            throw IllegalStateException("Chave da OpenRouter não configurada.")
        }

        DiagnosticsTracker.onLlmRequestStarted(
            engineName = "OpenRouter (${options.modelId.substringAfterLast("/")})",
            isLocal = false,
            quantization = "Cloud FP16"
        )

        val jsonBody = JSONObject().apply {
            put("model", options.modelId)
            put("temperature", if (context.isFastResponseMode) 0.5 else 0.7)
            put("max_tokens", if (context.isFastResponseMode) 220 else 512)
            put("stream", true)

            val messagesArray = JSONArray()

            // Strict Brazilian Portuguese system instruction
            val systemContent = StringBuilder().apply {
                append(context.systemPrompt)
                append(" REGRAS OBRIGATÓRIAS: O idioma é Português do Brasil (pt-BR). Entenda e responda em português brasileiro. ")
                if (context.isFastResponseMode) {
                    append("Seja extremamente direto, conciso e natural para síntese de voz (1 a 3 frases no máximo).")
                }
                if (context.userMemories.isNotEmpty()) {
                    append(" Fatos importantes do usuário: ")
                    append(context.userMemories.joinToString("; "))
                }
            }.toString()

            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemContent)
            })

            // Conversation history
            context.messages.forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }

            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://nexus.ai")
            .header("X-Title", "Nexus AI Assistant")
            .header("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val call = httpClient.newCall(request)
        activeCall = call

        try {
            val response = withContext(Dispatchers.IO) { call.execute() }

            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                val parsedMsg = try {
                    JSONObject(errorBody).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "Erro HTTP ${response.code}: $errorBody"
                }
                throw IllegalStateException(parsedMsg)
            }

            val body = response.body ?: throw IllegalStateException("Resposta vazia da OpenRouter")
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim().orEmpty()
                if (currentLine.isEmpty() || currentLine.startsWith(":")) continue

                if (currentLine == "data: [DONE]") {
                    break
                }

                if (currentLine.startsWith("data: ")) {
                    val dataJson = currentLine.removePrefix("data: ").trim()
                    try {
                        val json = JSONObject(dataJson)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val content = delta?.optString("content")
                            if (!content.isNullOrEmpty()) {
                                DiagnosticsTracker.onFirstTokenReceived()
                                emit(content)
                            }
                        }
                    } catch (e: Exception) {
                        // Skip unparseable heartbeats
                    }
                }
            }
        } catch (e: CancellationException) {
            Log.d("OpenRouterAIEngine", "Streaming cancelado por interrupção (Barge-in).")
            throw e
        } finally {
            activeCall = null
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun cancel() {
        withContext(Dispatchers.IO) {
            try {
                activeCall?.cancel()
                activeCall = null
                Log.d("OpenRouterAIEngine", "Active HTTP call cancelled.")
            } catch (e: Exception) {
                // Ignore cancel errors
            }
        }
    }
}

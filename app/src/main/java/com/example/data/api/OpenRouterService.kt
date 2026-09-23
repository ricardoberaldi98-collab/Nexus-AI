package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface OpenRouterApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String = "https://aistudio.google.com/nexus-ai",
        @Header("X-Title") title: String = "Nexus AI Assistant",
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}

class OpenRouterClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: OpenRouterApi = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenRouterApi::class.java)

    suspend fun generateResponse(
        apiKey: String,
        model: String,
        messages: List<MessagePayload>,
        temperature: Double = 0.7
    ): OpenRouterResult {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return OpenRouterResult.Error("Nenhuma chave de API da OpenRouter configurada. Adicione sua chave nas Configurações.")
        }

        val authHeader = if (cleanKey.startsWith("Bearer ", ignoreCase = true)) {
            cleanKey
        } else {
            "Bearer $cleanKey"
        }

        val startTime = System.currentTimeMillis()
        return try {
            val response = api.createChatCompletion(
                authHeader = authHeader,
                request = ChatCompletionRequest(
                    model = model,
                    messages = messages,
                    temperature = temperature
                )
            )

            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.choices?.firstOrNull()?.message?.content
                if (!text.isNullOrBlank()) {
                    OpenRouterResult.Success(
                        text = text.trim(),
                        model = model,
                        latencyMs = latency
                    )
                } else if (body?.error != null) {
                    OpenRouterResult.Error("OpenRouter: ${body.error.message ?: "Erro desconhecido"}")
                } else {
                    OpenRouterResult.Error("A IA retornou uma resposta em branco.")
                }
            } else {
                val code = response.code()
                val errorMsg = when (code) {
                    401 -> "Chave da OpenRouter inválida ou não autorizada (401). Verifique sua chave nas configurações."
                    402 -> "Créditos insuficientes na OpenRouter para este modelo (402)."
                    404 -> "Modelo '$model' não foi encontrado na OpenRouter (404)."
                    429 -> "Limite de requisições excedido na OpenRouter (429 Rate Limit). Aguarde alguns segundos."
                    500, 502, 503 -> "Serviço do modelo temporariamente indisponível ($code). Tente outro modelo."
                    else -> "Erro na requisição (${code}): ${response.message()}"
                }
                OpenRouterResult.Error(errorMsg, errorCode = code)
            }
        } catch (e: Exception) {
            OpenRouterResult.Error("Falha de conexão: ${e.localizedMessage ?: "Verifique sua internet."}")
        }
    }

    suspend fun testConnection(apiKey: String, model: String): OpenRouterResult {
        return generateResponse(
            apiKey = apiKey,
            model = model,
            messages = listOf(
                MessagePayload(role = "system", content = "Você é um testador de conexão de API."),
                MessagePayload(role = "user", content = "Responda apenas com a palavra 'CONECTADO'.")
            ),
            temperature = 0.1
        )
    }
}

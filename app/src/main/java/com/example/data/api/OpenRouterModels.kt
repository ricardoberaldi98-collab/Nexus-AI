package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessagePayload(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<MessagePayload>,
    @Json(name = "temperature") val temperature: Double? = 0.7
)

@JsonClass(generateAdapter = true)
data class ChoicePayload(
    @Json(name = "index") val index: Int? = 0,
    @Json(name = "message") val message: MessagePayload?,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterErrorPayload(
    @Json(name = "code") val code: Any? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<ChoicePayload>? = null,
    @Json(name = "error") val error: OpenRouterErrorPayload? = null
)

sealed class OpenRouterResult {
    data class Success(val text: String, val model: String, val latencyMs: Long) : OpenRouterResult()
    data class Error(val message: String, val errorCode: Int? = null) : OpenRouterResult()
}

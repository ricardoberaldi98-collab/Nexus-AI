package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AIExecutionMode {
    LOCAL_ONLY,
    REMOTE_ONLY,
    AUTOMATIC // Uses local if model is ready, falls back to remote API
}

enum class ResponseSpeedMode {
    FAST_RESPONSE, // Optimized for conversational voice: concise, low latency, lower temperature
    ADVANCED_REASONING // Deep thinking, larger tokens
}

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nexus_ai_prefs", Context.MODE_PRIVATE)

    // Language configuration (pt-BR default)
    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "pt-BR") ?: "pt-BR")
    val language: StateFlow<String> = _language.asStateFlow()

    // Show/hide transcription in voice screen
    private val _showVoiceTranscription =
        MutableStateFlow(prefs.getBoolean(KEY_SHOW_TRANSCRIPTION, true))
    val showVoiceTranscription: StateFlow<Boolean> = _showVoiceTranscription.asStateFlow()

    // TTS speech rate & pitch
    private val _speechRate = MutableStateFlow(prefs.getFloat(KEY_SPEECH_RATE, 1.05f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(prefs.getFloat(KEY_SPEECH_PITCH, 1.0f))
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    // System prompt with mandatory Brazilian Portuguese instruction
    private val defaultSystemPrompt =
        "Você é o Nexus AI, um assistente de voz conversacional e inteligente. " +
        "O idioma principal desta conversa é Português do Brasil (pt-BR). " +
        "Entenda e responda sempre em português brasileiro de forma natural, concisa e fluida para fala. " +
        "Não mude para inglês automaticamente. Mesmo com termos em outros idiomas, responda em português. " +
        "Em conversas por voz, forneça respostas diretas e objetivas para manter a conversa ágil."

    private val _systemPrompt =
        MutableStateFlow(prefs.getString(KEY_SYSTEM_PROMPT, defaultSystemPrompt) ?: defaultSystemPrompt)
    val systemPrompt: StateFlow<String> = _systemPrompt.asStateFlow()

    // AI Execution Mode (Local, Remote, Automatic)
    private val _aiExecutionMode = MutableStateFlow(
        try {
            AIExecutionMode.valueOf(
                prefs.getString(KEY_AI_EXECUTION_MODE, AIExecutionMode.AUTOMATIC.name) ?: AIExecutionMode.AUTOMATIC.name
            )
        } catch (e: Exception) {
            AIExecutionMode.AUTOMATIC
        }
    )
    val aiExecutionMode: StateFlow<AIExecutionMode> = _aiExecutionMode.asStateFlow()

    // Response Speed Mode (Fast vs Reasoning)
    private val _responseSpeedMode = MutableStateFlow(
        try {
            ResponseSpeedMode.valueOf(
                prefs.getString(KEY_RESPONSE_SPEED_MODE, ResponseSpeedMode.FAST_RESPONSE.name) ?: ResponseSpeedMode.FAST_RESPONSE.name
            )
        } catch (e: Exception) {
            ResponseSpeedMode.FAST_RESPONSE
        }
    )
    val responseSpeedMode: StateFlow<ResponseSpeedMode> = _responseSpeedMode.asStateFlow()

    // Continuous Conversation (Continuous listening via VAD)
    private val _continuousConversation =
        MutableStateFlow(prefs.getBoolean(KEY_CONTINUOUS_CONVERSATION, true))
    val continuousConversation: StateFlow<Boolean> = _continuousConversation.asStateFlow()

    // Barge-in (Immediate interruption when user speaks)
    private val _bargeInEnabled = MutableStateFlow(prefs.getBoolean(KEY_BARGE_IN, true))
    val bargeInEnabled: StateFlow<Boolean> = _bargeInEnabled.asStateFlow()

    // Active Local Qwen Model ID
    private val _activeLocalModelId =
        MutableStateFlow(prefs.getString(KEY_ACTIVE_LOCAL_MODEL, "qwen2.5-1.5b-instruct-q4") ?: "qwen2.5-1.5b-instruct-q4")
    val activeLocalModelId: StateFlow<String> = _activeLocalModelId.asStateFlow()

    // VAD Silence Timeout in Milliseconds (default 750ms)
    private val _vadSilenceTimeoutMs =
        MutableStateFlow(prefs.getLong(KEY_VAD_SILENCE_TIMEOUT, 750L))
    val vadSilenceTimeoutMs: StateFlow<Long> = _vadSilenceTimeoutMs.asStateFlow()

    fun setLanguage(lang: String) {
        _language.value = lang
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun setShowVoiceTranscription(show: Boolean) {
        _showVoiceTranscription.value = show
        prefs.edit().putBoolean(KEY_SHOW_TRANSCRIPTION, show).apply()
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
    }

    fun setSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        prefs.edit().putFloat(KEY_SPEECH_PITCH, pitch).apply()
    }

    fun setSystemPrompt(prompt: String) {
        _systemPrompt.value = prompt
        prefs.edit().putString(KEY_SYSTEM_PROMPT, prompt).apply()
    }

    fun setAiExecutionMode(mode: AIExecutionMode) {
        _aiExecutionMode.value = mode
        prefs.edit().putString(KEY_AI_EXECUTION_MODE, mode.name).apply()
    }

    fun setResponseSpeedMode(mode: ResponseSpeedMode) {
        _responseSpeedMode.value = mode
        prefs.edit().putString(KEY_RESPONSE_SPEED_MODE, mode.name).apply()
    }

    fun setContinuousConversation(enabled: Boolean) {
        _continuousConversation.value = enabled
        prefs.edit().putBoolean(KEY_CONTINUOUS_CONVERSATION, enabled).apply()
    }

    fun setBargeInEnabled(enabled: Boolean) {
        _bargeInEnabled.value = enabled
        prefs.edit().putBoolean(KEY_BARGE_IN, enabled).apply()
    }

    fun setActiveLocalModelId(modelId: String) {
        _activeLocalModelId.value = modelId
        prefs.edit().putString(KEY_ACTIVE_LOCAL_MODEL, modelId).apply()
    }

    fun setVadSilenceTimeoutMs(timeoutMs: Long) {
        _vadSilenceTimeoutMs.value = timeoutMs
        prefs.edit().putLong(KEY_VAD_SILENCE_TIMEOUT, timeoutMs).apply()
    }

    companion object {
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_SHOW_TRANSCRIPTION = "key_show_transcription"
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
        private const val KEY_SYSTEM_PROMPT = "key_system_prompt"
        private const val KEY_AI_EXECUTION_MODE = "key_ai_execution_mode"
        private const val KEY_RESPONSE_SPEED_MODE = "key_response_speed_mode"
        private const val KEY_CONTINUOUS_CONVERSATION = "key_continuous_conversation"
        private const val KEY_BARGE_IN = "key_barge_in"
        private const val KEY_ACTIVE_LOCAL_MODEL = "key_active_local_model"
        private const val KEY_VAD_SILENCE_TIMEOUT = "key_vad_silence_timeout"
    }
}

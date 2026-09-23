package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechHelper(
    private val context: Context,
    private val onInitStatus: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentOnComplete: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale("pt", "BR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.US)
                }
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        currentOnComplete?.invoke()
                        currentOnComplete = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        currentOnComplete = null
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        currentOnComplete = null
                    }
                })
            }
            _isReady.value = true
            onInitStatus(true)
        } else {
            _isReady.value = false
            onInitStatus(false)
        }
    }

    fun setLanguage(languageCode: String) {
        tts?.let { engine ->
            val locale = if (languageCode.startsWith("en", ignoreCase = true)) {
                Locale.US
            } else {
                Locale("pt", "BR")
            }
            val result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default if language not supported
                engine.setLanguage(Locale.getDefault())
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    fun speak(
        text: String,
        languageCode: String = "pt-BR",
        rate: Float = 1.0f,
        pitch: Float = 1.0f,
        onComplete: () -> Unit = {}
    ) {
        if (!_isReady.value || tts == null) {
            onComplete()
            return
        }

        stop()
        setLanguage(languageCode)
        setSpeechRate(rate)
        setPitch(pitch)

        currentOnComplete = onComplete

        // Clean text of markdown formatting for cleaner TTS pronunciation
        val cleanText = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`{1,3}(.*?)`{1,3}"), "$1")
            .replace(Regex("#+\\s*"), "")
            .trim()

        val utteranceId = "nexus_tts_${System.currentTimeMillis()}"
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            if (_isSpeaking.value) {
                tts?.stop()
                _isSpeaking.value = false
            }
            currentOnComplete = null
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            _isReady.value = false
        } catch (_: Exception) {}
    }
}

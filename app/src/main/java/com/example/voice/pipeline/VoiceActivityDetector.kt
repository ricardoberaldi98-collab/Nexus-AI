package com.example.voice.pipeline

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceActivityDetector(
    private var silenceTimeoutMs: Long = 750L,
    private val speechThresholdRms: Float = 0.25f,
    private val onSpeechStarted: () -> Unit,
    private val onSpeechEnded: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSpeaking = false

    private val _vadState = MutableStateFlow(VadState.SILENCE)
    val vadState: StateFlow<VadState> = _vadState.asStateFlow()

    private val silenceRunnable = Runnable {
        if (isUserSpeaking) {
            isUserSpeaking = false
            _vadState.value = VadState.SILENCE
            onSpeechEnded()
        }
    }

    fun updateSilenceTimeout(timeoutMs: Long) {
        this.silenceTimeoutMs = timeoutMs
    }

    /**
     * Feeds the normalized RMS level (0.0 to 1.0) into the detector.
     */
    fun processRmsLevel(normalizedRms: Float) {
        if (normalizedRms >= speechThresholdRms) {
            handler.removeCallbacks(silenceRunnable)
            if (!isUserSpeaking) {
                isUserSpeaking = true
                _vadState.value = VadState.SPEECH
                onSpeechStarted()
            }
            // Rearm silence timer
            handler.postDelayed(silenceRunnable, silenceTimeoutMs)
        }
    }

    fun reset() {
        handler.removeCallbacks(silenceRunnable)
        isUserSpeaking = false
        _vadState.value = VadState.SILENCE
    }
}

enum class VadState {
    SILENCE,
    SPEECH
}

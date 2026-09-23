package com.example.voice.pipeline

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConversationalState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    THINKING,
    SPEAKING,
    INTERRUPTED,
    ERROR
}

class ContinuousConversationManager(
    private val scope: CoroutineScope,
    private val onStartListening: () -> Unit,
    private val onStopListening: () -> Unit,
    private val onBargeInTriggered: () -> Unit,
    private val onSendTranscriptionToAI: (String) -> Unit
) {
    private val _state = MutableStateFlow(ConversationalState.IDLE)
    val state: StateFlow<ConversationalState> = _state.asStateFlow()

    private var isContinuousModeActive = true
    private var isProcessingAi = false
    private var listenRestartJob: Job? = null

    fun setContinuousMode(enabled: Boolean) {
        this.isContinuousModeActive = enabled
    }

    fun isContinuous(): Boolean = isContinuousModeActive

    fun onSpeechStarted() {
        Log.d("ConversationManager", "User speech started. Current state: ${_state.value}")

        if (_state.value == ConversationalState.SPEAKING || _state.value == ConversationalState.THINKING) {
            // IMMEDIATE BARGE-IN!
            Log.d("ConversationManager", "BARGE-IN TRIGGERED! Halting playback and generation.")
            _state.value = ConversationalState.INTERRUPTED
            onBargeInTriggered()
            _state.value = ConversationalState.LISTENING
        } else if (_state.value == ConversationalState.IDLE) {
            _state.value = ConversationalState.LISTENING
            onStartListening()
        }
    }

    fun onPartialTranscription(text: String) {
        if (text.isNotBlank() && _state.value == ConversationalState.LISTENING) {
            _state.value = ConversationalState.TRANSCRIBING
        }
    }

    fun onFinalTranscription(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) {
            _state.value = ConversationalState.THINKING
            isProcessingAi = true
            onSendTranscriptionToAI(trimmed)
        } else {
            if (isContinuousModeActive) {
                scheduleResumeListening(200)
            } else {
                _state.value = ConversationalState.IDLE
            }
        }
    }

    fun onAiGenerationStarted() {
        _state.value = ConversationalState.THINKING
    }

    fun onAiSpeakingStarted() {
        _state.value = ConversationalState.SPEAKING
    }

    fun onAiSpeakingFinished() {
        isProcessingAi = false
        if (isContinuousModeActive) {
            _state.value = ConversationalState.LISTENING
            scheduleResumeListening(400)
        } else {
            _state.value = ConversationalState.IDLE
        }
    }

    fun onError(message: String) {
        _state.value = ConversationalState.ERROR
        if (isContinuousModeActive) {
            scheduleResumeListening(1500)
        }
    }

    fun pauseConversation() {
        listenRestartJob?.cancel()
        _state.value = ConversationalState.IDLE
        onStopListening()
    }

    fun resumeConversation() {
        _state.value = ConversationalState.LISTENING
        onStartListening()
    }

    private fun scheduleResumeListening(delayMs: Long) {
        listenRestartJob?.cancel()
        listenRestartJob = scope.launch(Dispatchers.Main) {
            delay(delayMs)
            if (isContinuousModeActive && _state.value != ConversationalState.SPEAKING && _state.value != ConversationalState.THINKING) {
                _state.value = ConversationalState.LISTENING
                onStartListening()
            }
        }
    }
}

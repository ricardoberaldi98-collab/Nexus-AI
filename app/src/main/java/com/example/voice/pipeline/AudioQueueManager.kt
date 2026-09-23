package com.example.voice.pipeline

import android.util.Log
import com.example.voice.TextToSpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class AudioQueueManager(
    private val ttsHelper: TextToSpeechHelper,
    private val scope: CoroutineScope
) {
    private val queue = ConcurrentLinkedQueue<String>()
    private val isPlaying = AtomicBoolean(false)
    private var currentPlaybackJob: Job? = null

    private val _isAudioActive = MutableStateFlow(false)
    val isAudioActive: StateFlow<Boolean> = _isAudioActive.asStateFlow()

    private var onQueueFinishedListener: (() -> Unit)? = null

    fun setOnQueueFinishedListener(listener: () -> Unit) {
        this.onQueueFinishedListener = listener
    }

    /**
     * Enqueues a chunk of text to be spoken.
     * Automatically triggers playback if not currently speaking.
     */
    fun enqueue(chunk: String, languageCode: String = "pt-BR", rate: Float = 1.05f, pitch: Float = 1.0f) {
        val clean = chunk.trim()
        if (clean.isEmpty()) return

        queue.add(clean)
        _isAudioActive.value = true

        if (!isPlaying.get()) {
            processNext(languageCode, rate, pitch)
        }
    }

    private fun processNext(languageCode: String, rate: Float, pitch: Float) {
        val nextChunk = queue.poll()
        if (nextChunk == null) {
            isPlaying.set(false)
            _isAudioActive.value = false
            onQueueFinishedListener?.invoke()
            return
        }

        isPlaying.set(true)
        _isAudioActive.value = true

        currentPlaybackJob = scope.launch(Dispatchers.Main) {
            ttsHelper.speak(
                text = nextChunk,
                languageCode = languageCode,
                rate = rate,
                pitch = pitch,
                onComplete = {
                    processNext(languageCode, rate, pitch)
                }
            )
        }
    }

    /**
     * BARGE-IN: Instantly cancels playback, halts TTS engine, and discards all pending queue segments.
     * Must be called immediately when user voice is detected.
     */
    fun interruptImmediately() {
        queue.clear()
        isPlaying.set(false)
        currentPlaybackJob?.cancel()
        currentPlaybackJob = null
        ttsHelper.stop()
        _isAudioActive.value = false
        Log.d("AudioQueueManager", "Barge-in executed: Audio playback halted immediately.")
    }

    fun clear() {
        interruptImmediately()
    }
}

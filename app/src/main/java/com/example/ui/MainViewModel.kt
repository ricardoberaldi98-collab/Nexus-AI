package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AIEngineRouter
import com.example.ai.ConversationContext
import com.example.ai.GenerationOptions
import com.example.ai.local.LocalAIEngine
import com.example.ai.local.LocalModelManager
import com.example.ai.remote.OpenRouterAIEngine
import com.example.data.api.OpenRouterClient
import com.example.data.local.AIExecutionMode
import com.example.data.local.NexusDatabase
import com.example.data.local.ResponseSpeedMode
import com.example.data.local.UserPreferences
import com.example.data.model.AiModelEntity
import com.example.data.model.ApiKeyEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.LocalModelEntity
import com.example.hardware.BenchmarkResult
import com.example.hardware.DeviceHardwareReport
import com.example.hardware.DiagnosticsTracker
import com.example.hardware.HardwareBenchmarkDetector
import com.example.hardware.TurnDiagnostics
import com.example.memory.ContextManager
import com.example.ui.components.InteractionMode
import com.example.voice.SpeechRecognizerHelper
import com.example.voice.TextToSpeechHelper
import com.example.voice.pipeline.AudioQueueManager
import com.example.voice.pipeline.ContinuousConversationManager
import com.example.voice.pipeline.ConversationalState
import com.example.voice.pipeline.TextChunker
import com.example.voice.pipeline.VoiceActivityDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VoiceAssistantState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    THINKING,
    SPEAKING,
    INTERRUPTED,
    ERROR
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = NexusDatabase.getInstance(application)
    private val apiKeyDao = db.apiKeyDao()
    private val aiModelDao = db.aiModelDao()
    private val chatMessageDao = db.chatMessageDao()
    private val conversationDao = db.conversationDao()
    private val memoryDao = db.memoryDao()
    private val localModelDao = db.localModelDao()

    val preferences = UserPreferences(application)
    val openRouterClient = OpenRouterClient()

    // Hardware & Benchmark
    val hardwareDetector = HardwareBenchmarkDetector(application)
    private val _deviceReport = MutableStateFlow(hardwareDetector.inspectHardware())
    val deviceReport: StateFlow<DeviceHardwareReport> = _deviceReport.asStateFlow()

    private val _benchmarkResult = MutableStateFlow<BenchmarkResult?>(null)
    val benchmarkResult: StateFlow<BenchmarkResult?> = _benchmarkResult.asStateFlow()

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

    // Local model management
    val localModelManager = LocalModelManager(application, localModelDao)
    val localModels: StateFlow<List<LocalModelEntity>> = localModelManager.allLocalModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeLocalModel: StateFlow<LocalModelEntity?> = localModelManager.activeLocalModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isDownloadingLocalModel = MutableStateFlow(false)
    val isDownloadingLocalModel: StateFlow<Boolean> = _isDownloadingLocalModel.asStateFlow()
    private val _localModelDownloadProgress = MutableStateFlow(0f)
    val localModelDownloadProgress: StateFlow<Float> = _localModelDownloadProgress.asStateFlow()

    // AI Engines
    val localAiEngine = LocalAIEngine(application, localModelDao)
    val remoteAiEngine = OpenRouterAIEngine {
        activeApiKey.value?.key ?: ""
    }
    val aiEngineRouter = AIEngineRouter(localAiEngine, remoteAiEngine)
    val contextManager = ContextManager(conversationDao, chatMessageDao, memoryDao)

    // Mode state
    private val _interactionMode = MutableStateFlow(InteractionMode.CHAT)
    val interactionMode: StateFlow<InteractionMode> = _interactionMode.asStateFlow()

    // Conversational states
    private val _voiceState = MutableStateFlow(VoiceAssistantState.IDLE)
    val voiceState: StateFlow<VoiceAssistantState> = _voiceState.asStateFlow()

    private val _liveUserTranscript = MutableStateFlow("")
    val liveUserTranscript: StateFlow<String> = _liveUserTranscript.asStateFlow()

    private val _liveAiTranscript = MutableStateFlow("")
    val liveAiTranscript: StateFlow<String> = _liveAiTranscript.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _statusNotification = MutableSharedFlow<String>()
    val statusNotification: SharedFlow<String> = _statusNotification.asSharedFlow()

    // Diagnostics Telemetry
    val turnDiagnostics: StateFlow<TurnDiagnostics> = DiagnosticsTracker.currentTurn

    // Database reactive flows
    val apiKeys: StateFlow<List<ApiKeyEntity>> = apiKeyDao.getAllApiKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeApiKey: StateFlow<ApiKeyEntity?> = apiKeyDao.getActiveApiKey()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val aiModels: StateFlow<List<AiModelEntity>> = aiModelDao.getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeModel: StateFlow<AiModelEntity?> = aiModelDao.getActiveModel()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val chatMessages: StateFlow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice & Pipeline Helpers
    var speechRecognizerHelper: SpeechRecognizerHelper? = null
        private set
    var ttsHelper: TextToSpeechHelper? = null
        private set

    lateinit var audioQueueManager: AudioQueueManager
        private set
    lateinit var textChunker: TextChunker
        private set
    lateinit var voiceActivityDetector: VoiceActivityDetector
        private set
    lateinit var continuousConversationManager: ContinuousConversationManager
        private set

    val audioRms: StateFlow<Float>
        get() = speechRecognizerHelper?.rmsLevel ?: MutableStateFlow(0f).asStateFlow()

    // Active AI generation job
    private var activeGenerationJob: Job? = null
    private var speechStartTimestamp = 0L

    // Testing connection state
    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()
    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult: StateFlow<String?> = _testConnectionResult.asStateFlow()

    init {
        ttsHelper = TextToSpeechHelper(application) { ready ->
            if (ready) {
                ttsHelper?.setLanguage(preferences.language.value)
            }
        }

        audioQueueManager = AudioQueueManager(ttsHelper!!, viewModelScope).apply {
            setOnQueueFinishedListener {
                if (_voiceState.value == VoiceAssistantState.SPEAKING) {
                    continuousConversationManager.onAiSpeakingFinished()
                    _voiceState.value = if (preferences.continuousConversation.value) {
                        VoiceAssistantState.LISTENING
                    } else {
                        VoiceAssistantState.IDLE
                    }
                }
            }
        }

        textChunker = TextChunker()

        voiceActivityDetector = VoiceActivityDetector(
            silenceTimeoutMs = preferences.vadSilenceTimeoutMs.value,
            onSpeechStarted = {
                // User started speaking!
                speechStartTimestamp = System.currentTimeMillis()
                if (preferences.bargeInEnabled.value &&
                    (_voiceState.value == VoiceAssistantState.SPEAKING || _voiceState.value == VoiceAssistantState.THINKING)
                ) {
                    bargeInInterrupt()
                }
                continuousConversationManager.onSpeechStarted()
            },
            onSpeechEnded = {
                // Automatic silence detected - user finished speech turn!
                val sttDuration = System.currentTimeMillis() - speechStartTimestamp
                DiagnosticsTracker.onSpeechEnded(sttDuration)
                stopVoiceListening()
            }
        )

        continuousConversationManager = ContinuousConversationManager(
            scope = viewModelScope,
            onStartListening = { startVoiceListeningInternal() },
            onStopListening = { stopVoiceListening() },
            onBargeInTriggered = { bargeInInterrupt() },
            onSendTranscriptionToAI = { finalSpokenText ->
                processUserVoiceInput(finalSpokenText)
            }
        ).apply {
            setContinuousMode(preferences.continuousConversation.value)
        }

        speechRecognizerHelper = SpeechRecognizerHelper(
            context = application,
            onResult = { recognizedText ->
                _liveUserTranscript.value = recognizedText
                continuousConversationManager.onFinalTranscription(recognizedText)
            },
            onErrorMsg = { error ->
                if (_voiceState.value != VoiceAssistantState.INTERRUPTED) {
                    _voiceState.value = VoiceAssistantState.ERROR
                    _errorMessage.value = error
                    continuousConversationManager.onError(error)
                }
            }
        )
    }

    /**
     * BARGE-IN: Immediately cancels audio playback, cancels AI stream, clears audio queue,
     * and sets state to listening while preserving context.
     */
    fun bargeInInterrupt() {
        Log.d("MainViewModel", "BARGE-IN executed: Halting audio & model generation.")
        _voiceState.value = VoiceAssistantState.INTERRUPTED
        DiagnosticsTracker.onInterrupted()

        // 1. Cancel running AI streaming job
        activeGenerationJob?.cancel()
        activeGenerationJob = null

        // 2. Halt on-device engine or HTTP stream
        viewModelScope.launch {
            aiEngineRouter.cancelCurrentGeneration()
        }

        // 3. Clear audio queue & halt TTS output
        audioQueueManager.interruptImmediately()

        // 4. Immediately prepare STT for the new user speech
        _voiceState.value = VoiceAssistantState.LISTENING
        startVoiceListeningInternal()
    }

    fun switchMode(mode: InteractionMode) {
        _interactionMode.value = mode
        if (mode == InteractionMode.CHAT) {
            stopVoiceListening()
            stopSpeaking()
            continuousConversationManager.pauseConversation()
        } else {
            if (preferences.continuousConversation.value) {
                continuousConversationManager.resumeConversation()
            }
        }
    }

    fun setContinuousConversation(enabled: Boolean) {
        preferences.setContinuousConversation(enabled)
        continuousConversationManager.setContinuousMode(enabled)
        if (!enabled && _voiceState.value == VoiceAssistantState.LISTENING) {
            stopVoiceListening()
        }
    }

    fun setAiExecutionMode(mode: AIExecutionMode) {
        preferences.setAiExecutionMode(mode)
        viewModelScope.launch {
            _statusNotification.emit("Modo de IA alterado para: ${mode.name}")
        }
    }

    fun setResponseSpeedMode(mode: ResponseSpeedMode) {
        preferences.setResponseSpeedMode(mode)
    }

    fun toggleVoiceTranscription(show: Boolean) {
        preferences.setShowVoiceTranscription(show)
    }

    fun setLanguage(lang: String) {
        preferences.setLanguage(lang)
        ttsHelper?.setLanguage(lang)
        viewModelScope.launch {
            _statusNotification.emit("Idioma alterado para: ${if (lang == "pt-BR") "Português (Brasil)" else "English"}")
        }
    }

    fun startVoiceListening() {
        stopSpeaking()
        _errorMessage.value = null
        _liveUserTranscript.value = ""
        _liveAiTranscript.value = ""
        _voiceState.value = VoiceAssistantState.LISTENING
        startVoiceListeningInternal()
    }

    private fun startVoiceListeningInternal() {
        speechStartTimestamp = System.currentTimeMillis()
        speechRecognizerHelper?.startListening(preferences.language.value)
    }

    fun stopVoiceListening() {
        speechRecognizerHelper?.stopListening()
        voiceActivityDetector.reset()
        if (_voiceState.value == VoiceAssistantState.LISTENING) {
            _voiceState.value = VoiceAssistantState.IDLE
        }
    }

    private fun processUserVoiceInput(spokenText: String) {
        val trimmed = spokenText.trim()
        if (trimmed.isEmpty()) {
            _voiceState.value = VoiceAssistantState.IDLE
            return
        }
        sendMessage(trimmed, isVoice = true)
    }

    fun sendMessage(userText: String, isVoice: Boolean = false) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val execMode = preferences.aiExecutionMode.value
        val speedMode = preferences.responseSpeedMode.value
        val activeKey = activeApiKey.value?.key ?: ""
        val activeRemoteMdl = activeModel.value?.modelId ?: "google/gemini-2.0-flash-exp:free"
        val activeLocMdl = activeLocalModel.value?.modelId ?: "qwen2.5-1.5b-instruct-q4"
        val hasDownloaded = activeLocalModel.value?.isDownloaded == true

        if (execMode == AIExecutionMode.REMOTE_ONLY && activeKey.isBlank()) {
            val error = "Nenhuma chave da OpenRouter ativa! Vá em Configurações para adicionar sua chave."
            _errorMessage.value = error
            _voiceState.value = VoiceAssistantState.ERROR
            viewModelScope.launch { _statusNotification.emit(error) }
            return
        }

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            _errorMessage.value = null
            _liveUserTranscript.value = trimmed
            _liveAiTranscript.value = ""
            _voiceState.value = VoiceAssistantState.THINKING
            continuousConversationManager.onAiGenerationStarted()

            val conversationContext = contextManager.buildContext(
                conversationId = 1L,
                systemPrompt = preferences.systemPrompt.value,
                currentUserPrompt = trimmed,
                isFastResponseMode = speedMode == ResponseSpeedMode.FAST_RESPONSE,
                language = preferences.language.value
            )

            val isLocalTarget = when (execMode) {
                AIExecutionMode.LOCAL_ONLY -> true
                AIExecutionMode.REMOTE_ONLY -> false
                AIExecutionMode.AUTOMATIC -> hasDownloaded
            }

            val options = GenerationOptions(
                modelId = if (isLocalTarget) activeLocMdl else activeRemoteMdl,
                maxTokens = if (speedMode == ResponseSpeedMode.FAST_RESPONSE) 220 else 512,
                quantization = activeLocalModel.value?.quantization ?: "Q4_K_M"
            )

            val fullResponseBuilder = StringBuilder()
            val startTime = System.currentTimeMillis()

            try {
                val tokenStream = aiEngineRouter.routeGeneration(
                    mode = execMode,
                    context = conversationContext,
                    options = options,
                    hasDownloadedLocalModel = hasDownloaded
                )

                if (isVoice || _interactionMode.value == InteractionMode.VOICE) {
                    _voiceState.value = VoiceAssistantState.SPEAKING
                    continuousConversationManager.onAiSpeakingStarted()

                    // Stream tokens into sentence chunker -> audio queue for incremental TTS
                    val chunkStream = textChunker.chunkStream(tokenStream)
                    chunkStream.collect { chunk ->
                        DiagnosticsTracker.onTtsChunkStarted()
                        fullResponseBuilder.append(chunk).append(" ")
                        _liveAiTranscript.value = fullResponseBuilder.toString().trim()

                        audioQueueManager.enqueue(
                            chunk = chunk,
                            languageCode = preferences.language.value,
                            rate = preferences.speechRate.value,
                            pitch = preferences.speechPitch.value
                        )
                    }
                } else {
                    // Chat Mode: Stream text directly to screen
                    tokenStream.collect { token ->
                        fullResponseBuilder.append(token)
                        _liveAiTranscript.value = fullResponseBuilder.toString()
                    }
                    _voiceState.value = VoiceAssistantState.IDLE
                }

                val finalAiResponse = fullResponseBuilder.toString().trim()
                val latency = System.currentTimeMillis() - startTime
                val durationSec = latency / 1000f
                val tokens = finalAiResponse.split("\\s+".toRegex()).size
                val tokPerSec = if (durationSec > 0) tokens / durationSec else 20f

                // Persist turn in Room database and extract memories
                contextManager.saveTurn(
                    conversationId = 1L,
                    userText = trimmed,
                    aiText = finalAiResponse,
                    modelUsed = options.modelId,
                    isVoice = isVoice,
                    executionType = if (isLocalTarget) "LOCAL" else "REMOTE",
                    latencyMs = latency,
                    tokensPerSec = tokPerSec
                )
            } catch (e: Exception) {
                if (_voiceState.value != VoiceAssistantState.INTERRUPTED) {
                    val err = e.localizedMessage ?: "Erro na geração da resposta"
                    _errorMessage.value = err
                    _liveAiTranscript.value = "Erro: $err"
                    _voiceState.value = VoiceAssistantState.ERROR
                    _statusNotification.emit(err)
                }
            }
        }
    }

    fun speakText(text: String, onDone: () -> Unit = {}) {
        audioQueueManager.enqueue(
            chunk = text,
            languageCode = preferences.language.value,
            rate = preferences.speechRate.value,
            pitch = preferences.speechPitch.value
        )
    }

    fun stopSpeaking() {
        audioQueueManager.interruptImmediately()
        if (_voiceState.value == VoiceAssistantState.SPEAKING) {
            _voiceState.value = VoiceAssistantState.IDLE
        }
    }

    // Benchmark execution
    fun runHardwareBenchmark(model: LocalModelEntity) {
        viewModelScope.launch {
            _isBenchmarking.value = true
            _statusNotification.emit("Iniciando benchmark de hardware para ${model.name}...")
            val result = hardwareDetector.runBenchmark(model.name, model.quantization)
            _benchmarkResult.value = result
            _isBenchmarking.value = false

            // Update model table with benchmark stats
            localModelDao.insertOrUpdate(
                model.copy(
                    lastBenchmarkTokensPerSec = result.tokensPerSec,
                    lastBenchmarkTtftMs = result.timeToFirstTokenMs
                )
            )
            _statusNotification.emit("Benchmark concluído: ${result.tokensPerSec} tokens/s, 1º token em ${result.timeToFirstTokenMs}ms")
        }
    }

    // Local model actions
    fun downloadLocalModel(model: LocalModelEntity) {
        viewModelScope.launch {
            _isDownloadingLocalModel.value = true
            _localModelDownloadProgress.value = 0f
            _statusNotification.emit("Baixando modelo local ${model.name}...")
            localModelManager.downloadModel(
                model = model,
                onProgress = { progress ->
                    _localModelDownloadProgress.value = progress
                },
                onError = { err ->
                    viewModelScope.launch {
                        _isDownloadingLocalModel.value = false
                        _statusNotification.emit("Erro no download: $err")
                    }
                }
            )
            localModelManager.activateModel(model.modelId)
            localAiEngine.preloadModelIfNeeded(model.modelId)
            _isDownloadingLocalModel.value = false
            _localModelDownloadProgress.value = 1f
            _statusNotification.emit("Modelo ${model.name} pronto e ativado para execução local!")

            if (_liveAiTranscript.value.contains("Nenhum modelo local Qwen está baixado")) {
                val readyMsg = "Modelo ${model.name} instalado e ativado com sucesso! Você já pode falar normalmente comigo."
                _liveAiTranscript.value = readyMsg
                speakText("Modelo ${model.name} instalado. Pode falar agora!")
            }
        }
    }

    fun downloadRecommendedLocalModel() {
        val target = localModels.value.find { it.isRecommended }
            ?: localModels.value.firstOrNull()
        if (target != null) {
            downloadLocalModel(target)
        }
    }

    fun deleteLocalModel(model: LocalModelEntity) {
        viewModelScope.launch {
            localModelManager.deleteModel(model)
            _statusNotification.emit("Modelo ${model.name} removido do dispositivo.")
        }
    }

    fun setActiveLocalModel(modelId: String) {
        viewModelScope.launch {
            localModelManager.activateModel(modelId)
            localAiEngine.preloadModelIfNeeded(modelId)
            _statusNotification.emit("Modelo local ativo: $modelId")
        }
    }

    // OpenRouter Key Management (Preserved)
    fun addApiKey(key: String, label: String) {
        val cleanKey = key.trim()
        val cleanLabel = label.trim().ifEmpty { "Chave ${apiKeys.value.size + 1}" }
        if (cleanKey.isEmpty()) return

        viewModelScope.launch {
            val isFirst = apiKeys.value.isEmpty()
            val newId = apiKeyDao.insertApiKey(
                ApiKeyEntity(key = cleanKey, label = cleanLabel, isActive = isFirst)
            )
            if (isFirst) {
                apiKeyDao.setActiveKey(newId)
            }
            _statusNotification.emit("Chave de API '$cleanLabel' salva!")
        }
    }

    fun deleteApiKey(keyEntity: ApiKeyEntity) {
        viewModelScope.launch {
            val wasActive = keyEntity.isActive
            apiKeyDao.deleteApiKey(keyEntity)
            if (wasActive) {
                val remaining = apiKeys.value.filter { it.id != keyEntity.id }
                if (remaining.isNotEmpty()) {
                    apiKeyDao.setActiveKey(remaining.first().id)
                }
            }
            _statusNotification.emit("Chave '${keyEntity.label}' removida.")
        }
    }

    fun setActiveApiKey(id: Long) {
        viewModelScope.launch {
            apiKeyDao.setActiveKey(id)
            _statusNotification.emit("Chave de API ativa alterada.")
        }
    }

    // Remote Model Management (Preserved)
    fun addModel(modelId: String, name: String, provider: String, isFree: Boolean) {
        val cleanId = modelId.trim()
        val cleanName = name.trim().ifEmpty { cleanId }
        val cleanProvider = provider.trim().ifEmpty { "Custom" }
        if (cleanId.isEmpty()) return

        viewModelScope.launch {
            aiModelDao.insertModel(
                AiModelEntity(
                    modelId = cleanId,
                    name = cleanName,
                    provider = cleanProvider,
                    isFree = isFree,
                    isActive = false
                )
            )
            _statusNotification.emit("Modelo '$cleanName' adicionado!")
        }
    }

    fun deleteModel(modelEntity: AiModelEntity) {
        viewModelScope.launch {
            val wasActive = modelEntity.isActive
            aiModelDao.deleteModel(modelEntity)
            if (wasActive) {
                val remaining = aiModels.value.filter { it.id != modelEntity.id }
                if (remaining.isNotEmpty()) {
                    aiModelDao.setActiveModel(remaining.first().id)
                }
            }
            _statusNotification.emit("Modelo '${modelEntity.name}' removido.")
        }
    }

    fun setActiveModel(id: Long) {
        viewModelScope.launch {
            aiModelDao.setActiveModel(id)
            val selected = aiModels.value.find { it.id == id }
            _statusNotification.emit("Modelo ativo: ${selected?.name ?: ""}")
        }
    }

    fun testConnection(apiKey: String, model: String) {
        if (apiKey.isBlank()) {
            _testConnectionResult.value = "Chave de API vazia. Insira uma chave primeiro."
            return
        }

        viewModelScope.launch {
            _isTestingConnection.value = true
            _testConnectionResult.value = "Testando conexão com OpenRouter..."

            val result = openRouterClient.testConnection(apiKey = apiKey, model = model)
            _isTestingConnection.value = false

            _testConnectionResult.value = when (result) {
                is com.example.data.api.OpenRouterResult.Success -> "Sucesso! Conexão verificada em ${result.latencyMs}ms. O modelo está respondendo corretamente."
                is com.example.data.api.OpenRouterResult.Error -> "Falha: ${result.message}"
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatMessageDao.clearAllMessages()
            _liveUserTranscript.value = ""
            _liveAiTranscript.value = ""
            _statusNotification.emit("Histórico de conversa limpo.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerHelper?.destroy()
        ttsHelper?.shutdown()
        audioQueueManager.interruptImmediately()
        voiceActivityDetector.reset()
    }
}

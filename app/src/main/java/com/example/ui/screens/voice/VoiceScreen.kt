package com.example.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.AIExecutionMode
import com.example.ui.MainViewModel
import com.example.ui.VoiceAssistantState
import com.example.ui.components.AudioWavesVisualizer
import com.example.ui.components.CyberCard
import com.example.ui.components.GlowingStatusPill
import com.example.ui.theme.AiBubbleBorder
import com.example.ui.theme.AiBubbleColor
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberObsidian
import com.example.ui.theme.NeonAqua
import com.example.ui.theme.NeonCobalt
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonElectricBlue
import com.example.ui.theme.TechError
import com.example.ui.theme.TechSuccess
import com.example.ui.theme.TechTextMuted
import com.example.ui.theme.TechTextSecondary
import com.example.ui.theme.TechWhite
import com.example.ui.theme.UserBubbleBorder
import com.example.ui.theme.UserBubbleColor

@Composable
fun VoiceScreen(
    viewModel: MainViewModel,
    onSwitchToChat: () -> Unit,
    onOpenSettingsWithTab: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val voiceState by viewModel.voiceState.collectAsState()
    val liveUserText by viewModel.liveUserTranscript.collectAsState()
    val liveAiText by viewModel.liveAiTranscript.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val activeLocalModel by viewModel.activeLocalModel.collectAsState()
    val isDownloadingLocalModel by viewModel.isDownloadingLocalModel.collectAsState()
    val downloadProgress by viewModel.localModelDownloadProgress.collectAsState()
    val activeKey by viewModel.activeApiKey.collectAsState()
    val showTranscription by viewModel.preferences.showVoiceTranscription.collectAsState()
    val language by viewModel.preferences.language.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val continuousConversation by viewModel.preferences.continuousConversation.collectAsState()
    val aiExecutionMode by viewModel.preferences.aiExecutionMode.collectAsState()
    val diagnostics by viewModel.turnDiagnostics.collectAsState()

    var showDiagnosticsHud by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionDeniedMessage = null
            viewModel.startVoiceListening()
        } else {
            permissionDeniedMessage = "Permissão de microfone necessária para a conversa por voz contínua."
        }
    }

    fun handleMicAction() {
        if (voiceState == VoiceAssistantState.LISTENING) {
            viewModel.stopVoiceListening()
        } else if (voiceState == VoiceAssistantState.SPEAKING || voiceState == VoiceAssistantState.THINKING) {
            // Immediate barge-in interrupt
            viewModel.bargeInInterrupt()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                viewModel.startVoiceListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        CyberObsidian,
                        CyberDarkSurface,
                        CyberObsidian
                    )
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Voice HUD Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language selector button (pt-BR default)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CyberDarkSurface,
                border = BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier
                    .clickable {
                        val nextLang = if (language == "pt-BR") "en-US" else "pt-BR"
                        viewModel.setLanguage(nextLang)
                    }
                    .testTag("voice_language_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Idioma",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == "pt-BR") "PT-BR" else "EN-US",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TechWhite,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // AI Mode Pill (Local vs Online vs Auto)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when (aiExecutionMode) {
                    AIExecutionMode.LOCAL_ONLY -> NeonAqua.copy(alpha = 0.15f)
                    AIExecutionMode.AUTOMATIC -> NeonCyan.copy(alpha = 0.15f)
                    AIExecutionMode.REMOTE_ONLY -> NeonCobalt.copy(alpha = 0.15f)
                },
                border = BorderStroke(
                    1.dp,
                    when (aiExecutionMode) {
                        AIExecutionMode.LOCAL_ONLY -> NeonAqua
                        AIExecutionMode.AUTOMATIC -> NeonCyan
                        AIExecutionMode.REMOTE_ONLY -> NeonCobalt
                    }
                ),
                modifier = Modifier.clickable {
                    val nextMode = when (aiExecutionMode) {
                        AIExecutionMode.AUTOMATIC -> AIExecutionMode.LOCAL_ONLY
                        AIExecutionMode.LOCAL_ONLY -> AIExecutionMode.REMOTE_ONLY
                        AIExecutionMode.REMOTE_ONLY -> AIExecutionMode.AUTOMATIC
                    }
                    viewModel.setAiExecutionMode(nextMode)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (aiExecutionMode == AIExecutionMode.LOCAL_ONLY) Icons.Default.Memory else Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (aiExecutionMode == AIExecutionMode.LOCAL_ONLY) NeonAqua else NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (aiExecutionMode) {
                            AIExecutionMode.LOCAL_ONLY -> "IA LOCAL (QWEN)"
                            AIExecutionMode.AUTOMATIC -> "IA AUTO (QWEN/API)"
                            AIExecutionMode.REMOTE_ONLY -> "IA ONLINE"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TechWhite
                    )
                }
            }

            // Continuous Conversation Toggle
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (continuousConversation) NeonAqua.copy(alpha = 0.2f) else CyberDarkSurface,
                border = BorderStroke(1.dp, if (continuousConversation) NeonAqua else CyberCardBorder),
                modifier = Modifier
                    .clickable { viewModel.setContinuousConversation(!continuousConversation) }
                    .testTag("voice_continuous_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = "Conversa Contínua",
                        tint = if (continuousConversation) NeonAqua else TechTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (continuousConversation) "Contínua: ON" else "Contínua: OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (continuousConversation) TechWhite else TechTextMuted
                    )
                }
            }

            // Diagnostics HUD Toggle
            IconButton(
                onClick = { showDiagnosticsHud = !showDiagnosticsHud },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Diagnóstico",
                    tint = if (showDiagnosticsHud) NeonCyan else TechTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Diagnostics HUD Dropdown Card
        AnimatedVisibility(visible = showDiagnosticsHud) {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                borderColor = NeonCyan.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1º TOKEN", fontSize = 9.sp, color = TechTextMuted)
                        Text(
                            text = if (diagnostics.llmFirstTokenMs > 0) "${diagnostics.llmFirstTokenMs}ms" else "--",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VELOCIDADE", fontSize = 9.sp, color = TechTextMuted)
                        Text(
                            text = if (diagnostics.llmTokensPerSec > 0) "${String.format("%.1f", diagnostics.llmTokensPerSec)} tok/s" else "--",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAqua,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1ª FALA (TTS)", fontSize = 9.sp, color = TechTextMuted)
                        Text(
                            text = if (diagnostics.totalTimeToSpeechMs > 0) "${diagnostics.totalTimeToSpeechMs}ms" else "--",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TechSuccess,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MOTOR", fontSize = 9.sp, color = TechTextMuted)
                        Text(
                            text = if (diagnostics.isLocal) "Qwen Local" else "OpenRouter",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TechWhite
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Conversational Status Banner
        val stateText = when (voiceState) {
            VoiceAssistantState.IDLE -> if (language == "pt-BR") "🎤 AGUARDANDO SUA VOZ" else "🎤 WAITING FOR SPEECH"
            VoiceAssistantState.LISTENING -> if (language == "pt-BR") "🎤 OUVINDO SUA FALA..." else "🎤 LISTENING..."
            VoiceAssistantState.TRANSCRIBING -> if (language == "pt-BR") "📝 TRANSCREVENDO EM TEMPO REAL..." else "📝 TRANSCRIBING..."
            VoiceAssistantState.THINKING -> if (language == "pt-BR") "🧠 PROCESSANDO RESPOSTA..." else "🧠 THINKING..."
            VoiceAssistantState.SPEAKING -> if (language == "pt-BR") "🔊 FALANDO (FALE PARA INTERROMPER)" else "🔊 SPEAKING (SPEAK TO INTERRUPT)"
            VoiceAssistantState.INTERRUPTED -> if (language == "pt-BR") "⚡ INTERROMPIDO! OUVINDO VOCÊ..." else "⚡ INTERRUPTED! LISTENING..."
            VoiceAssistantState.ERROR -> if (language == "pt-BR") "⚠️ FALHA / TOQUE PARA RETENTAR" else "⚠️ ERROR / TAP TO RETRY"
        }

        val stateColor = when (voiceState) {
            VoiceAssistantState.IDLE -> TechTextSecondary
            VoiceAssistantState.LISTENING, VoiceAssistantState.TRANSCRIBING -> NeonCyan
            VoiceAssistantState.THINKING -> NeonCobalt
            VoiceAssistantState.SPEAKING -> NeonAqua
            VoiceAssistantState.INTERRUPTED -> Color(0xFFFFD54F)
            VoiceAssistantState.ERROR -> TechError
        }

        Text(
            text = stateText,
            color = stateColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Dynamic Soundwaves Visualizer
        AudioWavesVisualizer(
            isActive = voiceState == VoiceAssistantState.LISTENING ||
                    voiceState == VoiceAssistantState.TRANSCRIBING ||
                    voiceState == VoiceAssistantState.SPEAKING,
            amplitude = if (voiceState == VoiceAssistantState.SPEAKING) 0.75f else (audioRms * 1.6f).coerceIn(0.2f, 1f),
            barCount = 24,
            barColor = if (voiceState == VoiceAssistantState.SPEAKING) NeonAqua else NeonCyan
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Large Visible Transcription Canvas (Occupies large majority of the screen)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (showTranscription) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // USER SPEECH (Large prominent transcription)
                    AnimatedVisibility(
                        visible = liveUserText.isNotBlank() || voiceState == VoiceAssistantState.LISTENING || voiceState == VoiceAssistantState.TRANSCRIBING,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = UserBubbleColor.copy(alpha = 0.92f),
                            border = BorderStroke(
                                1.5.dp,
                                if (voiceState == VoiceAssistantState.LISTENING || voiceState == VoiceAssistantState.TRANSCRIBING)
                                    NeonCyan
                                else
                                    UserBubbleBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (voiceState == VoiceAssistantState.LISTENING || voiceState == VoiceAssistantState.TRANSCRIBING)
                                            "🎤 VOCÊ ESTÁ FALANDO:"
                                        else
                                            "VOCÊ DISSE:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonCyan,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (liveUserText.isNotBlank()) liveUserText else "Ouvindo sua fala...",
                                    color = TechWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }

                    // AI RESPONSE (Large prominent streaming typography)
                    AnimatedVisibility(
                        visible = liveAiText.isNotBlank() || voiceState == VoiceAssistantState.THINKING,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AiBubbleColor.copy(alpha = 0.95f),
                            border = BorderStroke(
                                1.5.dp,
                                if (voiceState == VoiceAssistantState.SPEAKING) NeonAqua else AiBubbleBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = NeonAqua,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "🤖 NEXUS AI:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeonAqua,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (voiceState == VoiceAssistantState.SPEAKING) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = NeonAqua.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, NeonAqua)
                                        ) {
                                            Text(
                                                text = "FALANDO...",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonAqua,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                if (liveAiText.isBlank() && voiceState == VoiceAssistantState.THINKING) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = NeonCyan,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Pensando na resposta...",
                                            color = TechTextSecondary,
                                            fontSize = 15.sp
                                        )
                                    }
                                } else {
                                    Text(
                                        text = liveAiText,
                                        color = TechWhite,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 26.sp
                                    )

                                    val isMissingLocalModel = liveAiText.contains("Nenhum modelo local Qwen está baixado") ||
                                            (aiExecutionMode == AIExecutionMode.LOCAL_ONLY && activeLocalModel?.isDownloaded != true)

                                    if (isMissingLocalModel) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = CyberObsidian.copy(alpha = 0.85f),
                                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Bolt,
                                                        contentDescription = null,
                                                        tint = NeonCyan,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "RESOLUÇÃO RÁPIDA (POCO X5 5G)",
                                                        color = NeonCyan,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                if (isDownloadingLocalModel) {
                                                    Column(modifier = Modifier.fillMaxWidth()) {
                                                        Text(
                                                            text = "Instalando Qwen 2.5 no celular... ${(downloadProgress * 100).toInt()}%",
                                                            color = TechWhite,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        LinearProgressIndicator(
                                                            progress = { downloadProgress },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(6.dp)
                                                                .clip(RoundedCornerShape(3.dp)),
                                                            color = NeonCyan,
                                                            trackColor = CyberDarkSurface
                                                        )
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = { viewModel.downloadRecommendedLocalModel() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Download,
                                                            contentDescription = null,
                                                            tint = CyberObsidian,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "1-Toque: Instalar Qwen 2.5 (Recomendado)",
                                                            color = CyberObsidian,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        OutlinedButton(
                                                            onClick = { onOpenSettingsWithTab(0) },
                                                            border = BorderStroke(1.dp, NeonCobalt),
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Settings,
                                                                contentDescription = null,
                                                                tint = NeonAqua,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Ver Modelos",
                                                                color = TechWhite,
                                                                fontSize = 11.sp,
                                                                maxLines = 1
                                                            )
                                                        }

                                                        OutlinedButton(
                                                            onClick = { viewModel.setAiExecutionMode(AIExecutionMode.AUTOMATIC) },
                                                            border = BorderStroke(1.dp, NeonCobalt),
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.CloudSync,
                                                                contentDescription = null,
                                                                tint = NeonAqua,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Modo Auto/Online",
                                                                color = TechWhite,
                                                                fontSize = 11.sp,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Empty state greeting
                    if (liveUserText.isBlank() && liveAiText.isBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (continuousConversation)
                                        "Conversa contínua ativada.\nBasta falar naturalmente que o Nexus detecta e responde."
                                    else
                                        "Toque no microfone abaixo para iniciar a conversa por voz.",
                                    color = TechTextSecondary,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "💡 Dica de Interrupção (Barge-in): Quando a IA estiver falando, basta começar a falar novamente para interrompê-la instantaneamente.",
                                    color = TechTextMuted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Audio-only mode banner
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Modo Somente Áudio (Transcrição na tela oculta)\nFale normalmente.",
                        color = TechTextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Continuous Action & Voice Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Switch to Chat Button
            Surface(
                shape = CircleShape,
                color = CyberDarkSurface,
                border = BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier
                    .size(52.dp)
                    .clickable { onSwitchToChat() }
                    .testTag("voice_to_chat_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Modo Texto",
                        tint = TechWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Central Voice / Barge-in Mic Button
            val isListening = voiceState == VoiceAssistantState.LISTENING || voiceState == VoiceAssistantState.TRANSCRIBING
            val isSpeaking = voiceState == VoiceAssistantState.SPEAKING
            val isThinking = voiceState == VoiceAssistantState.THINKING

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isListening -> Brush.radialGradient(listOf(NeonCyan, NeonCobalt))
                            isSpeaking -> Brush.radialGradient(listOf(NeonAqua, NeonElectricBlue))
                            isThinking -> Brush.radialGradient(listOf(NeonCobalt, CyberDarkSurface))
                            else -> Brush.radialGradient(listOf(NeonElectricBlue, CyberDarkSurface))
                        }
                    )
                    .border(
                        2.dp,
                        when {
                            isListening -> NeonCyan
                            isSpeaking -> NeonAqua
                            else -> NeonCobalt
                        },
                        CircleShape
                    )
                    .clickable { handleMicAction() }
                    .testTag("voice_main_action_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isThinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = CyberObsidian,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = when {
                            isSpeaking -> Icons.Default.Stop // Tap to interrupt
                            isListening -> Icons.Default.MicOff
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Ação de voz",
                        tint = if (isListening || isSpeaking) CyberObsidian else TechWhite,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Immediate Barge-in / Interrupt Action Button
            Surface(
                shape = CircleShape,
                color = if (isSpeaking || isThinking) TechError.copy(alpha = 0.2f) else CyberDarkSurface.copy(alpha = 0.4f),
                border = BorderStroke(
                    1.dp,
                    if (isSpeaking || isThinking) TechError else CyberCardBorder
                ),
                modifier = Modifier
                    .size(52.dp)
                    .clickable(enabled = isSpeaking || isThinking || isListening) {
                        viewModel.bargeInInterrupt()
                    }
                    .testTag("voice_barge_in_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Interromper fala imediatamente",
                        tint = if (isSpeaking || isThinking) TechError else TechTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

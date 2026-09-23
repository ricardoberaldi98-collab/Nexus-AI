package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.ui.MainViewModel
import com.example.ui.VoiceAssistantState
import com.example.ui.components.AudioWavesVisualizer
import com.example.ui.components.CyberCard
import com.example.ui.theme.AiBubbleBorder
import com.example.ui.theme.AiBubbleColor
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberObsidian
import com.example.ui.theme.NeonAqua
import com.example.ui.theme.NeonCobalt
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonElectricBlue
import com.example.ui.theme.TechTextMuted
import com.example.ui.theme.TechTextSecondary
import com.example.ui.theme.TechWhite
import com.example.ui.theme.UserBubbleBorder
import com.example.ui.theme.UserBubbleColor

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onOpenVoiceMode: () -> Unit,
    onOpenSettingsWithTab: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val activeKey by viewModel.activeApiKey.collectAsState()
    val isTtsSpeaking by (viewModel.ttsHelper?.isSpeaking ?: mutableStateOf(false)).let {
        viewModel.ttsHelper?.isSpeaking?.collectAsState() ?: remember { mutableStateOf(false) }
    }

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberObsidian)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Chat message list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                EmptyChatGreeting(
                    activeModelName = activeModel?.name ?: "Nenhum modelo",
                    hasKey = activeKey != null,
                    onSuggestionClick = { suggestion ->
                        textInput = suggestion
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            isSpeaking = isTtsSpeaking,
                            onSpeak = { viewModel.speakText(msg.content) },
                            onStopSpeak = { viewModel.stopSpeaking() },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Mensagem IA", msg.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Texto copiado!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (voiceState == VoiceAssistantState.THINKING) {
                        item {
                            ThinkingIndicator(modelName = activeModel?.name ?: "IA")
                        }
                    }
                }
            }
        }

        // Quick Suggestions bar if not empty
        if (messages.isNotEmpty() && textInput.isEmpty()) {
            QuickChipsRow(
                onChipClick = { textInput = it }
            )
        }

        // Bottom Input Row
        InputBar(
            text = textInput,
            onTextChanged = { textInput = it },
            onSend = {
                if (textInput.isNotBlank()) {
                    viewModel.sendMessage(textInput, isVoice = false)
                    textInput = ""
                }
            },
            onMicClick = onOpenVoiceMode,
            isSending = voiceState == VoiceAssistantState.THINKING
        )
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onStopSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 330.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Header info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                if (isUser) {
                    if (message.isVoice) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Por voz",
                            tint = NeonAqua,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "Você",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TechTextSecondary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "IA",
                        tint = NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = message.modelUsed.ifEmpty { "Nexus AI" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    if (message.executionType.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (message.executionType == "LOCAL") NeonAqua.copy(alpha = 0.2f) else NeonCobalt.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (message.executionType == "LOCAL") NeonAqua else NeonCobalt
                            )
                        ) {
                            Text(
                                text = if (message.executionType == "LOCAL") "LOCAL (QWEN)" else "ONLINE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (message.executionType == "LOCAL") NeonAqua else NeonCyan,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (message.latencyMs > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${message.latencyMs}ms",
                            fontSize = 10.sp,
                            color = TechTextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Message Bubble
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) UserBubbleColor else AiBubbleColor,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) UserBubbleBorder else AiBubbleBorder.copy(alpha = 0.4f)
                ),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = message.content,
                        color = TechWhite,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )

                    // Assistant Action Buttons
                    if (!isUser) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // TTS Speak / Stop Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberDarkSurface,
                                modifier = Modifier
                                    .clickable {
                                        if (isSpeaking) onStopSpeak() else onSpeak()
                                    }
                                    .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                        contentDescription = "Ouvir resposta",
                                        tint = if (isSpeaking) NeonAqua else TechTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSpeaking) "Parar" else "Ouvir",
                                        fontSize = 11.sp,
                                        color = if (isSpeaking) NeonAqua else TechTextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Copy Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberDarkSurface,
                                modifier = Modifier
                                    .clickable { onCopy() }
                                    .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar",
                                        tint = TechTextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Copiar",
                                        fontSize = 11.sp,
                                        color = TechTextSecondary,
                                        fontWeight = FontWeight.Medium
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

@Composable
fun ThinkingIndicator(modelName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        CyberCard(
            modifier = Modifier.widthIn(max = 280.dp),
            borderColor = NeonCyan.copy(alpha = 0.3f),
            glow = true
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Nexus processando...",
                        color = TechWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = modelName,
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun QuickChipsRow(onChipClick: (String) -> Unit) {
    val suggestions = listOf(
        "Explique computação quântica",
        "Dicas de desempenho Poco X5 5G",
        "Conte uma curiosidade tecnológica",
        "How does speech recognition work?",
        "Resuma a teoria da relatividade"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { item ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CyberCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.clickable { onChipClick(item) }
            ) {
                Text(
                    text = item,
                    color = TechTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun InputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    isSending: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CyberDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Mode / Mic quick launch
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonCyan, NeonAqua)))
                    .clickable { onMicClick() }
                    .testTag("chat_voice_mode_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Abrir Modo Voz",
                    tint = CyberObsidian,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Text input field
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = "Digite sua mensagem...",
                        color = TechTextMuted,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberCardBorder,
                    focusedTextColor = TechWhite,
                    unfocusedTextColor = TechWhite,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Send Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank() && !isSending)
                            Brush.linearGradient(listOf(NeonElectricBlue, NeonCobalt))
                        else
                            Brush.linearGradient(listOf(CyberCardSurface, CyberDarkSurface))
                    )
                    .clickable(enabled = text.isNotBlank() && !isSending) { onSend() }
                    .testTag("chat_send_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = NeonCyan
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = if (text.isNotBlank()) TechWhite else TechTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatGreeting(
    activeModelName: String,
    hasKey: Boolean,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(NeonCyan.copy(alpha = 0.3f), Color.Transparent)))
                .border(2.dp, NeonCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nexus AI Assistant",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TechWhite
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Voz & Texto com OpenRouter AI",
            fontSize = 13.sp,
            color = NeonAqua,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (!hasKey) {
            CyberCard(
                borderColor = Color(0xFFFFB300),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Chave da OpenRouter necessária",
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Abra as Configurações (ícone de engrenagem) e insira sua chave gratuita da OpenRouter para começar a conversar.",
                        color = TechTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        } else {
            Text(
                text = "Modelo ativo: $activeModelName",
                fontSize = 12.sp,
                color = TechTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sugestões para começar:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TechTextMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        val promptSuggestions = listOf(
            "O que você pode fazer por mim?",
            "Explique as novidades da IA em 2026",
            "Dicas de otimização para Poco X5 5G",
            "Tell me an interesting science fact"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            promptSuggestions.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyberCardSurface.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(prompt) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeonCobalt,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = prompt,
                            fontSize = 13.sp,
                            color = TechWhite
                        )
                    }
                }
            }
        }
    }
}

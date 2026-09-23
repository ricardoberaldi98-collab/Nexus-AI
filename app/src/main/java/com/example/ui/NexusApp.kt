package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AIExecutionMode
import com.example.ui.components.InteractionMode
import com.example.ui.components.ModeTogglePill
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.settings.SettingsDialog
import com.example.ui.screens.voice.VoiceScreen
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberObsidian
import com.example.ui.theme.NeonAqua
import com.example.ui.theme.NeonCobalt
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TechError
import com.example.ui.theme.TechSuccess
import com.example.ui.theme.TechTextSecondary
import com.example.ui.theme.TechWhite
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NexusApp(
    viewModel: MainViewModel = viewModel()
) {
    val interactionMode by viewModel.interactionMode.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val activeLocalModel by viewModel.activeLocalModel.collectAsState()
    val aiExecutionMode by viewModel.preferences.aiExecutionMode.collectAsState()
    val activeKey by viewModel.activeApiKey.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var settingsInitialTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val (modelLabel, isConfigured) = when (aiExecutionMode) {
        AIExecutionMode.LOCAL_ONLY -> {
            if (activeLocalModel?.isDownloaded == true) {
                Pair(activeLocalModel!!.name, true)
            } else {
                Pair("Sem Qwen local", false)
            }
        }
        AIExecutionMode.REMOTE_ONLY -> {
            if (activeKey != null) {
                Pair(activeModel?.name ?: "Online", true)
            } else {
                Pair("Sem chave API", false)
            }
        }
        AIExecutionMode.AUTOMATIC -> {
            if (activeLocalModel?.isDownloaded == true) {
                Pair("${activeLocalModel!!.name} (Auto)", true)
            } else if (activeKey != null) {
                Pair("${activeModel?.name ?: "Online"} (Auto)", true)
            } else {
                Pair("Sem modelo/chave", false)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.statusNotification.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberObsidian,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Futuristic Top Bar
            NexusTopAppBar(
                currentMode = interactionMode,
                onModeChange = { viewModel.switchMode(it) },
                activeModelName = modelLabel,
                hasActiveKey = isConfigured,
                onOpenSettings = {
                    settingsInitialTab = if (aiExecutionMode == AIExecutionMode.LOCAL_ONLY) 0 else 1
                    showSettingsDialog = true
                }
            )

            // Dynamic Content: Chat or Voice Mode
            Crossfade(
                targetState = interactionMode,
                animationSpec = tween(300),
                modifier = Modifier.weight(1f),
                label = "mode_crossfade"
            ) { mode ->
                when (mode) {
                    InteractionMode.CHAT -> {
                        ChatScreen(
                            viewModel = viewModel,
                            onOpenVoiceMode = { viewModel.switchMode(InteractionMode.VOICE) },
                            onOpenSettingsWithTab = { tab ->
                                settingsInitialTab = tab
                                showSettingsDialog = true
                            }
                        )
                    }
                    InteractionMode.VOICE -> {
                        VoiceScreen(
                            viewModel = viewModel,
                            onSwitchToChat = { viewModel.switchMode(InteractionMode.CHAT) },
                            onOpenSettingsWithTab = { tab ->
                                settingsInitialTab = tab
                                showSettingsDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            initialTab = settingsInitialTab,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun NexusTopAppBar(
    currentMode: InteractionMode,
    onModeChange: (InteractionMode) -> Unit,
    activeModelName: String,
    hasActiveKey: Boolean,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CyberDarkSurface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            CyberCardBorder.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand & Model status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenSettings() }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .border(1.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Nexus AI",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NEXUS",
                            color = TechWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (hasActiveKey) TechSuccess else TechError)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activeModelName.take(16),
                            color = TechTextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Mode Toggle Switch (Chat vs Voice)
            ModeTogglePill(
                currentMode = currentMode,
                onModeChange = onModeChange
            )

            // Settings Action Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyberObsidian)
                    .border(1.dp, CyberCardBorder, CircleShape)
                    .testTag("app_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configurações",
                    tint = if (hasActiveKey) NeonCyan else Color(0xFFFFB300),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

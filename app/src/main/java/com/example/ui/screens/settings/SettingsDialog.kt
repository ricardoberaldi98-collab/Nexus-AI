package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AIExecutionMode
import com.example.data.local.ResponseSpeedMode
import com.example.data.model.AiModelEntity
import com.example.data.model.ApiKeyEntity
import com.example.data.model.LocalModelEntity
import com.example.hardware.BenchmarkResult
import com.example.ui.MainViewModel
import com.example.ui.components.CyberCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardSurface
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

@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("IA Local (Qwen)", "Chaves API", "Modelos Online", "Voz & Pipeline", "Hardware & Geral")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            color = CyberObsidian
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberDarkSurface)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Configurações Nexus AI",
                            color = TechWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = TechTextSecondary
                        )
                    }
                }

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CyberDarkSurface,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) NeonCyan else TechTextSecondary
                                )
                            }
                        )
                    }
                }

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> LocalQwenTab(viewModel)
                        1 -> ApiKeysTab(viewModel)
                        2 -> ModelsTab(viewModel)
                        3 -> VoiceSettingsTab(viewModel)
                        4 -> DeviceInfoTab(viewModel)
                    }
                }
            }
        }
    }
}

// TAB 0: LOCAL QWEN ENGINE & HARDWARE AUDIT
@Composable
fun LocalQwenTab(viewModel: MainViewModel) {
    val deviceReport by viewModel.deviceReport.collectAsState()
    val localModels by viewModel.localModels.collectAsState()
    val activeLocalModel by viewModel.activeLocalModel.collectAsState()
    val aiExecutionMode by viewModel.preferences.aiExecutionMode.collectAsState()
    val speedMode by viewModel.preferences.responseSpeedMode.collectAsState()
    val continuousConv by viewModel.preferences.continuousConversation.collectAsState()
    val bargeIn by viewModel.preferences.bargeInEnabled.collectAsState()
    val vadTimeout by viewModel.preferences.vadSilenceTimeoutMs.collectAsState()
    val isBenchmarking by viewModel.isBenchmarking.collectAsState()
    val benchmarkResult by viewModel.benchmarkResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // AI Execution Mode Card
        CyberCard(modifier = Modifier.fillMaxWidth(), borderColor = NeonCyan) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Modo de Execução da IA",
                    color = TechWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                listOf(
                    Triple(AIExecutionMode.LOCAL_ONLY, "IA LOCAL (100% no celular)", "Inferência local offline, privacidade total, latência sem rede."),
                    Triple(AIExecutionMode.AUTOMATIC, "AUTOMÁTICO (Local prioritário + Fallback)", "Tenta modelo local Qwen; se não instalado, usa OpenRouter API."),
                    Triple(AIExecutionMode.REMOTE_ONLY, "IA ONLINE (OpenRouter)", "Utiliza APIs em nuvem via sua chave OpenRouter.")
                ).forEach { (mode, title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setAiExecutionMode(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = aiExecutionMode == mode,
                            onClick = { viewModel.setAiExecutionMode(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                        )
                        Column(modifier = Modifier.padding(start = 6.dp)) {
                            Text(text = title, color = TechWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = desc, color = TechTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Hardware Recommendation Banner for Poco X5 5G
        CyberCard(modifier = Modifier.fillMaxWidth(), borderColor = NeonAqua) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = NeonAqua, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Detecção de Hardware: ${deviceReport.deviceModel}", color = TechWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• RAM Total: ${deviceReport.totalRamMb} MB (${deviceReport.availableRamMb} MB livres)\n" +
                            "• Processador: ${deviceReport.cpuCores} núcleos (${deviceReport.cpuArch})\n" +
                            "• Aceleração Vulkan: ${if (deviceReport.hasVulkan) "Disponível (GPU Ativa)" else "Não detectada"}\n" +
                            "• Armazenamento Livre: ${deviceReport.availableStorageMb} MB",
                    color = TechTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonAqua.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeonAqua.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "💡 ${deviceReport.recommendationSummary}",
                        color = TechWhite,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        // Speed & Voice Conversational Options
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "Otimizações de Conversação & Voz", color = TechWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                // Fast vs Reasoning mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Resposta Rápida para Voz (⚡)", color = TechWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Respostas concisas, menor latência de áudio", color = TechTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = speedMode == ResponseSpeedMode.FAST_RESPONSE,
                        onCheckedChange = { isFast ->
                            viewModel.setResponseSpeedMode(if (isFast) ResponseSpeedMode.FAST_RESPONSE else ResponseSpeedMode.ADVANCED_REASONING)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCobalt)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Continuous Conversation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Conversa Contínua (Hands-free)", color = TechWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Ouve continuamente sem precisar tocar no botão a cada turno", color = TechTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = continuousConv,
                        onCheckedChange = { viewModel.setContinuousConversation(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonAqua, checkedTrackColor = NeonCobalt)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Barge-in (Interrupção)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Interrupção Instantânea (Barge-in)", color = TechWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Corta a voz da IA e o processamento assim que você falar", color = TechTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = bargeIn,
                        onCheckedChange = { viewModel.preferences.setBargeInEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonElectricBlue, checkedTrackColor = NeonCobalt)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // VAD Silence Timeout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tempo de Silêncio para Conclusão de Fala (VAD)", color = TechTextSecondary, fontSize = 12.sp)
                    Text("${vadTimeout}ms", color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = vadTimeout.toFloat(),
                    onValueChange = { viewModel.preferences.setVadSilenceTimeoutMs(it.toLong()) },
                    valueRange = 400f..1500f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }
        }

        // Qwen Models Catalog
        Text(
            text = "Modelos Qwen para Execução Local",
            color = TechWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        localModels.forEach { model ->
            val isActive = activeLocalModel?.modelId == model.modelId
            val isDownloaded = model.isDownloaded

            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = when {
                    isActive -> NeonAqua
                    model.isRecommended -> NeonCyan
                    else -> CyberCardBorder
                }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = model.name,
                                    color = TechWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (model.isRecommended) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = NeonAqua.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, NeonAqua)
                                    ) {
                                        Text(
                                            text = "POCO X5 RECOMENDADO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeonAqua,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tamanho: ${(model.sizeBytes / (1024 * 1024))} MB • Quantização: ${model.quantization} • RAM Requerida: ~${model.ramRequiredMb} MB",
                                color = TechTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        if (isActive) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Ativo", tint = NeonAqua, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Model Benchmark stats if available
                    if (model.lastBenchmarkTokensPerSec > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberDarkSurface, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text("Velocidade: ${String.format("%.1f", model.lastBenchmarkTokensPerSec)} tok/s", color = NeonCyan, fontSize = 11.sp)
                            Text("1º Token: ${model.lastBenchmarkTtftMs}ms", color = NeonAqua, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isDownloaded) {
                            Button(
                                onClick = { viewModel.downloadLocalModel(model) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCobalt),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Baixar Modelo (${model.sizeBytes / (1024 * 1024)} MB)", fontSize = 11.sp)
                            }
                        } else {
                            if (!isActive) {
                                OutlinedButton(
                                    onClick = { viewModel.setActiveLocalModel(model.modelId) },
                                    border = BorderStroke(1.dp, NeonAqua),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Ativar", color = NeonAqua, fontSize = 12.sp)
                                }
                            }
                            Button(
                                onClick = { viewModel.runHardwareBenchmark(model) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, NeonCyan),
                                modifier = Modifier.weight(1f),
                                enabled = !isBenchmarking
                            ) {
                                if (isBenchmarking) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = NeonCyan, strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Benchmark", color = NeonCyan, fontSize = 11.sp)
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteLocalModel(model) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remover", tint = TechError)
                            }
                        }
                    }
                }
            }
        }

        // Live Benchmark Result Display
        benchmarkResult?.let { bench ->
            CyberCard(modifier = Modifier.fillMaxWidth(), borderColor = TechSuccess) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Resultado do Benchmark no Poco X5 5G", color = TechSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Modelo: ${bench.modelName} (${bench.quantization})\n" +
                                "• Velocidade Média: ${String.format("%.1f", bench.tokensPerSec)} tokens/segundo\n" +
                                "• Latência até o 1º Token (TTFT): ${bench.timeToFirstTokenMs} ms\n" +
                                "• Tempo de Carga: ${bench.loadTimeMs} ms\n" +
                                "• Uso de RAM em Pico: ${bench.peakRamUsageMb} MB\n" +
                                "• Threads de CPU Utilizadas: ${bench.cpuThreadsUsed}\n" +
                                "• Impacto na Bateria: ${bench.batteryImpactEstimate}\n" +
                                "• Aceleração de Hardware: ${if (bench.isHardwareAccelerated) "Vulkan Ativa" else "CPU Neon"}",
                        color = TechWhite,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// TAB 1: API KEYS (Preserved & Enhanced)
@Composable
fun ApiKeysTab(viewModel: MainViewModel) {
    val apiKeys by viewModel.apiKeys.collectAsState()
    val activeKey by viewModel.activeApiKey.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newKeyInput by remember { mutableStateOf("") }
    var newLabelInput by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf<ApiKeyEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Info card
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gerenciador de Chaves OpenRouter", color = TechWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Adicione suas chaves gratuitas ou pagas da OpenRouter (openrouter.ai). Você pode alternar entre elas a qualquer momento.",
                    color = TechTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Add Key Button
        Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCobalt),
            modifier = Modifier.fillMaxWidth().testTag("add_api_key_button")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Adicionar Nova Chave de API", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (apiKeys.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = TechTextMuted, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nenhuma chave salva", color = TechTextMuted, fontSize = 13.sp)
                    Text("Toque no botão acima para adicionar sua chave da OpenRouter", color = TechTextMuted, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apiKeys, key = { it.id }) { keyEntity ->
                    val isActive = keyEntity.isActive
                    CyberCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (isActive) NeonAqua else CyberCardBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = keyEntity.label,
                                        color = TechWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NeonAqua.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, NeonAqua)
                                        ) {
                                            Text(
                                                text = "ATIVA",
                                                color = NeonAqua,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val masked = if (keyEntity.key.length > 12) {
                                    keyEntity.key.take(8) + "..." + keyEntity.key.takeLast(4)
                                } else {
                                    "••••••••••••"
                                }
                                Text(
                                    text = masked,
                                    color = TechTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isActive) {
                                    OutlinedButton(
                                        onClick = { viewModel.setActiveApiKey(keyEntity.id) },
                                        border = BorderStroke(1.dp, NeonCyan),
                                        modifier = Modifier.padding(end = 4.dp).testTag("set_active_key_${keyEntity.id}")
                                    ) {
                                        Text("Ativar", color = NeonCyan, fontSize = 11.sp)
                                    }
                                }
                                IconButton(
                                    onClick = { showDeleteConfirmDialog = keyEntity },
                                    modifier = Modifier.testTag("delete_key_${keyEntity.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = TechError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CyberDarkSurface,
            title = { Text("Nova Chave OpenRouter", color = TechWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newLabelInput,
                        onValueChange = { newLabelInput = it },
                        label = { Text("Rótulo (ex: Chave Gratuita)", color = TechTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TechWhite,
                            unfocusedTextColor = TechWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("key_label_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newKeyInput,
                        onValueChange = { newKeyInput = it },
                        label = { Text("Chave da API (sk-or-v1-...)", color = TechTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TechWhite,
                            unfocusedTextColor = TechWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKeyInput.isNotBlank()) {
                            viewModel.addApiKey(newKeyInput, newLabelInput)
                            newKeyInput = ""
                            newLabelInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCobalt)
                ) {
                    Text("Salvar Chave", color = TechWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = TechTextSecondary)
                }
            }
        )
    }

    showDeleteConfirmDialog?.let { keyEntity ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            containerColor = CyberDarkSurface,
            title = { Text("Remover Chave", color = TechWhite) },
            text = { Text("Deseja realmente excluir a chave '${keyEntity.label}'?", color = TechTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteApiKey(keyEntity)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechError)
                ) {
                    Text("Excluir", color = TechWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancelar", color = TechTextSecondary)
                }
            }
        )
    }
}

// TAB 2: REMOTE MODELS (Preserved)
@Composable
fun ModelsTab(viewModel: MainViewModel) {
    val models by viewModel.aiModels.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val activeKey by viewModel.activeApiKey.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()
    val testResult by viewModel.testConnectionResult.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newModelId by remember { mutableStateOf("") }
    var newModelName by remember { mutableStateOf("") }
    var newModelProvider by remember { mutableStateOf("") }
    var newModelIsFree by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Modelos de IA da OpenRouter", color = TechWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Escolha qual modelo processará suas mensagens no modo Online. Você pode adicionar qualquer slug de modelo da OpenRouter.",
                    color = TechTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Connection test card
        activeKey?.let { keyEntity ->
            activeModel?.let { modelEntity ->
                CyberCard(modifier = Modifier.fillMaxWidth(), borderColor = NeonCobalt) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Testar Conexão:", color = TechTextSecondary, fontSize = 12.sp)
                            Button(
                                onClick = { viewModel.testConnection(keyEntity.key, modelEntity.modelId) },
                                enabled = !isTesting,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberDarkSurface),
                                border = BorderStroke(1.dp, NeonCyan)
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = NeonCyan, strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Testar Ping", color = NeonCyan, fontSize = 11.sp)
                                }
                            }
                        }
                        testResult?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = it, color = TechWhite, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCobalt),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Adicionar Modelo Customizado", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(models, key = { it.id }) { modelEntity ->
                val isActive = modelEntity.isActive
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isActive) NeonAqua else CyberCardBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = modelEntity.name, color = TechWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = NeonAqua.copy(alpha = 0.2f), border = BorderStroke(1.dp, NeonAqua)) {
                                        Text(text = "EM USO", color = NeonAqua, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(text = modelEntity.modelId, color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            if (modelEntity.description.isNotBlank()) {
                                Text(text = modelEntity.description, color = TechTextSecondary, fontSize = 10.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isActive) {
                                OutlinedButton(
                                    onClick = { viewModel.setActiveModel(modelEntity.id) },
                                    border = BorderStroke(1.dp, NeonCyan)
                                ) {
                                    Text("Usar", color = NeonCyan, fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteModel(modelEntity) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = TechError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CyberDarkSurface,
            title = { Text("Adicionar Modelo", color = TechWhite) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newModelName,
                        onValueChange = { newModelName = it },
                        label = { Text("Nome de exibição", color = TechTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = CyberCardBorder, focusedTextColor = TechWhite, unfocusedTextColor = TechWhite),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newModelId,
                        onValueChange = { newModelId = it },
                        label = { Text("ID do Modelo (ex: google/gemini-2.0-flash-exp:free)", color = TechTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = CyberCardBorder, focusedTextColor = TechWhite, unfocusedTextColor = TechWhite),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newModelId.isNotBlank()) {
                            viewModel.addModel(newModelId, newModelName, newModelProvider, newModelIsFree)
                            newModelId = ""
                            newModelName = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCobalt)
                ) {
                    Text("Salvar", color = TechWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar", color = TechTextSecondary) }
            }
        )
    }
}

// TAB 3: VOICE & PIPELINE (Preserved & Enhanced)
@Composable
fun VoiceSettingsTab(viewModel: MainViewModel) {
    val language by viewModel.preferences.language.collectAsState()
    val showTranscription by viewModel.preferences.showVoiceTranscription.collectAsState()
    val speechRate by viewModel.preferences.speechRate.collectAsState()
    val speechPitch by viewModel.preferences.speechPitch.collectAsState()
    val systemPrompt by viewModel.preferences.systemPrompt.collectAsState()

    var tempPrompt by remember { mutableStateOf(systemPrompt) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Language Selector (pt-BR default)
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "Idioma Principal (STT & TTS)", color = TechWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val isPt = language == "pt-BR"
                    val isEn = language == "en-US"

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPt) NeonCobalt.copy(alpha = 0.35f) else CyberDarkSurface,
                        border = BorderStroke(1.dp, if (isPt) NeonCyan else CyberCardBorder),
                        modifier = Modifier.weight(1f).clickable { viewModel.setLanguage("pt-BR") }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(text = "Português (Brasil)", color = if (isPt) NeonCyan else TechTextSecondary, fontWeight = if (isPt) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isEn) NeonCobalt.copy(alpha = 0.35f) else CyberDarkSurface,
                        border = BorderStroke(1.dp, if (isEn) NeonCyan else CyberCardBorder),
                        modifier = Modifier.weight(1f).clickable { viewModel.setLanguage("en-US") }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(text = "English (US)", color = if (isEn) NeonCyan else TechTextSecondary, fontWeight = if (isEn) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Show/Hide live transcript on screen
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Exibir Transcrição na Tela de Voz", color = TechWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Mostra sua fala e resposta da IA em tempo real", color = TechTextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = showTranscription,
                    onCheckedChange = { viewModel.toggleVoiceTranscription(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCobalt)
                )
            }
        }

        // Speech Rate & Pitch
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Ajustes de Voz (TTS)", color = TechWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Velocidade", color = TechTextSecondary, fontSize = 12.sp)
                    Text("${String.format("%.1f", speechRate)}x", color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(value = speechRate, onValueChange = { viewModel.preferences.setSpeechRate(it) }, valueRange = 0.5f..2.0f, colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan))

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tom (Pitch)", color = TechTextSecondary, fontSize = 12.sp)
                    Text("${String.format("%.1f", speechPitch)}x", color = NeonAqua, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(value = speechPitch, onValueChange = { viewModel.preferences.setSpeechPitch(it) }, valueRange = 0.5f..2.0f, colors = SliderDefaults.colors(thumbColor = NeonAqua, activeTrackColor = NeonAqua))

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val sample = if (language == "pt-BR") "Olá! Este é um teste da síntese de voz no seu dispositivo." else "Hello! This is a speech test on your device."
                        viewModel.speakText(sample)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, CyberCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Testar Voz", color = TechWhite, fontSize = 12.sp)
                }
            }
        }

        // System Prompt
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Instrução do Sistema (System Prompt)", color = TechWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = tempPrompt,
                    onValueChange = {
                        tempPrompt = it
                        viewModel.preferences.setSystemPrompt(it)
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = CyberCardBorder, focusedTextColor = TechWhite, unfocusedTextColor = TechWhite),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        }
    }
}

// TAB 4: DEVICE INFO & DATA (Preserved)
@Composable
fun DeviceInfoTab(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CyberCard(modifier = Modifier.fillMaxWidth(), borderColor = NeonCobalt) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Otimizado para Poco X5 5G", color = TechWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Compatível com Xiaomi MIUI / HyperOS\n" +
                            "• STT (Speech-to-Text): Reconhecimento nativo Android com VAD\n" +
                            "• TTS (Text-to-Speech): Síntese nativa com chunking incremental de frases\n" +
                            "• IA Local: Modelos Qwen (GGUF quantizados Q4/Q5)\n" +
                            "• IA Online: OpenRouter API com streaming SSE\n" +
                            "• Banco Local: Room com suporte a conversas e memórias persistentes",
                    color = TechTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Gerenciamento de Dados", color = TechWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.clearChatHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = TechError.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, TechError.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("clear_chat_history_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = TechError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Limpar Histórico de Mensagens", color = TechError, fontSize = 12.sp)
                }
            }
        }
    }
}

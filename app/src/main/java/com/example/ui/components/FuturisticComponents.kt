package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

enum class InteractionMode {
    CHAT,
    VOICE
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CyberCardBorder,
    glow: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .then(
                if (glow) Modifier.border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CyberCardSurface.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}

@Composable
fun ModeTogglePill(
    currentMode: InteractionMode,
    onModeChange: (InteractionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(32.dp)),
        color = CyberDarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isChat = currentMode == InteractionMode.CHAT
            // Chat Tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .then(
                        if (isChat) Modifier.background(Brush.horizontalGradient(listOf(NeonElectricBlue, NeonCobalt)))
                        else Modifier
                    )
                    .clickable { onModeChange(InteractionMode.CHAT) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("mode_toggle_chat"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Modo Texto",
                        tint = if (isChat) TechWhite else TechTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Chat",
                        color = if (isChat) TechWhite else TechTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isChat) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // Voice Tab
            val isVoice = currentMode == InteractionMode.VOICE
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .then(
                        if (isVoice) Modifier.background(Brush.horizontalGradient(listOf(NeonCyan, NeonAqua)))
                        else Modifier
                    )
                    .clickable { onModeChange(InteractionMode.VOICE) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("mode_toggle_voice"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Modo Voz",
                        tint = if (isVoice) CyberObsidian else TechTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Voz IA",
                        color = if (isVoice) CyberObsidian else TechTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isVoice) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AudioWavesVisualizer(
    isActive: Boolean,
    amplitude: Float = 0.5f,
    barCount: Int = 18,
    modifier: Modifier = Modifier,
    barColor: Color = NeonCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val barFraction = if (isActive) {
                val wave = kotlin.math.sin(phase + (i * 0.45f)).toFloat()
                val heightPercent = ((wave + 1f) / 2f) * (0.3f + amplitude * 0.7f)
                heightPercent.coerceIn(0.15f, 1f)
            } else {
                0.12f
            }

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height((36 * barFraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                barColor,
                                barColor.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun GlowingStatusPill(
    text: String,
    isActive: Boolean,
    activeColor: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isActive) activeColor.copy(alpha = 0.5f) else CyberCardBorder,
                RoundedCornerShape(12.dp)
            ),
        color = CyberDarkSurface.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) activeColor else TechTextMuted)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (isActive) TechWhite else TechTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

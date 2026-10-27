package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun VoiceRecordingOverlay(
    isListening: Boolean,
    soundLevel: Float,
    transcription: String,
    onStop: () -> Unit,
    onSimulateSampleVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isListening) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("voice_recording_overlay"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE SPEECH DICTATION",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "AES Encrypted Stream",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Mic Pulse + Audio Waveform Bars
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale + (soundLevel * 0.04f))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFEF4444).copy(alpha = 0.4f),
                                    Color(0xFFEF4444).copy(alpha = 0.05f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone Active",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sound Waveform Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(36.dp)
            ) {
                repeat(18) { index ->
                    val barHeight = remember(soundLevel, index) {
                        val base = 6.dp
                        val dynamic = (soundLevel * (2.5f + (index % 5))).dp
                        (base + dynamic).coerceIn(6.dp, 34.dp)
                    }
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        Color(0xFFEF4444)
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Transcription box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (transcription.isNotBlank()) transcription else "Listening to your voice... Speak clearly into microphone",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (transcription.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulateSampleVoice,
                    modifier = Modifier.weight(1f).testTag("simulate_voice_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Dictate Demo Text")
                }

                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("stop_recording_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Done Dictating")
                }
            }
        }
    }
}

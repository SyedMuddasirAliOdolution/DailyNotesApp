package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("How to use Daily Notes AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HelpItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "Gemini AI Summaries & Tags",
                    description = "Tap '✨ AI Enrich' inside the note editor to instantly generate executive summaries, automatic tags (#work, #ideas), and sentiment analysis."
                )

                HelpItem(
                    icon = Icons.Default.Mic,
                    title = "Live Speech-to-Text Dictation",
                    description = "Tap the microphone icon to speak continuously. Live audio decibel waveforms visualize your speech, and words appear in your note in real time."
                )

                HelpItem(
                    icon = Icons.Default.CloudSync,
                    title = "Google Sign-In & Cloud Sync",
                    description = "Sign in with your Google account to automatically synchronize and back up your encrypted notes across devices."
                )

                HelpItem(
                    icon = Icons.Default.Security,
                    title = "AES-256 GCM Zero-Knowledge Security",
                    description = "All note titles and body content are encrypted on-device before being saved into the local Room database or uploaded."
                )

                HelpItem(
                    icon = Icons.Default.Search,
                    title = "Ask AI Semantic Search",
                    description = "Tap the sparkle search icon on the top bar to ask natural questions like 'What tasks do I have for tomorrow?' across all your notes."
                )

                HelpItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode & Custom Themes",
                    description = "Switch seamlessly between Light, Dark, or System theme anytime from the top bar."
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Got It!")
            }
        }
    )
}

@Composable
private fun HelpItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

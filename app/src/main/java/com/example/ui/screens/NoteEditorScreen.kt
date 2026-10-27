package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Note
import com.example.ui.components.VoiceRecordingOverlay

val notePalette = listOf(
    "#6366F1", // Indigo
    "#0EA5E9", // Sky
    "#10B981", // Emerald
    "#F59E0B", // Amber
    "#EC4899", // Rose
    "#8B5CF6", // Purple
    "#64748B"  // Slate
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    initialNote: Note?,
    isListeningVoice: Boolean,
    voiceSoundLevel: Float,
    voiceTranscription: String,
    isEnrichingAi: Boolean,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSimulateSampleVoice: () -> Unit,
    onTriggerAiEnrichment: (String, String, (String, List<String>, String) -> Unit) -> Unit,
    onSave: (id: String?, title: String, content: String, isPinned: Boolean, colorHex: String, tags: List<String>, aiSummary: String?, audioDuration: Int) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var selectedColor by remember { mutableStateOf(initialNote?.colorHex ?: notePalette.first()) }
    var tags by remember { mutableStateOf(initialNote?.tags ?: emptyList()) }
    var aiSummary by remember { mutableStateOf(initialNote?.aiSummary) }
    var newTagInput by remember { mutableStateOf("") }
    var showTagInput by remember { mutableStateOf(false) }

    // When voice transcript arrives, automatically append to content
    LaunchedEffect(voiceTranscription) {
        if (voiceTranscription.isNotBlank()) {
            content = if (content.isBlank()) {
                voiceTranscription
            } else {
                "$content $voiceTranscription"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (initialNote != null) "Edit Note" else "New Daily Note",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier.testTag("editor_pin_button")
                    ) {
                        Icon(
                            if (isPinned) Icons.Default.PushPin else Icons.Default.OutlinedFlag,
                            contentDescription = "Pin Note",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            onSave(
                                initialNote?.id,
                                title,
                                content,
                                isPinned,
                                selectedColor,
                                tags,
                                aiSummary,
                                initialNote?.audioDurationSeconds ?: 0
                            )
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("editor_save_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    // Color Palette Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            notePalette.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColor = hex }
                                        .then(
                                            if (selectedColor == hex) {
                                                Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            } else Modifier
                                        )
                                )
                            }
                        }

                        // Voice Dictation FAB button
                        FloatingActionButton(
                            onClick = {
                                if (isListeningVoice) onStopVoice() else onStartVoice()
                            },
                            containerColor = if (isListeningVoice) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("mic_dictation_button")
                        ) {
                            Icon(
                                if (isListeningVoice) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Dictate",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Title Field
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            "Title...",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                // AI Action Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = ButtonDefaults.outlinedButtonBorder(true),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Gemini AI Assistant",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "Auto-generate summary & smart tags",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onTriggerAiEnrichment(title, content) { summary, generatedTags, _ ->
                                    aiSummary = summary
                                    tags = (tags + generatedTags).distinct()
                                }
                            },
                            enabled = !isEnrichingAi && (content.isNotBlank() || title.isNotBlank()),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("generate_ai_summary_button")
                        ) {
                            if (isEnrichingAi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Enrich", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // AI Summary Display (if generated)
                if (!aiSummary.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Summarize,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "AI Generated Summary",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                IconButton(
                                    onClick = { aiSummary = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                aiSummary!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                            )
                        }
                    }
                }

                // Tags Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        SuggestionChip(
                            onClick = { tags = tags - tag },
                            label = { Text("#$tag", fontSize = 12.sp) },
                            icon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove tag",
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    if (showTagInput) {
                        OutlinedTextField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it },
                            placeholder = { Text("tag", fontSize = 12.sp) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (newTagInput.isNotBlank()) {
                                            tags = (tags + newTagInput.trim().lowercase()).distinct()
                                            newTagInput = ""
                                            showTagInput = false
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier
                                .width(120.dp)
                                .height(50.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        IconButton(
                            onClick = { showTagInput = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Tag", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content Field
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            "Start typing or tap the microphone to dictate your note in real-time...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .testTag("note_content_input"),
                    minLines = 8
                )

                Spacer(modifier = Modifier.height(100.dp))
            }

            // Live Voice Recording Overlay
            VoiceRecordingOverlay(
                isListening = isListeningVoice,
                soundLevel = voiceSoundLevel,
                transcription = voiceTranscription,
                onStop = onStopVoice,
                onSimulateSampleVoice = onSimulateSampleVoice,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

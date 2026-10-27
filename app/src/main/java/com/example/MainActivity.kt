package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NotesViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val isListeningVoice by viewModel.voiceManager.isListening.collectAsStateWithLifecycle()
            val voiceSoundLevel by viewModel.voiceManager.soundLevel.collectAsStateWithLifecycle()
            val voiceTranscription by viewModel.voiceManager.transcription.collectAsStateWithLifecycle()

            var isCreatingNewNote by remember { mutableStateOf(false) }

            MyApplicationTheme(themeMode = state.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isCreatingNewNote || state.selectedNoteForEdit != null) {
                        NoteEditorScreen(
                            initialNote = state.selectedNoteForEdit,
                            isListeningVoice = isListeningVoice,
                            voiceSoundLevel = voiceSoundLevel,
                            voiceTranscription = voiceTranscription,
                            isEnrichingAi = state.isEnrichingAi,
                            onStartVoice = { viewModel.voiceManager.startListening() },
                            onStopVoice = { viewModel.voiceManager.stopListening() },
                            onSimulateSampleVoice = {
                                viewModel.voiceManager.appendSampleVoiceInput(
                                    "Sprint Planning: Synchronize encrypted AES-256 note storage with Google Cloud and test Gemini AI tag extraction."
                                )
                            },
                            onTriggerAiEnrichment = { title, content, onComplete ->
                                viewModel.requestAiEnrichment(title, content, onComplete)
                            },
                            onSave = { id, title, content, isPinned, colorHex, tags, aiSummary, audioDuration ->
                                viewModel.saveNote(
                                    id = id,
                                    title = title,
                                    content = content,
                                    isPinned = isPinned,
                                    colorHex = colorHex,
                                    tags = tags,
                                    aiSummary = aiSummary,
                                    audioDuration = audioDuration
                                )
                                isCreatingNewNote = false
                                viewModel.setEditingNote(null)
                                viewModel.voiceManager.clearTranscription()
                            },
                            onBack = {
                                isCreatingNewNote = false
                                viewModel.setEditingNote(null)
                                viewModel.voiceManager.stopListening()
                                viewModel.voiceManager.clearTranscription()
                            }
                        )
                    } else {
                        DashboardScreen(
                            viewModel = viewModel,
                            onOpenEditor = { note ->
                                viewModel.voiceManager.clearTranscription()
                                if (note == null) {
                                    isCreatingNewNote = true
                                    viewModel.setEditingNote(null)
                                } else {
                                    isCreatingNewNote = false
                                    viewModel.setEditingNote(note)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

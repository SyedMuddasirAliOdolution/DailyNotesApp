package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Note
import com.example.ui.components.AiSearchDialog
import com.example.ui.components.GoogleSignInModalSheet
import com.example.ui.components.HelpGuideDialog
import com.example.ui.components.ThemeSelectionDialog
import com.example.ui.theme.AppThemeMode
import com.example.ui.viewmodel.NoteFilterTab
import com.example.ui.viewmodel.NoteSortOrder
import com.example.ui.viewmodel.NotesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: NotesViewModel,
    onOpenEditor: (Note?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    // Show temporary snackbar messages with Undo support
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            if (msg.contains("deleted", ignoreCase = true) && uiState.lastDeletedNote != null) {
                val result = snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDeleteNote()
                }
            } else {
                snackbarHostState.showSnackbar(msg)
            }
            viewModel.clearStatusMessage()
        }
    }

    // Filter notes
    val baseList = if (uiState.selectedFilterTab == NoteFilterTab.ARCHIVED) {
        uiState.archivedNotes
    } else {
        uiState.notes
    }

    val filteredList = baseList.filter { note ->
        val matchesSearch = uiState.searchQuery.isBlank() ||
                note.title.contains(uiState.searchQuery, ignoreCase = true) ||
                note.content.contains(uiState.searchQuery, ignoreCase = true) ||
                (note.aiSummary?.contains(uiState.searchQuery, ignoreCase = true) == true) ||
                note.tags.any { it.contains(uiState.searchQuery, ignoreCase = true) }

        val matchesTag = uiState.selectedTag == null || note.tags.contains(uiState.selectedTag)

        val matchesTab = when (uiState.selectedFilterTab) {
            NoteFilterTab.ALL -> true
            NoteFilterTab.PINNED -> note.isPinned
            NoteFilterTab.VOICE -> note.audioDurationSeconds > 0
            NoteFilterTab.AI_SUMMARIZED -> !note.aiSummary.isNullOrBlank()
            NoteFilterTab.ARCHIVED -> true
        }

        matchesSearch && matchesTag && matchesTab
    }.let { list ->
        when (uiState.sortOrder) {
            NoteSortOrder.NEWEST_FIRST -> list.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.createdAt })
            NoteSortOrder.OLDEST_FIRST -> list.sortedWith(compareByDescending<Note> { it.isPinned }.thenBy { it.createdAt })
            NoteSortOrder.TITLE_AZ -> list.sortedWith(compareByDescending<Note> { it.isPinned }.thenBy { it.title.lowercase() })
        }
    }

    // All available tags
    val allTags = remember(uiState.notes) {
        uiState.notes.flatMap { it.tags }.distinct().sorted()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Daily Notes AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "AES-256 Vault",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Google Cloud Sync Status / Button
                    IconButton(
                        onClick = { viewModel.toggleAuthSheet(true) }
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.userSession.isSyncingNow) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                        Text("...", style = MaterialTheme.typography.labelSmall)
                                    }
                                } else if (uiState.userSession.isSignedInWithGoogle) {
                                    Badge(containerColor = Color(0xFF10B981))
                                }
                            }
                        ) {
                            Icon(
                                if (uiState.userSession.isSignedInWithGoogle) Icons.Default.CloudDone else Icons.Outlined.CloudQueue,
                                contentDescription = "Google Account & Sync",
                                tint = if (uiState.userSession.isSignedInWithGoogle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Ask AI Semantic Search
                    IconButton(onClick = { viewModel.toggleAiSearchSheet(true) }) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Ask Gemini AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Theme selector (Light / Dark / System)
                    IconButton(onClick = { viewModel.toggleThemeDialog(true) }) {
                        Icon(
                            when (uiState.themeMode) {
                                AppThemeMode.DARK -> Icons.Default.DarkMode
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            },
                            contentDescription = "Switch Theme"
                        )
                    }

                    // Help Guide
                    IconButton(onClick = { viewModel.toggleHelpGuide(true) }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help Guide")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenEditor(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = "New Note") },
                text = { Text("New Note", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Google Cloud Sync Banner (if not synced or syncing)
            if (uiState.userSession.isSignedInWithGoogle && uiState.userSession.isSyncingNow) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            // Search Bar & Sort Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search encrypted notes, AI tags...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Dropdown
                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Notes")
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest First") },
                            trailingIcon = { if (uiState.sortOrder == NoteSortOrder.NEWEST_FIRST) Icon(Icons.Default.Check, null) },
                            onClick = {
                                viewModel.setSortOrder(NoteSortOrder.NEWEST_FIRST)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest First") },
                            trailingIcon = { if (uiState.sortOrder == NoteSortOrder.OLDEST_FIRST) Icon(Icons.Default.Check, null) },
                            onClick = {
                                viewModel.setSortOrder(NoteSortOrder.OLDEST_FIRST)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Title (A-Z)") },
                            trailingIcon = { if (uiState.sortOrder == NoteSortOrder.TITLE_AZ) Icon(Icons.Default.Check, null) },
                            onClick = {
                                viewModel.setSortOrder(NoteSortOrder.TITLE_AZ)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Filter Tabs (All, Pinned, Voice, AI Summaries, Archived)
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedFilterTab.ordinal,
                edgePadding = 16.dp,
                divider = {},
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                NoteFilterTab.entries.forEach { tab ->
                    val isSelected = uiState.selectedFilterTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setFilterTab(tab) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = when (tab) {
                                    NoteFilterTab.ALL -> Icons.AutoMirrored.Filled.FormatListBulleted
                                    NoteFilterTab.PINNED -> Icons.Default.PushPin
                                    NoteFilterTab.VOICE -> Icons.Default.Mic
                                    NoteFilterTab.AI_SUMMARIZED -> Icons.Default.AutoAwesome
                                    NoteFilterTab.ARCHIVED -> Icons.Default.Archive
                                }
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    when (tab) {
                                        NoteFilterTab.ALL -> "All (${uiState.notes.size})"
                                        NoteFilterTab.PINNED -> "Pinned (${uiState.notes.count { it.isPinned }})"
                                        NoteFilterTab.VOICE -> "Voice (${uiState.notes.count { it.audioDurationSeconds > 0 }})"
                                        NoteFilterTab.AI_SUMMARIZED -> "AI Summaries"
                                        NoteFilterTab.ARCHIVED -> "Archive (${uiState.archivedNotes.size})"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            // Tag Filter Carousel (if available)
            if (allTags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTags) { tag ->
                        val isSelected = uiState.selectedTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectTag(tag) },
                            label = { Text("#$tag") },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Notes List or Empty State
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    when (uiState.selectedFilterTab) {
                                        NoteFilterTab.PINNED -> Icons.Default.PushPin
                                        NoteFilterTab.VOICE -> Icons.Default.Mic
                                        NoteFilterTab.ARCHIVED -> Icons.Default.Archive
                                        else -> Icons.AutoMirrored.Filled.Notes
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = when {
                                uiState.searchQuery.isNotBlank() -> "No notes match '${uiState.searchQuery}'"
                                uiState.selectedFilterTab == NoteFilterTab.PINNED -> "No pinned notes yet"
                                uiState.selectedFilterTab == NoteFilterTab.VOICE -> "No voice notes recorded yet"
                                uiState.selectedFilterTab == NoteFilterTab.ARCHIVED -> "Archive is empty"
                                else -> "No notes found in your vault"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Tap the button below to create your encrypted note with AI summary & voice dictation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onOpenEditor(null) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Note")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            onClick = { onOpenEditor(note) },
                            onTogglePin = { viewModel.togglePin(note) },
                            onToggleArchive = { viewModel.toggleArchive(note) },
                            onDelete = { viewModel.deleteNote(note) },
                            onShare = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Note")
                                context.startActivity(shareIntent)
                            },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Note Content", "${note.title}\n\n${note.content}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Google Sign In Sheet
    if (uiState.showAuthSheet) {
        GoogleSignInModalSheet(
            userSession = uiState.userSession,
            noteCount = uiState.notes.size,
            onSignIn = { email, name -> viewModel.signInWithGoogle(email, name) },
            onSignOut = { viewModel.signOutGoogle() },
            onSyncNow = { viewModel.syncWithGoogle() },
            onDismiss = { viewModel.toggleAuthSheet(false) }
        )
    }

    // Ask AI Dialog
    if (uiState.showAiSearchSheet) {
        AiSearchDialog(
            isSearching = uiState.isAiSearching,
            aiResult = uiState.aiQueryResult,
            onQuery = { viewModel.askAiAboutNotes(it) },
            onDismiss = { viewModel.toggleAiSearchSheet(false) }
        )
    }

    // Theme Picker Dialog
    if (uiState.showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.themeMode,
            onSelectTheme = { viewModel.setThemeMode(it) },
            onDismiss = { viewModel.toggleThemeDialog(false) }
        )
    }

    // Help Guide Dialog
    if (uiState.showHelpGuide) {
        HelpGuideDialog(
            onDismiss = { viewModel.toggleHelpGuide(false) }
        )
    }
}

@Composable
fun NoteCardItem(
    note: Note,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit
) {
    var expandedSummary by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val cardAccentColor = remember(note.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(note.colorHex))
        } catch (e: Exception) {
            Color(0xFF4F46E5)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (note.isPinned) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(cardAccentColor)
        ) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Accent Pill, Title, Pin badge, Overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored Indicator
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(cardAccentColor)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = cardAccentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                if (note.audioDurationSeconds > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "${note.audioDurationSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Note Options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (note.isPinned) "Unpin Note" else "Pin Note to Top") },
                            leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onTogglePin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Text") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onCopy()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Note") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (note.isArchived) "Restore to Dashboard" else "Archive Note") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onToggleArchive()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Content Snippet
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // AI Summary section (if available)
            note.aiSummary?.let { summary ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedSummary = !expandedSummary },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Gemini AI Summary",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (expandedSummary) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (expandedSummary) 20 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Tags & Timestamp Footer
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tags
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    note.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Date
                val formattedDate = remember(note.createdAt) {
                    SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(note.createdAt))
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

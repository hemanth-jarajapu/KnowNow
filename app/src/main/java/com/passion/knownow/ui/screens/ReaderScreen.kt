package com.passion.knownow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import android.speech.tts.TextToSpeech
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikepenz.markdown.m3.Markdown
import com.passion.knownow.core.rendering.HtmlTableParser
import com.passion.knownow.core.rendering.Segment
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.ui.components.AdaptiveTable
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(documentId: Long, onBackClick: () -> Unit) {
    val vm: ReaderViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    LaunchedEffect(documentId) {
        vm.load(documentId)
        settingsVm.recordRead()
    }
    val doc by vm.document.collectAsState()
    val content by vm.content.collectAsState()
    val fontScale by settingsVm.fontScale.collectAsState()
    val sepia by settingsVm.sepia.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    val isBookmarkedState = remember { mutableStateOf(false) }
    LaunchedEffect(documentId) {
        ServiceLocator.database.bookmarkDao().isBookmarked(documentId).collect { isBookmarkedState.value = it }
    }

    // Save reading progress on scroll
    LaunchedEffect(listState, documentId) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.layoutInfo.totalItemsCount }
            .collectLatest { (first, total) ->
                if (total > 0) {
                    val progress = first.toFloat() / total.coerceAtLeast(1)
                    ServiceLocator.database.readingProgressDao().upsert(
                        com.passion.knownow.data.db.ReadingProgressEntity(documentId, progress)
                    )
                }
            }
    }

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var noteTags by remember { mutableStateOf("") }
    var showHighlightDialog by remember { mutableStateOf(false) }
    var highlightText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(doc?.title ?: "Reader") },
                actions = {
                    IconButton(onClick = {
                        if (isBookmarkedState.value) {
                            scope.launch { ServiceLocator.database.bookmarkDao().delete(documentId) }
                        } else {
                            scope.launch { ServiceLocator.database.bookmarkDao().upsert(com.passion.knownow.data.db.BookmarkEntity(documentId)) }
                        }
                    }) {
                        Icon(
                            if (isBookmarkedState.value) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark",
                        )
                    }
                    IconButton(onClick = { showNoteDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Add note")
                    }
                    IconButton(onClick = { showHighlightDialog = true }) {
                        Icon(Icons.Filled.Highlight, contentDescription = "Add highlight")
                    }
                    doc?.canonicalUrl?.let { url ->
                        IconButton(onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = "Open on ServiceNow docs")
                        }
                    }
                    IconButton(onClick = {
                        tts?.speak(content, TextToSpeech.QUEUE_FLUSH, null, "doc")
                    }) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Read aloud")
                    }
                },
            )
        },
    ) { padding ->
        val segments = HtmlTableParser.splitSegments(content)
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = fontScale)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            items(segments, key = { it.hashCode() }) { seg ->
                when (seg) {
                    is Segment.Text -> Markdown(content = seg.content)
                    is Segment.Table -> AdaptiveTable(seg.model)
                }
            }
            } // CompositionLocalProvider
        } // Scaffold content
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Add note") },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = noteTags,
                        onValueChange = { noteTags = it },
                        label = { Text("Tags (comma separated)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (noteText.isNotBlank()) {
                        scope.launch {
                            ServiceLocator.database.noteDao().upsert(
                                com.passion.knownow.data.db.NoteEntity(
                                    documentId = documentId,
                                    publicationId = doc?.publicationId ?: 0L,
                                    releaseName = doc?.let { null } ?: "",
                                    documentTitleSnapshot = doc?.title ?: "Untitled",
                                    publicationTitleSnapshot = "",
                                    canonicalUrl = doc?.canonicalUrl,
                                    content = noteText,
                                    tags = noteTags,
                                )
                            )
                        }
                    }
                    noteText = ""
                    showNoteDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showHighlightDialog) {
        AlertDialog(
            onDismissRequest = { showHighlightDialog = false },
            title = { Text("Add highlight") },
            text = {
                OutlinedTextField(
                    value = highlightText,
                    onValueChange = { highlightText = it },
                    label = { Text("Highlighted text") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (highlightText.isNotBlank()) {
                        scope.launch {
                            ServiceLocator.database.highlightDao().upsert(
                                com.passion.knownow.data.db.HighlightEntity(
                                    documentId = documentId,
                                    text = highlightText,
                                )
                            )
                        }
                    }
                    highlightText = ""
                    showHighlightDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showHighlightDialog = false }) { Text("Cancel") }
            },
        )
    }
}
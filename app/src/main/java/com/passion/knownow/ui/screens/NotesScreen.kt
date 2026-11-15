package com.passion.knownow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.passion.knownow.data.ServiceLocator
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(onBackClick: () -> Unit, onOpenDocument: (Long) -> Unit) {
    val db = ServiceLocator.database
    var query by remember { mutableStateOf("") }
    var tagFilter by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val notesFlow = remember(query, tagFilter) {
        if (query.isBlank()) db.noteDao().observeAll() else db.noteDao().search(query)
    }
    val rawNotes by notesFlow.collectAsState(initial = emptyList())
    val notes = if (tagFilter.isBlank()) rawNotes else rawNotes.filter { it.tags.contains(tagFilter, ignoreCase = true) }

    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Notes") },
            navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            actions = {
                IconButton(onClick = {
                    val md = notes.joinToString("\n\n") { "- ${it.content} (${it.documentTitleSnapshot})" }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, md)
                    }
                    context.startActivity(Intent.createChooser(intent, "Export notes"))
                }) { Icon(Icons.Filled.Share, contentDescription = "Export") }
            },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search notes") },
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.material3.OutlinedTextField(
                value = tagFilter,
                onValueChange = { tagFilter = it },
                label = { Text("Filter by tag") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp)) {
                items(notes, key = { it.id }) { note ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(note.content, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = note.documentTitleSnapshot.ifBlank { "Document #${note.documentId}" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (note.isOrphaned) {
                                    Text(
                                        "Detached (source removed)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            TextButton(onClick = {
                                scope.launch { ServiceLocator.database.noteDao().deleteById(note.id) }
                            }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}


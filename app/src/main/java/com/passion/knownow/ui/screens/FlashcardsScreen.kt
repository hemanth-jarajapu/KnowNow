package com.passion.knownow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.passion.knownow.data.ServiceLocator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(onBackClick: () -> Unit) {
    val highlights by ServiceLocator.database.highlightDao().observeAll().collectAsState(initial = emptyList())
    Scaffold(topBar = { TopAppBar(title = { Text("Flashcards") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(highlights, key = { it.id }) { h ->
                var revealed by remember { mutableStateOf(false) }
                Column(Modifier.padding(vertical = 8.dp).clickable { revealed = !revealed }) {
                    Text(h.text, style = MaterialTheme.typography.titleMedium)
                    if (revealed) {
                        Text(h.note ?: "(no note)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Tap to reveal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}